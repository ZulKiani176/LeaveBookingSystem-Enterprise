package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Broker-confirmed event delivery")
class ConfirmedRabbitPublisherTests {
    @Mock private RabbitTemplate template;
    @Mock private EventStoreService store;
    @Mock private RabbitOutboxRouter router;

    @Test
    @DisplayName("A broker acknowledgement allows the event to be marked published")
    void acknowledgedEventIsPublished() {
        arrangeSend(correlation -> correlation.getFuture()
                .complete(new CorrelationData.Confirm(true, null)));

        delivery().deliver(event());

        verify(store).updateStatus(7L, StatusOfMessageDelivery.PUBLISHED, false);
    }

    @Test
    @DisplayName("A negative broker acknowledgement keeps the event retryable")
    void negativeAcknowledgementDoesNotPublish() {
        arrangeSend(correlation -> correlation.getFuture()
                .complete(new CorrelationData.Confirm(false, "Rejected")));

        assertThrows(AmqpException.class, () -> delivery().deliver(event()));

        assertRetryable();
    }

    @Test
    @DisplayName("An acknowledged but returned message is marked unroutable, not published")
    void returnedMessageDoesNotPublish() {
        arrangeSend(correlation -> {
            correlation.setReturned(new ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", "leave.events", "leave.request.rejected"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        delivery().deliver(event());

        verify(store).updateStatus(7L, StatusOfMessageDelivery.UNROUTABLE, false);
        verify(store, never()).updateStatus(anyLong(), eq(StatusOfMessageDelivery.PUBLISHED), anyBoolean());
    }

    @Test
    @DisplayName("A missing confirmation times out and leaves the event retryable")
    void timeoutDoesNotPublish() {
        arrangeSend(correlation -> { });

        assertThrows(AmqpException.class, () -> delivery().deliver(event()));

        assertRetryable();
    }

    @Test
    @DisplayName("A failed confirmation future leaves the event retryable")
    void failedFutureDoesNotPublish() {
        arrangeSend(correlation -> correlation.getFuture()
                .completeExceptionally(new IllegalStateException("Connection closed")));

        assertThrows(AmqpException.class, () -> delivery().deliver(event()));

        assertRetryable();
    }

    @Test
    @DisplayName("A confirmed dead-letter copy can complete recovery")
    void rawMessageUsesConfirmation() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        assertDoesNotThrow(() -> new ConfirmedRabbitPublisher(template, 10).send(
                "dead-letter.events", "leave.audit.dlq",
                new Message(new byte[0], new MessageProperties())));
    }

    private void arrangeSend(Consumer<CorrelationData> response) {
        when(router.resolve(event())).thenReturn(new RabbitOutboxRouter.Destination(
                "leave.events", "leave.request.rejected"));
        doAnswer(invocation -> {
            response.accept(invocation.getArgument(3));
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }

    private RemoteEventDeliveryService delivery() {
        return new RemoteEventDeliveryService(store, new ConfirmedRabbitPublisher(template, 10), router);
    }

    private RemoteEvent event() {
        return new LeaveRequestRejectedIntegrationEvent(
                7L, LocalDate.of(2026, 9, 14), "request-1", "staff-1");
    }

    private void assertRetryable() {
        verify(store).updateStatus(7L, StatusOfMessageDelivery.PENDING, true);
        verify(store, never()).updateStatus(anyLong(), eq(StatusOfMessageDelivery.PUBLISHED), anyBoolean());
    }
}
