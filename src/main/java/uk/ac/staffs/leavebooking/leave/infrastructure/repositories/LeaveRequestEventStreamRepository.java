package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestStreamEventJpa;

import java.util.List;

@Repository
public interface LeaveRequestEventStreamRepository
        extends JpaRepository<LeaveRequestStreamEventJpa, Long> {
    List<LeaveRequestStreamEventJpa> findByAggregateIdOrderBySequenceNumber(String aggregateId);

    long countByAggregateId(String aggregateId);
}
