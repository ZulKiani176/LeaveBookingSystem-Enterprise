package uk.ac.staffs.leavebooking.reporting.ui.controllers;

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
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.identity.security.Role;
import uk.ac.staffs.leavebooking.reporting.application.LeaveReportingService;
import uk.ac.staffs.leavebooking.reporting.infrastructure.repositories.LeaveReportingProjectionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "rabbitmq.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("Leave reporting API")
class LeaveReportingHttpIntegrationTests {
    private static final LocalDate OCCURRED_ON = LocalDate.of(2026, 8, 26);
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 14);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 18);

    @Autowired private MockMvc mockMvc;
    @Autowired private LeaveReportingService reportingService;
    @Autowired private LeaveReportingProjectionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("The reporting read model applies later events and is ADMIN-only over HTTP")
    void reportingProjectionAppliesLatestEventAndEnforcesRole() throws Exception {
        reportingService.apply(new LeaveRequestSubmittedIntegrationEvent(
                OCCURRED_ON, "request-report", "staff-report", "manager-report",
                START_DATE, END_DATE, IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, new BigDecimal("5.0")
        ).withId(101L));
        reportingService.apply(new LeaveRequestApprovedIntegrationEvent(
                OCCURRED_ON, "request-report", "staff-report",
                START_DATE, END_DATE, IntegrationLeaveType.ANNUAL,
                IntegrationLeaveDayPortion.FULL_DAY, new BigDecimal("5.0")
        ).withId(102L));

        mockMvc.perform(get("/api/reports/leave")
                        .with(as(Role.STAFF, "staff-report"))
                        .queryParam("startDate", "2026-09-01")
                        .queryParam("endDate", "2026-09-30"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/leave")
                        .with(as(Role.ADMIN, null))
                        .queryParam("startDate", "2026-09-01")
                        .queryParam("endDate", "2026-09-30")
                        .queryParam("leaveType", "ANNUAL")
                        .queryParam("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaveRequestId").value("request-report"))
                .andExpect(jsonPath("$[0].sourceEventId").value(102))
                .andExpect(jsonPath("$[0].chargedLeaveDays").value(5.0))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].reason").doesNotExist());

        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("Sickness reporting contains operational fields but no private reason")
    void sicknessProjectionIsPrivacySafe() throws Exception {
        reportingService.apply(new SickLeaveRecordedIntegrationEvent(
                OCCURRED_ON, "sick-report", "staff-report", "manager-report",
                START_DATE, END_DATE
        ).withId(201L));

        mockMvc.perform(get("/api/reports/leave")
                        .with(as(Role.ADMIN, null))
                        .queryParam("startDate", "2026-09-01")
                        .queryParam("endDate", "2026-09-30")
                        .queryParam("leaveType", "SICK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaveRequestId").value("sick-report"))
                .andExpect(jsonPath("$[0].leaveType").value("SICK"))
                .andExpect(jsonPath("$[0].chargedLeaveDays").value(0.0))
                .andExpect(jsonPath("$[0].status").value("RECORDED"))
                .andExpect(jsonPath("$[0].reason").doesNotExist());
    }

    @Test
    @DisplayName("A reversed reporting range uses the shared 400 error contract")
    void reversedReportingRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/leave")
                        .with(as(Role.ADMIN, null))
                        .queryParam("startDate", "2026-09-30")
                        .queryParam("endDate", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
}
