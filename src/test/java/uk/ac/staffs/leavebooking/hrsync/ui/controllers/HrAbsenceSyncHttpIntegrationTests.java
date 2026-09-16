package uk.ac.staffs.leavebooking.hrsync.ui.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.hrsync.application.HrAbsenceSyncService;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.repositories.HrAbsenceSyncRepository;
import uk.ac.staffs.leavebooking.identity.security.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"firebase.enabled=false", "rabbitmq.enabled=false"})
@AutoConfigureMockMvc
@DisplayName("HR absence records API")
class HrAbsenceSyncHttpIntegrationTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Autowired private MockMvc mockMvc;
    @Autowired private HrAbsenceSyncService service;
    @Autowired private HrAbsenceSyncRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Outbound HR absence records are ADMIN-only and filterable by staff")
    void hrAbsenceSyncIsAdminOnly() throws Exception {
        service.record(new LeaveRequestApprovedIntegrationEvent(
                DATE, "request-1", "staff-1", DATE, DATE,
                IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, BigDecimal.ONE
        ).withId(201L));

        mockMvc.perform(get("/api/integrations/hr/absences").with(as(Role.HR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/integrations/hr/absences")
                        .with(as(Role.ADMIN))
                        .queryParam("staffMemberId", "staff-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].syncAction").value("ANNUAL_LEAVE_APPROVED"))
                .andExpect(jsonPath("$[0].chargedLeaveDays").value(1.0));
    }

    @Test
    @DisplayName("Outbound sickness JSON is operational and contains no private reason")
    void sicknessSyncDoesNotExposeReason() throws Exception {
        service.record(new SickLeaveRecordedIntegrationEvent(
                DATE, "sick-1", "staff-1", "manager-1", DATE, DATE
        ).withId(202L));

        mockMvc.perform(get("/api/integrations/hr/absences").with(as(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].syncAction").value("SICK_LEAVE_RECORDED"))
                .andExpect(jsonPath("$[0].chargedLeaveDays").value(0.0))
                .andExpect(jsonPath("$[0].reason").doesNotExist());
    }

    private RequestPostProcessor as(Role role) {
        return jwt().jwt(token -> token.subject("firebase-" + role.name().toLowerCase())
                        .claim("role", role.getAuthority()))
                .authorities(new SimpleGrantedAuthority(role.getAuthority()));
    }
}
