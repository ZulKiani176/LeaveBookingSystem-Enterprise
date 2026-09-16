package uk.ac.staffs.leavebooking.staff.ui.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.GlobalExceptionHandler;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import uk.ac.staffs.leavebooking.identity.security.ControllerSecurityTestConfiguration;
import uk.ac.staffs.leavebooking.staff.ContextFacade;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.domain.exceptions.InvalidStaffMemberException;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffMemberController.class)
@Import({GlobalExceptionHandler.class, ControllerSecurityTestConfiguration.class})
@WithMockFirebaseUser
@DisplayName("Staff management API")
class StaffMemberControllerTests {
    private static final String STAFF_ID = "staff-1";
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);
    private static final String VALID_CREATE_JSON = """
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

    @MockitoBean
    private ContextFacade contextFacade;

    @Test
    @DisplayName("A valid staff creation returns 201, Location and the active staff DTO")
    void validStaffCreationReturnsCreatedResource() throws Exception {
        when(contextFacade.createStaffMember(any(CreateStaffMemberDetails.class)))
                .thenReturn(STAFF_ID);
        when(contextFacade.findStaffMemberById(STAFF_ID)).thenReturn(staffDto());

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/staff/staff-1"))
                .andExpect(jsonPath("$.id").value(STAFF_ID))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.surname").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.jobRole").value("Software Engineer"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));

        var captor = org.mockito.ArgumentCaptor.forClass(CreateStaffMemberDetails.class);
        verify(contextFacade).createStaffMember(captor.capture());
        assertEquals("Software Engineer", captor.getValue().jobRole());
        assertEquals(HIRE_DATE, captor.getValue().hireDate());
    }

    @Test
    @DisplayName("An invalid email returns 400")
    void invalidEmailIsRejected() throws Exception {
        String body = VALID_CREATE_JSON.replace("ada@example.com", "not-an-email");

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("email: Email must be valid"));

        verifyNoInteractions(contextFacade);
    }

    @Test
    @DisplayName("A blank required field returns 400")
    void blankRequiredFieldIsRejected() throws Exception {
        String body = VALID_CREATE_JSON.replace("\"firstName\": \"Ada\"", "\"firstName\": \"   \"");

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("firstName: First name is required"));

        verifyNoInteractions(contextFacade);
    }

    @Test
    @DisplayName("Missing hire and role dates return 400")
    void missingDatesAreRejected() throws Exception {
        String body = """
                {
                  "firstName": "Ada",
                  "surname": "Lovelace",
                  "email": "ada@example.com",
                  "department": "Engineering",
                  "managerId": "manager-1",
                  "jobRole": "Software Engineer",
                  "jobLevel": "Level 2",
                  "employmentType": "Permanent"
                }
                """;

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("hireDate: Hire date is required")))
                .andExpect(jsonPath("$.message", containsString(
                        "roleStartDate: Job role start date is required"
                )));

        verifyNoInteractions(contextFacade);
    }

    @Test
    @DisplayName("Duplicate email returns the shared 409 response")
    void duplicateEmailReturnsConflict() throws Exception {
        when(contextFacade.createStaffMember(any(CreateStaffMemberDetails.class)))
                .thenThrow(new StaffEmailAlreadyExistsException("ada@example.com"));

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("STAFF_EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(
                        "A staff member with email ada@example.com already exists"
                ))
                .andExpect(jsonPath("$.path").value("/api/staff"));
    }

    @Test
    @DisplayName("Finding an existing staff member returns its DTO")
    void findExistingStaffMemberReturnsDto() throws Exception {
        when(contextFacade.findStaffMemberById(STAFF_ID)).thenReturn(staffDto());

        mockMvc.perform(get("/api/staff/{staffMemberId}", STAFF_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STAFF_ID))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("Finding a missing staff member returns the shared 404 response")
    void findMissingStaffMemberReturnsNotFound() throws Exception {
        when(contextFacade.findStaffMemberById("missing"))
                .thenThrow(new StaffMemberNotFoundException("missing"));

        mockMvc.perform(get("/api/staff/{staffMemberId}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("STAFF_MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Staff member not found: missing"))
                .andExpect(jsonPath("$.path").value("/api/staff/missing"));
    }

    @Test
    @DisplayName("Manager lookup returns assigned staff as a JSON array")
    void managerStaffQueryReturnsList() throws Exception {
        when(contextFacade.findStaffByManagerId("manager-1")).thenReturn(List.of(staffDto()));

        mockMvc.perform(get("/api/managers/{managerId}/staff", "manager-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(STAFF_ID))
                .andExpect(jsonPath("$[0].managerId").value("manager-1"));
    }

    @Test
    @DisplayName("A manager with no assigned staff receives an empty array")
    void managerWithNoStaffReturnsEmptyArray() throws Exception {
        when(contextFacade.findStaffByManagerId("manager-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/managers/{managerId}/staff", "manager-1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Department filtering returns matching staff")
    void departmentFilterReturnsMatchingStaff() throws Exception {
        when(contextFacade.findStaffByDepartment("Engineering")).thenReturn(List.of(staffDto()));

        mockMvc.perform(get("/api/staff").queryParam("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Engineering"));

    }

    @Test
    @DisplayName("Department filtering requires the department query parameter")
    void missingDepartmentFilterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    }

    @Test
    @DisplayName("A valid department patch returns the updated staff member")
    void validDepartmentPatchReturnsUpdatedStaff() throws Exception {
        StaffMemberDTO updated = staffDto("Finance", "Software Engineer", HIRE_DATE);
        when(contextFacade.findStaffMemberById(STAFF_ID)).thenReturn(updated);

        mockMvc.perform(patch("/api/staff/{staffMemberId}/department", STAFF_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Finance\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Finance"));

        verify(contextFacade).changeDepartment(STAFF_ID, "Finance");
    }

    @Test
    @DisplayName("A blank department patch returns 400")
    void invalidDepartmentPatchIsRejected() throws Exception {
        mockMvc.perform(patch("/api/staff/{staffMemberId}/department", STAFF_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(contextFacade);
    }

    @Test
    @DisplayName("A valid job-role patch returns the updated placement")
    void validJobRolePatchReturnsUpdatedStaff() throws Exception {
        LocalDate newStartDate = LocalDate.of(2026, 8, 1);
        StaffMemberDTO updated = staffDto("Engineering", "Team Leader", newStartDate);
        when(contextFacade.findStaffMemberById(STAFF_ID)).thenReturn(updated);

        mockMvc.perform(patch("/api/staff/{staffMemberId}/job-role", STAFF_ID)
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

        verify(contextFacade).changeJobRole(STAFF_ID, "Team Leader", newStartDate);
    }

    @Test
    @DisplayName("A role date before hire returns the shared domain 409 response")
    void invalidJobRoleDateReturnsConflict() throws Exception {
        doThrow(new InvalidStaffMemberException(StaffMember.ROLE_START_BEFORE_HIRE))
                .when(contextFacade)
                .changeJobRole(STAFF_ID, "Team Leader", LocalDate.of(2023, 8, 1));

        mockMvc.perform(patch("/api/staff/{staffMemberId}/job-role", STAFF_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobRole": "Team Leader",
                                  "roleStartDate": "2023-08-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_STAFF_MEMBER_STATE"))
                .andExpect(jsonPath("$.message").value(StaffMember.ROLE_START_BEFORE_HIRE))
                .andExpect(jsonPath("$.path").value("/api/staff/staff-1/job-role"));
    }

    private StaffMemberDTO staffDto() {
        return staffDto("Engineering", "Software Engineer", HIRE_DATE);
    }

    private StaffMemberDTO staffDto(
            String department,
            String jobRole,
            LocalDate roleStartDate
    ) {
        return new StaffMemberDTO(
                STAFF_ID,
                "Ada",
                "Lovelace",
                "ada@example.com",
                HIRE_DATE,
                department,
                "manager-1",
                jobRole,
                roleStartDate,
                "Level 2",
                "Permanent",
                EmploymentStatus.ACTIVE
        );
    }
}
