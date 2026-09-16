package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.Event;
import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.SickLeaveRecordedEvent;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Half-day leave and sickness")
class LeaveRequestTask17Tests {
    private static final Identity<LeaveRequest> ID = Identity.of("request-task-17");

    @Test
    @DisplayName("Sickness is recorded immediately without consuming annual allowance")
    void sicknessIsRecordedImmediately() {
        LeaveRequest request = sickRequest();

        assertEquals(LeaveStatus.RECORDED, request.status());
        assertEquals(LeaveDayPortion.FULL_DAY, request.dayPortion());
        assertEquals(LeaveDays.zero(), request.chargedLeaveDays());
        assertEquals(1, request.listOfDomainEvents().stream()
                .filter(SickLeaveRecordedEvent.class::isInstance).count());
        assertEquals(1, request.listOfDomainEvents().stream()
                .filter(SickLeaveRecordedIntegrationEvent.class::isInstance).count());
        assertFalse(request.listOfDomainEvents().stream()
                .anyMatch(LeaveRequestSubmittedEvent.class::isInstance));
    }

    @Test
    @DisplayName("The remote sickness event contains no reason field")
    void remoteSicknessEventExcludesReason() {
        List<String> componentNames = Arrays.stream(
                        SickLeaveRecordedIntegrationEvent.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .toList();

        assertFalse(componentNames.contains("reason"));
    }

    @Test
    @DisplayName("Recorded sickness cannot enter manager or HR approval workflows")
    void sicknessCannotEnterApprovalWorkflow() {
        LeaveRequest request = sickRequest();
        request.clearDomainEvents();

        assertThrows(InvalidLeaveRequestStateException.class, request::approve);
        assertThrows(InvalidLeaveRequestStateException.class, request::reject);
        assertThrows(InvalidLeaveRequestStateException.class, request::referForHrApproval);
        assertEquals(LeaveStatus.RECORDED, request.status());
        assertFalse(request.domainEventsExist());
    }

    @Test
    @DisplayName("Recorded sickness may be cancelled without an allowance charge")
    void sicknessCanBeCancelled() {
        LeaveRequest request = sickRequest();
        request.clearDomainEvents();

        request.cancel();

        assertEquals(LeaveStatus.CANCELLED, request.status());
        assertEquals(LeaveDays.zero(), request.chargedLeaveDays());
    }

    @Test
    @DisplayName("A valid annual half day retains its session and exact charge")
    void annualHalfDayRetainsExactCharge() {
        LeaveRequest request = annualRequest(
                LeaveDayPortion.MORNING,
                LeaveDays.of("0.5")
        );

        assertEquals(LeaveDayPortion.MORNING, request.dayPortion());
        assertEquals(LeaveDays.of("0.5"), request.chargedLeaveDays());
        assertEquals(LeaveStatus.PENDING, request.status());
    }

    @Test
    @DisplayName("A half-day request cannot span more than one date")
    void halfDayCannotSpanDates() {
        assertThrows(InvalidLeaveRequestException.class, () -> LeaveRequest.create(
                ID,
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-25")),
                "Annual leave",
                LeaveType.ANNUAL,
                LeaveDayPortion.AFTERNOON,
                LeaveDays.of("0.5")
        ));
    }

    @Test
    @DisplayName("Decision comments are optional, trimmed and replayed")
    void decisionCommentIsNormalisedAndReplayed() {
        LeaveRequest request = annualRequest(LeaveDayPortion.FULL_DAY, LeaveDays.of("1.0"));
        List<LocalEvent> history = localEvents(request.listOfDomainEvents());
        request.clearDomainEvents();

        request.approveWithComment("  Team coverage confirmed.  ");
        history.addAll(localEvents(request.listOfDomainEvents()));
        LeaveRequest replayed = LeaveRequest.replay(ID, history);

        assertEquals(LeaveStatus.APPROVED, replayed.status());
        assertEquals("Team coverage confirmed.", replayed.decisionComment());
        assertEquals(2L, replayed.streamVersion());
        assertFalse(replayed.domainEventsExist());
    }

    @Test
    @DisplayName("A blank decision comment is stored as null")
    void blankDecisionCommentBecomesNull() {
        LeaveRequest request = annualRequest(LeaveDayPortion.FULL_DAY, LeaveDays.of("1.0"));
        request.clearDomainEvents();

        request.rejectWithComment("   ");

        assertEquals(LeaveStatus.REJECTED, request.status());
        assertNull(request.decisionComment());
    }

    @Test
    @DisplayName("An oversized decision comment fails before state mutation or event emission")
    void oversizedCommentDoesNotMutateState() {
        LeaveRequest request = annualRequest(LeaveDayPortion.FULL_DAY, LeaveDays.of("1.0"));
        request.clearDomainEvents();

        assertThrows(
                InvalidLeaveRequestException.class,
                () -> request.approveWithComment("x".repeat(501))
        );

        assertEquals(LeaveStatus.PENDING, request.status());
        assertFalse(request.domainEventsExist());
    }

    @Test
    @DisplayName("Sickness state and private reason are rebuilt from its internal stream event")
    void sicknessReplaysFromInternalEvent() {
        LeaveRequest original = sickRequest();
        List<LocalEvent> history = localEvents(original.listOfDomainEvents());

        LeaveRequest replayed = LeaveRequest.replay(ID, history);

        assertEquals(LeaveType.SICK, replayed.leaveType());
        assertEquals("Migraine", replayed.reason());
        assertEquals(LeaveStatus.RECORDED, replayed.status());
        assertEquals(LeaveDays.zero(), replayed.chargedLeaveDays());
        assertFalse(replayed.domainEventsExist());
    }

    @Test
    @DisplayName("A replayed aggregate retains its state-machine guards")
    void replayedAggregateRetainsLifecycleGuards() {
        LeaveRequest request = annualRequest(LeaveDayPortion.FULL_DAY, LeaveDays.of("1.0"));
        List<LocalEvent> history = localEvents(request.listOfDomainEvents());
        request.clearDomainEvents();
        request.approve();
        assertInstanceOf(LeaveRequestApprovedEvent.class, localEvents(request.listOfDomainEvents()).getFirst());
        history.addAll(localEvents(request.listOfDomainEvents()));
        LeaveRequest replayed = LeaveRequest.replay(ID, history);

        assertThrows(InvalidLeaveRequestStateException.class, replayed::approve);
        assertEquals(LeaveStatus.APPROVED, replayed.status());
        assertFalse(replayed.domainEventsExist());
    }

    private LeaveRequest annualRequest(LeaveDayPortion portion, LeaveDays charge) {
        return LeaveRequest.create(
                ID,
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-24")),
                "Annual leave",
                LeaveType.ANNUAL,
                portion,
                charge
        );
    }

    private LeaveRequest sickRequest() {
        return LeaveRequest.create(
                ID,
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-26")),
                "Migraine",
                LeaveType.SICK,
                LeaveDayPortion.FULL_DAY,
                LeaveDays.zero()
        );
    }

    private List<LocalEvent> localEvents(List<Event> events) {
        return events.stream()
                .filter(LocalEvent.class::isInstance)
                .map(LocalEvent.class::cast)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
