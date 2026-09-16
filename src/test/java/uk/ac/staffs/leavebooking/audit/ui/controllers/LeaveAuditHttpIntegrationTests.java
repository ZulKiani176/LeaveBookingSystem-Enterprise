package uk.ac.staffs.leavebooking.audit.ui.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.ac.staffs.leavebooking.audit.application.LeaveAuditService;
import uk.ac.staffs.leavebooking.audit.infrastructure.repositories.LeaveAuditRepository;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.identity.security.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"firebase.enabled=false", "rabbitmq.enabled=false"})
@AutoConfigureMockMvc
@DisplayName("Leave audit API")
class LeaveAuditHttpIntegrationTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Autowired private MockMvc mockMvc;
    @Autowired private LeaveAuditService service;
    @Autowired private LeaveAuditRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Audit history is ADMIN-only and exposes lifecycle decision information")
    void auditHistoryIsAdminOnly() throws Exception {
        service.record(new LeaveRequestApprovedIntegrationEvent(
                101L, DATE, "request-1", "staff-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL, IntegrationLeaveDayPortion.FULL_DAY,
                BigDecimal.ONE, IntegrationLeaveStatus.APPROVED,
                IntegrationLeaveStatus.PENDING_HR_APPROVAL
        ));

        mockMvc.perform(get("/api/audit/leave").with(as(Role.STAFF)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit/leave")
                        .with(as(Role.ADMIN))
                        .queryParam("leaveRequestId", "request-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("HR_APPROVED"));
    }

    @Test
    @DisplayName("Sickness audit JSON never contains a reason")
    void sicknessAuditDoesNotExposeReason() throws Exception {
        service.record(new SickLeaveRecordedIntegrationEvent(
                DATE, "sick-1", "staff-1", "manager-1", DATE, DATE
        ).withId(102L));

        mockMvc.perform(get("/api/audit/leave").with(as(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SICK_LEAVE_RECORDED"))
                .andExpect(jsonPath("$[0].reason").doesNotExist());
    }

    private RequestPostProcessor as(Role role) {
        return jwt().jwt(token -> token.subject("firebase-" + role.name().toLowerCase())
                        .claim("role", role.getAuthority()))
                .authorities(new SimpleGrantedAuthority(role.getAuthority()));
    }
}
