package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;

import java.time.LocalDate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(RemoteOutboxRetryIntegrationTests.RetryConfiguration.class)
@TestPropertySource(properties = {
        "rabbitmq.outbox.retry.max-attempts=3",
        "rabbitmq.outbox.retry.initial-delay=1",
        "rabbitmq.outbox.retry.multiplier=1.0"
})
@DisplayName("Retrying undelivered messages")
class RemoteOutboxRetryIntegrationTests {
    @Autowired
    private RemoteOutboxListener listener;

    @Autowired
    private EventStoreService eventStoreService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitOutboxRouter rabbitOutboxRouter;

    @BeforeEach
    void resetMocks() {
        reset(eventStoreService, rabbitTemplate, rabbitOutboxRouter);
    }

    @Test
    @DisplayName("Spring Retry performs three failed attempts before recovery marks the event failed")
    void exhaustedPublicationRetriesThreeTimes() {
        RemoteEvent event = new LeaveRequestRejectedIntegrationEvent(
                7L,
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
        RabbitOutboxRouter.Destination destination = new RabbitOutboxRouter.Destination(
                "leave.events",
                "leave.request.rejected"
        );
        when(rabbitOutboxRouter.resolve(event)).thenReturn(destination);
        doThrow(new AmqpException("broker unavailable")).when(rabbitTemplate).convertAndSend(
                eq(destination.exchange()),
                eq(destination.routingKey()),
                eq(event), any(CorrelationData.class)
        );

        listener.handleRemoteEvent(event);

        verify(rabbitTemplate, times(3)).convertAndSend(
                eq(destination.exchange()),
                eq(destination.routingKey()),
                eq(event), any(CorrelationData.class)
        );
        verify(eventStoreService, times(3)).updateStatus(
                7L,
                StatusOfMessageDelivery.PENDING,
                true
        );
        verify(eventStoreService).updateStatus(
                7L,
                StatusOfMessageDelivery.FAILED,
                false
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRetry
    static class RetryConfiguration {
        @Bean
        EventStoreService eventStoreService() {
            return mock(EventStoreService.class);
        }

        @Bean
        RabbitTemplate rabbitTemplate() {
            return mock(RabbitTemplate.class);
        }

        @Bean
        RabbitOutboxRouter rabbitOutboxRouter() {
            return mock(RabbitOutboxRouter.class);
        }

        @Bean
        RemoteOutboxListener remoteOutboxListener(
                EventStoreService eventStoreService,
                RabbitTemplate rabbitTemplate,
                RabbitOutboxRouter rabbitOutboxRouter
        ) {
            return new RemoteOutboxListener(
                    eventStoreService,
                    rabbitTemplate,
                    rabbitOutboxRouter
            );
        }
    }
}
