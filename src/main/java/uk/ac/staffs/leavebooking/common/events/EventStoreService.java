package uk.ac.staffs.leavebooking.common.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Service
public class EventStoreService {
    public static final String EVENT_NOT_NULL = "Event cannot be null";
    public static final String EVENT_ID_NOT_NULL = "Event-store identity cannot be null";
    public static final String DELIVERY_STATUS_NOT_NULL = "Delivery status cannot be null";
    public static final String EVENT_STORE_ENTRY_NOT_FOUND = "Event-store entry not found: ";
    public static final String EVENT_SERIALISATION_FAILED = "Failed to serialise event payload";

    private static final Logger LOGGER = LoggerFactory.getLogger(EventStoreService.class);

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    public EventStoreService(
            EventStoreRepository eventStoreRepository,
            ObjectMapper objectMapper
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventStoreJpa append(Event event) {
        Event eventToStore = Objects.requireNonNull(event, EVENT_NOT_NULL);
        try {
            EventStoreJpa eventJpa = new EventStoreJpa(
                    null,
                    eventToStore.occurredOn(),
                    eventToStore.getClass().getSimpleName(),
                    objectMapper.writeValueAsString(eventToStore),
                    eventToStore instanceof RemoteEvent
                            ? StatusOfMessageDelivery.PENDING
                            : StatusOfMessageDelivery.LOCAL,
                    0
            );
            return eventStoreRepository.save(eventJpa);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(EVENT_SERIALISATION_FAILED, exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(
            Long eventId,
            StatusOfMessageDelivery status,
            boolean incrementRetryCount
    ) {
        Long validatedEventId = Objects.requireNonNull(eventId, EVENT_ID_NOT_NULL);
        StatusOfMessageDelivery validatedStatus = Objects.requireNonNull(
                status,
                DELIVERY_STATUS_NOT_NULL
        );
        EventStoreJpa event = eventStoreRepository.findById(validatedEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        EVENT_STORE_ENTRY_NOT_FOUND + validatedEventId
                ));

        event.setStatus(validatedStatus);
        if (incrementRetryCount) {
            event.setRetryCount(Math.incrementExact(event.getRetryCount()));
        }
        eventStoreRepository.save(event);
        LOGGER.info(
                "Event-store entry {} marked {} with retry count {}",
                validatedEventId,
                validatedStatus,
                event.getRetryCount()
        );
    }
}
