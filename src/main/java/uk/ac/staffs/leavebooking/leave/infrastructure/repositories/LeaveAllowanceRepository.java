package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveAllowanceRepository extends CrudRepository<LeaveAllowanceJpa, String> {
    List<LeaveAllowanceJpa> findByStaffMemberId(String staffMemberId);

    Optional<LeaveAllowanceJpa> findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    );

    List<LeaveAllowanceJpa> findByBusinessYearStartAndBusinessYearEnd(
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    );
}
