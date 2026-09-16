package uk.ac.staffs.leavebooking.staff.ui.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockFirebaseUser
@DisplayName("Staff management through the API")
class StaffHttpIntegrationTests {
    private static final String CREATE_STAFF_JSON = """
            {
              "firstName": "Ada",
              "surname": "Lovelace",
              "email": "ada@example.com",
              "hireDate": "2024-04-01",
              "department": "Engineering",
              "managerId": "manager-1",
              "jobRole": "Software Engineer",
              "roleStartDate": "2024-04-01",
              "jobLevel": "Level 2",
              "employmentType": "Permanent"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffMemberRepository repository;

    @BeforeEach
    void clearStaffMembers() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Staff can be created, updated and queried by identity and manager over HTTP")
    void completeStaffManagementWorkflowPersistsChanges() throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_STAFF_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"))
                .andReturn();

        String staffMemberId = JsonPath.read(
                creation.getResponse().getContentAsString(),
                "$.id"
        );
        assertEquals(
                "/api/staff/" + staffMemberId,
                creation.getResponse().getHeader("Location")
        );

        mockMvc.perform(get("/api/staff/{staffMemberId}", staffMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Engineering"));

        mockMvc.perform(patch("/api/staff/{staffMemberId}/department", staffMemberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Finance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Finance"));

        mockMvc.perform(patch("/api/staff/{staffMemberId}/job-role", staffMemberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobRole": "Team Leader",
                                  "roleStartDate": "2026-08-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobRole").value("Team Leader"))
                .andExpect(jsonPath("$.roleStartDate").value("2026-08-01"));

        mockMvc.perform(get("/api/staff/{staffMemberId}", staffMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Finance"))
                .andExpect(jsonPath("$.jobRole").value("Team Leader"));

        mockMvc.perform(get("/api/managers/{managerId}/staff", "manager-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(staffMemberId));

        assertEquals("Finance", repository.findById(staffMemberId).orElseThrow().getDepartment());
        assertEquals("Team Leader", repository.findById(staffMemberId).orElseThrow().getJobRole());
    }

    @Test
    @DisplayName("Duplicate staff email is rejected as 409 without creating another record")
    void duplicateEmailReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_STAFF_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_STAFF_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("STAFF_EMAIL_ALREADY_EXISTS"));

        assertEquals(1, repository.count());
    }
}
