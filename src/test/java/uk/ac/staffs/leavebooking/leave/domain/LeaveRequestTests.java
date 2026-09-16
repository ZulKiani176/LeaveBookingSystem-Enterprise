package uk.ac.staffs.leavebooking.leave.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.ac.staffs.leavebooking.common.AggregateRoot;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Leave request rules")
class LeaveRequestTests {
    private static final Identity<LeaveRequest> REQUEST_ID = Identity.of("leave-request-id");
    private static final String STAFF_MEMBER_ID = "staff-member-id";
    private static final String MANAGER_ID = "manager-id";
    private static final LeavePeriod LEAVE_PERIOD = new LeavePeriod(
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 5)
    );
    private static final String REASON = "Family holiday";

    @Test
    @DisplayName("A new leave request retains its identity and supplied details")
    void newRequestRetainsSuppliedDetails() {
        LeaveRequest request = validRequest();

        assertAll(
                () -> assertTrue(request instanceof AggregateRoot),
                () -> assertEquals(REQUEST_ID, request.id()),
                () -> assertEquals(STAFF_MEMBER_ID, request.staffMemberId()),
                () -> assertEquals(MANAGER_ID, request.managerId()),
                () -> assertEquals(LEAVE_PERIOD, request.leavePeriod()),
                () -> assertEquals(REASON, request.reason()),
                () -> assertEquals(LeaveType.ANNUAL, request.leaveType())
        );
    }

    @Test
    @DisplayName("A leave request trims its staff identity, manager identity and reason")
    void surroundingWhitespaceIsTrimmed() {
        LeaveRequest request = LeaveRequest.create(
                REQUEST_ID,
                "  staff-member-id  ",
                "  manager-id  ",
                LEAVE_PERIOD,
                "  Family holiday  ",
                LeaveType.ANNUAL
        );

        assertAll(
                () -> assertEquals(STAFF_MEMBER_ID, request.staffMemberId()),
                () -> assertEquals(MANAGER_ID, request.managerId()),
                () -> assertEquals(REASON, request.reason())
        );
    }

    @Test
    @DisplayName("A new leave request starts with pending status")
    void newlyCreatedRequestHasPendingStatus() {
        LeaveRequest request = validRequest();

        assertEquals(LeaveStatus.PENDING, request.status());
    }

    @Test
    @DisplayName("A null staff member identity is rejected")
    void nullStaffMemberIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, null, MANAGER_ID, LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.STAFF_MEMBER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An empty staff member identity is rejected")
    void emptyStaffMemberIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, "", MANAGER_ID, LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.STAFF_MEMBER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A whitespace-only staff member identity is rejected")
    void whitespaceOnlyStaffMemberIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, "   ", MANAGER_ID, LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.STAFF_MEMBER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A null manager identity snapshot is rejected")
    void nullManagerIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, null, LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.MANAGER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An empty manager identity snapshot is rejected")
    void emptyManagerIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, "", LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.MANAGER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A whitespace-only manager identity snapshot is rejected")
    void whitespaceOnlyManagerIdIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, "   ", LEAVE_PERIOD, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.MANAGER_ID_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A null leave period is rejected")
    void nullLeavePeriodIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, null, REASON, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.LEAVE_PERIOD_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("A null reason is rejected")
    void nullReasonIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, LEAVE_PERIOD, null, LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.REASON_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("An empty reason is rejected")
    void emptyReasonIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, LEAVE_PERIOD, "", LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.REASON_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A whitespace-only reason is rejected")
    void whitespaceOnlyReasonIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, LEAVE_PERIOD, "   ", LeaveType.ANNUAL
                )
        );

        assertEquals(LeaveRequest.REASON_NOT_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("A null leave type is rejected")
    void nullLeaveTypeIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.create(
                        REQUEST_ID, STAFF_MEMBER_ID, MANAGER_ID, LEAVE_PERIOD, REASON, null
                )
        );

        assertEquals(LeaveRequest.LEAVE_TYPE_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("Approving a pending leave request changes its status to approved")
    void approvingPendingRequestChangesStatusToApproved() {
        LeaveRequest request = validRequest();

        request.approve();

        assertEquals(LeaveStatus.APPROVED, request.status());
    }

    @Test
    @DisplayName("An approved leave request cannot be approved again")
    void approvedRequestCannotBeApprovedAgain() {
        LeaveRequest request = validRequest();
        request.approve();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::approve);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED, exception.getMessage());
        assertEquals(LeaveStatus.APPROVED, request.status());
    }

    @Test
    @DisplayName("A rejected leave request cannot be approved")
    void rejectedRequestCannotBeApproved() {
        LeaveRequest request = validRequest();
        request.reject();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::approve);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED, exception.getMessage());
        assertEquals(LeaveStatus.REJECTED, request.status());
    }

    @Test
    @DisplayName("A cancelled leave request cannot be approved")
    void cancelledRequestCannotBeApproved() {
        LeaveRequest request = validRequest();
        request.cancel();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::approve);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED, exception.getMessage());
        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("Rejecting a pending leave request changes its status to rejected")
    void rejectingPendingRequestChangesStatusToRejected() {
        LeaveRequest request = validRequest();

        request.reject();

        assertEquals(LeaveStatus.REJECTED, request.status());
    }

    @Test
    @DisplayName("An approved leave request cannot be rejected")
    void approvedRequestCannotBeRejected() {
        LeaveRequest request = validRequest();
        request.approve();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::reject);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED, exception.getMessage());
        assertEquals(LeaveStatus.APPROVED, request.status());
    }

    @Test
    @DisplayName("A rejected leave request cannot be rejected again")
    void rejectedRequestCannotBeRejectedAgain() {
        LeaveRequest request = validRequest();
        request.reject();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::reject);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED, exception.getMessage());
        assertEquals(LeaveStatus.REJECTED, request.status());
    }

    @Test
    @DisplayName("A cancelled leave request cannot be rejected")
    void cancelledRequestCannotBeRejected() {
        LeaveRequest request = validRequest();
        request.cancel();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::reject);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED, exception.getMessage());
        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("A pending leave request can be referred for HR approval")
    void pendingRequestCanBeReferredForHrApproval() {
        LeaveRequest request = validRequest();

        request.referForHrApproval();

        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, request.status());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = LeaveStatus.class,
            names = {"APPROVED", "REJECTED", "CANCELLED", "PENDING_HR_APPROVAL"}
    )
    @DisplayName("Only pending requests can be referred to HR")
    void nonPendingRequestCannotBeReferredForHrApproval(LeaveStatus status) {
        LeaveRequest request = requestWithStatus(status);

        Throwable exception = assertThrows(
                InvalidLeaveRequestStateException.class,
                request::referForHrApproval
        );

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REFERRED_FOR_HR_APPROVAL, exception.getMessage());
        assertEquals(status, request.status());
    }

    @Test
    @DisplayName("HR can approve a request pending HR approval")
    void hrCanApproveRequestPendingHrApproval() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);

        request.approveByHr();

        assertEquals(LeaveStatus.APPROVED, request.status());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = LeaveStatus.class,
            names = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"}
    )
    @DisplayName("HR cannot approve a request that is not waiting for HR review")
    void hrCannotApproveRequestNotPendingHrApproval(LeaveStatus status) {
        LeaveRequest request = requestWithStatus(status);

        Throwable exception = assertThrows(
                InvalidLeaveRequestStateException.class,
                request::approveByHr
        );

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED_BY_HR, exception.getMessage());
        assertEquals(status, request.status());
    }

    @Test
    @DisplayName("HR can reject a request pending HR approval")
    void hrCanRejectRequestPendingHrApproval() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);

        request.rejectByHr();

        assertEquals(LeaveStatus.REJECTED, request.status());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = LeaveStatus.class,
            names = {"PENDING", "APPROVED", "REJECTED", "CANCELLED"}
    )
    @DisplayName("HR cannot reject a request that is not waiting for HR review")
    void hrCannotRejectRequestNotPendingHrApproval(LeaveStatus status) {
        LeaveRequest request = requestWithStatus(status);

        Throwable exception = assertThrows(
                InvalidLeaveRequestStateException.class,
                request::rejectByHr
        );

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED_BY_HR, exception.getMessage());
        assertEquals(status, request.status());
    }

    @Test
    @DisplayName("A request pending HR approval cannot use ordinary approval")
    void hrPendingRequestCannotUseOrdinaryApproval() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::approve);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED, exception.getMessage());
        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, request.status());
    }

    @Test
    @DisplayName("A request pending HR approval cannot use ordinary rejection")
    void hrPendingRequestCannotUseOrdinaryRejection() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::reject);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED, exception.getMessage());
        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, request.status());
    }

    @Test
    @DisplayName("An approved leave request can be cancelled")
    void approvedRequestCanBeCancelled() {
        LeaveRequest request = validRequest();
        request.approve();

        request.cancel();

        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("A request pending HR approval can be cancelled")
    void hrPendingRequestCanBeCancelled() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);

        request.cancel();

        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("A cancelled HR-referred request cannot subsequently be approved by HR")
    void cancelledHrReferredRequestCannotBeApprovedByHr() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);
        request.cancel();

        Throwable exception = assertThrows(
                InvalidLeaveRequestStateException.class,
                request::approveByHr
        );

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_APPROVED_BY_HR, exception.getMessage());
        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("A cancelled HR-referred request cannot subsequently be rejected by HR")
    void cancelledHrReferredRequestCannotBeRejectedByHr() {
        LeaveRequest request = requestWithStatus(LeaveStatus.PENDING_HR_APPROVAL);
        request.cancel();

        Throwable exception = assertThrows(
                InvalidLeaveRequestStateException.class,
                request::rejectByHr
        );

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_REJECTED_BY_HR, exception.getMessage());
        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("Cancelling a leave request changes its status to cancelled")
    void cancellingRequestChangesStatusToCancelled() {
        LeaveRequest request = validRequest();

        request.cancel();

        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @Test
    @DisplayName("A rejected leave request cannot be cancelled")
    void rejectedRequestCannotBeCancelled() {
        LeaveRequest request = validRequest();
        request.reject();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::cancel);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_CANCELLED, exception.getMessage());
        assertEquals(LeaveStatus.REJECTED, request.status());
    }

    @Test
    @DisplayName("A cancelled leave request cannot be cancelled again")
    void cancelledRequestCannotBeCancelledAgain() {
        LeaveRequest request = validRequest();
        request.cancel();

        Throwable exception = assertThrows(InvalidLeaveRequestStateException.class, request::cancel);

        assertEquals(LeaveRequest.REQUEST_CANNOT_BE_CANCELLED, exception.getMessage());
        assertEquals(LeaveStatus.CANCELLED, request.status());
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(LeaveStatus.class)
    @DisplayName("A reconstituted leave request preserves its stored status")
    void reconstitutedRequestPreservesStoredStatus(LeaveStatus storedStatus) {
        LeaveRequest request = LeaveRequest.reconstitute(
                REQUEST_ID,
                STAFF_MEMBER_ID,
                MANAGER_ID,
                LEAVE_PERIOD,
                REASON,
                LeaveType.ANNUAL,
                storedStatus
        );

        assertEquals(storedStatus, request.status());
        assertEquals(MANAGER_ID, request.managerId());
    }

    @Test
    @DisplayName("A leave request cannot be reconstituted with a null status")
    void nullReconstitutedStatusIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.reconstitute(
                        REQUEST_ID,
                        STAFF_MEMBER_ID,
                        MANAGER_ID,
                        LEAVE_PERIOD,
                        REASON,
                        LeaveType.ANNUAL,
                        null
                )
        );

        assertEquals(LeaveRequest.STATUS_NOT_NULL, exception.getMessage());
    }

    @Test
    @DisplayName("Reconstitution enforces existing structural invariants")
    void reconstitutionEnforcesStructuralInvariants() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                LeaveRequest.reconstitute(
                        REQUEST_ID,
                        STAFF_MEMBER_ID,
                        MANAGER_ID,
                        LEAVE_PERIOD,
                        "   ",
                        LeaveType.ANNUAL,
                        LeaveStatus.APPROVED
                )
        );

        assertEquals(LeaveRequest.REASON_NOT_EMPTY, exception.getMessage());
    }

    private LeaveRequest validRequest() {
        LeaveRequest request = LeaveRequest.create(
                REQUEST_ID,
                STAFF_MEMBER_ID,
                MANAGER_ID,
                LEAVE_PERIOD,
                REASON,
                LeaveType.ANNUAL
        );
        request.clearDomainEvents();
        return request;
    }

    private LeaveRequest requestWithStatus(LeaveStatus status) {
        return LeaveRequest.reconstitute(
                REQUEST_ID,
                STAFF_MEMBER_ID,
                MANAGER_ID,
                LEAVE_PERIOD,
                REASON,
                LeaveType.ANNUAL,
                status
        );
    }
}
