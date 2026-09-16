package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.Event;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestReferredForHrApprovalIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestReferredForHrApprovalEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Events raised by leave requests")
class LeaveRequestDomainEventTests {
    private static final Identity<LeaveRequest> REQUEST_ID = Identity.of("request-1");
    private static final LeavePeriod LEAVE_PERIOD = new LeavePeriod(
            LocalDate.of(2026, 9, 14),
            LocalDate.of(2026, 9, 18)
    );

    @Test
    @DisplayName("Creating a request raises local and remote submission facts")
    void createRaisesSubmittedEventPair() {
        LeaveRequest request = createRequest();

        assertEquals(2, request.listOfDomainEvents().size());
        LeaveRequestSubmittedEvent event = onlyEvent(
                request,
                LeaveRequestSubmittedEvent.class
        );
        LeaveRequestSubmittedIntegrationEvent remoteEvent = onlyEvent(
                request,
                LeaveRequestSubmittedIntegrationEvent.class
        );
        assertNull(event.id());
        assertEquals(LocalDate.now(), event.occurredOn());
        assertEquals("request-1", event.leaveRequestId());
        assertEquals("staff-1", event.staffMemberId());
        assertEquals("manager-1", event.managerId());
        assertEquals(LEAVE_PERIOD.startDate(), event.startDate());
        assertEquals(LEAVE_PERIOD.endDate(), event.endDate());
        assertEquals(event.occurredOn(), remoteEvent.occurredOn());
        assertEquals(event.leaveRequestId(), remoteEvent.leaveRequestId());
        assertEquals(event.staffMemberId(), remoteEvent.staffMemberId());
        assertEquals(event.managerId(), remoteEvent.managerId());
        assertEquals(event.startDate(), remoteEvent.startDate());
        assertEquals(event.endDate(), remoteEvent.endDate());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(LeaveStatus.class)
    @DisplayName("Restoring a saved request does not raise new events")
    void reconstitutionRaisesNoEvent(LeaveStatus status) {
        LeaveRequest request = reconstitute(status);

        assertFalse(request.domainEventsExist());
        assertTrue(request.listOfDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Ordinary approval raises an approval event after successful mutation")
    void approveRaisesApprovedEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.PENDING);

        request.approve();

        LeaveRequestApprovedEvent event = onlyEvent(request, LeaveRequestApprovedEvent.class);
        LeaveRequestApprovedIntegrationEvent remoteEvent = onlyEvent(
                request,
                LeaveRequestApprovedIntegrationEvent.class
        );
        assertEquals("request-1", event.leaveRequestId());
        assertEquals("staff-1", event.staffMemberId());
        assertEquals(LEAVE_PERIOD.startDate(), event.startDate());
        assertEquals(LEAVE_PERIOD.endDate(), event.endDate());
        assertEquals(event.leaveRequestId(), remoteEvent.leaveRequestId());
        assertEquals(event.staffMemberId(), remoteEvent.staffMemberId());
    }

    @Test
    @DisplayName("Invalid approval does not add an event")
    void invalidApprovalAddsNoEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.APPROVED);

        assertThrows(InvalidLeaveRequestStateException.class, request::approve);

