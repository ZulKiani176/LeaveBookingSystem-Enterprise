package uk.ac.staffs.leavebooking.hrsync.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.entities.HrAbsenceSyncJpa;

import java.util.List;

@Repository
public interface HrAbsenceSyncRepository extends CrudRepository<HrAbsenceSyncJpa, Long> {
    List<HrAbsenceSyncJpa> findAllByOrderBySourceEventId();
    List<HrAbsenceSyncJpa> findByStaffMemberIdOrderBySourceEventId(String staffMemberId);
}
