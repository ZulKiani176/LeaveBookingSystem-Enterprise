package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestAccessView;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;

@Component("leaveAccess")
public class LeaveAccessPolicy {
    private final CurrentUser currentUser;
    private final ContextFacade leaveContextFacade;
    private final uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;

    public LeaveAccessPolicy(
            CurrentUser currentUser,
            ContextFacade leaveContextFacade,
            uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade
    ) {
        this.currentUser = currentUser;
        this.leaveContextFacade = leaveContextFacade;
        this.staffContextFacade = staffContextFacade;
    }

    public boolean canSubmit(Authentication authentication, String staffMemberId) {
        return currentUser.isOwner(authentication, staffMemberId);
    }

    public boolean canReadStaffRequests(Authentication authentication, String staffMemberId) {
        return currentUser.hasRole(authentication, Role.ADMIN)
                || currentUser.isOwner(authentication, staffMemberId);
    }

    public boolean canReadRequest(Authentication authentication, String requestId) {
        if (currentUser.hasRole(authentication, Role.ADMIN)) {
            return true;
        }
        try {
            LeaveRequestAccessView request = leaveContextFacade
                    .findLeaveRequestAccessView(requestId);
            return currentUser.isOwner(authentication, request.staffMemberId())
                    || !"SICK".equals(request.leaveType())
                    && isAssignedManager(
                            authentication,
                            request.managerId(),
                            request.staffMemberId()
                    )
                    || currentUser.hasRole(authentication, Role.HR)
                    && "PENDING_HR_APPROVAL".equals(request.status());
        } catch (LeaveRequestNotFoundException exception) {
            return authentication != null && authentication.isAuthenticated();
        }
    }

    public boolean canCancel(Authentication authentication, String requestId) {
        try {
            LeaveRequestAccessView request = leaveContextFacade
                    .findLeaveRequestAccessView(requestId);
            return currentUser.isOwner(authentication, request.staffMemberId());
        } catch (LeaveRequestNotFoundException exception) {
            return authentication != null && authentication.isAuthenticated();
        }
    }

    public boolean canApproveOrReject(Authentication authentication, String requestId) {
        if (currentUser.hasRole(authentication, Role.ADMIN)) {
            return true;
        }
        try {
            LeaveRequestAccessView request = leaveContextFacade
                    .findLeaveRequestAccessView(requestId);
            return isAssignedManager(authentication, request.managerId(), request.staffMemberId());
        } catch (LeaveRequestNotFoundException exception) {
            return currentUser.hasRole(authentication, Role.MANAGER);
        }
    }

    public boolean canReferToHr(Authentication authentication, String requestId) {
        return canApproveOrReject(authentication, requestId);
    }

    public boolean canPerformHrReview(Authentication authentication) {
        return currentUser.hasRole(authentication, Role.HR);
    }

    public boolean canViewHrOutstanding(Authentication authentication) {
        return currentUser.hasAnyRole(authentication, Role.HR, Role.ADMIN);
    }

    public boolean canViewManagerOutstanding(Authentication authentication, String managerId) {
        return currentUser.hasRole(authentication, Role.ADMIN)
                || currentUser.hasRole(authentication, Role.MANAGER)
                && currentUser.isOwner(authentication, managerId);
    }

    public boolean canQueryAllRequests(Authentication authentication) {
        return currentUser.hasRole(authentication, Role.ADMIN);
    }

    public boolean canReadAllowance(Authentication authentication, String staffMemberId) {
        return currentUser.hasRole(authentication, Role.ADMIN)
                || currentUser.isOwner(authentication, staffMemberId)
                || isAssignedManager(authentication, staffMemberId);
    }

    public boolean canManageAllowances(Authentication authentication) {
        return currentUser.hasRole(authentication, Role.ADMIN);
    }

    private boolean isAssignedManager(Authentication authentication, String staffMemberId) {
        if (!currentUser.hasRole(authentication, Role.MANAGER)) {
            return false;
        }
        try {
            StaffMemberDTO staffMember = staffContextFacade.findStaffMemberById(staffMemberId);
            return currentUser.staffId(authentication)
                    .map(staffMember.managerId()::equals)
                    .orElse(false);
        } catch (StaffMemberNotFoundException exception) {
            return false;
        }
    }

    private boolean isAssignedManager(
            Authentication authentication,
            String managerId,
            String staffMemberId
    ) {
        return currentUser.hasRole(authentication, Role.MANAGER)
                && currentUser.staffId(authentication).map(managerId::equals).orElse(false)
                && !currentUser.isOwner(authentication, staffMemberId);
    }
}
