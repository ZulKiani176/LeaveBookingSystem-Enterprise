package uk.ac.staffs.leavebooking.leave.ui.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockFirebaseUser
@DisplayName("Staff and leave working together")
class LeaveStaffHttpIntegrationTests {
    private static final String MANAGER_ID = "manager-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @BeforeEach
    void clearData() {
        leaveRequestRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
        staffMemberRepository.deleteAll();
    }

    @Test
    @DisplayName("Staff snapshots support allowance creation, manager reporting and usage reporting")
    void staffAndLeaveModulesCollaborateThroughTheirPublicApplicationBoundary() throws Exception {
        MvcResult staffCreation = mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createStaffJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"))
                .andReturn();
        String staffMemberId = JsonPath.read(
                staffCreation.getResponse().getContentAsString(),
                "$.id"
        );

        MvcResult allowanceCreation = mockMvc.perform(
                        post("/api/staff/{staffMemberId}/leave-allowances", staffMemberId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createAllowanceJson())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.surname").value("Lovelace"))
                .andExpect(jsonPath("$.managerId").value(MANAGER_ID))
                .andExpect(jsonPath("$.annualEntitlement").value(25))
                .andExpect(jsonPath("$.remainingDays").value(25))
                .andReturn();
        assertEquals(
                "/api/staff/" + staffMemberId
                        + "/leave-allowances/year?start=2026-04-01&end=2027-03-31",
                allowanceCreation.getResponse().getHeader("Location")
        );

        MvcResult requestCreation = mockMvc.perform(
                        post("/api/staff/{staffMemberId}/leave-requests", staffMemberId)
                                .with(jwt()
                                        .jwt(token -> token
                                                .subject("firebase-staff")
                                                .claim("role", "ROLE_STAFF")
                                                .claim("staffId", staffMemberId))
                                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createLeaveRequestJson())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId").value(MANAGER_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String leaveRequestId = JsonPath.read(
                requestCreation.getResponse().getContentAsString(),
                "$.id"
        );
        assertEquals(
                MANAGER_ID,
                leaveRequestRepository.findById(leaveRequestId).orElseThrow().getManagerId()
        );

        mockMvc.perform(get(
                        "/api/managers/{managerId}/leave-requests/outstanding",
                        MANAGER_ID
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(leaveRequestId))
                .andExpect(jsonPath("$[0].managerId").value(MANAGER_ID))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/api/leave-allowances/usage")
                        .queryParam("start", "2026-04-01")
                        .queryParam("end", "2027-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCount").value(1))
                .andExpect(jsonPath("$.totalEntitlement").value(25))
                .andExpect(jsonPath("$.totalRemainingDays").value(25))
                .andExpect(jsonPath("$.totalUsedDays").value(0));
    }

    private String createStaffJson() {
        return """
                {
                  "firstName": "Ada",
                  "surname": "Lovelace",
                  "email": "ada@example.com",
                  "hireDate": "2025-01-01",
                  "department": "Engineering",
                  "managerId": "manager-1",
                  "jobRole": "Developer",
                  "roleStartDate": "2025-01-01",
                  "jobLevel": "Senior",
                  "employmentType": "Permanent"
                }
                """;
    }

    private String createAllowanceJson() {
        return """
                {
                  "businessYearStart": "2026-04-01",
                  "businessYearEnd": "2027-03-31",
                  "annualEntitlement": 25
                }
                """;
    }

    private String createLeaveRequestJson() {
        return """
                {
                  "startDate": "2026-09-14",
                  "endDate": "2026-09-18",
                  "reason": "Annual leave",
                  "leaveType": "ANNUAL"
                }
                """;
    }
}
