package uk.ac.staffs.leavebooking.common.events;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class ConfirmedRabbitPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMillis;

    public ConfirmedRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rabbitmq.publisher.confirm-timeout-millis:10000}") long confirmTimeoutMillis
    ) {
        if (confirmTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Publisher confirmation timeout must be positive");
        }
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    public void publish(String exchange, String routingKey, Object event) {
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.convertAndSend(exchange, routingKey, event, correlation);
        awaitConfirmation(correlation);
    }

    public void send(String exchange, String routingKey, Message message) {
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        awaitConfirmation(correlation);
    }

    private void awaitConfirmation(CorrelationData correlation) {
        try {
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
            if (correlation.getReturned() != null) {
                throw new UnroutableMessageException();
            }
            if (!confirm.ack()) {
                throw new AmqpException("The broker did not accept the message");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while awaiting broker confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new AmqpException("Broker confirmation was not received", exception);
        }
    }

    public static class UnroutableMessageException extends AmqpException {
        public UnroutableMessageException() {
            super("The broker returned an unroutable message");
        }
    }
}
