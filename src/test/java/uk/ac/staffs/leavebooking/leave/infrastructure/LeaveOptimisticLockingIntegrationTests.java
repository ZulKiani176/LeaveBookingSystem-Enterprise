package uk.ac.staffs.leavebooking.leave.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "firebase.enabled=false",
        "rabbitmq.enabled=false"
})
@DisplayName("Conflicting updates")
class LeaveOptimisticLockingIntegrationTests {
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private LeaveAllowanceRepository leaveAllowanceRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        leaveAllowanceRepository.deleteAll();
    }

    @Test
    @DisplayName("A stale allowance balance cannot overwrite a committed deduction")
    void staleAllowanceUpdateIsRejected() {
        leaveAllowanceRepository.save(new LeaveAllowanceJpa(
                "allowance-lock", "staff-lock", "Ada", "Lovelace", "manager-lock",
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("25.0"), BigDecimal.ZERO, new BigDecimal("25.0"), null
        ));

        EntityManager firstSession = entityManagerFactory.createEntityManager();
        EntityManager staleSession = entityManagerFactory.createEntityManager();
        try {
            LeaveAllowanceJpa first = findAllowance(firstSession);
            LeaveAllowanceJpa stale = findAllowance(staleSession);

            updateAllowance(firstSession, first, "24.0");

            staleSession.getTransaction().begin();
            stale.setRemainingDays(new BigDecimal("23.0"));
            assertThrows(OptimisticLockException.class, staleSession::flush);
            staleSession.getTransaction().rollback();
        } finally {
            firstSession.close();
            staleSession.close();
        }

        assertEquals(
                0,
                new BigDecimal("24.0").compareTo(
                        leaveAllowanceRepository.findById("allowance-lock")
                                .orElseThrow()
                                .getRemainingLeaveDays()
                )
        );
    }

    @Test
    @DisplayName("A stale LeaveRequest projection cannot overwrite a committed decision")
    void staleLeaveRequestProjectionUpdateIsRejected() {
        leaveRequestRepository.save(new LeaveRequestJpa(
                "request-lock", "staff-lock", "manager-lock",
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18),
                "Annual leave", LeaveType.ANNUAL, LeaveStatus.PENDING
        ));

        EntityManager firstSession = entityManagerFactory.createEntityManager();
        EntityManager staleSession = entityManagerFactory.createEntityManager();
        try {
            LeaveRequestJpa first = findRequest(firstSession);
            LeaveRequestJpa stale = findRequest(staleSession);

            firstSession.getTransaction().begin();
            first.setStatus(LeaveStatus.APPROVED);
            firstSession.flush();
            firstSession.getTransaction().commit();

            staleSession.getTransaction().begin();
            stale.setStatus(LeaveStatus.REJECTED);
            assertThrows(OptimisticLockException.class, staleSession::flush);
            staleSession.getTransaction().rollback();
        } finally {
            firstSession.close();
            staleSession.close();
        }

        assertEquals(
                LeaveStatus.APPROVED,
                leaveRequestRepository.findById("request-lock").orElseThrow().getStatus()
        );
    }

    private LeaveAllowanceJpa findAllowance(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        LeaveAllowanceJpa allowance = entityManager.find(
                LeaveAllowanceJpa.class,
                "allowance-lock"
        );
        entityManager.getTransaction().commit();
        return allowance;
    }

    private LeaveRequestJpa findRequest(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        LeaveRequestJpa request = entityManager.find(LeaveRequestJpa.class, "request-lock");
        entityManager.getTransaction().commit();
        return request;
    }

    private void updateAllowance(
            EntityManager entityManager,
            LeaveAllowanceJpa allowance,
            String remainingDays
    ) {
        entityManager.getTransaction().begin();
        allowance.setRemainingDays(new BigDecimal(remainingDays));
        entityManager.flush();
        entityManager.getTransaction().commit();
    }
}
