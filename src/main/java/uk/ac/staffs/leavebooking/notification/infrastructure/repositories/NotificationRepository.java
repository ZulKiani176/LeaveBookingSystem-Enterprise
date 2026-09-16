package uk.ac.staffs.leavebooking.notification.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;

import java.util.List;

public interface NotificationRepository extends CrudRepository<NotificationJpa, String> {
    List<NotificationJpa> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    boolean existsBySourceEventId(Long sourceEventId);
}
