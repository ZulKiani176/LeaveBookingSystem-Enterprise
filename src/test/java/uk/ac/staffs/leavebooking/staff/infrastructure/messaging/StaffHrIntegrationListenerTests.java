package uk.ac.staffs.leavebooking.staff.infrastructure.messaging;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffPersonalDetailsUpdatedIntegrationEvent;
import uk.ac.staffs.leavebooking.staff.application.StaffHrIntegrationService;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Staff HR Integration Listener")
class StaffHrIntegrationListenerTests {
    @Mock private StaffHrIntegrationService integrationService;

    @Test
    @DisplayName("A new-staff message is sent to the staff update service")
    void createdEventIsDelegatedToApplicationService() {
        StaffHrIntegrationListener listener = new StaffHrIntegrationListener(integrationService);
        HrStaffMemberCreatedIntegrationEvent event = new HrStaffMemberCreatedIntegrationEvent(
                1L, LocalDate.of(2026, 8, 25), "staff-1", "Ada", "Lovelace",
                "ada@example.com", LocalDate.of(2024, 1, 1), "Engineering", "manager-1",
                "Developer", LocalDate.of(2024, 1, 1), "Senior", "Permanent", "ACTIVE"
        );

        listener.receive(event);

        verify(integrationService).process(event);
    }

    @Test
    @DisplayName("A personal-details message is sent to the staff update service")
    void personalDetailsEventIsDelegatedToApplicationService() {
        StaffHrIntegrationListener listener = new StaffHrIntegrationListener(integrationService);
        HrStaffPersonalDetailsUpdatedIntegrationEvent event =
                new HrStaffPersonalDetailsUpdatedIntegrationEvent(
                        2L, LocalDate.of(2026, 8, 25), "staff-1",
                        "Grace", "Hopper", "grace@example.com"
                );

        listener.receive(event);

        verify(integrationService).process(event);
    }
}
