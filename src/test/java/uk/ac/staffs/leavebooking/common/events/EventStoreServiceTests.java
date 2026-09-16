package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Saving event history")
class EventStoreServiceTests {
    @Mock
    private EventStoreRepository eventStoreRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventStoreService eventStoreService;

    @Test
    @DisplayName("Appending an event serialises and stores its factual data")
    void appendStoresEvent() {
        Event event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"leaveRequestId\":\"request-1\"}");
        when(eventStoreRepository.save(any(EventStoreJpa.class))).thenAnswer(invocation -> {
            EventStoreJpa saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        EventStoreJpa result = eventStoreService.append(event);

        ArgumentCaptor<EventStoreJpa> captor = ArgumentCaptor.forClass(EventStoreJpa.class);
        verify(eventStoreRepository).save(captor.capture());
        EventStoreJpa stored = captor.getValue();
        assertEquals(7L, result.getId());
        assertEquals(event.occurredOn(), stored.getOccurredOn());
        assertEquals("LeaveRequestRejectedEvent", stored.getEventType());
        assertEquals("{\"leaveRequestId\":\"request-1\"}", stored.getEventBody());
        assertEquals(StatusOfMessageDelivery.LOCAL, stored.getStatus());
        assertEquals(0, stored.getRetryCount());
    }

    @Test
    @DisplayName("A remote event is initially stored as pending with no retries")
    void remoteEventStartsPending() {
        Event event = new LeaveRequestRejectedIntegrationEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(eventStoreRepository.save(any(EventStoreJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventStoreJpa stored = eventStoreService.append(event);

        assertEquals(StatusOfMessageDelivery.PENDING, stored.getStatus());
        assertEquals(0, stored.getRetryCount());
    }

    @Test
    @DisplayName("Updating delivery status can increment retry count")
    void updateStatusIncrementsRetryCount() {
        EventStoreJpa stored = storedEvent();
        when(eventStoreRepository.findById(7L)).thenReturn(Optional.of(stored));

        eventStoreService.updateStatus(7L, StatusOfMessageDelivery.PENDING, true);

        assertEquals(StatusOfMessageDelivery.PENDING, stored.getStatus());
        assertEquals(1, stored.getRetryCount());
        verify(eventStoreRepository).save(stored);
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(
            value = StatusOfMessageDelivery.class,
            names = {"PUBLISHED", "FAILED", "UNROUTABLE"}
    )
    @DisplayName("Changing delivery status does not add a retry")
    void finalStatusWithoutIncrementPreservesRetryCount(StatusOfMessageDelivery status) {
        EventStoreJpa stored = storedEvent();
        stored.setRetryCount(2);
        when(eventStoreRepository.findById(7L)).thenReturn(Optional.of(stored));

        eventStoreService.updateStatus(7L, status, false);

        assertEquals(status, stored.getStatus());
        assertEquals(2, stored.getRetryCount());
    }

    @Test
    @DisplayName("Updating a missing event-store entry fails clearly")
    void missingEventStoreEntryIsRejected() {
        when(eventStoreRepository.findById(99L)).thenReturn(Optional.empty());

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                eventStoreService.updateStatus(99L, StatusOfMessageDelivery.FAILED, false)
        );

        assertEquals(EventStoreService.EVENT_STORE_ENTRY_NOT_FOUND + 99L, exception.getMessage());
        verify(eventStoreRepository, never()).save(any());
    }

    @Test
    @DisplayName("A null event is rejected before serialisation or persistence")
    void nullEventIsRejected() {
        Throwable exception = assertThrows(NullPointerException.class, () ->
                eventStoreService.append(null)
        );

        assertEquals(EventStoreService.EVENT_NOT_NULL, exception.getMessage());
        verifyNoInteractions(objectMapper, eventStoreRepository);
    }

    @Test
    @DisplayName("JSON serialisation failure is surfaced clearly and is not persisted")
    void serialisationFailureIsSurfaced() {
        Event event = event();
        when(objectMapper.writeValueAsString(event)).thenThrow(new JacksonException("broken") {
        });

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                eventStoreService.append(event)
        );

        assertEquals(EventStoreService.EVENT_SERIALISATION_FAILED, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(JacksonException.class, exception.getCause());
        verify(eventStoreRepository, never()).save(any());
    }

    private Event event() {
        return new LeaveRequestRejectedEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
    }

    private EventStoreJpa storedEvent() {
        return new EventStoreJpa(
                7L,
                LocalDate.of(2026, 8, 25),
                "LeaveRequestRejectedIntegrationEvent",
                "{}",
                StatusOfMessageDelivery.PENDING,
                0
        );
    }
}
