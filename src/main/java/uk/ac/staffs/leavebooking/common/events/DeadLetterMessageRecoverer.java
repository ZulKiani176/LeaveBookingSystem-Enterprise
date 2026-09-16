package uk.ac.staffs.leavebooking.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
public class DeadLetterMessageRecoverer implements MessageRecoverer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeadLetterMessageRecoverer.class);
    private final ConfirmedRabbitPublisher publisher;

    public DeadLetterMessageRecoverer(ConfirmedRabbitPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        String sourceQueue = Objects.requireNonNull(
                message.getMessageProperties().getConsumerQueue(),
                "Failed Rabbit message has no source queue"
        );
        String deadLetterQueue = RabbitTopologyConfiguration.deadLetterQueueName(sourceQueue);
        message.getMessageProperties().setHeader("x-original-queue", sourceQueue);
        message.getMessageProperties().setHeader(
                "x-failure-type",
                cause == null ? "Unknown" : cause.getClass().getSimpleName()
        );
        try {
            publisher.send(
                    RabbitTopologyConfiguration.DEAD_LETTER_EXCHANGE,
                    deadLetterQueue,
                    message
            );
        } catch (AmqpException exception) {

            throw new ImmediateRequeueAmqpException(
                    "Dead-letter publication was not confirmed; retaining the original message",
                    exception
            );
        }
        LOGGER.error("Moved failed message from {} to {}", sourceQueue, deadLetterQueue);
    }
}
