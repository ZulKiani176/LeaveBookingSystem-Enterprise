package uk.ac.staffs.leavebooking.notification.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uk.ac.staffs.leavebooking.notification.application.NotificationType;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@DisplayName("Notification Repository")
class NotificationRepositoryTests {
    @Autowired
    private NotificationRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Notification fields survive persistence and recipient results are newest first")
    void persistenceAndRecipientOrdering() {
        repository.save(notification(
                "notification-1", 1L, "staff-1", Instant.parse("2026-08-25T10:00:00Z")
        ));
        repository.save(notification(
                "notification-2", 2L, "staff-1", Instant.parse("2026-08-25T11:00:00Z")
        ));
        flushAndClear();

        List<NotificationJpa> results = repository
                .findByRecipientIdOrderByCreatedAtDesc("staff-1");

        assertEquals(List.of("notification-2", "notification-1"),
                results.stream().map(NotificationJpa::getId).toList());
        NotificationJpa restored = results.getFirst();
        assertEquals(2L, restored.getSourceEventId());
        assertEquals("request-1", restored.getLeaveRequestId());
        assertEquals(NotificationType.STAFF_LEAVE_APPROVED, restored.getNotificationType());
        assertEquals("Approved", restored.getMessage());
        assertEquals(Instant.parse("2026-08-25T11:00:00Z"), restored.getCreatedAt());
    }

    @Test
    @DisplayName("A source event can create at most one notification")
    void sourceEventIdentityIsUnique() {
        repository.save(notification(
                "notification-1", 7L, "staff-1", Instant.parse("2026-08-25T10:00:00Z")
        ));
        flushAndClear();

        repository.save(notification(
                "notification-2", 7L, "staff-1", Instant.parse("2026-08-25T11:00:00Z")
        ));

        assertThrows(PersistenceException.class, this::flushAndClear);
    }

    @Test
    @DisplayName("Recipient queries do not leak another user's notifications")
    void recipientQueriesRemainSeparated() {
        repository.save(notification(
                "notification-1", 1L, "staff-1", Instant.parse("2026-08-25T10:00:00Z")
        ));
        repository.save(notification(
                "notification-2", 2L, "manager-1", Instant.parse("2026-08-25T11:00:00Z")
        ));
        flushAndClear();

        List<NotificationJpa> results = repository
                .findByRecipientIdOrderByCreatedAtDesc("staff-1");

        assertEquals(1, results.size());
        assertEquals("staff-1", results.getFirst().getRecipientId());
        assertEquals(true, repository.existsBySourceEventId(2L));
    }

    private NotificationJpa notification(
            String id,
            Long sourceEventId,
            String recipientId,
            Instant createdAt
    ) {
        return new NotificationJpa(
                id,
                sourceEventId,
                recipientId,
                "request-1",
                NotificationType.STAFF_LEAVE_APPROVED,
                "Approved",
                createdAt
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
