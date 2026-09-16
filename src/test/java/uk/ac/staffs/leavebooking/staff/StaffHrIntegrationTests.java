package uk.ac.staffs.leavebooking.staff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffPersonalDetailsUpdatedIntegrationEvent;
import uk.ac.staffs.leavebooking.staff.application.StaffHrIntegrationService;
import uk.ac.staffs.leavebooking.staff.application.exceptions.InvalidHrEmploymentStatusException;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffHrEventReceiptRepository;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "rabbitmq.enabled=false")
@DisplayName("Staff changes received from HR")
class StaffHrIntegrationTests {
    @Autowired private StaffHrIntegrationService integrationService;
    @Autowired private StaffMemberRepository staffRepository;
    @Autowired private StaffHrEventReceiptRepository receiptRepository;

    @BeforeEach
    void clearPersistence() {
        receiptRepository.deleteAll();
        staffRepository.deleteAll();
    }

    @Test
    @DisplayName("HR creation, duplicate delivery and personal update are transactionally idempotent")
    void completeHrIntegrationWorkflow() {
        HrStaffMemberCreatedIntegrationEvent created = createdEvent(901L, "ON_LEAVE");

        integrationService.process(created);
        integrationService.process(created);

        StaffMemberJpa stored = staffRepository.findById("external-staff-901").orElseThrow();
        assertEquals("external-staff-901", stored.getId());
        assertEquals(EmploymentStatus.ON_LEAVE, stored.getEmploymentStatus());
        assertEquals(1, staffRepository.count());
        assertEquals(1, receiptRepository.count());

        integrationService.process(new HrStaffPersonalDetailsUpdatedIntegrationEvent(
                901L,
                LocalDate.of(2026, 8, 26),
                "external-staff-901",
                "Grace",
                "Hopper",
                "grace.hopper@example.com"
        ));

        StaffMemberJpa updated = staffRepository.findById("external-staff-901").orElseThrow();
        assertEquals("Grace", updated.getFirstName());
        assertEquals("Hopper", updated.getSurname());
        assertEquals("grace.hopper@example.com", updated.getEmail());
        assertEquals("Engineering", updated.getDepartment());
        assertEquals("manager-1", updated.getManagerId());
        assertEquals("Developer", updated.getJobRole());
        assertEquals(2, receiptRepository.count());
    }

    @Test
    @DisplayName("Failed HR processing commits neither Staff state nor a receipt")
    void failedProcessingStoresNoReceipt() {
        assertThrows(
                InvalidHrEmploymentStatusException.class,
                () -> integrationService.process(createdEvent(902L, "UNKNOWN"))
        );

        assertEquals(0, staffRepository.count());
        assertEquals(0, receiptRepository.count());
    }

    private HrStaffMemberCreatedIntegrationEvent createdEvent(Long id, String status) {
        return new HrStaffMemberCreatedIntegrationEvent(
                id, LocalDate.of(2026, 8, 25), "external-staff-901", "Ada", "Lovelace",
                "ada@example.com", LocalDate.of(2024, 1, 1), "Engineering", "manager-1",
                "Developer", LocalDate.of(2024, 1, 1), "Senior", "Permanent", status
        );
    }
}
