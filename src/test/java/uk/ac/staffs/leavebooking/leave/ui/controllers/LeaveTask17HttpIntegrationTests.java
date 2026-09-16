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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uk.ac.staffs.leavebooking.common.events.EventStoreJpa;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.common.events.StatusOfMessageDelivery;
import uk.ac.staffs.leavebooking.identity.security.Role;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceCarryOverRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestEventStreamRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;
import uk.ac.staffs.leavebooking.reporting.infrastructure.repositories.LeaveReportingProjectionRepository;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "rabbitmq.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("Leave rules through the API")
class LeaveTask17HttpIntegrationTests {
    private static final String STAFF_ID = "staff-task17";
    private static final String MANAGER_ID = "manager-task17";
    private static final LocalDate YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 3, 31);

    @Autowired private MockMvc mockMvc;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveAllowanceRepository leaveAllowanceRepository;
    @Autowired private LeaveAllowanceCarryOverRepository carryOverRepository;
    @Autowired private PublicHolidayRepository publicHolidayRepository;
    @Autowired private LeaveRequestEventStreamRepository eventStreamRepository;
    @Autowired private LeaveRequestEventStore leaveRequestEventStore;
    @Autowired private EventStoreRepository eventStoreRepository;
    @Autowired private StaffMemberRepository staffMemberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private LeaveReportingProjectionRepository reportingRepository;

    @BeforeEach
    void setUp() {
        reportingRepository.deleteAll();
        notificationRepository.deleteAll();
        eventStoreRepository.deleteAll();
        eventStreamRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        carryOverRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
        publicHolidayRepository.deleteAll();
        staffMemberRepository.deleteAll();

        staffMemberRepository.save(staff(MANAGER_ID, "manager-task17@example.com", "director-1"));
        staffMemberRepository.save(staff(STAFF_ID, "staff-task17@example.com", MANAGER_ID));
        leaveAllowanceRepository.save(allowance(
                "allowance-task17", YEAR_START, YEAR_END,
                new BigDecimal("25.0"), BigDecimal.ZERO, new BigDecimal("25.0")
        ));
    }

    @Test
    @DisplayName("Annual leave excludes configured holidays and cancellation restores its stored charge")
    void annualLeaveUsesWorkingDaysAndRestoresHistoricalCharge() throws Exception {
        mockMvc.perform(post("/api/public-holidays")
                        .with(as(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date":"2026-08-31",
                                  "name":"Summer bank holiday"
                                }
                                """))
                .andExpect(status().isCreated());

        String requestId = submitLeave("""
                {
                  "startDate":"2026-08-28",
                  "endDate":"2026-08-31",
                  "reason":"Working-day calculation evidence",
                  "leaveType":"ANNUAL",
                  "dayPortion":"FULL_DAY"
                }
                """, "1.0", "PENDING");

        mockMvc.perform(patch("/api/leave-requests/{requestId}/approve", requestId)
                        .with(as(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Workload covered\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionComment").value("Workload covered"));

        assertDecimal("24.0", storedAllowance().getRemainingLeaveDays());

        mockMvc.perform(delete("/api/public-holidays/{date}", "2026-08-31")
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/leave-requests/{requestId}/cancel", requestId)
                        .with(as(Role.STAFF, STAFF_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.chargedLeaveDays").value(1.0));

        assertDecimal("25.0", storedAllowance().getRemainingLeaveDays());
        assertEquals(3, eventStreamRepository
                .findByAggregateIdOrderBySequenceNumber(requestId).size());

        var replayed = leaveRequestEventStore.load(requestId);
        assertEquals(LeaveStatus.CANCELLED, replayed.status());
        assertDecimal("1.0", replayed.chargedLeaveDays().value());
        assertEquals("Workload covered", replayed.decisionComment());

        var projection = leaveRequestRepository.findById(requestId).orElseThrow();
        assertEquals(replayed.status(), projection.getStatus());
        assertDecimal(replayed.chargedLeaveDays().value(), projection.getChargedLeaveDays());

        List<EventStoreJpa> events = storedEventsFor(requestId);
        assertEquals(6, events.size());
        assertEquals(3, events.stream()
                .filter(event -> event.getStatus() == StatusOfMessageDelivery.LOCAL)
                .count());
        assertEquals(3, events.stream()
                .filter(event -> event.getStatus() == StatusOfMessageDelivery.PENDING)
                .count());
    }

    @Test
    @DisplayName("Complementary half days are allowed while a duplicate session is rejected atomically")
    void halfDayOverlapAndAllowanceRulesWorkTogether() throws Exception {
        String morningId = submitLeave(halfDayJson("MORNING"), "0.5", "PENDING");
        String afternoonId = submitLeave(halfDayJson("AFTERNOON"), "0.5", "PENDING");

        mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                        .with(as(Role.STAFF, STAFF_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(halfDayJson("MORNING")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVERLAPPING_LEAVE_REQUEST"));

        assertEquals(2, leaveRequestRepository.count());
        assertEquals(2, eventStreamRepository.count());
        assertEquals(4, eventStoreRepository.count());

        approve(morningId);
        approve(afternoonId);

        assertDecimal("24.0", storedAllowance().getRemainingLeaveDays());
        assertEquals(4, eventStreamRepository.count());
        assertEquals(8, eventStoreRepository.count());
    }

    @Test
    @DisplayName("Sickness is recorded without allowance use and remains private in manager views")
    void sicknessPreservesAllowanceAndManagerPrivacy() throws Exception {
        String reason = "Private medical details for the owner only";
        String requestId = submitLeave("""
                {
                  "startDate":"2026-10-12",
                  "endDate":"2026-10-13",
                  "reason":"%s",
                  "leaveType":"SICK",
                  "dayPortion":"FULL_DAY"
                }
                """.formatted(reason), "0.0", "RECORDED");

        assertDecimal("25.0", storedAllowance().getRemainingLeaveDays());

        mockMvc.perform(get("/api/leave-requests/{requestId}", requestId)
                        .with(as(Role.MANAGER, MANAGER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/leave-requests/{requestId}", requestId)
                        .with(as(Role.STAFF, STAFF_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value(reason));

        mockMvc.perform(get("/api/managers/{managerId}/team-leave-calendar", MANAGER_ID)
                        .with(as(Role.MANAGER, MANAGER_ID))
                        .queryParam("startDate", "2026-10-01")
                        .queryParam("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaveRequestId").value(requestId))
                .andExpect(jsonPath("$[0].leaveType").value("SICK"))
                .andExpect(jsonPath("$[0].status").value("RECORDED"))
                .andExpect(jsonPath("$[0].reason").doesNotExist());

        mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", MANAGER_ID)
                        .with(as(Role.MANAGER, MANAGER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        EventStoreJpa remoteEvent = storedEventsFor(requestId).stream()
                .filter(event -> event.getEventType().equals("SickLeaveRecordedIntegrationEvent"))
                .findFirst()
                .orElseThrow();
        assertFalse(remoteEvent.getEventBody().contains(reason));
        assertFalse(remoteEvent.getEventBody().contains("\"reason\""));

        var internalStream = eventStreamRepository
                .findByAggregateIdOrderBySequenceNumber(requestId);
        assertEquals(1, internalStream.size());
        assertTrue(internalStream.getFirst().getEventBody().contains(reason));

        mockMvc.perform(patch("/api/leave-requests/{requestId}/cancel", requestId)
                        .with(as(Role.STAFF, STAFF_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertDecimal("25.0", storedAllowance().getRemainingLeaveDays());
    }

    @Test
    @DisplayName("Fractional carry-over is ADMIN-only, exact and idempotent")
    void carryOverSupportsFractionalDaysWithoutLegacyJsonFailure() throws Exception {
        leaveAllowanceRepository.deleteAll();
        leaveAllowanceRepository.save(allowance(
                "allowance-source", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31),
                new BigDecimal("25.0"), BigDecimal.ZERO, new BigDecimal("3.5")
        ));
        leaveAllowanceRepository.save(allowance(
                "allowance-target", YEAR_START, YEAR_END,
                new BigDecimal("25.0"), BigDecimal.ZERO, new BigDecimal("25.0")
        ));
        String body = """
                {
                  "sourceYearStart":"2025-04-01",
                  "sourceYearEnd":"2026-03-31",
                  "targetYearStart":"2026-04-01",
                  "targetYearEnd":"2027-03-31"
                }
                """;

        mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances/carry-over", STAFF_ID)
                        .with(as(Role.STAFF, STAFF_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        carryOver(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseEntitlement").value(25.0))
                .andExpect(jsonPath("$.carriedOverDays").value(3.5))
                .andExpect(jsonPath("$.totalEntitlement").value(28.5))
                .andExpect(jsonPath("$.annualEntitlement").value(28.5))
                .andExpect(jsonPath("$.remainingDays").value(28.5));

        carryOver(body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carriedOverDays").value(3.5))
                .andExpect(jsonPath("$.totalEntitlement").value(28.5));

        var target = leaveAllowanceRepository.findById("allowance-target").orElseThrow();
        assertDecimal("3.5", target.getCarriedOverDays());
        assertDecimal("28.5", target.getRemainingLeaveDays());
        assertEquals(1, carryOverRepository.count());

        mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances/carry-over", STAFF_ID)
                        .with(as(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceYearStart":"2025-04-01",
                                  "sourceYearEnd":"2026-03-31",
                                  "targetYearStart":"2027-04-01",
                                  "targetYearEnd":"2028-03-31"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_CARRY_OVER"));
    }

    private String submitLeave(String json, String chargedDays, String expectedStatus)
            throws Exception {
        MvcResult result = mockMvc.perform(post(
                                "/api/staff/{staffMemberId}/leave-requests", STAFF_ID
                        )
                        .with(as(Role.STAFF, STAFF_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargedLeaveDays").value(Double.parseDouble(chargedDays)))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions carryOver(String body)
            throws Exception {
        return mockMvc.perform(post(
                                "/api/staff/{staffMemberId}/leave-allowances/carry-over", STAFF_ID
                        )
                        .with(as(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private void approve(String requestId) throws Exception {
        mockMvc.perform(patch("/api/leave-requests/{requestId}/approve", requestId)
                        .with(as(Role.ADMIN, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private String halfDayJson(String portion) {
        return """
                {
                  "startDate":"2026-09-15",
                  "endDate":"2026-09-15",
                  "reason":"Half-day appointment",
                  "leaveType":"ANNUAL",
                  "dayPortion":"%s"
                }
                """.formatted(portion);
    }

    private LeaveAllowanceJpa storedAllowance() {
        return leaveAllowanceRepository.findById("allowance-task17").orElseThrow();
    }

    private LeaveAllowanceJpa allowance(
            String id,
            LocalDate start,
            LocalDate end,
            BigDecimal baseEntitlement,
            BigDecimal carriedOverDays,
            BigDecimal remainingDays
    ) {
        return new LeaveAllowanceJpa(
                id, STAFF_ID, "Ada", "Lovelace", MANAGER_ID,
                start, end, baseEntitlement, carriedOverDays, remainingDays, null
        );
    }

    private StaffMemberJpa staff(String id, String email, String managerId) {
        return new StaffMemberJpa(
                id, "Ada", "Lovelace", email, LocalDate.of(2025, 1, 1),
                "Engineering", managerId, "Developer", LocalDate.of(2025, 1, 1),
                "Senior", "Permanent", EmploymentStatus.ACTIVE
        );
    }

    private List<EventStoreJpa> storedEventsFor(String leaveRequestId) {
        return StreamSupport.stream(eventStoreRepository.findAll().spliterator(), false)
                .filter(event -> event.getEventBody().contains(leaveRequestId))
                .toList();
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

    private void assertDecimal(String expected, BigDecimal actual) {
        assertDecimal(new BigDecimal(expected), actual);
    }

    private void assertDecimal(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
