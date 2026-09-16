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
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockFirebaseUser
@DisplayName("HR review through the API")
class LeaveHrHttpIntegrationTests {
    private static final String REQUEST_JSON = """
            {
              "startDate": "2026-09-14",
              "endDate": "2026-09-18",
              "reason": "Annual leave",
              "leaveType": "ANNUAL"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @BeforeEach
    void prepareStaffMember() {
        eventStoreRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
        staffMemberRepository.deleteAll();
        staffMemberRepository.save(new StaffMemberJpa(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(2025, 1, 1),
                "Engineering",
                "manager-1",
                "Developer",
                LocalDate.of(2025, 1, 1),
                "Senior",
                "Permanent",
                EmploymentStatus.ACTIVE
        ));
        leaveAllowanceRepository.save(new LeaveAllowanceJpa(
                "allowance-1",
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                25,
                25
        ));
    }

    @Test
    @DisplayName("An HR-referred request appears in the queue and persists final HR approval")
    void hrApprovalWorkflowPersistsAcrossHttpCalls() throws Exception {
        String requestId = createLeaveRequest();

        mockMvc.perform(patch("/api/leave-requests/{requestId}/refer-to-hr", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_HR_APPROVAL"));

        mockMvc.perform(get("/api/leave-requests/hr/outstanding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId))
                .andExpect(jsonPath("$[0].status").value("PENDING_HR_APPROVAL"));

        mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-approve", requestId)
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("firebase-hr")
                                        .claim("role", "ROLE_HR")
                                        .claim("staffId", "hr-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_HR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/leave-requests/hr/outstanding"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/leave-requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(
                LeaveStatus.APPROVED,
                leaveRequestRepository.findById(requestId).orElseThrow().getStatus()
        );
    }

    @Test
    @DisplayName("HR rejection persists and removes the request from the HR queue")
    void hrRejectionWorkflowPersistsAcrossHttpCalls() throws Exception {
        String requestId = createLeaveRequest();
        mockMvc.perform(patch("/api/leave-requests/{requestId}/refer-to-hr", requestId))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-reject", requestId)
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("firebase-hr")
                                        .claim("role", "ROLE_HR")
                                        .claim("staffId", "hr-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_HR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/leave-requests/hr/outstanding"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        assertEquals(
                LeaveStatus.REJECTED,
                leaveRequestRepository.findById(requestId).orElseThrow().getStatus()
        );
    }

    private String createLeaveRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/staff/{staffMemberId}/leave-requests", "staff-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REQUEST_JSON)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
