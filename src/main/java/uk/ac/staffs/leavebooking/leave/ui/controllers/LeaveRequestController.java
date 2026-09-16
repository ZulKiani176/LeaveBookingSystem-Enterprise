package uk.ac.staffs.leavebooking.leave.ui.controllers;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.LeaveQueryHandler;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.TeamLeaveCalendarEntryDTO;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.leave.ui.requests.RequestLeaveRequest;
import uk.ac.staffs.leavebooking.leave.ui.requests.LeaveDecisionRequest;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LeaveRequestController {
    private final ContextFacade contextFacade;

    public LeaveRequestController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @PostMapping("/staff/{staffMemberId}/leave-requests")
    @PreAuthorize("@leaveAccess.canSubmit(authentication, #staffMemberId)")
    public ResponseEntity<LeaveRequestDTO> requestLeave(
            @PathVariable String staffMemberId,
            @Valid @RequestBody RequestLeaveRequest request
    ) {
        String requestId = contextFacade.requestLeave(new RequestLeaveCommand(
                staffMemberId,
                request.startDate(),
                request.endDate(),
                request.reason(),
                request.leaveType(),
                request.dayPortion()
        ));
        LeaveRequestDTO createdRequest = contextFacade.findLeaveRequestById(requestId);

        return ResponseEntity
                .created(URI.create("/api/leave-requests/" + requestId))
                .body(createdRequest);
    }

    @GetMapping("/leave-requests/{requestId}")
    @PreAuthorize("@leaveAccess.canReadRequest(authentication, #requestId)")
    public LeaveRequestDTO findLeaveRequestById(@PathVariable String requestId) {
        return contextFacade.findLeaveRequestById(requestId);
    }

    @GetMapping("/staff/{staffMemberId}/leave-requests")
    @PreAuthorize("@leaveAccess.canReadStaffRequests(authentication, #staffMemberId)")
    public List<LeaveRequestDTO> findLeaveRequestsByStaffMemberId(
            @PathVariable String staffMemberId,
            @RequestParam(required = false) LeaveStatus status
    ) {
        if (status == null) {
            return contextFacade.findLeaveRequestsByStaffMemberId(staffMemberId);
        }

        return contextFacade.findLeaveRequestsByStaffMemberIdAndStatus(staffMemberId, status);
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("@leaveAccess.canQueryAllRequests(authentication)")
    public List<LeaveRequestDTO> findLeaveRequestsByStatus(@RequestParam LeaveStatus status) {
        return contextFacade.findLeaveRequestsByStatus(status);
    }

    @GetMapping("/leave-requests/hr/outstanding")
    @PreAuthorize("@leaveAccess.canViewHrOutstanding(authentication)")
    public List<LeaveRequestDTO> findOutstandingHrLeaveRequests() {
        return contextFacade.findOutstandingHrLeaveRequests();
    }

    @GetMapping("/managers/{managerId}/leave-requests/outstanding")
    @PreAuthorize("@leaveAccess.canViewManagerOutstanding(authentication, #managerId)")
    public List<LeaveRequestDTO> findOutstandingLeaveRequestsByManagerId(
            @PathVariable String managerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end
    ) {
        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException(LeaveQueryHandler.REPORTING_DATES_REQUIRED);
        }
        if (start == null) {
            return contextFacade.findOutstandingLeaveRequestsByManagerId(managerId);
        }

        return contextFacade.findOutstandingLeaveRequestsByManagerId(managerId, start, end);
    }

    @GetMapping("/managers/{managerId}/team-leave-calendar")
    @PreAuthorize("@leaveAccess.canViewManagerOutstanding(authentication, #managerId)")
    public List<TeamLeaveCalendarEntryDTO> findTeamLeaveCalendar(
            @PathVariable String managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return contextFacade.findTeamLeaveCalendar(managerId, startDate, endDate);
    }

    @PatchMapping("/leave-requests/{requestId}/approve")
    @PreAuthorize("@leaveAccess.canApproveOrReject(authentication, #requestId)")
    public LeaveRequestDTO approveLeaveRequest(
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request
    ) {
        contextFacade.approveLeaveRequest(new ApproveLeaveRequestCommand(
                requestId, request == null ? null : request.comment()
        ));
        return contextFacade.findLeaveRequestById(requestId);
    }

    @PatchMapping("/leave-requests/{requestId}/reject")
    @PreAuthorize("@leaveAccess.canApproveOrReject(authentication, #requestId)")
    public LeaveRequestDTO rejectLeaveRequest(
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request
    ) {
        contextFacade.rejectLeaveRequest(new RejectLeaveRequestCommand(
                requestId, request == null ? null : request.comment()
        ));
        return contextFacade.findLeaveRequestById(requestId);
    }

    @PatchMapping("/leave-requests/{requestId}/cancel")
    @PreAuthorize("@leaveAccess.canCancel(authentication, #requestId)")
    public LeaveRequestDTO cancelLeaveRequest(@PathVariable String requestId) {
        contextFacade.cancelLeaveRequest(new CancelLeaveRequestCommand(requestId));
        return contextFacade.findLeaveRequestById(requestId);
    }

    @PatchMapping("/leave-requests/{requestId}/refer-to-hr")
    @PreAuthorize("@leaveAccess.canReferToHr(authentication, #requestId)")
    public LeaveRequestDTO referLeaveRequestForHrApproval(@PathVariable String requestId) {
        contextFacade.referLeaveRequestForHrApproval(
                new ReferLeaveRequestForHrApprovalCommand(requestId)
        );
        return contextFacade.findLeaveRequestById(requestId);
    }

    @PatchMapping("/leave-requests/{requestId}/hr-approve")
    @PreAuthorize("@leaveAccess.canPerformHrReview(authentication)")
    public LeaveRequestDTO approveLeaveRequestByHr(
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request
    ) {
        contextFacade.approveLeaveRequestByHr(new ApproveHrLeaveRequestCommand(
                requestId, request == null ? null : request.comment()
        ));
        return contextFacade.findLeaveRequestById(requestId);
    }

    @PatchMapping("/leave-requests/{requestId}/hr-reject")
    @PreAuthorize("@leaveAccess.canPerformHrReview(authentication)")
    public LeaveRequestDTO rejectLeaveRequestByHr(
            @PathVariable String requestId,
            @Valid @RequestBody(required = false) LeaveDecisionRequest request
    ) {
        contextFacade.rejectLeaveRequestByHr(new RejectHrLeaveRequestCommand(
                requestId, request == null ? null : request.comment()
        ));
        return contextFacade.findLeaveRequestById(requestId);
    }
}
