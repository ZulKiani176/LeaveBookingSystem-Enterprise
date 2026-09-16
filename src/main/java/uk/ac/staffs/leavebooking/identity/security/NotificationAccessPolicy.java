package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("notificationAccess")
public class NotificationAccessPolicy {
    private final CurrentUser currentUser;

    public NotificationAccessPolicy(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public boolean canReadInbox(Authentication authentication, String recipientId) {
        return currentUser.hasRole(authentication, Role.ADMIN)
                || currentUser.isOwner(authentication, recipientId);
    }
}
