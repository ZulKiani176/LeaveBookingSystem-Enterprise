package uk.ac.staffs.leavebooking.leave.infrastructure;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.leave.application.exceptions.ConcurrentLeaveModificationException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestReferredForHrApprovalEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.SickLeaveRecordedEvent;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestStreamEventJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestEventStreamRepository;

import java.util.List;
import java.util.Map;

@Repository
public class LeaveRequestEventStore {
    public static final String EVENT_SERIALISATION_FAILED =
            "Failed to serialise LeaveRequest event";
    public static final String UNSUPPORTED_EVENT_TYPE = "Unsupported LeaveRequest event type: ";

    private static final Map<String, Class<? extends LocalEvent>> EVENT_TYPES = Map.of(
            LeaveRequestSubmittedEvent.class.getSimpleName(), LeaveRequestSubmittedEvent.class,
            SickLeaveRecordedEvent.class.getSimpleName(), SickLeaveRecordedEvent.class,
            LeaveRequestApprovedEvent.class.getSimpleName(), LeaveRequestApprovedEvent.class,
            LeaveRequestRejectedEvent.class.getSimpleName(), LeaveRequestRejectedEvent.class,
            LeaveRequestReferredForHrApprovalEvent.class.getSimpleName(),
            LeaveRequestReferredForHrApprovalEvent.class,
            LeaveRequestCancelledEvent.class.getSimpleName(), LeaveRequestCancelledEvent.class
    );

    private final LeaveRequestEventStreamRepository repository;
    private final ObjectMapper objectMapper;

    public LeaveRequestEventStore(
            LeaveRequestEventStreamRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public LeaveRequest load(String aggregateId) {
        List<LeaveRequestStreamEventJpa> storedEvents =
                repository.findByAggregateIdOrderBySequenceNumber(aggregateId);
        if (storedEvents.isEmpty()) {
            throw new LeaveRequestNotFoundException(aggregateId);
        }
        List<LocalEvent> events = storedEvents.stream()
                .map(this::deserialise)
                .toList();
        return LeaveRequest.replay(Identity.of(aggregateId), events);
    }

    @Transactional
    public void append(LeaveRequest aggregate) {
        List<LocalEvent> newEvents = aggregate.listOfDomainEvents().stream()
                .filter(LocalEvent.class::isInstance)
                .map(LocalEvent.class::cast)
                .toList();
        long expectedVersion = aggregate.streamVersion();
        long actualVersion = repository.countByAggregateId(aggregate.id().id());
        if (actualVersion != expectedVersion) {
            throw new ConcurrentLeaveModificationException();
        }
        try {
            List<LeaveRequestStreamEventJpa> entries = java.util.stream.IntStream
                    .range(0, newEvents.size())
                    .mapToObj(index -> map(
                            aggregate.id().id(),
                            expectedVersion + index + 1,
                            newEvents.get(index)
                    ))
                    .toList();
            repository.saveAllAndFlush(entries);
        } catch (DataIntegrityViolationException exception) {
            throw new ConcurrentLeaveModificationException(exception);
        }
    }

    private LeaveRequestStreamEventJpa map(
            String aggregateId,
            long sequenceNumber,
            LocalEvent event
    ) {
        try {
            return new LeaveRequestStreamEventJpa(
                    aggregateId,
                    sequenceNumber,
                    event.getClass().getSimpleName(),
                    objectMapper.writeValueAsString(event),
                    event.occurredOn()
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(EVENT_SERIALISATION_FAILED, exception);
        }
    }

    private LocalEvent deserialise(LeaveRequestStreamEventJpa storedEvent) {
        Class<? extends LocalEvent> eventType = EVENT_TYPES.get(storedEvent.getEventType());
        if (eventType == null) {
            throw new IllegalArgumentException(UNSUPPORTED_EVENT_TYPE + storedEvent.getEventType());
        }
        try {
            return objectMapper.readValue(storedEvent.getEventBody(), eventType);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(EVENT_SERIALISATION_FAILED, exception);
        }
    }
}
