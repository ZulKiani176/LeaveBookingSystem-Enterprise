package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.staff.ContextFacade;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;

@Component("staffAccess")
public class StaffAccessPolicy {
    private final CurrentUser currentUser;
    private final ContextFacade staffContextFacade;

    public StaffAccessPolicy(CurrentUser currentUser, ContextFacade staffContextFacade) {
        this.currentUser = currentUser;
        this.staffContextFacade = staffContextFacade;
    }

    public boolean isAdmin(Authentication authentication) {
        return currentUser.hasRole(authentication, Role.ADMIN);
    }

    public boolean canReadStaffMember(Authentication authentication, String staffMemberId) {
        if (isAdmin(authentication) || currentUser.isOwner(authentication, staffMemberId)) {
            return true;
        }
        if (!currentUser.hasRole(authentication, Role.MANAGER)) {
            return false;
        }
        try {
            StaffMemberDTO staffMember = staffContextFacade.findStaffMemberById(staffMemberId);
            return currentUser.staffId(authentication)
                    .map(staffMember.managerId()::equals)
                    .orElse(false);
        } catch (StaffMemberNotFoundException exception) {
            return true;
        }
    }

    public boolean canReadManagerTeam(Authentication authentication, String managerId) {
        return isAdmin(authentication)
                || currentUser.hasRole(authentication, Role.MANAGER)
                && currentUser.isOwner(authentication, managerId);
    }
}
