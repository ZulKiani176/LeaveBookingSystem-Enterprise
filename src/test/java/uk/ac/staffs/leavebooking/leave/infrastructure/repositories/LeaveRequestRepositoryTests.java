package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestJpaToDomainMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@DisplayName("Leave Request Repository")
class LeaveRequestRepositoryTests {
    @Autowired
    private LeaveRequestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("A leave request can be saved and reloaded by identity")
    void saveAndFindById() {
        repository.save(request("request-1", "staff-1", LeaveStatus.PENDING));
        flushAndClear();

        LeaveRequestJpa restored = repository.findById("request-1").orElseThrow();

        assertEquals("request-1", restored.getId());
        assertEquals("staff-1", restored.getStaffMemberId());
        assertEquals("manager-1", restored.getManagerId());
        assertEquals(LocalDate.of(2026, 11, 2), restored.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 6), restored.getEndDate());
        assertEquals("Family holiday", restored.getReason());
        assertEquals(LeaveType.ANNUAL, restored.getLeaveType());
        assertEquals(LeaveStatus.PENDING, restored.getStatus());
    }

    @Test
    @DisplayName("Leave requests can be found by staff member identity")
    void findByStaffMemberId() {
        repository.save(request("request-1", "staff-1", LeaveStatus.PENDING));
        repository.save(request("request-2", "staff-1", LeaveStatus.APPROVED));
        repository.save(request("request-3", "staff-2", LeaveStatus.PENDING));
        flushAndClear();

        List<LeaveRequestJpa> results = repository.findByStaffMemberId("staff-1");

        Set<String> ids = results.stream().map(LeaveRequestJpa::getId).collect(Collectors.toSet());
        assertEquals(Set.of("request-1", "request-2"), ids);
    }

    @Test
    @DisplayName("Leave requests can be found by staff member identity and status")
    void findByStaffMemberIdAndStatus() {
        repository.save(request("request-1", "staff-1", LeaveStatus.PENDING));
        repository.save(request("request-2", "staff-1", LeaveStatus.APPROVED));
        repository.save(request("request-3", "staff-2", LeaveStatus.APPROVED));
        flushAndClear();

        List<LeaveRequestJpa> results = repository.findByStaffMemberIdAndStatus(
                "staff-1",
                LeaveStatus.APPROVED
        );

        assertEquals(1, results.size());
        assertEquals("request-2", results.getFirst().getId());
    }

    @Test
    @DisplayName("Leave requests can be found by status across staff members")
    void findByStatus() {
        repository.save(request("request-1", "staff-1", LeaveStatus.PENDING));
        repository.save(request("request-2", "staff-1", LeaveStatus.APPROVED));
        repository.save(request("request-3", "staff-2", LeaveStatus.PENDING));
        flushAndClear();

        List<LeaveRequestJpa> results = repository.findByStatus(LeaveStatus.PENDING);

        Set<String> ids = results.stream().map(LeaveRequestJpa::getId).collect(Collectors.toSet());
        assertEquals(Set.of("request-1", "request-3"), ids);
    }

    @Test
    @DisplayName("HR outstanding lookup returns only requests pending HR approval")
    void findByPendingHrApprovalStatus() {
        repository.save(request("request-1", "staff-1", LeaveStatus.PENDING_HR_APPROVAL));
        repository.save(request("request-2", "staff-2", LeaveStatus.PENDING));
        repository.save(request("request-3", "staff-3", LeaveStatus.APPROVED));
        flushAndClear();

        List<LeaveRequestJpa> results = repository.findByStatus(
                LeaveStatus.PENDING_HR_APPROVAL
        );

        assertEquals(List.of("request-1"), results.stream().map(LeaveRequestJpa::getId).toList());
    }

    @Test
    @DisplayName("Manager outstanding lookup returns only that manager's pending requests")
    void findByManagerIdAndPendingStatus() {
        repository.save(request("request-1", "staff-1", "manager-1", LeaveStatus.PENDING));
        repository.save(request("request-2", "staff-2", "manager-1", LeaveStatus.APPROVED));
        repository.save(request("request-3", "staff-3", "manager-1", LeaveStatus.REJECTED));
        repository.save(request("request-4", "staff-4", "manager-1", LeaveStatus.CANCELLED));
        repository.save(request("request-5", "staff-5", "manager-2", LeaveStatus.PENDING));
        flushAndClear();

        List<LeaveRequestJpa> results = repository.findByManagerIdAndStatus(
                "manager-1",
                LeaveStatus.PENDING
        );

        assertEquals(List.of("request-1"), results.stream().map(LeaveRequestJpa::getId).toList());
    }

    @Test
    @DisplayName("Manager date filtering uses inclusive LeavePeriod overlap boundaries")
    void managerDateFilterUsesInclusiveOverlap() {
        LocalDate reportingStart = LocalDate.of(2026, 11, 10);
        LocalDate reportingEnd = LocalDate.of(2026, 11, 20);
        repository.save(request(
                "request-1", "staff-1", "manager-1",
                LocalDate.of(2026, 11, 5), reportingStart, LeaveStatus.PENDING
        ));
        repository.save(request(
                "request-2", "staff-2", "manager-1",
                reportingEnd, LocalDate.of(2026, 11, 25), LeaveStatus.PENDING
        ));
        repository.save(request(
                "request-3", "staff-3", "manager-1",
                LocalDate.of(2026, 11, 5), reportingStart.minusDays(1), LeaveStatus.PENDING
        ));
        repository.save(request(
                "request-4", "staff-4", "manager-1",
                reportingEnd.plusDays(1), LocalDate.of(2026, 11, 25), LeaveStatus.PENDING
        ));
        repository.save(request(
                "request-5", "staff-5", "manager-1",
                reportingStart, reportingEnd, LeaveStatus.APPROVED
        ));
        flushAndClear();

        List<LeaveRequestJpa> results = repository
                .findByManagerIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        "manager-1",
                        LeaveStatus.PENDING,
                        reportingEnd,
                        reportingStart
                );

        assertEquals(
                Set.of("request-1", "request-2"),
                results.stream().map(LeaveRequestJpa::getId).collect(Collectors.toSet())
        );
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = LeaveStatus.class,
            names = {"PENDING_HR_APPROVAL", "APPROVED", "REJECTED", "CANCELLED"}
    )
    @DisplayName("A saved request keeps its status when loaded from the database")
    void nonPendingRequestStatusSurvivesDatabaseRoundTrip(LeaveStatus status) {
        LeaveRequest original = LeaveRequest.reconstitute(
                Identity.of("request-1"),
                "staff-1",
                "manager-1",
                new LeavePeriod(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)),
                "Family holiday",
                LeaveType.ANNUAL,
                status
        );
        repository.save(LeaveRequestDomainToJpaMapper.map(original));
        flushAndClear();

        LeaveRequest restored = repository.findById("request-1")
                .map(LeaveRequestJpaToDomainMapper::map)
                .orElseThrow();

        assertEquals(original.id(), restored.id());
        assertEquals(original.staffMemberId(), restored.staffMemberId());
        assertEquals(original.managerId(), restored.managerId());
        assertEquals(original.leavePeriod(), restored.leavePeriod());
        assertEquals(original.reason(), restored.reason());
        assertEquals(original.leaveType(), restored.leaveType());
        assertEquals(status, restored.status());
    }

    @Test
    @DisplayName("The database rejects a leave period whose end date precedes its start date")
    void invalidLeavePeriodIsRejectedBySchema() {
        LeaveRequestJpa invalidRequest = new LeaveRequestJpa(
                "request-1",
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 11, 6),
                LocalDate.of(2026, 11, 2),
                "Family holiday",
                LeaveType.ANNUAL,
                LeaveStatus.PENDING
        );

        repository.save(invalidRequest);

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    private LeaveRequestJpa request(String id, String staffMemberId, LeaveStatus status) {
        return request(id, staffMemberId, "manager-1", status);
    }

    private LeaveRequestJpa request(
            String id,
            String staffMemberId,
            String managerId,
            LeaveStatus status
    ) {
        return request(
                id,
                staffMemberId,
                managerId,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                status
        );
    }

    private LeaveRequestJpa request(
            String id,
            String staffMemberId,
            String managerId,
            LocalDate startDate,
            LocalDate endDate,
            LeaveStatus status
    ) {
        return new LeaveRequestJpa(
                id,
                staffMemberId,
                managerId,
                startDate,
                endDate,
                "Family holiday",
                LeaveType.ANNUAL,
                status
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
