package uk.ac.staffs.leavebooking.leave.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.common.events.DomainEventManager;
import uk.ac.staffs.leavebooking.common.events.Event;
import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestReferredForHrApprovalEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceCarryOverRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestJpaToDomainMapper;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Application Service")
class LeaveApplicationServiceTests {
    private static final LocalDate LEAVE_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate LEAVE_END = LocalDate.of(2026, 8, 14);
    private static final LocalDate BUSINESS_YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate BUSINESS_YEAR_END = LocalDate.of(2027, 3, 31);

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Mock
    private uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;

    @Mock
    private DomainEventManager domainEventManager;

    @Mock
    private PublicHolidayRepository publicHolidayRepository;

    @Mock
    private LeaveRequestEventStore leaveRequestEventStore;

    @Mock
    private LeaveAllowanceCarryOverRepository carryOverRepository;

    @Mock
    private uk.ac.staffs.leavebooking.leave.infrastructure.LeaveSubmissionLock submissionLock;

    @InjectMocks
    private LeaveApplicationService service;

    @BeforeEach
    void configureRequestCreationDefaults() {
        lenient().when(publicHolidayRepository.findByDateBetweenOrderByDate(any(), any()))
                .thenReturn(List.of());
        lenient().when(leaveRequestRepository
                        .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                any(), any(), any(), any()
                        ))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Submitting leave saves the pending request with its new identity and staff snapshot")
    void submissionSavesCompletePendingRequestAndDispatchesEvent() {
        RequestLeaveCommand command = validRequestCommand();
        whenStaffLookupReturns(staffMemberDto());

        String requestId = service.requestLeave(command);

        LeaveRequestJpa saved = captureSavedRequest();
        assertAll(
                () -> assertEquals(7, UUID.fromString(requestId).version()),
                () -> assertEquals(requestId, saved.getId()),
                () -> assertEquals(LeaveStatus.PENDING, saved.getStatus()),
                () -> assertEquals("staff-1", saved.getStaffMemberId()),
                () -> assertEquals("manager-1", saved.getManagerId()),
                () -> assertEquals(LEAVE_START, saved.getStartDate()),
                () -> assertEquals(LEAVE_END, saved.getEndDate()),
                () -> assertEquals("Summer holiday", saved.getReason()),
                () -> assertEquals(LeaveType.ANNUAL, saved.getLeaveType())
        );
        verify(staffContextFacade).findStaffMemberById("staff-1");
        captureDispatchedEvent(LeaveRequestSubmittedEvent.class);
    }

    @Test
    @DisplayName("Domain trimming is preserved when a request is saved")
    void domainTrimmingIsPreservedWhenRequestIsSaved() {
        RequestLeaveCommand command = new RequestLeaveCommand(
                "  staff-1  ",
                LEAVE_START,
                LEAVE_END,
                "  Summer holiday  ",
                LeaveType.ANNUAL
        );
        whenStaffLookupReturns(staffMemberDto());

        service.requestLeave(command);

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals("staff-1", saved.getStaffMemberId());
        assertEquals("Summer holiday", saved.getReason());
        verify(staffContextFacade).findStaffMemberById("staff-1");
    }

