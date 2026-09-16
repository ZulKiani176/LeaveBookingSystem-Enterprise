package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.ImmediateRequeueAmqpException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("Handling failed messages")
class DeadLetterMessageRecovererTests {
    @Mock private ConfirmedRabbitPublisher publisher;

    @Test
    @DisplayName("An exhausted consumer message is republished once to its queue-specific DLQ")
    void exhaustedMessageIsRepublishedToDlq() {
        MessageProperties properties = new MessageProperties();
        properties.setConsumerQueue(RabbitTopologyConfiguration.LEAVE_AUDIT_QUEUE);
        Message message = new Message(new byte[]{1, 2, 3}, properties);

        new DeadLetterMessageRecoverer(publisher)
                .recover(message, new IllegalStateException("processing failed"));

        verify(publisher).send(
                RabbitTopologyConfiguration.DEAD_LETTER_EXCHANGE,
                "leave.audit.dlq",
                message
        );
        assertEquals("leave.audit", properties.getHeader("x-original-queue"));
        assertEquals("IllegalStateException", properties.getHeader("x-failure-type"));
    }

    @Test
    @DisplayName("A message without a source queue cannot be misrouted")
    void sourceQueueIsRequired() {
        Message message = new Message(new byte[0], new MessageProperties());

        assertThrows(NullPointerException.class, () ->
                new DeadLetterMessageRecoverer(publisher)
                        .recover(message, new RuntimeException())
        );
    }

    @Test
    @DisplayName("An unconfirmed dead-letter copy keeps the original message available for retry")
    void failedDeadLetterSendRequeuesOriginal() {
        MessageProperties properties = new MessageProperties();
        properties.setConsumerQueue("leave.audit");
        Message message = new Message(new byte[0], properties);
        doThrow(new AmqpException("No confirmation"))
                .when(publisher).send(anyString(), anyString(), any(Message.class));

        assertThrows(ImmediateRequeueAmqpException.class, () ->
                new DeadLetterMessageRecoverer(publisher).recover(message, new RuntimeException()));
    }
}
