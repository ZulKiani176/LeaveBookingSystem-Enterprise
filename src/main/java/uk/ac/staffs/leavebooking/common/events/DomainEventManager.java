package uk.ac.staffs.leavebooking.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;

@Service
public class DomainEventManager {
    public static final String SOURCE_CONTEXT_NOT_EMPTY = "Context cannot be empty";
    public static final String EVENTS_NOT_NULL = "Events cannot be null";
    public static final String STORED_EVENT_ID_NOT_NULL = "Stored event identity cannot be null";

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventManager.class);

    private final ApplicationEventPublisher eventPublisher;
    private final EventStoreService eventStoreService;

    public DomainEventManager(
            ApplicationEventPublisher eventPublisher,
            EventStoreService eventStoreService
    ) {
        this.eventPublisher = eventPublisher;
        this.eventStoreService = eventStoreService;
    }

    @Transactional
    public void manageDomainEvents(String sourceContext, List<Event> events) {
        String validatedContext = argumentNotEmpty(sourceContext, SOURCE_CONTEXT_NOT_EMPTY);
        List<Event> validatedEvents = Objects.requireNonNull(events, EVENTS_NOT_NULL);

        for (Event event : validatedEvents) {
            EventStoreJpa storedEvent = eventStoreService.append(event);
            Long eventId = Objects.requireNonNull(
                    storedEvent.getId(),
                    STORED_EVENT_ID_NOT_NULL
            );
            Event publishedEvent = event.withId(eventId);
            LOGGER.info(
                    "Dispatching {} event {} with event-store ID {}",
                    validatedContext,
                    event.getClass().getSimpleName(),
                    eventId
            );
            eventPublisher.publishEvent(publishedEvent);
        }
    }
}
