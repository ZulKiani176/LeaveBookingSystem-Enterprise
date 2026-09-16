package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("Publishing messages after commit")
class RemoteOutboxListenerTests {
    @Mock
    private EventStoreService eventStoreService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitOutboxRouter rabbitOutboxRouter;

    @Test
    @DisplayName("Successful publication uses the resolved route and marks the event published")
    void successfulPublicationMarksPublished() {
        RemoteEvent event = event();
        RabbitOutboxRouter.Destination destination = new RabbitOutboxRouter.Destination(
                "leave.events",
                "leave.request.rejected"
        );
        when(rabbitOutboxRouter.resolve(event)).thenReturn(destination);

        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(destination.exchange()), eq(destination.routingKey()), eq(event),
                any(CorrelationData.class)
        );

        listener().handleRemoteEvent(event);

        verify(rabbitTemplate).convertAndSend(
                eq(destination.exchange()),
                eq(destination.routingKey()),
                eq(event), any(CorrelationData.class)
        );
        verify(eventStoreService).updateStatus(
                7L,
                StatusOfMessageDelivery.PUBLISHED,
                false
        );
    }

    @Test
    @DisplayName("An event without a route is marked unroutable and is not sent")
    void unroutableEventIsRecordedWithoutSend() {
        RemoteEvent event = event();
        when(rabbitOutboxRouter.resolve(event))
                .thenThrow(new IllegalArgumentException("missing route"));

        listener().handleRemoteEvent(event);

        verifyNoInteractions(rabbitTemplate);
        verify(eventStoreService).updateStatus(
                7L,
                StatusOfMessageDelivery.UNROUTABLE,
                false
        );
    }

    @Test
    @DisplayName("A failed Rabbit attempt increments retry count and remains retryable")
    void failedAttemptIsCountedAndRethrown() {
        RemoteEvent event = event();
        RabbitOutboxRouter.Destination destination = new RabbitOutboxRouter.Destination(
                "leave.events",
                "leave.request.rejected"
        );
        when(rabbitOutboxRouter.resolve(event)).thenReturn(destination);
        AmqpException failure = new AmqpException("broker unavailable");
        org.mockito.Mockito.doThrow(failure).when(rabbitTemplate).convertAndSend(
                eq(destination.exchange()),
                eq(destination.routingKey()),
                eq(event), any(CorrelationData.class)
        );

        AmqpException thrown = assertThrows(
                AmqpException.class,
                () -> listener().handleRemoteEvent(event)
        );

        assertEquals(failure, thrown);
        verify(eventStoreService).updateStatus(
                7L,
                StatusOfMessageDelivery.PENDING,
                true
        );
    }

    @Test
    @DisplayName("Retry recovery marks an exhausted event failed without another increment")
    void recoveryMarksFailed() {
        RemoteEvent event = event();

        listener().recover(new AmqpException("still unavailable"), event);

        verify(eventStoreService).updateStatus(
                7L,
                StatusOfMessageDelivery.FAILED,
                false
        );
    }

    @Test
    @DisplayName("Remote publication is asynchronous and starts only after commit")
    void listenerUsesAfterCommitAsynchronously() throws Exception {
        Method method = RemoteOutboxListener.class.getMethod(
                "handleRemoteEvent",
                RemoteEvent.class
        );

        Async async = method.getAnnotation(Async.class);
        TransactionalEventListener transactional = method.getAnnotation(
                TransactionalEventListener.class
        );
        Retryable retryable = method.getAnnotation(Retryable.class);

        assertNotNull(async);
        assertNotNull(transactional);
        assertNotNull(retryable);
        assertEquals(TransactionPhase.AFTER_COMMIT, transactional.phase());
        assertEquals("${rabbitmq.outbox.retry.max-attempts:3}",
                retryable.maxAttemptsExpression());
        assertEquals("${rabbitmq.outbox.retry.initial-delay:500}",
                retryable.backoff().delayExpression());
        assertEquals("${rabbitmq.outbox.retry.multiplier:2.0}",
                retryable.backoff().multiplierExpression());
    }

    private RemoteOutboxListener listener() {
        return new RemoteOutboxListener(
                eventStoreService,
                rabbitTemplate,
                rabbitOutboxRouter
        );
    }

    private RemoteEvent event() {
        return new LeaveRequestRejectedIntegrationEvent(
                7L,
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
    }
}