        assertFalse(request.domainEventsExist());
    }

    @Test
    @DisplayName("HR approval raises the same final approval event")
    void hrApprovalRaisesApprovedEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.PENDING_HR_APPROVAL);

        request.approveByHr();

        onlyEvent(request, LeaveRequestApprovedEvent.class);
        onlyEvent(request, LeaveRequestApprovedIntegrationEvent.class);
    }

    @Test
    @DisplayName("Ordinary rejection raises a rejection event")
    void rejectRaisesRejectedEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.PENDING);

        request.reject();

        onlyEvent(request, LeaveRequestRejectedEvent.class);
        onlyEvent(request, LeaveRequestRejectedIntegrationEvent.class);
    }

    @Test
    @DisplayName("HR rejection raises the same final rejection event")
    void hrRejectionRaisesRejectedEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.PENDING_HR_APPROVAL);

        request.rejectByHr();

        onlyEvent(request, LeaveRequestRejectedEvent.class);
        onlyEvent(request, LeaveRequestRejectedIntegrationEvent.class);
    }

    @Test
    @DisplayName("Referral to HR raises local reconstruction and remote audit facts")
    void hrReferralRaisesReferralEvent() {
        LeaveRequest request = reconstitute(LeaveStatus.PENDING);

        request.referForHrApproval();

        onlyEvent(request, LeaveRequestReferredForHrApprovalEvent.class);
        onlyEvent(request, LeaveRequestReferredForHrApprovalIntegrationEvent.class);
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = LeaveStatus.class,
            names = {"PENDING", "PENDING_HR_APPROVAL", "APPROVED"}
    )
    @DisplayName("A cancellation event records the request's previous status")
    void cancellationEventCapturesPreviousStatus(LeaveStatus previousStatus) {
        LeaveRequest request = reconstitute(previousStatus);

        request.cancel();

        LeaveRequestCancelledEvent event = onlyEvent(
                request,
                LeaveRequestCancelledEvent.class
        );
        LeaveRequestCancelledIntegrationEvent remoteEvent = onlyEvent(
                request,
                LeaveRequestCancelledIntegrationEvent.class
        );
        assertEquals(previousStatus, event.previousStatus());
        assertEquals(previousStatus.name(), remoteEvent.previousStatus());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(value = LeaveStatus.class, names = {"REJECTED", "CANCELLED"})
    @DisplayName("An invalid cancellation does not raise an event")
    void invalidCancellationAddsNoEvent(LeaveStatus status) {
        LeaveRequest request = reconstitute(status);

        assertThrows(InvalidLeaveRequestStateException.class, request::cancel);

        assertFalse(request.domainEventsExist());
    }

    @Test
    @DisplayName("Clearing aggregate events empties the event collection")
    void clearDomainEventsEmptiesCollection() {
        LeaveRequest request = createRequest();

        request.clearDomainEvents();

        assertFalse(request.domainEventsExist());
        assertTrue(request.listOfDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("The aggregate does not expose a mutable internal event list")
    void domainEventListIsImmutable() {
        LeaveRequest request = createRequest();
        List<Event> events = request.listOfDomainEvents();

        assertThrows(UnsupportedOperationException.class, events::clear);

        assertTrue(request.domainEventsExist());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @MethodSource("events")
    @DisplayName("Assigning an event identity preserves its other details")
    void withIdCreatesEquivalentIdBearingEvent(Event event) {
        Event persistedEvent = event.withId(42L);

        assertNull(event.id());
        assertEquals(42L, persistedEvent.id());
        assertEquals(event.getClass(), persistedEvent.getClass());
        assertEquals(event, persistedEvent.withId(null));
    }

    @Test
    @DisplayName("Every event record validates its required payload")
    void eventRecordsValidateRequiredPayload() {
        assertThrows(IllegalArgumentException.class, () -> new LeaveRequestSubmittedEvent(
                LocalDate.now(), "request-1", "staff-1", " ",
                LEAVE_PERIOD.startDate(), LEAVE_PERIOD.endDate()
        ));
        assertThrows(IllegalArgumentException.class, () -> new LeaveRequestApprovedEvent(
                LocalDate.now(), "request-1", "staff-1",
                LEAVE_PERIOD.endDate(), LEAVE_PERIOD.startDate()
        ));
        assertThrows(IllegalArgumentException.class, () -> new LeaveRequestRejectedEvent(
                LocalDate.now(), " ", "staff-1"
        ));
        assertThrows(IllegalArgumentException.class, () -> new LeaveRequestCancelledEvent(
                LocalDate.now(), "request-1", "staff-1",
                LEAVE_PERIOD.startDate(), LEAVE_PERIOD.endDate(), null
        ));
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestReferredForHrApprovalEvent(null, "request-1", "staff-1")
        );
    }

    private LeaveRequest createRequest() {
        return LeaveRequest.create(
                REQUEST_ID,
                "staff-1",
                "manager-1",
                LEAVE_PERIOD,
                "Annual leave",
                LeaveType.ANNUAL
        );
    }

    private LeaveRequest reconstitute(LeaveStatus status) {
        return LeaveRequest.reconstitute(
                REQUEST_ID,
                "staff-1",
                "manager-1",
                LEAVE_PERIOD,
                "Annual leave",
                LeaveType.ANNUAL,
                status
        );
    }

    private static <T extends Event> T onlyEvent(
            LeaveRequest request,
            Class<T> eventType
    ) {
        List<T> matchingEvents = request.listOfDomainEvents().stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .toList();
        assertEquals(1, matchingEvents.size());
        return matchingEvents.getFirst();
    }

    private static Stream<Event> events() {
        LocalDate occurredOn = LocalDate.of(2026, 8, 25);
        LocalDate startDate = LocalDate.of(2026, 9, 14);
        LocalDate endDate = LocalDate.of(2026, 9, 18);
        return Stream.of(
                new LeaveRequestSubmittedEvent(
                        occurredOn, "request-1", "staff-1", "manager-1", startDate, endDate
                ),
                new LeaveRequestApprovedEvent(
                        occurredOn, "request-1", "staff-1", startDate, endDate
                ),
                new LeaveRequestRejectedEvent(occurredOn, "request-1", "staff-1"),
                new LeaveRequestCancelledEvent(
                        occurredOn, "request-1", "staff-1", startDate, endDate,
                        LeaveStatus.APPROVED
                ),
                new LeaveRequestReferredForHrApprovalEvent(
                        occurredOn, "request-1", "staff-1"
                ),
                new LeaveRequestSubmittedIntegrationEvent(
                        occurredOn, "request-1", "staff-1", "manager-1", startDate, endDate
                ),
                new LeaveRequestApprovedIntegrationEvent(
                        occurredOn, "request-1", "staff-1", startDate, endDate
                ),
                new LeaveRequestRejectedIntegrationEvent(
                        occurredOn, "request-1", "staff-1"
                ),
                new LeaveRequestCancelledIntegrationEvent(
                        occurredOn, "request-1", "staff-1", startDate, endDate, "APPROVED"
                )
        );
    }
}
