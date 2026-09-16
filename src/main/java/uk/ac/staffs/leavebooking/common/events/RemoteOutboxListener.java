package uk.ac.staffs.leavebooking.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
public class RemoteOutboxListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteOutboxListener.class);

    private final RemoteEventDeliveryService deliveryService;
    private final EventStoreService eventStoreService;

    @Autowired
    public RemoteOutboxListener(
            RemoteEventDeliveryService deliveryService,
            EventStoreService eventStoreService
    ) {
        this.deliveryService = deliveryService;
        this.eventStoreService = eventStoreService;
    }

    public RemoteOutboxListener(
            EventStoreService eventStoreService,
            RabbitTemplate rabbitTemplate,
            RabbitOutboxRouter rabbitOutboxRouter
    ) {
        this(
                new RemoteEventDeliveryService(
                        eventStoreService, new ConfirmedRabbitPublisher(rabbitTemplate, 10000),
                        rabbitOutboxRouter
                ),
                eventStoreService
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
            retryFor = AmqpException.class,
            maxAttemptsExpression = "${rabbitmq.outbox.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${rabbitmq.outbox.retry.initial-delay:500}",
                    multiplierExpression = "${rabbitmq.outbox.retry.multiplier:2.0}"
            )
    )
    public void handleRemoteEvent(RemoteEvent event) {
        deliveryService.deliver(event);
    }

    @Recover
    public void recover(AmqpException exception, RemoteEvent event) {
        LOGGER.error(
                "Remote event {} failed after configured RabbitMQ attempts",
                event.id()
        );
        eventStoreService.updateStatus(
                event.id(), StatusOfMessageDelivery.FAILED, false
        );
    }
}
