package uk.ac.staffs.leavebooking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import uk.ac.staffs.leavebooking.common.events.integration.*;
import uk.ac.staffs.leavebooking.hrsync.application.HrAbsenceSyncService;
import uk.ac.staffs.leavebooking.hrsync.domain.HrAbsenceSyncAction;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.messaging.HrAbsenceSyncRabbitListener;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.repositories.HrAbsenceSyncRepository;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.exceptions.OverlappingLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.domain.*;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveSubmissionLock;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.ui.commands.*;
import uk.ac.staffs.leavebooking.reporting.application.LeaveReportingService;
import uk.ac.staffs.leavebooking.reporting.infrastructure.messaging.LeaveReportingRabbitListener;
import uk.ac.staffs.leavebooking.reporting.infrastructure.repositories.LeaveReportingProjectionRepository;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "firebase.enabled=false", "rabbitmq.enabled=false",
        "spring.datasource.generate-unique-name=true"
})
@AutoConfigureMockMvc
@DisplayName("Assessment fixes with real application transactions")
class AssessmentFixesIntegrationTests {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 14);
    @Autowired private ContextFacade leave;
    @Autowired private uk.ac.staffs.leavebooking.staff.ContextFacade staff;
    @Autowired private LeaveRequestRepository requests;
    @Autowired private LeaveAllowanceRepository allowances;
    @Autowired private EventStoreRepository events;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LeaveReportingService reporting;
    @Autowired private LeaveReportingProjectionRepository projections;
    @Autowired private HrAbsenceSyncService hrSync;
    @Autowired private HrAbsenceSyncRepository hrRecords;
    @Autowired private MockMvc mockMvc;
    @MockitoSpyBean private LeaveSubmissionLock submissionLock;

    @Test
    @DisplayName("An HR referral updates the persisted report without waiting for an HR decision")
    void referralUpdatesReport() {
        String requestId = request(createStaff(), LeaveType.ANNUAL);
        var listener = new LeaveReportingRabbitListener(reporting);
        listener.receive(event(LeaveRequestSubmittedIntegrationEvent.class, requestId));

        leave.referLeaveRequestForHrApproval(new ReferLeaveRequestForHrApprovalCommand(requestId));
        var referral = event(LeaveRequestReferredForHrApprovalIntegrationEvent.class, requestId);
        listener.receive(referral);

        var stored = projections.findById(requestId).orElseThrow();
        assertEquals(IntegrationLeaveStatus.PENDING_HR_APPROVAL, stored.getStatus());
        assertEquals(referral.id(), stored.getSourceEventId());
        assertEquals(DATE, stored.getStartDate());
    }

    @Test
    @DisplayName("A referral delivered before submission still creates the correct report")
    void outOfOrderReferralCreatesReport() {
        String requestId = request(createStaff(), LeaveType.ANNUAL);
        leave.referLeaveRequestForHrApproval(new ReferLeaveRequestForHrApprovalCommand(requestId));
        var listener = new LeaveReportingRabbitListener(reporting);
        var referral = event(LeaveRequestReferredForHrApprovalIntegrationEvent.class, requestId);

        listener.receive(referral);
        listener.receive(event(LeaveRequestSubmittedIntegrationEvent.class, requestId));
        listener.receive(referral);

        var stored = projections.findById(requestId).orElseThrow();
        assertEquals(IntegrationLeaveStatus.PENDING_HR_APPROVAL, stored.getStatus());
        assertEquals(referral.id(), stored.getSourceEventId());
    }

    @Test
    @DisplayName("Cancelling recorded sickness creates one matching HR correction")
    void sickCancellationReachesHr() {
        String staffId = createStaff();
        String requestId = request(staffId, LeaveType.SICK);
        var listener = new HrAbsenceSyncRabbitListener(hrSync);
        listener.receive(event(SickLeaveRecordedIntegrationEvent.class, requestId));
        leave.cancelLeaveRequest(new CancelLeaveRequestCommand(requestId));
        var cancellation = event(LeaveRequestCancelledIntegrationEvent.class, requestId);

        listener.receive(cancellation);
        listener.receive(cancellation);

        var records = hrRecords.findByStaffMemberIdOrderBySourceEventId(staffId);
        assertEquals(2, records.size());
        var correction = records.getLast();
        assertEquals(HrAbsenceSyncAction.SICK_LEAVE_CANCELLED, correction.getSyncAction());
        assertEquals(IntegrationLeaveStatus.CANCELLED, correction.getStatus());
        assertEquals(0, correction.getChargedLeaveDays().compareTo(BigDecimal.ZERO));
        assertEquals(cancellation.id(), correction.getSourceEventId());
    }

    @Test
    @DisplayName("Simultaneous overlapping submissions cannot both commit")
    void concurrentOverlapIsRejected() throws Exception {
        String staffId = createStaff();
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondAttempting = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.incrementAndGet() == 1) {
                invocation.callRealMethod();
                firstLocked.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out releasing the first submission");
                }
            } else {
                secondAttempting.countDown();
                invocation.callRealMethod();
            }
            return null;
        }).when(AopTestUtils.<LeaveSubmissionLock>getUltimateTargetObject(submissionLock)).acquire(eq(staffId));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> request(staffId, LeaveType.ANNUAL));
            try {
                assertTrue(firstLocked.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> {
                    try {
                        request(staffId, LeaveType.ANNUAL);
                        return false;
                    } catch (OverlappingLeaveRequestException expected) {
                        return true;
                    }
                });
                assertTrue(secondAttempting.await(10, TimeUnit.SECONDS));
                assertFalse(second.isDone());
                releaseFirst.countDown();
                String requestId = first.get(10, TimeUnit.SECONDS);
                assertTrue(second.get(10, TimeUnit.SECONDS));
                assertEquals(1, requests.findByStaffMemberId(staffId).size());
                assertEquals(2, eventCount(requestId));
            } finally {
                releaseFirst.countDown();
            }
        }
    }

    @Test
    @DisplayName("Oversized entitlement returns a clear client error and creates no allowance")
    void oversizedAllowanceReturnsBadRequest() throws Exception {
        String staffId = createStaff();

        mockMvc.perform(post("/api/staff/" + staffId + "/leave-allowances")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content("""
                                {"businessYearStart":"2026-04-01","businessYearEnd":"2027-03-31",
                                 "annualEntitlement":10000.0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(LeaveDays.VALUE_EXCEEDS_CAPACITY));

        assertTrue(allowances.findByStaffMemberId(staffId).isEmpty());
    }

    @Test
    @DisplayName("The largest supported allowance survives a database round trip")
    void largestAllowancePersists() {
        String staffId = createStaff();
        leave.createLeaveAllowance(new CreateLeaveAllowanceCommand(staffId,
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), new BigDecimal("9999.5")));

        assertEquals(new BigDecimal("9999.5"),
                allowances.findByStaffMemberId(staffId).getFirst().getRemainingLeaveDays());
    }

    private String createStaff() {
        return staff.createStaffMember(new CreateStaffMemberDetails(
                "Audit", "Example", UUID.randomUUID() + "@example.test", DATE.minusYears(1),
                "Engineering", "manager-1", "Developer", DATE.minusYears(1), "Senior", "Permanent"));
    }

    private String request(String staffId, LeaveType type) {
        return leave.requestLeave(new RequestLeaveCommand(staffId, DATE, DATE, "Private reason", type));
    }

    private <T extends RemoteEvent> T event(Class<T> type, String requestId) {
        return StreamSupport.stream(events.findAll().spliterator(), false)
                .filter(row -> row.getEventType().equals(type.getSimpleName()))
                .filter(row -> objectMapper.readTree(row.getEventBody())
                        .get("leaveRequestId").asText().equals(requestId))
                .map(row -> type.cast(objectMapper.readValue(row.getEventBody(), type).withId(row.getId())))
                .findFirst().orElseThrow();
    }

    private long eventCount(String requestId) {
        return StreamSupport.stream(events.findAll().spliterator(), false)
                .filter(row -> objectMapper.readTree(row.getEventBody())
                        .get("leaveRequestId").asText().equals(requestId)).count();
    }
}
