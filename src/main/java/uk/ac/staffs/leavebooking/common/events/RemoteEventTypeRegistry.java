package uk.ac.staffs.leavebooking.common.events;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.ac.staffs.leavebooking.common.events.integration.*;

import java.util.Map;

@Component
public class RemoteEventTypeRegistry {
    private static final Map<String, Class<? extends RemoteEvent>> APPROVED_TYPES = Map.of(
            LeaveRequestSubmittedIntegrationEvent.class.getSimpleName(),
            LeaveRequestSubmittedIntegrationEvent.class,
            SickLeaveRecordedIntegrationEvent.class.getSimpleName(),
            SickLeaveRecordedIntegrationEvent.class,
            LeaveRequestApprovedIntegrationEvent.class.getSimpleName(),
            LeaveRequestApprovedIntegrationEvent.class,
            LeaveRequestRejectedIntegrationEvent.class.getSimpleName(),
            LeaveRequestRejectedIntegrationEvent.class,
            LeaveRequestCancelledIntegrationEvent.class.getSimpleName(),
            LeaveRequestCancelledIntegrationEvent.class,
            LeaveRequestReferredForHrApprovalIntegrationEvent.class.getSimpleName(),
            LeaveRequestReferredForHrApprovalIntegrationEvent.class
    );

    private final ObjectMapper objectMapper;

    public RemoteEventTypeRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RemoteEvent reconstruct(EventStoreJpa storedEvent) {
        Class<? extends RemoteEvent> type = APPROVED_TYPES.get(storedEvent.getEventType());
        if (type == null) {
            throw new IllegalArgumentException(
                    "Stored remote event type is not approved: " + storedEvent.getEventType()
            );
        }
        try {
            RemoteEvent event = objectMapper.readValue(storedEvent.getEventBody(), type);
            return (RemoteEvent) event.withId(storedEvent.getId());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Failed to reconstruct stored remote event", exception);
        }
    }
}
