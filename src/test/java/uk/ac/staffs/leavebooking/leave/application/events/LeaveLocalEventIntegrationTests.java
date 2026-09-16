package uk.ac.staffs.leavebooking.leave.application.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.common.events.EventStoreJpa;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("Leave approval and allowance updates")
class LeaveLocalEventIntegrationTests {
    private static final LocalDate BUSINESS_YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate BUSINESS_YEAR_END = LocalDate.of(2027, 3, 31);
    private static final LocalDate LEAVE_START = LocalDate.of(2026, 9, 14);
    private static final LocalDate LEAVE_END = LocalDate.of(2026, 9, 18);

    @Autowired
    private ContextFacade leaveContextFacade;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

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
    }

    @Test
    @DisplayName("Approval atomically deducts allowance and stores submitted and approved events")
    void approvalDeductsAllowanceAndStoresEventHistory() {
        createAllowance(25, BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        String requestId = createRequest();

        leaveContextFacade.approveLeaveRequest(new ApproveLeaveRequestCommand(requestId));

        assertEquals(LeaveStatus.APPROVED, persistedStatus(requestId));
        assertEquals(20, persistedRemainingDays());
        assertEquals(1, countEvents("LeaveRequestSubmittedEvent"));
        assertEquals(1, countEvents("LeaveRequestApprovedEvent"));
        assertEquals(4, eventStoreRepository.count());
    }

    @Test
    @DisplayName("Cancelling approved leave restores its previously deducted allowance")
    void approvedCancellationRestoresAllowance() {
        createAllowance(25, BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        String requestId = createRequest();
        leaveContextFacade.approveLeaveRequest(new ApproveLeaveRequestCommand(requestId));
        assertEquals(20, persistedRemainingDays());

        leaveContextFacade.cancelLeaveRequest(new CancelLeaveRequestCommand(requestId));

        assertEquals(LeaveStatus.CANCELLED, persistedStatus(requestId));
        assertEquals(25, persistedRemainingDays());
        assertEquals(1, countEvents("LeaveRequestCancelledEvent"));
        assertEquals(6, eventStoreRepository.count());
    }

    @Test
    @DisplayName("Cancelling pending leave does not restore allowance")
    void pendingCancellationDoesNotRestoreAllowance() {
        createAllowance(25, BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        String requestId = createRequest();

        leaveContextFacade.cancelLeaveRequest(new CancelLeaveRequestCommand(requestId));

        assertEquals(LeaveStatus.CANCELLED, persistedStatus(requestId));
        assertEquals(25, persistedRemainingDays());
        assertEquals(1, countEvents("LeaveRequestCancelledEvent"));
        assertEquals(4, eventStoreRepository.count());
    }

    @Test
    @DisplayName("HR approval deducts allowance exactly once after referral")
    void hrApprovalDeductsAllowanceOnce() {
        createAllowance(25, BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        String requestId = createRequest();
        leaveContextFacade.referLeaveRequestForHrApproval(
                new ReferLeaveRequestForHrApprovalCommand(requestId)
        );

        leaveContextFacade.approveLeaveRequestByHr(
                new ApproveHrLeaveRequestCommand(requestId)
        );

        assertEquals(LeaveStatus.APPROVED, persistedStatus(requestId));
        assertEquals(20, persistedRemainingDays());
        assertEquals(1, countEvents("LeaveRequestSubmittedEvent"));
        assertEquals(1, countEvents("LeaveRequestReferredForHrApprovalEvent"));
        assertEquals(1, countEvents("LeaveRequestApprovedEvent"));
    }

    @Test
    @DisplayName("Insufficient allowance rolls back approval and its event atomically")
    void insufficientAllowanceRollsBackApproval() {
        createAllowance(4, BUSINESS_YEAR_START, BUSINESS_YEAR_END);
        String requestId = createRequest();

        assertThrows(InvalidLeaveAllowanceException.class, () ->
                leaveContextFacade.approveLeaveRequest(
                        new ApproveLeaveRequestCommand(requestId)
                )
        );

        assertEquals(LeaveStatus.PENDING, persistedStatus(requestId));
        assertEquals(4, persistedRemainingDays());
        assertEquals(0, countEvents("LeaveRequestApprovedEvent"));
        assertEquals(2, eventStoreRepository.count());
    }

    @Test
    @DisplayName("No containing business-year allowance rolls back approval and its event")
    void missingApplicableAllowanceRollsBackApproval() {
        createAllowance(
                25,
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2026, 3, 31)
        );
        String requestId = createRequest();

        assertThrows(LeaveAllowanceNotFoundException.class, () ->
                leaveContextFacade.approveLeaveRequest(
                        new ApproveLeaveRequestCommand(requestId)
                )
        );

        assertEquals(LeaveStatus.PENDING, persistedStatus(requestId));
        assertEquals(25, persistedRemainingDays());
        assertEquals(0, countEvents("LeaveRequestApprovedEvent"));
        assertEquals(2, eventStoreRepository.count());
    }

    private void createAllowance(
            int entitlement,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        leaveContextFacade.createLeaveAllowance(new CreateLeaveAllowanceCommand(
                "staff-1",
                businessYearStart,
                businessYearEnd,
                entitlement
        ));
    }

    private String createRequest() {
        return leaveContextFacade.requestLeave(new RequestLeaveCommand(
                "staff-1",
                LEAVE_START,
                LEAVE_END,
                "Annual leave",
                LeaveType.ANNUAL
        ));
    }

    private LeaveStatus persistedStatus(String requestId) {
        return leaveRequestRepository.findById(requestId).orElseThrow().getStatus();
    }

    private int persistedRemainingDays() {
        return leaveAllowanceRepository.findByStaffMemberId("staff-1")
                .getFirst()
                .getRemainingDays();
    }

    private long countEvents(String eventType) {
        return StreamSupport.stream(eventStoreRepository.findAll().spliterator(), false)
                .map(EventStoreJpa::getEventType)
                .filter(eventType::equals)
                .count();
    }
}
