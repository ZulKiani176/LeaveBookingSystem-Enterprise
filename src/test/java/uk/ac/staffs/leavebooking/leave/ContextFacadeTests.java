package uk.ac.staffs.leavebooking.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.LeaveApplicationService;
import uk.ac.staffs.leavebooking.leave.application.LeaveQueryHandler;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave public operations")
class ContextFacadeTests {
    private static final LocalDate BUSINESS_YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate BUSINESS_YEAR_END = LocalDate.of(2027, 3, 31);

    @Mock
    private LeaveApplicationService leaveApplicationService;

    @Mock
    private LeaveQueryHandler leaveQueryHandler;

    @InjectMocks
    private ContextFacade facade;

    @Test
    @DisplayName("Requesting leave returns the new request identity")
    void requestLeaveDelegatesAndReturnsIdentity() {
        RequestLeaveCommand command = new RequestLeaveCommand(
                "staff-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL
        );
        when(leaveApplicationService.requestLeave(command)).thenReturn("request-1");

        String result = facade.requestLeave(command);

        assertEquals("request-1", result);
    }

    @Test
    @DisplayName("An approval command is sent for the selected request")
    void approveLeaveRequestDelegates() {
        ApproveLeaveRequestCommand command = new ApproveLeaveRequestCommand("request-1");

        facade.approveLeaveRequest(command);

        verify(leaveApplicationService).approveLeaveRequest(command);
    }

    @Test
    @DisplayName("A rejection command is sent for the selected request")
    void rejectLeaveRequestDelegates() {
        RejectLeaveRequestCommand command = new RejectLeaveRequestCommand("request-1");

        facade.rejectLeaveRequest(command);

        verify(leaveApplicationService).rejectLeaveRequest(command);
    }

    @Test
    @DisplayName("A cancellation command is sent for the selected request")
    void cancelLeaveRequestDelegates() {
        CancelLeaveRequestCommand command = new CancelLeaveRequestCommand("request-1");

        facade.cancelLeaveRequest(command);

        verify(leaveApplicationService).cancelLeaveRequest(command);
    }

    @Test
    @DisplayName("An HR referral command is sent for the selected request")
    void referLeaveRequestForHrApprovalDelegates() {
        ReferLeaveRequestForHrApprovalCommand command =
                new ReferLeaveRequestForHrApprovalCommand("request-1");

        facade.referLeaveRequestForHrApproval(command);

        verify(leaveApplicationService).referLeaveRequestForHrApproval(command);
    }

    @Test
    @DisplayName("An HR approval command is sent for the selected request")
    void approveLeaveRequestByHrDelegates() {
        ApproveHrLeaveRequestCommand command = new ApproveHrLeaveRequestCommand("request-1");

        facade.approveLeaveRequestByHr(command);

        verify(leaveApplicationService).approveLeaveRequestByHr(command);
    }

    @Test
    @DisplayName("An HR rejection command is sent for the selected request")
    void rejectLeaveRequestByHrDelegates() {
        RejectHrLeaveRequestCommand command = new RejectHrLeaveRequestCommand("request-1");

        facade.rejectLeaveRequestByHr(command);

        verify(leaveApplicationService).rejectLeaveRequestByHr(command);
    }

    @Test
    @DisplayName("The selected yearly allowance is sent for amendment")
    void amendLeaveAllowanceDelegates() {
        AmendLeaveAllowanceCommand command = new AmendLeaveAllowanceCommand(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                30
        );

        facade.amendLeaveAllowance(command);

        verify(leaveApplicationService).amendLeaveAllowance(command);
    }

    @Test
    @DisplayName("Creating an allowance returns its new identity")
    void createLeaveAllowanceDelegates() {
        CreateLeaveAllowanceCommand command = new CreateLeaveAllowanceCommand(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                25
        );
        when(leaveApplicationService.createLeaveAllowance(command)).thenReturn("allowance-1");

        String result = facade.createLeaveAllowance(command);

        assertEquals("allowance-1", result);
    }

