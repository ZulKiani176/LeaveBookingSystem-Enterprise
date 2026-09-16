package uk.ac.staffs.leavebooking.staff.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffHrEventReceiptJpa;

@Repository
public interface StaffHrEventReceiptRepository
        extends CrudRepository<StaffHrEventReceiptJpa, Long> {
    boolean existsBySourceEventIdAndEventType(Long sourceEventId, String eventType);
}
