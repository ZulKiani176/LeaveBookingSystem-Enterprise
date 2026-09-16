package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceCarryOverJpa;

@Repository
public interface LeaveAllowanceCarryOverRepository
        extends CrudRepository<LeaveAllowanceCarryOverJpa, Long> {
    boolean existsBySourceAllowanceIdAndTargetAllowanceId(
            String sourceAllowanceId,
            String targetAllowanceId
    );
}
