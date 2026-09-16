package uk.ac.staffs.leavebooking.common;

import uk.ac.staffs.leavebooking.common.events.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AggregateRoot<T> extends Entity<T> {
    public static final String DOMAIN_EVENT_NOT_NULL = "Domain event cannot be null";

    private final List<Event> domainEvents = new ArrayList<>();

    protected AggregateRoot(Identity<T> id) {
        super(id);
    }

    protected void addDomainEvent(Event event) {
        domainEvents.add(Objects.requireNonNull(event, DOMAIN_EVENT_NOT_NULL));
    }

    public List<Event> listOfDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public boolean domainEventsExist() {
        return !domainEvents.isEmpty();
    }
}
