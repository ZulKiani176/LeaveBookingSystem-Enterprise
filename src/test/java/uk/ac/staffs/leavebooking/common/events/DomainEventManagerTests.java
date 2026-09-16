package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestReferredForHrApprovalEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Saving and publishing events")
class DomainEventManagerTests {
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EventStoreService eventStoreService;

    @InjectMocks
    private DomainEventManager domainEventManager;

    @Test
    @DisplayName("An event is stored before its ID-bearing copy is published")
    void eventIsStoredBeforePublishing() {
        Event event = rejectedEvent();
        when(eventStoreService.append(event)).thenReturn(storedEvent(9L));

        domainEventManager.manageDomainEvents("LeaveManagement", List.of(event));

        ArgumentCaptor<Object> publishedCaptor = ArgumentCaptor.forClass(Object.class);
        InOrder order = inOrder(eventStoreService, eventPublisher);
        order.verify(eventStoreService).append(event);
        order.verify(eventPublisher).publishEvent(publishedCaptor.capture());
        Event published = (Event) publishedCaptor.getValue();
        assertEquals(9L, published.id());
        assertEquals(event, published.withId(null));
    }

    @Test
    @DisplayName("Multiple events are each stored and published with their own identity")
    void multipleEventsAreStoredAndPublished() {
        Event first = rejectedEvent();
        Event second = new LeaveRequestReferredForHrApprovalEvent(
                LocalDate.of(2026, 8, 25),
                "request-2",
                "staff-2"
        );
        when(eventStoreService.append(first)).thenReturn(storedEvent(10L));
        when(eventStoreService.append(second)).thenReturn(storedEvent(11L));

        domainEventManager.manageDomainEvents("LeaveManagement", List.of(first, second));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
        List<Long> publishedIds = captor.getAllValues().stream()
                .map(Event.class::cast)
                .map(Event::id)
                .toList();
        assertEquals(List.of(10L, 11L), publishedIds);
    }

    @Test
    @DisplayName("A null context is rejected before event collaboration")
    void nullContextIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                domainEventManager.manageDomainEvents(null, List.of(rejectedEvent()))
        );

        assertEquals(DomainEventManager.SOURCE_CONTEXT_NOT_EMPTY, exception.getMessage());
        verifyNoInteractions(eventStoreService, eventPublisher);
    }

    @Test
    @DisplayName("A blank context is rejected before event collaboration")
    void blankContextIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                domainEventManager.manageDomainEvents("   ", List.of(rejectedEvent()))
        );

        verifyNoInteractions(eventStoreService, eventPublisher);
    }

    @Test
    @DisplayName("A null event list is rejected before event collaboration")
    void nullEventListIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                domainEventManager.manageDomainEvents("LeaveManagement", null)
        );

        assertEquals(DomainEventManager.EVENTS_NOT_NULL, exception.getMessage());
        verifyNoInteractions(eventStoreService, eventPublisher);
    }

    @Test
    @DisplayName("An empty event list is safe and performs no collaboration")
    void emptyEventListIsSafe() {
        domainEventManager.manageDomainEvents("LeaveManagement", List.of());

        verifyNoInteractions(eventStoreService, eventPublisher);
    }

    @Test
    @DisplayName("A missing generated event-store identity prevents publication")
    void missingStoredEventIdentityPreventsPublication() {
        Event event = rejectedEvent();
        when(eventStoreService.append(event)).thenReturn(storedEvent(null));

        Throwable exception = assertThrows(NullPointerException.class, () ->
                domainEventManager.manageDomainEvents("LeaveManagement", List.of(event))
        );

        assertEquals(DomainEventManager.STORED_EVENT_ID_NOT_NULL, exception.getMessage());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }

    private Event rejectedEvent() {
        return new LeaveRequestRejectedEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
    }

    private EventStoreJpa storedEvent(Long id) {
        return new EventStoreJpa(
                id,
                LocalDate.of(2026, 8, 25),
                "TestEvent",
                "{}",
                StatusOfMessageDelivery.LOCAL,
                0
        );
    }
}