    @Test
    @DisplayName("Finding a request returns its details")
    void findLeaveRequestByIdDelegates() {
        LeaveRequestDTO expected = requestDto("request-1", LeaveStatus.PENDING);
        when(leaveQueryHandler.findLeaveRequestById("request-1")).thenReturn(expected);

        LeaveRequestDTO result = facade.findLeaveRequestById("request-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding staff requests returns the matching list")
    void findLeaveRequestsByStaffMemberIdDelegates() {
        List<LeaveRequestDTO> expected = List.of(requestDto("request-1", LeaveStatus.PENDING));
        when(leaveQueryHandler.findLeaveRequestsByStaffMemberId("staff-1")).thenReturn(expected);

        List<LeaveRequestDTO> result = facade.findLeaveRequestsByStaffMemberId("staff-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding staff requests by status returns the matching list")
    void findLeaveRequestsByStaffMemberIdAndStatusDelegates() {
        List<LeaveRequestDTO> expected = List.of(requestDto("request-1", LeaveStatus.PENDING));
        when(leaveQueryHandler.findLeaveRequestsByStaffMemberIdAndStatus(
                "staff-1",
                LeaveStatus.PENDING
        )).thenReturn(expected);

        List<LeaveRequestDTO> result = facade.findLeaveRequestsByStaffMemberIdAndStatus(
                "staff-1",
                LeaveStatus.PENDING
        );

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding requests by status returns the matching list")
    void findLeaveRequestsByStatusDelegates() {
        List<LeaveRequestDTO> expected = List.of(requestDto("request-1", LeaveStatus.PENDING));
        when(leaveQueryHandler.findLeaveRequestsByStatus(LeaveStatus.PENDING)).thenReturn(expected);

        List<LeaveRequestDTO> result = facade.findLeaveRequestsByStatus(LeaveStatus.PENDING);

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding requests awaiting HR review returns the matching list")
    void findOutstandingHrLeaveRequestsDelegates() {
        List<LeaveRequestDTO> expected = List.of(
                requestDto("request-1", LeaveStatus.PENDING_HR_APPROVAL)
        );
        when(leaveQueryHandler.findOutstandingHrLeaveRequests()).thenReturn(expected);

        List<LeaveRequestDTO> result = facade.findOutstandingHrLeaveRequests();

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding a manager's pending requests returns the matching list")
    void findOutstandingLeaveRequestsByManagerIdDelegates() {
        List<LeaveRequestDTO> expected = List.of(requestDto("request-1", LeaveStatus.PENDING));
        when(leaveQueryHandler.findOutstandingLeaveRequestsByManagerId("manager-1"))
                .thenReturn(expected);

        List<LeaveRequestDTO> result =
                facade.findOutstandingLeaveRequestsByManagerId("manager-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding a manager's requests by date returns the matching list")
    void findDateFilteredManagerRequestsDelegates() {
        LocalDate reportingStart = LocalDate.of(2026, 8, 1);
        LocalDate reportingEnd = LocalDate.of(2026, 8, 31);
        List<LeaveRequestDTO> expected = List.of(requestDto("request-1", LeaveStatus.PENDING));
        when(leaveQueryHandler.findOutstandingLeaveRequestsByManagerId(
                "manager-1",
                reportingStart,
                reportingEnd
        )).thenReturn(expected);

        List<LeaveRequestDTO> result = facade.findOutstandingLeaveRequestsByManagerId(
                "manager-1",
                reportingStart,
                reportingEnd
        );

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding a yearly allowance returns its details")
    void findLeaveAllowanceDelegates() {
        LeaveAllowanceDTO expected = allowanceDto("allowance-1", BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        when(leaveQueryHandler.findLeaveAllowance(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(expected);

        LeaveAllowanceDTO result = facade.findLeaveAllowance(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding staff allowances returns all matching years")
    void findLeaveAllowancesByStaffMemberIdDelegates() {
        List<LeaveAllowanceDTO> expected = List.of(
                allowanceDto("allowance-1", BUSINESS_YEAR_START, BUSINESS_YEAR_END)
        );
        when(leaveQueryHandler.findLeaveAllowancesByStaffMemberId("staff-1")).thenReturn(expected);

        List<LeaveAllowanceDTO> result = facade.findLeaveAllowancesByStaffMemberId("staff-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Company leave usage returns the requested summary")
    void findSystemWideUsageDelegates() {
        LeaveUsageSummaryDTO expected = new LeaveUsageSummaryDTO(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                2,
                55,
                38,
                17
        );
        when(leaveQueryHandler.findSystemWideUsage(BUSINESS_YEAR_START, BUSINESS_YEAR_END))
                .thenReturn(expected);

        LeaveUsageSummaryDTO result = facade.findSystemWideUsage(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertSame(expected, result);
    }

    private LeaveRequestDTO requestDto(String id, LeaveStatus status) {
        return new LeaveRequestDTO(
                id,
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL,
                status
        );
    }

    private LeaveAllowanceDTO allowanceDto(
            String id,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        return new LeaveAllowanceDTO(
                id,
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                businessYearStart,
                businessYearEnd,
                25,
                20,
                5
        );
    }
}
