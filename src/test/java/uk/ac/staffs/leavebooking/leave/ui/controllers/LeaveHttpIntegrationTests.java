package uk.ac.staffs.leavebooking.leave.ui.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockFirebaseUser
@DisplayName("Leave requests through the API")
class LeaveHttpIntegrationTests {
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
    @DisplayName("A leave request can be created, queried, approved and queried again over HTTP")
    void requestApprovalWorkflowPersistsAcrossHttpCalls() throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", "staff-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId").value("manager-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String requestId = JsonPath.read(
                creation.getResponse().getContentAsString(),
                "$.id"
        );
        assertEquals(
                "/api/leave-requests/" + requestId,
                creation.getResponse().getHeader("Location")
        );

        mockMvc.perform(get("/api/leave-requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(patch("/api/leave-requests/{requestId}/approve", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/leave-requests/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(
                "APPROVED",
                leaveRequestRepository.findById(requestId).orElseThrow().getStatus().name()
        );
    }

    @Test
    @DisplayName("A reversed leave period returns 400 over HTTP and is not persisted")
    void reversedLeavePeriodReturnsBadRequestWithoutPersistence() throws Exception {
        String reversedPeriodJson = REQUEST_JSON.replace("2026-09-14", "2026-09-19");

        mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", "staff-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reversedPeriodJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "Leave end date cannot be before the start date"
                ));

        assertEquals(0, leaveRequestRepository.count());
    }
}
