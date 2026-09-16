package uk.ac.staffs.leavebooking.leave.ui.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.GlobalExceptionHandler;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import uk.ac.staffs.leavebooking.identity.security.ControllerSecurityTestConfiguration;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.LeaveQueryHandler;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveUsageController.class)
@Import({GlobalExceptionHandler.class, ControllerSecurityTestConfiguration.class})
@WithMockFirebaseUser
@DisplayName("Company leave usage API")
class LeaveUsageControllerTests {
    private static final LocalDate YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 3, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContextFacade contextFacade;

    @Test
    @DisplayName("A valid business year returns the system-wide usage summary")
    void validBusinessYearReturnsUsageSummary() throws Exception {
        when(contextFacade.findSystemWideUsage(YEAR_START, YEAR_END)).thenReturn(
                new LeaveUsageSummaryDTO(YEAR_START, YEAR_END, 2, 55, 38, 17)
        );

        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", YEAR_START.toString())
                        .queryParam("end", YEAR_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessYearStart").value("2026-04-01"))
                .andExpect(jsonPath("$.businessYearEnd").value("2027-03-31"))
                .andExpect(jsonPath("$.staffCount").value(2))
                .andExpect(jsonPath("$.totalEntitlement").value(55))
                .andExpect(jsonPath("$.totalRemainingDays").value(38))
                .andExpect(jsonPath("$.totalUsedDays").value(17));

    }

    @Test
    @DisplayName("A malformed usage date returns a framework 400")
    void malformedUsageDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", "not-a-date")
                        .queryParam("end", YEAR_END.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    }

    @Test
    @DisplayName("A missing usage date returns a framework 400")
    void missingUsageDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", YEAR_START.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    }

    @Test
    @DisplayName("A reversed usage range returns 400")
    void reversedUsageRangeReturnsBadRequest() throws Exception {
        when(contextFacade.findSystemWideUsage(YEAR_END, YEAR_START))
                .thenThrow(new IllegalArgumentException(
                        LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START
                ));

        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", YEAR_END.toString())
                        .queryParam("end", YEAR_START.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START
                ));
    }
}
