package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recovering pending messages")
class RemoteOutboxRecoveryTests {
    @Mock private EventStoreRepository repository;
    @Mock private RemoteEventTypeRegistry typeRegistry;
    @Mock private RemoteEventDeliveryService deliveryService;
    @Mock private EventStoreService eventStoreService;
    @Mock private RemoteEvent remoteEvent;

    @Test
    @DisplayName("Recovery selects a bounded batch of pending and eligible failed events")
    void recoverySelectsBoundedEligibleBatch() {
        EventStoreJpa stored = stored(7L, StatusOfMessageDelivery.PENDING, 0);
        when(repository.findByStatusInAndRetryCountLessThanOrderByIdAsc(
                any(), eq(5), any()
        )).thenReturn(List.of(stored));
        when(typeRegistry.reconstruct(stored)).thenReturn(remoteEvent);

        recovery(50, 5).recover();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StatusOfMessageDelivery>> statuses =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByStatusInAndRetryCountLessThanOrderByIdAsc(
                statuses.capture(), eq(5), page.capture()
        );
        assertEquals(List.of(
                StatusOfMessageDelivery.PENDING,
                StatusOfMessageDelivery.FAILED
        ), statuses.getValue());
        assertEquals(50, page.getValue().getPageSize());
        verify(deliveryService).deliver(remoteEvent);
    }

    @Test
    @DisplayName("An event reaching the retry limit is marked failed")
    void exhaustedDeliveryIsMarkedFailed() {
        EventStoreJpa stored = stored(7L, StatusOfMessageDelivery.PENDING, 4);
        when(repository.findByStatusInAndRetryCountLessThanOrderByIdAsc(
                any(), eq(5), any()
        )).thenReturn(List.of(stored));
        when(typeRegistry.reconstruct(stored)).thenReturn(remoteEvent);
        doThrow(new AmqpException("broker unavailable"))
                .when(deliveryService).deliver(remoteEvent);

        recovery(50, 5).recover();

        verify(eventStoreService).updateStatus(
                7L, StatusOfMessageDelivery.FAILED, false
        );
    }

    @Test
    @DisplayName("An unapproved or corrupt stored event is marked unroutable")
    void rejectedStoredTypeIsMarkedUnroutable() {
        EventStoreJpa stored = stored(7L, StatusOfMessageDelivery.PENDING, 0);
        when(repository.findByStatusInAndRetryCountLessThanOrderByIdAsc(
                any(), eq(5), any()
        )).thenReturn(List.of(stored));
        when(typeRegistry.reconstruct(stored))
                .thenThrow(new IllegalArgumentException("not approved"));

        recovery(50, 5).recover();

        verify(eventStoreService).updateStatus(
                7L, StatusOfMessageDelivery.UNROUTABLE, false
        );
        verify(deliveryService, never()).deliver(any());
    }

    @Test
    @DisplayName("An empty recovery batch performs no delivery work")
    void emptyBatchIsSafe() {
        when(repository.findByStatusInAndRetryCountLessThanOrderByIdAsc(
                any(), eq(5), any()
        )).thenReturn(List.of());

        recovery(25, 5).recover();

        verify(typeRegistry, never()).reconstruct(any());
        verify(deliveryService, never()).deliver(any());
    }

    private RemoteOutboxRecovery recovery(int batchSize, int maxAttempts) {
        return new RemoteOutboxRecovery(
                repository,
                typeRegistry,
                deliveryService,
                eventStoreService,
                batchSize,
                maxAttempts
        );
    }

    private EventStoreJpa stored(Long id, StatusOfMessageDelivery status, int retryCount) {
        return new EventStoreJpa(
                id, LocalDate.parse("2026-08-24"), "RemoteEvent", "{}",
                status, retryCount
        );
    }
}
