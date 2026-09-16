package uk.ac.staffs.leavebooking.common.events;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface EventStoreRepository extends CrudRepository<EventStoreJpa, Long> {
    List<EventStoreJpa> findByStatusInAndRetryCountLessThanOrderByIdAsc(
            List<StatusOfMessageDelivery> statuses,
            int retryCount,
            Pageable pageable
    );
}
