package uk.ac.staffs.leavebooking.common.events;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;

@Component
@ConfigurationProperties(prefix = "rabbitmq.outbox")
public class RabbitOutboxRouter {
    public static final String EVENT_NOT_NULL = "Remote event cannot be null";
    public static final String DESTINATION_NOT_CONFIGURED =
            "No RabbitMQ destination configured for ";
    public static final String EXCHANGE_NOT_EMPTY = "RabbitMQ exchange cannot be empty";
    public static final String ROUTING_KEY_NOT_EMPTY = "RabbitMQ routing key cannot be empty";

    public record Destination(String exchange, String routingKey) {
        public Destination {
            exchange = argumentNotEmpty(exchange, EXCHANGE_NOT_EMPTY);
            routingKey = argumentNotEmpty(routingKey, ROUTING_KEY_NOT_EMPTY);
        }
    }

    private final Map<String, Destination> bindings = new HashMap<>();

    public Map<String, Destination> getBindings() {
        return bindings;
    }

    public Destination resolve(RemoteEvent event) {
        RemoteEvent validatedEvent = Objects.requireNonNull(event, EVENT_NOT_NULL);
        String className = validatedEvent.getClass().getName();
        Destination destination = bindings.get(className);
        if (destination == null) {
            throw new IllegalArgumentException(DESTINATION_NOT_CONFIGURED + className);
        }
        return destination;
    }
}
