package uk.ac.staffs.leavebooking.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;

@Service
public class RemoteEventDeliveryService {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RemoteEventDeliveryService.class);

    private final EventStoreService eventStoreService;
    private final ConfirmedRabbitPublisher publisher;
    private final RabbitOutboxRouter router;

    public RemoteEventDeliveryService(
            EventStoreService eventStoreService,
            ConfirmedRabbitPublisher publisher,
            RabbitOutboxRouter router
    ) {
        this.eventStoreService = eventStoreService;
        this.publisher = publisher;
        this.router = router;
    }

    public void deliver(RemoteEvent event) {
        RabbitOutboxRouter.Destination destination;
        try {
            destination = router.resolve(event);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Remote event {} is unroutable", event.id());
            eventStoreService.updateStatus(
                    event.id(), StatusOfMessageDelivery.UNROUTABLE, false
            );
            return;
        }
        try {
            publisher.publish(
                    destination.exchange(), destination.routingKey(), event
            );
            eventStoreService.updateStatus(
                    event.id(), StatusOfMessageDelivery.PUBLISHED, false
            );
        } catch (ConfirmedRabbitPublisher.UnroutableMessageException exception) {
            LOGGER.error("The broker returned remote event {} as unroutable", event.id());
            eventStoreService.updateStatus(
                    event.id(), StatusOfMessageDelivery.UNROUTABLE, false
            );
        } catch (AmqpException exception) {
            eventStoreService.updateStatus(
                    event.id(), StatusOfMessageDelivery.PENDING, true
            );
            throw exception;
        }
    }
}
