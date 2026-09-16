package uk.ac.staffs.leavebooking.leave;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.leave.application.LeaveApplicationService;
import uk.ac.staffs.leavebooking.leave.application.LeaveQueryHandler;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestAccessView;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.PublicHolidayDTO;
import uk.ac.staffs.leavebooking.leave.application.PublicHolidayService;
import uk.ac.staffs.leavebooking.leave.application.TeamLeaveCalendarService;
import uk.ac.staffs.leavebooking.leave.application.dto.TeamLeaveCalendarEntryDTO;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CarryOverLeaveAllowanceCommand;

import java.time.LocalDate;
import java.util.List;

@Component
public class ContextFacade {
    private final LeaveApplicationService leaveApplicationService;
    private final LeaveQueryHandler leaveQueryHandler;
    private final PublicHolidayService publicHolidayService;
    private final TeamLeaveCalendarService teamLeaveCalendarService;

    public ContextFacade(
            LeaveApplicationService leaveApplicationService,
            LeaveQueryHandler leaveQueryHandler,
            PublicHolidayService publicHolidayService,
            TeamLeaveCalendarService teamLeaveCalendarService
    ) {
        this.leaveApplicationService = leaveApplicationService;
        this.leaveQueryHandler = leaveQueryHandler;
        this.publicHolidayService = publicHolidayService;
        this.teamLeaveCalendarService = teamLeaveCalendarService;
    }

    public String requestLeave(RequestLeaveCommand command) {
        return leaveApplicationService.requestLeave(command);
    }

    public void approveLeaveRequest(ApproveLeaveRequestCommand command) {
        leaveApplicationService.approveLeaveRequest(command);
    }

    public void rejectLeaveRequest(RejectLeaveRequestCommand command) {
        leaveApplicationService.rejectLeaveRequest(command);
    }

    public void cancelLeaveRequest(CancelLeaveRequestCommand command) {
        leaveApplicationService.cancelLeaveRequest(command);
    }

    public void referLeaveRequestForHrApproval(ReferLeaveRequestForHrApprovalCommand command) {
        leaveApplicationService.referLeaveRequestForHrApproval(command);
    }

    public void approveLeaveRequestByHr(ApproveHrLeaveRequestCommand command) {
        leaveApplicationService.approveLeaveRequestByHr(command);
    }

    public void rejectLeaveRequestByHr(RejectHrLeaveRequestCommand command) {
        leaveApplicationService.rejectLeaveRequestByHr(command);
    }

    public void amendLeaveAllowance(AmendLeaveAllowanceCommand command) {
        leaveApplicationService.amendLeaveAllowance(command);
    }

    public String createLeaveAllowance(CreateLeaveAllowanceCommand command) {
        return leaveApplicationService.createLeaveAllowance(command);
    }

    public void carryOverLeaveAllowance(CarryOverLeaveAllowanceCommand command) {
        leaveApplicationService.carryOverLeaveAllowance(command);
    }

    public LeaveRequestDTO findLeaveRequestById(String requestId) {
        return leaveQueryHandler.findLeaveRequestById(requestId);
    }

    public LeaveRequestAccessView findLeaveRequestAccessView(String requestId) {
        return leaveQueryHandler.findLeaveRequestAccessView(requestId);
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStaffMemberId(String staffMemberId) {
        return leaveQueryHandler.findLeaveRequestsByStaffMemberId(staffMemberId);
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStaffMemberIdAndStatus(
            String staffMemberId,
            LeaveStatus status
    ) {
        return leaveQueryHandler.findLeaveRequestsByStaffMemberIdAndStatus(staffMemberId, status);
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStatus(LeaveStatus status) {
        return leaveQueryHandler.findLeaveRequestsByStatus(status);
    }

    public List<LeaveRequestDTO> findOutstandingHrLeaveRequests() {
        return leaveQueryHandler.findOutstandingHrLeaveRequests();
    }

    public List<LeaveRequestDTO> findOutstandingLeaveRequestsByManagerId(String managerId) {
        return leaveQueryHandler.findOutstandingLeaveRequestsByManagerId(managerId);
    }

    public List<LeaveRequestDTO> findOutstandingLeaveRequestsByManagerId(
            String managerId,
            LocalDate reportingStart,
            LocalDate reportingEnd
    ) {
        return leaveQueryHandler.findOutstandingLeaveRequestsByManagerId(
                managerId,
                reportingStart,
                reportingEnd
        );
    }

    public LeaveAllowanceDTO findLeaveAllowance(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        return leaveQueryHandler.findLeaveAllowance(
                staffMemberId,
                businessYearStart,
                businessYearEnd
        );
    }

    public List<LeaveAllowanceDTO> findLeaveAllowancesByStaffMemberId(String staffMemberId) {
        return leaveQueryHandler.findLeaveAllowancesByStaffMemberId(staffMemberId);
    }

    public LeaveUsageSummaryDTO findSystemWideUsage(
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        return leaveQueryHandler.findSystemWideUsage(businessYearStart, businessYearEnd);
    }

    public PublicHolidayDTO createPublicHoliday(LocalDate date, String name) {
        return publicHolidayService.create(date, name);
    }

    public List<PublicHolidayDTO> findPublicHolidays(LocalDate startDate, LocalDate endDate) {
        return publicHolidayService.findBetween(startDate, endDate);
    }

    public void deletePublicHoliday(LocalDate date) {
        publicHolidayService.delete(date);
    }

    public List<TeamLeaveCalendarEntryDTO> findTeamLeaveCalendar(
            String managerId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return teamLeaveCalendarService.find(managerId, startDate, endDate);
    }
}
