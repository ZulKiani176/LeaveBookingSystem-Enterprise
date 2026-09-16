package uk.ac.staffs.leavebooking.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression(
        "'${rabbitmq.enabled:false}' == 'true' and "
                + "'${rabbitmq.outbox.recovery.enabled:true}' == 'true'"
)
public class RemoteOutboxRecovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteOutboxRecovery.class);

    private final EventStoreRepository repository;
    private final RemoteEventTypeRegistry typeRegistry;
    private final RemoteEventDeliveryService deliveryService;
    private final EventStoreService eventStoreService;
    private final int batchSize;
    private final int maxAttempts;

    public RemoteOutboxRecovery(
            EventStoreRepository repository,
            RemoteEventTypeRegistry typeRegistry,
            RemoteEventDeliveryService deliveryService,
            EventStoreService eventStoreService,
            @Value("${rabbitmq.outbox.recovery.batch-size:50}") int batchSize,
            @Value("${rabbitmq.outbox.recovery.max-attempts:5}") int maxAttempts
    ) {
        this.repository = repository;
        this.typeRegistry = typeRegistry;
        this.deliveryService = deliveryService;
        this.eventStoreService = eventStoreService;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${rabbitmq.outbox.recovery.fixed-delay:30000}")
    public void recover() {
        List<EventStoreJpa> recoverable = repository
                .findByStatusInAndRetryCountLessThanOrderByIdAsc(
                        List.of(
                                StatusOfMessageDelivery.PENDING,
                                StatusOfMessageDelivery.FAILED
                        ),
                        maxAttempts,
                        PageRequest.of(0, batchSize)
                );
        for (EventStoreJpa storedEvent : recoverable) {
            try {
                deliveryService.deliver(typeRegistry.reconstruct(storedEvent));
            } catch (AmqpException exception) {
                if (storedEvent.getRetryCount() + 1 >= maxAttempts) {
                    eventStoreService.updateStatus(
                            storedEvent.getId(),
                            StatusOfMessageDelivery.FAILED,
                            false
                    );
                }
                LOGGER.warn("Outbox recovery could not deliver event {}", storedEvent.getId());
            } catch (IllegalArgumentException exception) {
                eventStoreService.updateStatus(
                        storedEvent.getId(),
                        StatusOfMessageDelivery.UNROUTABLE,
                        false
                );
                LOGGER.error("Outbox recovery rejected event {}", storedEvent.getId());
            }
        }
    }
}