    @Test
    @DisplayName("A null request-leave command is rejected before repository access")
    void nullRequestLeaveCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.requestLeave(null)
        );

        assertEquals(LeaveApplicationService.REQUEST_LEAVE_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository, staffContextFacade);
    }

    @Test
    @DisplayName("An invalid leave period propagates domain validation and is not saved")
    void invalidLeavePeriodIsNotSaved() {
        RequestLeaveCommand command = new RequestLeaveCommand(
                "staff-1",
                LEAVE_END,
                LEAVE_START,
                "Summer holiday",
                LeaveType.ANNUAL
        );
        whenStaffLookupReturns(staffMemberDto());

        assertThrows(IllegalArgumentException.class, () -> service.requestLeave(command));

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A missing staff member prevents leave-request persistence")
    void missingStaffMemberPreventsRequestCreation() {
        when(staffContextFacade.findStaffMemberById("staff-1"))
                .thenThrow(new StaffMemberNotFoundException("staff-1"));

        assertThrows(StaffMemberNotFoundException.class, () ->
                service.requestLeave(validRequestCommand())
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A repository failure prevents submission-event dispatch")
    void repositoryFailurePreventsEventDispatch() {
        whenStaffLookupReturns(staffMemberDto());
        when(leaveRequestRepository.save(any(LeaveRequestJpa.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () ->
                service.requestLeave(validRequestCommand())
        );

        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null approval command is rejected before repository access")
    void nullApprovalCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.approveLeaveRequest(null)
        );

        assertEquals(LeaveApplicationService.APPROVE_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An existing pending request is approved and saved")
    void existingPendingRequestIsApprovedAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        service.approveLeaveRequest(new ApproveLeaveRequestCommand("request-1"));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(LeaveStatus.APPROVED, saved.getStatus());
        captureDispatchedEvent(LeaveRequestApprovedEvent.class);
    }

    @Test
    @DisplayName("Approving a missing request raises an application exception")
    void approvingMissingRequestFails() {
        whenRequestLookupIsMissing("missing-request");

        Throwable exception = assertThrows(LeaveRequestNotFoundException.class, () ->
                service.approveLeaveRequest(new ApproveLeaveRequestCommand("missing-request"))
        );

        assertEquals("Leave request not found: missing-request", exception.getMessage());
        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("An invalid approval propagates the domain exception and is not saved")
    void invalidApprovalIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.APPROVED);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.approveLeaveRequest(new ApproveLeaveRequestCommand("request-1"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null rejection command is rejected before repository access")
    void nullRejectionCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.rejectLeaveRequest(null)
        );

        assertEquals(LeaveApplicationService.REJECT_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An existing pending request is rejected and saved")
    void existingPendingRequestIsRejectedAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        service.rejectLeaveRequest(new RejectLeaveRequestCommand("request-1"));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(LeaveStatus.REJECTED, saved.getStatus());
        captureDispatchedEvent(LeaveRequestRejectedEvent.class);
    }

    @Test
    @DisplayName("Rejecting a missing request raises an application exception")
    void rejectingMissingRequestFails() {
        whenRequestLookupIsMissing("missing-request");

        assertThrows(LeaveRequestNotFoundException.class, () ->
                service.rejectLeaveRequest(new RejectLeaveRequestCommand("missing-request"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("An invalid rejection propagates the domain exception and is not saved")
    void invalidRejectionIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.APPROVED);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.rejectLeaveRequest(new RejectLeaveRequestCommand("request-1"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null cancellation command is rejected before repository access")
    void nullCancellationCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.cancelLeaveRequest(null)
        );

        assertEquals(LeaveApplicationService.CANCEL_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An existing pending request is cancelled and saved")
    void existingPendingRequestIsCancelledAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        service.cancelLeaveRequest(new CancelLeaveRequestCommand("request-1"));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(LeaveStatus.CANCELLED, saved.getStatus());
        captureDispatchedEvent(LeaveRequestCancelledEvent.class);
    }

    @Test
    @DisplayName("An existing approved request is cancelled and saved")
    void existingApprovedRequestIsCancelledAndSaved() {
        whenRequestLookupReturns(LeaveStatus.APPROVED);

        service.cancelLeaveRequest(new CancelLeaveRequestCommand("request-1"));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(LeaveStatus.CANCELLED, saved.getStatus());
        captureDispatchedEvent(LeaveRequestCancelledEvent.class);
    }

    @Test
    @DisplayName("Cancelling a missing request raises an application exception")
    void cancellingMissingRequestFails() {
        whenRequestLookupIsMissing("missing-request");

        assertThrows(LeaveRequestNotFoundException.class, () ->
                service.cancelLeaveRequest(new CancelLeaveRequestCommand("missing-request"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("An invalid cancellation propagates the domain exception and is not saved")
    void invalidCancellationIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.REJECTED);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.cancelLeaveRequest(new CancelLeaveRequestCommand("request-1"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null HR-referral command is rejected before repository access")
    void nullHrReferralCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.referLeaveRequestForHrApproval(null)
        );

        assertEquals(LeaveApplicationService.REFER_FOR_HR_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An existing pending request is referred for HR approval and saved")
    void pendingRequestIsReferredForHrApprovalAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        service.referLeaveRequestForHrApproval(
                new ReferLeaveRequestForHrApprovalCommand("request-1")
        );

        assertEquals(LeaveStatus.PENDING_HR_APPROVAL, captureSavedRequest().getStatus());
        captureDispatchedEvent(LeaveRequestReferredForHrApprovalEvent.class);
    }

    @Test
    @DisplayName("Referring a missing request for HR approval raises an application exception")
    void referringMissingRequestForHrApprovalFails() {
        whenRequestLookupIsMissing("missing-request");

        assertThrows(LeaveRequestNotFoundException.class, () ->
                service.referLeaveRequestForHrApproval(
                        new ReferLeaveRequestForHrApprovalCommand("missing-request")
                )
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("An invalid HR referral propagates the domain exception and is not saved")
    void invalidHrReferralIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.APPROVED);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.referLeaveRequestForHrApproval(
                        new ReferLeaveRequestForHrApprovalCommand("request-1")
                )
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null HR-approval command is rejected before repository access")
    void nullHrApprovalCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.approveLeaveRequestByHr(null)
        );

        assertEquals(LeaveApplicationService.HR_APPROVE_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("HR approves an HR-pending request and saves the final approved status")
    void hrPendingRequestIsApprovedByHrAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING_HR_APPROVAL);

        service.approveLeaveRequestByHr(new ApproveHrLeaveRequestCommand("request-1"));

        assertEquals(LeaveStatus.APPROVED, captureSavedRequest().getStatus());
        captureDispatchedEvent(LeaveRequestApprovedEvent.class);
    }

    @Test
    @DisplayName("HR approval of a missing request raises an application exception")
    void hrApprovalOfMissingRequestFails() {
        whenRequestLookupIsMissing("missing-request");

        assertThrows(LeaveRequestNotFoundException.class, () ->
                service.approveLeaveRequestByHr(
                        new ApproveHrLeaveRequestCommand("missing-request")
                )
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("Invalid HR approval propagates the domain exception and is not saved")
    void invalidHrApprovalIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.approveLeaveRequestByHr(new ApproveHrLeaveRequestCommand("request-1"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null HR-rejection command is rejected before repository access")
    void nullHrRejectionCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.rejectLeaveRequestByHr(null)
        );

        assertEquals(LeaveApplicationService.HR_REJECT_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("HR rejects an HR-pending request and saves the final rejected status")
    void hrPendingRequestIsRejectedByHrAndSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING_HR_APPROVAL);

        service.rejectLeaveRequestByHr(new RejectHrLeaveRequestCommand("request-1"));

        assertEquals(LeaveStatus.REJECTED, captureSavedRequest().getStatus());
        captureDispatchedEvent(LeaveRequestRejectedEvent.class);
    }

    @Test
    @DisplayName("HR rejection of a missing request raises an application exception")
    void hrRejectionOfMissingRequestFails() {
        whenRequestLookupIsMissing("missing-request");

        assertThrows(LeaveRequestNotFoundException.class, () ->
                service.rejectLeaveRequestByHr(
                        new RejectHrLeaveRequestCommand("missing-request")
                )
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("Invalid HR rejection propagates the domain exception and is not saved")
    void invalidHrRejectionIsNotSaved() {
        whenRequestLookupReturns(LeaveStatus.PENDING);

        assertThrows(InvalidLeaveRequestStateException.class, () ->
                service.rejectLeaveRequestByHr(new RejectHrLeaveRequestCommand("request-1"))
        );

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(domainEventManager);
    }

    @Test
    @DisplayName("A null allowance-creation command is rejected before collaboration")
    void nullAllowanceCreationCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.createLeaveAllowance(null)
        );

        assertEquals(LeaveApplicationService.CREATE_ALLOWANCE_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository, staffContextFacade);
    }

    @Test
    @DisplayName("Allowance creation copies the Staff snapshot and starts with the full entitlement")
    void validAllowanceCreationUsesStaffSnapshot() {
        whenStaffLookupReturns(staffMemberDto());
        whenExactAllowanceLookupReturns(Optional.empty());

        String allowanceId = service.createLeaveAllowance(createAllowanceCommand());

        LeaveAllowanceJpa saved = captureSavedAllowance();
        assertEquals(7, UUID.fromString(allowanceId).version());
        assertEquals(allowanceId, saved.getId());
        assertEquals("staff-1", saved.getStaffMemberId());
        assertEquals("Ada", saved.getFirstName());
        assertEquals("Lovelace", saved.getSurname());
        assertEquals("manager-1", saved.getManagerId());
        assertEquals(BUSINESS_YEAR_START, saved.getBusinessYearStart());
        assertEquals(BUSINESS_YEAR_END, saved.getBusinessYearEnd());
        assertEquals(25, saved.getAnnualEntitlement());
        assertEquals(25, saved.getRemainingDays());
        verify(staffContextFacade).findStaffMemberById("staff-1");
    }

    @Test
    @DisplayName("A duplicate exact-year allowance is rejected without saving")
    void duplicateAllowanceIsRejectedWithoutSaving() {
        whenStaffLookupReturns(staffMemberDto());
        whenExactAllowanceLookupReturns(Optional.of(allowanceJpa(25, 25)));

        Throwable exception = assertThrows(LeaveAllowanceAlreadyExistsException.class, () ->
                service.createLeaveAllowance(createAllowanceCommand())
        );

        assertEquals(
                "Leave allowance already exists for staff member staff-1 "
                        + "and business year 2026-04-01 to 2027-03-31",
                exception.getMessage()
        );
        verify(leaveAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("A missing staff member prevents allowance persistence")
    void missingStaffMemberPreventsAllowanceCreation() {
        when(staffContextFacade.findStaffMemberById("staff-1"))
                .thenThrow(new StaffMemberNotFoundException("staff-1"));

        assertThrows(StaffMemberNotFoundException.class, () ->
                service.createLeaveAllowance(createAllowanceCommand())
        );

        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An invalid business year is rejected before duplicate lookup or save")
    void invalidBusinessYearPreventsAllowanceCreation() {
        whenStaffLookupReturns(staffMemberDto());
        CreateLeaveAllowanceCommand command = new CreateLeaveAllowanceCommand(
                "staff-1",
                BUSINESS_YEAR_END,
                BUSINESS_YEAR_START,
                25
        );

        assertThrows(IllegalArgumentException.class, () -> service.createLeaveAllowance(command));

        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("A null allowance-amendment command is rejected before repository access")
    void nullAllowanceAmendmentCommandIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                service.amendLeaveAllowance(null)
        );

        assertEquals(LeaveApplicationService.AMEND_ALLOWANCE_COMMAND_NOT_NULL, exception.getMessage());
        verifyNoInteractions(leaveRequestRepository, leaveAllowanceRepository);
    }

    @Test
    @DisplayName("An exact-year allowance is found, amended and saved")
    void exactYearAllowanceIsAmendedAndSaved() {
        whenExactAllowanceLookupReturns(allowanceJpa(25, 20));

        service.amendLeaveAllowance(amendAllowanceCommand(30));

        LeaveAllowanceJpa saved = captureSavedAllowance();
        assertEquals(30, saved.getAnnualEntitlement());
        assertEquals(25, saved.getRemainingDays());
    }

    @Test
    @DisplayName("Allowance amendment preserves used leave through persistence mapping")
    void allowanceAmendmentPreservesUsedLeave() {
        whenExactAllowanceLookupReturns(allowanceJpa(25, 17));

        service.amendLeaveAllowance(amendAllowanceCommand(30));

        LeaveAllowanceJpa saved = captureSavedAllowance();
        assertEquals(8, saved.getAnnualEntitlement() - saved.getRemainingDays());
    }

    @Test
    @DisplayName("A missing exact-year allowance raises an application exception")
    void missingExactYearAllowanceFails() {
        when(leaveAllowanceRepository.findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(Optional.empty());

        Throwable exception = assertThrows(LeaveAllowanceNotFoundException.class, () ->
                service.amendLeaveAllowance(amendAllowanceCommand(30))
        );

        assertEquals(
                "Leave allowance not found for staff member staff-1 and business year 2026-04-01 to 2027-03-31",
                exception.getMessage()
        );
        verify(leaveAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("An invalid allowance amendment propagates the domain exception and is not saved")
    void invalidAllowanceAmendmentIsNotSaved() {
        whenExactAllowanceLookupReturns(allowanceJpa(25, 20));

        assertThrows(InvalidLeaveAllowanceException.class, () ->
                service.amendLeaveAllowance(amendAllowanceCommand(4))
        );

        verify(leaveAllowanceRepository, never()).save(any());
    }

    private RequestLeaveCommand validRequestCommand() {
        return new RequestLeaveCommand(
                "staff-1",
                LEAVE_START,
                LEAVE_END,
                "Summer holiday",
                LeaveType.ANNUAL
        );
    }

    private LeaveRequestJpa requestJpa(LeaveStatus status) {
        return new LeaveRequestJpa(
                "request-1",
                "staff-1",
                "manager-1",
                LEAVE_START,
                LEAVE_END,
                "Summer holiday",
                LeaveType.ANNUAL,
                status
        );
    }

    private LeaveAllowanceJpa allowanceJpa(int annualEntitlement, int remainingDays) {
        return new LeaveAllowanceJpa(
                "allowance-1",
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                annualEntitlement,
                remainingDays
        );
    }

    private AmendLeaveAllowanceCommand amendAllowanceCommand(int newEntitlement) {
        return new AmendLeaveAllowanceCommand(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                newEntitlement
        );
    }

    private CreateLeaveAllowanceCommand createAllowanceCommand() {
        return new CreateLeaveAllowanceCommand(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                25
        );
    }

    private StaffMemberDTO staffMemberDto() {
        return new StaffMemberDTO(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(2025, 1, 1),
                "Engineering",
                "manager-1",
                "Developer",
                LocalDate.of(2025, 1, 1),
                "Senior",
                "Permanent",
                EmploymentStatus.ACTIVE
        );
    }

    private void whenStaffLookupReturns(StaffMemberDTO staffMember) {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staffMember);
    }

    private void whenRequestLookupReturns(LeaveStatus status) {
        LeaveRequestJpa projection = requestJpa(status);
        projection.setVersion(0L);
        LeaveRequest aggregate = LeaveRequestJpaToDomainMapper.map(projection);
        when(leaveRequestEventStore.load("request-1")).thenReturn(aggregate);
        when(leaveRequestRepository.findById("request-1")).thenReturn(Optional.of(projection));
    }

    private void whenRequestLookupIsMissing(String requestId) {
        when(leaveRequestEventStore.load(requestId))
                .thenThrow(new LeaveRequestNotFoundException(requestId));
    }

    private void whenExactAllowanceLookupReturns(Optional<LeaveAllowanceJpa> allowance) {
        when(leaveAllowanceRepository.findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(allowance);
    }

    private void whenExactAllowanceLookupReturns(LeaveAllowanceJpa allowanceJpa) {
        when(leaveAllowanceRepository.findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(Optional.of(allowanceJpa));
    }

    private LeaveRequestJpa captureSavedRequest() {
        ArgumentCaptor<LeaveRequestJpa> captor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
        verify(leaveRequestRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> T captureDispatchedEvent(Class<T> eventType) {
        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainEventManager).manageDomainEvents(
                eq(LeaveApplicationService.SOURCE_CONTEXT),
                captor.capture()
        );
        List<Event> dispatchedEvents = captor.getValue();
        assertEquals(2, dispatchedEvents.size());
        assertEquals(1, dispatchedEvents.stream().filter(RemoteEvent.class::isInstance).count());
        return dispatchedEvents.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .findFirst()
                .orElseThrow();
    }

    private LeaveAllowanceJpa captureSavedAllowance() {
        ArgumentCaptor<LeaveAllowanceJpa> captor = ArgumentCaptor.forClass(LeaveAllowanceJpa.class);
        verify(leaveAllowanceRepository).save(captor.capture());
        return captor.getValue();
    }
}
