package uk.ac.staffs.leavebooking.common.events.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;


import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Leave Integration Event")
class LeaveIntegrationEventTests {
    private static final LocalDate OCCURRED_ON = LocalDate.of(2026, 8, 25);
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 14);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 18);

    @Test
    @DisplayName("Submitted integration event retains its routing payload and creates an ID copy")
    void submittedEventRetainsPayloadAndCopiesWithId() {
        LeaveRequestSubmittedIntegrationEvent event =
                new LeaveRequestSubmittedIntegrationEvent(
                        OCCURRED_ON,
                        "request-1",
                        "staff-1",
                        "manager-1",
                        START_DATE,
                        END_DATE
                );

        LeaveRequestSubmittedIntegrationEvent identified = event.withId(11L);

        assertNull(event.id());
        assertEquals(11L, identified.id());
        assertEquals(OCCURRED_ON, identified.occurredOn());
        assertEquals("request-1", identified.leaveRequestId());
        assertEquals("staff-1", identified.staffMemberId());
        assertEquals("manager-1", identified.managerId());
        assertEquals(START_DATE, identified.startDate());
        assertEquals(END_DATE, identified.endDate());
    }

    @Test
    @DisplayName("Approved integration event retains its payload and creates an ID copy")
    void approvedEventRetainsPayloadAndCopiesWithId() {
        LeaveRequestApprovedIntegrationEvent event = new LeaveRequestApprovedIntegrationEvent(
                OCCURRED_ON, "request-1", "staff-1", START_DATE, END_DATE
        );

        LeaveRequestApprovedIntegrationEvent identified = event.withId(12L);

        assertNull(event.id());
        assertEquals(12L, identified.id());
        assertEquals("request-1", identified.leaveRequestId());
        assertEquals("staff-1", identified.staffMemberId());
        assertEquals(START_DATE, identified.startDate());
        assertEquals(END_DATE, identified.endDate());
    }

    @Test
    @DisplayName("Previous status distinguishes an HR approval without free-text payload")
    void approvedEventRetainsDecisionMetadata() {
        LeaveRequestApprovedIntegrationEvent event =
                new LeaveRequestApprovedIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", START_DATE, END_DATE,
                        IntegrationLeaveType.ANNUAL,
                        IntegrationLeaveDayPortion.FULL_DAY,
                        new BigDecimal("5.0"),
                        IntegrationLeaveStatus.PENDING_HR_APPROVAL
                );

        LeaveRequestApprovedIntegrationEvent identified = event.withId(15L);

        assertEquals(
                IntegrationLeaveStatus.PENDING_HR_APPROVAL,
                identified.previousStatus()
        );
        assertFalse(List.of(LeaveRequestApprovedIntegrationEvent.class.getDeclaredFields())
                .stream().anyMatch(field -> field.getName().equals("decisionComment")));
    }

    @Test
    @DisplayName("HR referral integration event carries only lifecycle identity and status")
    void hrReferralEventRetainsPrivacySafePayload() {
        LeaveRequestReferredForHrApprovalIntegrationEvent event =
                new LeaveRequestReferredForHrApprovalIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1"
                ).withId(16L);

        assertEquals(IntegrationLeaveStatus.PENDING, event.previousStatus());
        assertEquals(IntegrationLeaveStatus.PENDING_HR_APPROVAL, event.status());
        assertEquals("staff-1", event.staffMemberId());
    }

    @Test
    @DisplayName("Rejected integration event retains its payload and creates an ID copy")
    void rejectedEventRetainsPayloadAndCopiesWithId() {
        LeaveRequestRejectedIntegrationEvent event = new LeaveRequestRejectedIntegrationEvent(
                OCCURRED_ON, "request-1", "staff-1"
        );

        LeaveRequestRejectedIntegrationEvent identified = event.withId(13L);

        assertNull(event.id());
        assertEquals(13L, identified.id());
        assertEquals("request-1", identified.leaveRequestId());
        assertEquals("staff-1", identified.staffMemberId());
    }

    @Test
    @DisplayName("Cancelled integration event uses a Common-safe status snapshot")
    void cancelledEventRetainsStringStatusAndCopiesWithId() {
        LeaveRequestCancelledIntegrationEvent event = new LeaveRequestCancelledIntegrationEvent(
                OCCURRED_ON,
                "request-1",
                "staff-1",
                START_DATE,
                END_DATE,
                "APPROVED"
        );

        LeaveRequestCancelledIntegrationEvent identified = event.withId(14L);

        assertNull(event.id());
        assertEquals(14L, identified.id());
        assertEquals("request-1", identified.leaveRequestId());
        assertEquals("staff-1", identified.staffMemberId());
        assertEquals("APPROVED", identified.previousStatus());
    }

    @Test
    @DisplayName("Integration events reject missing identity payloads")
    void missingIdentityPayloadIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestSubmittedIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", " ", START_DATE, END_DATE
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestApprovedIntegrationEvent(
                        OCCURRED_ON, " ", "staff-1", START_DATE, END_DATE
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestRejectedIntegrationEvent(OCCURRED_ON, "request-1", " ")
        );
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestCancelledIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", START_DATE, END_DATE, " "
                )
        );
    }

    @Test
    @DisplayName("Integration events reject reversed leave periods")
    void reversedLeavePeriodIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestSubmittedIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", "manager-1", END_DATE, START_DATE
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestApprovedIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", END_DATE, START_DATE
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                new LeaveRequestCancelledIntegrationEvent(
                        OCCURRED_ON, "request-1", "staff-1", END_DATE, START_DATE, "APPROVED"
                )
        );
    }
}
