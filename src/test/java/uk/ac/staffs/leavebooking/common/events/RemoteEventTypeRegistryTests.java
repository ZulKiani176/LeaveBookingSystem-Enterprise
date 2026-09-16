package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestReferredForHrApprovalIntegrationEvent;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Recognising supported messages")
class RemoteEventTypeRegistryTests {
    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final RemoteEventTypeRegistry registry = new RemoteEventTypeRegistry(objectMapper);

    @Test
    @DisplayName("The explicit registry reconstructs the new sickness event with its store ID")
    void registryReconstructsSicknessEvent() throws Exception {
        SickLeaveRecordedIntegrationEvent original = new SickLeaveRecordedIntegrationEvent(
                LocalDate.parse("2026-08-24"), "sick-1", "staff-1", "manager-1",
                LocalDate.parse("2026-08-24"), LocalDate.parse("2026-08-25")
        );
        EventStoreJpa stored = stored(
                42L,
                SickLeaveRecordedIntegrationEvent.class.getSimpleName(),
                objectMapper.writeValueAsString(original)
        );

        SickLeaveRecordedIntegrationEvent reconstructed = assertInstanceOf(
                SickLeaveRecordedIntegrationEvent.class,
                registry.reconstruct(stored)
        );

        assertEquals(42L, reconstructed.id());
        assertEquals("sick-1", reconstructed.leaveRequestId());
    }

    @Test
    @DisplayName("The explicit registry reconstructs HR referral for outbox recovery")
    void registryReconstructsHrReferralEvent() throws Exception {
        LeaveRequestReferredForHrApprovalIntegrationEvent original =
                new LeaveRequestReferredForHrApprovalIntegrationEvent(
                        LocalDate.parse("2026-08-24"), "request-1", "staff-1"
                );
        EventStoreJpa stored = stored(
                43L,
                LeaveRequestReferredForHrApprovalIntegrationEvent.class.getSimpleName(),
                objectMapper.writeValueAsString(original)
        );

        LeaveRequestReferredForHrApprovalIntegrationEvent reconstructed = assertInstanceOf(
                LeaveRequestReferredForHrApprovalIntegrationEvent.class,
                registry.reconstruct(stored)
        );

        assertEquals(43L, reconstructed.id());
        assertEquals("request-1", reconstructed.leaveRequestId());
    }

    @Test
    @DisplayName("An unapproved stored class name is never instantiated")
    void unapprovedTypeIsRejected() {
        EventStoreJpa stored = stored(42L, "java.lang.Runtime", "{}");

        assertThrows(IllegalArgumentException.class, () -> registry.reconstruct(stored));
    }

    @Test
    @DisplayName("Malformed JSON for an approved type is rejected clearly")
    void malformedApprovedEventIsRejected() {
        EventStoreJpa stored = stored(
                42L,
                SickLeaveRecordedIntegrationEvent.class.getSimpleName(),
                "not-json"
        );

        assertThrows(IllegalArgumentException.class, () -> registry.reconstruct(stored));
    }

    private EventStoreJpa stored(Long id, String type, String body) {
        return new EventStoreJpa(
                id, LocalDate.parse("2026-08-24"), type, body,
                StatusOfMessageDelivery.PENDING, 0
        );
    }
}
