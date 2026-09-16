package uk.ac.staffs.leavebooking.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestEventStreamRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DisplayName("Leave public operations with the database")
class ContextFacadeIntegrationTests {
    @Autowired
    private ContextFacade facade;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @Autowired
    private LeaveRequestEventStreamRepository leaveRequestEventStreamRepository;

    @Autowired
    private LeaveRequestEventStore leaveRequestEventStore;

    @BeforeEach
    void preparePersistence() {
        eventStoreRepository.deleteAll();
        leaveRequestEventStreamRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
        staffMemberRepository.deleteAll();
        staffMemberRepository.save(staffMemberJpa());
        leaveAllowanceRepository.save(allowanceJpa());
    }

    @Test
    @DisplayName("The facade approves a persisted pending request through the domain command path")
    void facadeApprovesPersistedPendingRequest() {
        persistPendingEventSourcedRequest();

        facade.approveLeaveRequest(new ApproveLeaveRequestCommand("request-1"));

        LeaveRequestJpa restored = leaveRequestRepository.findById("request-1").orElseThrow();
        assertEquals(LeaveStatus.APPROVED, restored.getStatus());
    }

    @Test
    @DisplayName("The facade queries persisted request data through the DTO read path")
    void facadeQueriesPersistedRequestAsDto() {
        leaveRequestRepository.save(requestJpa(LeaveStatus.APPROVED));

        LeaveRequestDTO result = facade.findLeaveRequestById("request-1");

        assertEquals("request-1", result.id());
        assertEquals("staff-1", result.staffMemberId());
        assertEquals("manager-1", result.managerId());
        assertEquals(LocalDate.of(2026, 8, 10), result.startDate());
        assertEquals(LocalDate.of(2026, 8, 14), result.endDate());
        assertEquals("Summer holiday", result.reason());
        assertEquals(LeaveType.ANNUAL, result.leaveType());
        assertEquals(LeaveStatus.APPROVED, result.status());
    }

    private LeaveRequestJpa requestJpa(LeaveStatus status) {
        return new LeaveRequestJpa(
                "request-1",
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL,
                status
        );
    }

    private void persistPendingEventSourcedRequest() {
        LeaveRequest request = LeaveRequest.create(
                Identity.of("request-1"),
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14)),
                "Summer holiday",
                LeaveType.ANNUAL
        );
        leaveRequestEventStore.append(request);
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.map(request));
        request.clearDomainEvents();
    }

    private LeaveAllowanceJpa allowanceJpa() {
        return new LeaveAllowanceJpa(
                "allowance-1",
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                25,
                25
        );
    }

    private StaffMemberJpa staffMemberJpa() {
        return new StaffMemberJpa(
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
        );
    }
}
