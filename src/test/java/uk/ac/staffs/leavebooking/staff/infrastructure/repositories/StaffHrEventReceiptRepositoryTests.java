package uk.ac.staffs.leavebooking.staff.infrastructure.repositories;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffHrEventReceiptJpa;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("Staff HR Event Receipt Repository")
class StaffHrEventReceiptRepositoryTests {
    @Autowired private StaffHrEventReceiptRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("Receipt lookup distinguishes source identity and event type")
    void receiptLookupUsesSourceIdentityAndType() {
        repository.save(receipt(11L, "Created"));
        entityManager.flush();

        assertTrue(repository.existsBySourceEventIdAndEventType(11L, "Created"));
        assertFalse(repository.existsBySourceEventIdAndEventType(11L, "Updated"));
    }

    @Test
    @DisplayName("The same source identity may be used by a different event type")
    void sameIdentityDifferentTypeIsAllowed() {
        repository.save(receipt(11L, "Created"));
        repository.save(receipt(11L, "Updated"));

        entityManager.flush();

        assertTrue(repository.existsBySourceEventIdAndEventType(11L, "Created"));
        assertTrue(repository.existsBySourceEventIdAndEventType(11L, "Updated"));
    }

    @Test
    @DisplayName("The database prevents duplicate source identity and event type receipts")
    void duplicateReceiptIsRejected() {
        repository.save(receipt(11L, "Created"));
        entityManager.flush();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(receipt(11L, "Created"))
        );
    }

    private StaffHrEventReceiptJpa receipt(Long sourceId, String type) {
        return new StaffHrEventReceiptJpa(sourceId, type, Instant.parse("2026-08-25T12:00:00Z"));
    }
}
