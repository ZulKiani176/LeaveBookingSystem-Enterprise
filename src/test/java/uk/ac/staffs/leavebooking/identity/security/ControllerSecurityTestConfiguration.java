package uk.ac.staffs.leavebooking.identity.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;

@TestConfiguration(proxyBeanMethods = false)
@Import({
        SecurityConfig.class,
        uk.ac.staffs.leavebooking.identity.authservice.FirebaseConfig.class,
        FirebaseJwtAuthenticationConverter.class,
        FirebaseAuthenticationEntryPoint.class,
        FirebaseAccessDeniedHandler.class,
        SecurityErrorWriter.class,
        CurrentUser.class
})
public class ControllerSecurityTestConfiguration {
    @Bean("leaveAccess")
    LeaveAccessPolicy leaveAccessPolicy() {
        return new LeaveAccessPolicy(null, null, null) {
            @Override public boolean canSubmit(Authentication a, String id) { return true; }
            @Override public boolean canReadStaffRequests(Authentication a, String id) { return true; }
            @Override public boolean canReadRequest(Authentication a, String id) { return true; }
            @Override public boolean canCancel(Authentication a, String id) { return true; }
            @Override public boolean canApproveOrReject(Authentication a, String id) { return true; }
            @Override public boolean canReferToHr(Authentication a, String id) { return true; }
            @Override public boolean canPerformHrReview(Authentication a) { return true; }
            @Override public boolean canViewHrOutstanding(Authentication a) { return true; }
            @Override public boolean canViewManagerOutstanding(Authentication a, String id) {
                return true;
            }
            @Override public boolean canQueryAllRequests(Authentication a) { return true; }
            @Override public boolean canReadAllowance(Authentication a, String id) { return true; }
            @Override public boolean canManageAllowances(Authentication a) { return true; }
        };
    }

    @Bean("staffAccess")
    StaffAccessPolicy staffAccessPolicy() {
        return new StaffAccessPolicy(null, null) {
            @Override public boolean isAdmin(Authentication authentication) { return true; }
            @Override public boolean canReadStaffMember(Authentication a, String id) { return true; }
            @Override public boolean canReadManagerTeam(Authentication a, String id) { return true; }
        };
    }

    @Bean("notificationAccess")
    NotificationAccessPolicy notificationAccessPolicy() {
        return new NotificationAccessPolicy(null) {
            @Override public boolean canReadInbox(Authentication a, String id) { return true; }
        };
    }
}
