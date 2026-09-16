package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Rabbit Outbox Router")
class RabbitOutboxRouterTests {
    @Test
    @DisplayName("A configured integration event resolves to its exchange and routing key")
    void configuredEventResolves() {
        RabbitOutboxRouter router = new RabbitOutboxRouter();
        router.getBindings().put(
                LeaveRequestRejectedIntegrationEvent.class.getName(),
                new RabbitOutboxRouter.Destination(
                        "leave.events",
                        "leave.request.rejected"
                )
        );

        RabbitOutboxRouter.Destination destination = router.resolve(event());

        assertEquals("leave.events", destination.exchange());
        assertEquals("leave.request.rejected", destination.routingKey());
    }

    @Test
    @DisplayName("An integration event without a configured route fails clearly")
    void unknownEventIsRejected() {
        RabbitOutboxRouter router = new RabbitOutboxRouter();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                router.resolve(event())
        );

        assertEquals(
                RabbitOutboxRouter.DESTINATION_NOT_CONFIGURED + event().getClass().getName(),
                exception.getMessage()
        );
    }

    private LeaveRequestRejectedIntegrationEvent event() {
        return new LeaveRequestRejectedIntegrationEvent(
                7L,
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1"
        );
    }
}
