package uk.ac.staffs.leavebooking.leave.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.application.exceptions.ConcurrentLeaveModificationException;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestStreamEventJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestEventStreamRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "rabbitmq.enabled=false",
        "firebase.enabled=false"
})
@DisplayName("Rebuilding leave requests from saved events")
class LeaveRequestEventStoreIntegrationTests {
    private static final String REQUEST_ID = "event-sourced-request";

    @Autowired private LeaveRequestEventStore eventStore;
    @Autowired private LeaveRequestEventStreamRepository repository;

    @BeforeEach
    void clearStream() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("A new half-day request starts an ordered stream at sequence one")
    void newHalfDayRequestStartsOrderedStream() {
        LeaveRequest request = annualHalfDay();

        eventStore.append(request);

        List<LeaveRequestStreamEventJpa> stored =
                repository.findByAggregateIdOrderBySequenceNumber(REQUEST_ID);
        assertEquals(1, stored.size());
        assertEquals(1L, stored.getFirst().getSequenceNumber());
        assertEquals("LeaveRequestSubmittedEvent", stored.getFirst().getEventType());

        LeaveRequest replayed = eventStore.load(REQUEST_ID);
        assertEquals(LeaveDayPortion.MORNING, replayed.dayPortion());
        assertEquals(LeaveDays.of("0.5"), replayed.chargedLeaveDays());
        assertEquals(LeaveStatus.PENDING, replayed.status());
        assertEquals(1L, replayed.streamVersion());
        assertFalse(replayed.domainEventsExist());
    }

    @Test
    @DisplayName("HR referral and approval append ordered events and replay the decision comment")
    void hrDecisionReplaysFromOrderedEvents() {
        LeaveRequest request = annualFullDay();
        request = appendAndReload(request);

        request.referForHrApproval();
        request = appendAndReload(request);

        request.approveByHrWithComment("  HR policy confirmed  ");
        eventStore.append(request);

        List<LeaveRequestStreamEventJpa> stored =
                repository.findByAggregateIdOrderBySequenceNumber(REQUEST_ID);
        assertEquals(List.of(1L, 2L, 3L), stored.stream()
                .map(LeaveRequestStreamEventJpa::getSequenceNumber).toList());
        assertEquals(List.of(
                "LeaveRequestSubmittedEvent",
                "LeaveRequestReferredForHrApprovalEvent",
                "LeaveRequestApprovedEvent"
        ), stored.stream().map(LeaveRequestStreamEventJpa::getEventType).toList());

        LeaveRequest replayed = eventStore.load(REQUEST_ID);
        assertEquals(LeaveStatus.APPROVED, replayed.status());
        assertEquals("HR policy confirmed", replayed.decisionComment());
        assertEquals(3L, replayed.streamVersion());
        assertFalse(replayed.domainEventsExist());
    }

    @Test
    @DisplayName("Sickness and its cancellation reconstruct without an annual charge")
    void sicknessCancellationReplays() {
        LeaveRequest request = sickRequest();
        request = appendAndReload(request);
        request.cancel();
        eventStore.append(request);

        LeaveRequest replayed = eventStore.load(REQUEST_ID);

        assertEquals(LeaveType.SICK, replayed.leaveType());
        assertEquals(LeaveStatus.CANCELLED, replayed.status());
        assertEquals(LeaveDays.zero(), replayed.chargedLeaveDays());
        assertEquals("Private sickness detail", replayed.reason());
        assertEquals(2L, replayed.streamVersion());
    }

    @Test
    @DisplayName("The query projection can be regenerated from replayed authoritative state")
    void projectionMatchesReplayedState() {
        LeaveRequest request = annualHalfDay();
        request = appendAndReload(request);
        request.rejectWithComment("Insufficient cover");
        eventStore.append(request);

        LeaveRequest replayed = eventStore.load(REQUEST_ID);
        LeaveRequestJpa projection = LeaveRequestDomainToJpaMapper.map(replayed, 4L);

        assertEquals(replayed.id().id(), projection.getId());
        assertEquals(replayed.status(), projection.getStatus());
        assertEquals(replayed.dayPortion(), projection.getDayPortion());
        assertEquals(replayed.chargedLeaveDays().value(), projection.getChargedLeaveDays());
        assertEquals(replayed.decisionComment(), projection.getDecisionComment());
        assertEquals(4L, projection.getVersion());
    }

    @Test
    @DisplayName("A stale aggregate cannot append the same next stream sequence")
    void staleWriterCannotAppendSameSequence() {
        LeaveRequest initial = annualFullDay();
        eventStore.append(initial);
        LeaveRequest firstWriter = eventStore.load(REQUEST_ID);
        LeaveRequest staleWriter = eventStore.load(REQUEST_ID);

        firstWriter.approve();
        eventStore.append(firstWriter);
        staleWriter.reject();

        assertThrows(
                ConcurrentLeaveModificationException.class,
                () -> eventStore.append(staleWriter)
        );
        assertEquals(2L, repository.countByAggregateId(REQUEST_ID));
        assertEquals(LeaveStatus.APPROVED, eventStore.load(REQUEST_ID).status());
    }

    private LeaveRequest appendAndReload(LeaveRequest request) {
        eventStore.append(request);
        return eventStore.load(request.id().id());
    }

    private LeaveRequest annualHalfDay() {
        return LeaveRequest.create(
                Identity.of(REQUEST_ID), "staff-1", "manager-1",
                new LeavePeriod(date("2026-08-24"), date("2026-08-24")),
                "Appointment", LeaveType.ANNUAL,
                LeaveDayPortion.MORNING, LeaveDays.of("0.5")
        );
    }

    private LeaveRequest annualFullDay() {
        return LeaveRequest.create(
                Identity.of(REQUEST_ID), "staff-1", "manager-1",
                new LeavePeriod(date("2026-08-24"), date("2026-08-28")),
                "Annual leave", LeaveType.ANNUAL,
                LeaveDayPortion.FULL_DAY, LeaveDays.of("5.0")
        );
    }

    private LeaveRequest sickRequest() {
        return LeaveRequest.create(
                Identity.of(REQUEST_ID), "staff-1", "manager-1",
                new LeavePeriod(date("2026-08-24"), date("2026-08-26")),
                "Private sickness detail", LeaveType.SICK,
                LeaveDayPortion.FULL_DAY, LeaveDays.zero()
        );
    }

    private LocalDate date(String value) {
        return LocalDate.parse(value);
    }
}
