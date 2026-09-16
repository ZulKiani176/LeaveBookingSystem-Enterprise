package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestJpa, String> {
    List<LeaveRequestJpa> findByStaffMemberId(String staffMemberId);

    List<LeaveRequestJpa> findByStaffMemberIdAndStatus(String staffMemberId, LeaveStatus status);

    List<LeaveRequestJpa> findByStatus(LeaveStatus status);

    List<LeaveRequestJpa> findByManagerIdAndStatus(String managerId, LeaveStatus status);

    List<LeaveRequestJpa>
    findByManagerIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String managerId,
            LeaveStatus status,
            LocalDate reportingEnd,
            LocalDate reportingStart
    );

    List<LeaveRequestJpa>
    findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String staffMemberId,
            List<LeaveStatus> statuses,
            LocalDate requestedEnd,
            LocalDate requestedStart
    );

    List<LeaveRequestJpa>
    findByStaffMemberIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            List<String> staffMemberIds,
            LocalDate reportingEnd,
            LocalDate reportingStart
    );
}
