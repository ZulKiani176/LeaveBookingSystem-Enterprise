package uk.ac.staffs.leavebooking.staff.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffPersonalDetailsUpdatedIntegrationEvent;
import uk.ac.staffs.leavebooking.staff.application.exceptions.InvalidHrEmploymentStatusException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffHrEventReceiptJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffHrEventReceiptRepository;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@DisplayName("Staff HR Integration Service")
class StaffHrIntegrationServiceTests {
    @Mock private StaffMemberRepository staffRepository;
    @Mock private StaffHrEventReceiptRepository receiptRepository;

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(EmploymentStatus.class)
    @DisplayName("HR creation preserves supplied identity, snapshot fields and status")
    void hrCreationSavesSuppliedState(EmploymentStatus status) {
        StaffHrIntegrationService service = service();
        HrStaffMemberCreatedIntegrationEvent event = createdEvent(101L, status.name());

        service.process(event);

        ArgumentCaptor<StaffMemberJpa> staffCaptor = ArgumentCaptor.forClass(StaffMemberJpa.class);
        verify(staffRepository).save(staffCaptor.capture());
        StaffMemberJpa saved = staffCaptor.getValue();
        assertEquals("hr-staff-1", saved.getId());
        assertEquals("Ada", saved.getFirstName());
        assertEquals("Lovelace", saved.getSurname());
        assertEquals("ada@example.com", saved.getEmail());
        assertEquals("Engineering", saved.getDepartment());
        assertEquals("manager-1", saved.getManagerId());
        assertEquals(status, saved.getEmploymentStatus());
        verify(receiptRepository).save(any(StaffHrEventReceiptJpa.class));
    }

    @Test
    @DisplayName("Duplicate source identity and type are acknowledged without another Staff write")
    void duplicateEventIsIgnored(CapturedOutput output) {
        StaffHrIntegrationService service = service();
        HrStaffMemberCreatedIntegrationEvent event = createdEvent(101L, "ACTIVE");
        when(receiptRepository.existsBySourceEventIdAndEventType(
                101L,
                HrStaffMemberCreatedIntegrationEvent.class.getSimpleName()
        )).thenReturn(true);

        service.process(event);

        verifyNoInteractions(staffRepository);
        verify(receiptRepository, never()).save(any());
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("Ignoring duplicate HR integration event")
                .contains("sourceEventId=101")
                .doesNotContain("ada@example.com");
    }

    @Test
    @DisplayName("A different event for an existing Staff identity is rejected without a receipt")
    void duplicateStaffIdentityIsRejected() {
        StaffHrIntegrationService service = service();
        when(staffRepository.existsById("hr-staff-1")).thenReturn(true);

        assertThrows(
                StaffMemberAlreadyExistsException.class,
                () -> service.process(createdEvent(101L, "ACTIVE"))
        );

        verify(staffRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unknown external status is rejected rather than defaulted")
    void unknownStatusIsRejected() {
        StaffHrIntegrationService service = service();

        InvalidHrEmploymentStatusException exception = assertThrows(
                InvalidHrEmploymentStatusException.class,
                () -> service.process(createdEvent(101L, "SABBATICAL"))
        );

        assertEquals("Unsupported HR employment status: SABBATICAL", exception.getMessage());
        verify(staffRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("An HR-created duplicate email is rejected without saving Staff or receipt")
    void duplicateEmailIsRejected() {
        StaffHrIntegrationService service = service();
        when(staffRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThrows(
                StaffEmailAlreadyExistsException.class,
                () -> service.process(createdEvent(101L, "ACTIVE"))
        );

        verify(staffRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Personal details update changes only name and email and stores its receipt")
    void personalDetailsUpdateUsesAggregate() {
        StaffHrIntegrationService service = service();
        when(staffRepository.findById("hr-staff-1")).thenReturn(Optional.of(staffJpa()));

        service.process(updatedEvent(102L, "Grace", "Hopper", "grace@example.com"));

        ArgumentCaptor<StaffMemberJpa> captor = ArgumentCaptor.forClass(StaffMemberJpa.class);
        verify(staffRepository).save(captor.capture());
        StaffMemberJpa saved = captor.getValue();
        assertEquals("Grace", saved.getFirstName());
        assertEquals("Hopper", saved.getSurname());
        assertEquals("grace@example.com", saved.getEmail());
        assertEquals("Engineering", saved.getDepartment());
        assertEquals("manager-1", saved.getManagerId());
        assertEquals("Developer", saved.getJobRole());
        verify(receiptRepository).save(any(StaffHrEventReceiptJpa.class));
    }

    @Test
    @DisplayName("An update may retain the email already owned by the same Staff member")
    void sameOwnerEmailIsAllowed() {
        StaffHrIntegrationService service = service();
        StaffMemberJpa existing = staffJpa();
        when(staffRepository.findById("hr-staff-1")).thenReturn(Optional.of(existing));
        when(staffRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));

        service.process(updatedEvent(102L, "Augusta", "King", "ada@example.com"));

        verify(staffRepository).save(any(StaffMemberJpa.class));
        verify(receiptRepository).save(any(StaffHrEventReceiptJpa.class));
    }

    @Test
    @DisplayName("A missing update target fails without storing a receipt")
    void missingUpdateTargetIsRejected() {
        StaffHrIntegrationService service = service();
        when(staffRepository.findById("hr-staff-1")).thenReturn(Optional.empty());

        assertThrows(
                StaffMemberNotFoundException.class,
                () -> service.process(updatedEvent(102L, "Grace", "Hopper", "grace@example.com"))
        );

        verify(staffRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("An email owned by another Staff member prevents update and receipt")
    void conflictingUpdateEmailIsRejected() {
        StaffHrIntegrationService service = service();
        when(staffRepository.findById("hr-staff-1")).thenReturn(Optional.of(staffJpa()));
        StaffMemberJpa owner = staffJpa();
        owner.setId("other-staff");
        when(staffRepository.findByEmail("used@example.com")).thenReturn(Optional.of(owner));

        assertThrows(
                StaffEmailAlreadyExistsException.class,
                () -> service.process(updatedEvent(102L, "Grace", "Hopper", "used@example.com"))
        );

        verify(staffRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    private StaffHrIntegrationService service() {
        return new StaffHrIntegrationService(staffRepository, receiptRepository);
    }

    private HrStaffMemberCreatedIntegrationEvent createdEvent(Long id, String status) {
        return new HrStaffMemberCreatedIntegrationEvent(
                id, LocalDate.of(2026, 8, 25), "hr-staff-1", "Ada", "Lovelace",
                "ada@example.com", LocalDate.of(2024, 1, 1), "Engineering", "manager-1",
                "Developer", LocalDate.of(2024, 1, 1), "Senior", "Permanent", status
        );
    }

    private HrStaffPersonalDetailsUpdatedIntegrationEvent updatedEvent(
            Long id,
            String firstName,
            String surname,
            String email
    ) {
        return new HrStaffPersonalDetailsUpdatedIntegrationEvent(
                id, LocalDate.of(2026, 8, 25), "hr-staff-1", firstName, surname, email
        );
    }

    private StaffMemberJpa staffJpa() {
        return new StaffMemberJpa(
                "hr-staff-1", "Ada", "Lovelace", "ada@example.com",
                LocalDate.of(2024, 1, 1), "Engineering", "manager-1", "Developer",
                LocalDate.of(2024, 1, 1), "Senior", "Permanent", EmploymentStatus.ACTIVE
        );
    }
}
