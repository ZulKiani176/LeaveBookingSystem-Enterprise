package uk.ac.staffs.leavebooking.identity.security;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestEventStreamRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.notification.application.NotificationType;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Access to protected operations")
class SecurityAuthorizationIntegrationTests {
    private static final String REQUEST_ID = "request-1";

    @Autowired private MockMvc mockMvc;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveAllowanceRepository leaveAllowanceRepository;
    @Autowired private StaffMemberRepository staffMemberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EventStoreRepository eventStoreRepository;
    @Autowired private LeaveRequestEventStreamRepository leaveRequestEventStreamRepository;
    @Autowired private LeaveRequestEventStore leaveRequestEventStore;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        eventStoreRepository.deleteAll();
        leaveRequestEventStreamRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
        staffMemberRepository.deleteAll();

        staffMemberRepository.save(staff("manager-1", "manager@example.com", "director-1"));
        staffMemberRepository.save(staff("manager-x", "manager-x@example.com", "director-1"));
        staffMemberRepository.save(staff("staff-1", "staff@example.com", "manager-1"));
        leaveAllowanceRepository.save(new LeaveAllowanceJpa(
                "allowance-1", "staff-1", "Ada", "Lovelace", "manager-1",
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), 25, 25
        ));
        LeaveRequest request = LeaveRequest.create(
                Identity.of(REQUEST_ID),
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18)),
                "Annual leave",
                LeaveType.ANNUAL
        );
        leaveRequestEventStore.append(request);
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.map(request));
        request.clearDomainEvents();
        notificationRepository.save(new NotificationJpa(
                "notification-1", 900L, "staff-1", REQUEST_ID,
                NotificationType.STAFF_LEAVE_APPROVED, "Approved", Instant.now()
        ));
    }

    @Test
    @DisplayName("Missing and malformed tokens return 401")
    void anonymousAndMalformedTokensReturn401() throws Exception {
        mockMvc.perform(get("/api/staff/staff-1/leave-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/staff/staff-1/leave-requests")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("Staff can read their own leave requests but not another person's")
    void staffCanReadOwnRequestsButNotAnotherIdentity() throws Exception {
        mockMvc.perform(get("/api/staff/staff-1/leave-requests")
                        .with(as(Role.STAFF, "staff-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(REQUEST_ID));

        mockMvc.perform(get("/api/staff/staff-1/leave-requests")
                        .with(as(Role.STAFF, "staff-2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("The assigned manager can approve leave but other managers and staff cannot")
    void onlyAssignedManagerOrAdminCanOrdinarilyApprove() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/{id}/approve", REQUEST_ID)
                        .with(as(Role.STAFF, "staff-1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/leave-requests/{id}/approve", REQUEST_ID)
                        .with(as(Role.MANAGER, "manager-x")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/leave-requests/{id}/approve", REQUEST_ID)
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("The requesting staff member can cancel leave but their manager cannot")
    void cancellationIsOwnerOnly() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/{id}/cancel", REQUEST_ID)
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/leave-requests/{id}/cancel", REQUEST_ID)
                        .with(as(Role.STAFF, "staff-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Only HR can approve a request referred by a manager")
    void hrApprovalRequiresHrRoleAfterManagerReferral() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/{id}/refer-to-hr", REQUEST_ID)
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_HR_APPROVAL"));

        mockMvc.perform(patch("/api/leave-requests/{id}/hr-approve", REQUEST_ID)
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/leave-requests/{id}/hr-approve", REQUEST_ID)
                        .with(as(Role.HR, "hr-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Managers can read team allowances and only admins can read company usage")
    void allowanceAndUsageEnforceOwnerManagerAndAdminRules() throws Exception {
        mockMvc.perform(get("/api/staff/staff-1/leave-allowances")
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff/staff-1/leave-allowances")
                        .with(as(Role.MANAGER, "manager-x")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", "2026-04-01")
                        .queryParam("end", "2027-03-31")
                        .with(as(Role.STAFF, "staff-1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", "2026-04-01")
                        .queryParam("end", "2027-03-31")
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("An admin can add staff but a staff user cannot")
    void staffAdministrationIsAdminOnly() throws Exception {
        String body = createStaffJson("new@example.com");
        mockMvc.perform(post("/api/staff")
                        .with(as(Role.STAFF, "staff-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/staff")
                        .with(as(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Managers can read their own team but not another manager's team")
    void managerCanReadOnlyOwnTeam() throws Exception {
        mockMvc.perform(get("/api/managers/manager-1/staff")
                        .with(as(Role.MANAGER, "manager-1")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/managers/manager-1/staff")
                        .with(as(Role.MANAGER, "manager-x")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Only the recipient or an admin can read a notification inbox")
    void notificationInboxIsOwnerOrAdminOnly() throws Exception {
        mockMvc.perform(get("/api/users/staff-1/notifications")
                        .with(as(Role.STAFF, "staff-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("notification-1"));

        mockMvc.perform(get("/api/users/staff-1/notifications")
                        .with(as(Role.STAFF, "staff-2")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/staff-1/notifications")
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Missing requests return 404 and invalid approval returns 409")
    void authorisedBusinessErrorsRemain404And409() throws Exception {
        mockMvc.perform(get("/api/leave-requests/missing")
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEAVE_REQUEST_NOT_FOUND"));

        mockMvc.perform(patch("/api/leave-requests/{id}/reject", REQUEST_ID)
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/leave-requests/{id}/approve", REQUEST_ID)
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_LEAVE_REQUEST_STATE"));
    }

    private RequestPostProcessor as(Role role, String staffId) {
        var processor = jwt().jwt(token -> {
            token.subject("firebase-" + role.name().toLowerCase())
                    .claim("role", role.getAuthority());
            if (staffId != null) {
                token.claim("staffId", staffId);
            }
        });
        return processor.authorities(new SimpleGrantedAuthority(role.getAuthority()));
    }

    private StaffMemberJpa staff(String id, String email, String managerId) {
        return new StaffMemberJpa(
                id, "Test", "User", email, LocalDate.of(2025, 1, 1),
                "Engineering", managerId, "Developer", LocalDate.of(2025, 1, 1),
                "Senior", "Permanent", EmploymentStatus.ACTIVE
        );
    }

    private String createStaffJson(String email) {
        return """
                {
                  "firstName":"New",
                  "surname":"User",
                  "email":"%s",
                  "hireDate":"2025-01-01",
                  "department":"Engineering",
                  "managerId":"manager-1",
                  "jobRole":"Developer",
                  "roleStartDate":"2025-01-01",
                  "jobLevel":"Junior",
                  "employmentType":"Permanent"
                }
                """.formatted(email);
    }
}
