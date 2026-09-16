package uk.ac.staffs.leavebooking.audit.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.audit.infrastructure.entities.LeaveAuditJpa;

import java.util.List;

@Repository
public interface LeaveAuditRepository extends CrudRepository<LeaveAuditJpa, Long> {
    List<LeaveAuditJpa> findAllByOrderBySourceEventId();
    List<LeaveAuditJpa> findByLeaveRequestIdOrderBySourceEventId(String leaveRequestId);
    List<LeaveAuditJpa> findByStaffMemberIdOrderBySourceEventId(String staffMemberId);
}
