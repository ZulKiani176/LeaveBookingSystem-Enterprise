package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DisplayName("Rabbit Outbox Router Integration")
class RabbitOutboxRouterIntegrationTests {
    @Autowired
    private RabbitOutboxRouter router;

    @Test
    @DisplayName("Application YAML binds all Leave integration-event destinations")
    void yamlBindsEveryLeaveIntegrationEvent() {
        LocalDate date = LocalDate.of(2026, 8, 25);

        assertDestination(
                new LeaveRequestSubmittedIntegrationEvent(
                        date, "request-1", "staff-1", "manager-1", date, date
                ),
                RabbitTopologyConfiguration.SUBMITTED_ROUTING_KEY
        );
        assertDestination(
                new LeaveRequestApprovedIntegrationEvent(
                        date, "request-1", "staff-1", date, date
                ),
                RabbitTopologyConfiguration.APPROVED_ROUTING_KEY
        );
        assertDestination(
                new LeaveRequestRejectedIntegrationEvent(date, "request-1", "staff-1"),
                RabbitTopologyConfiguration.REJECTED_ROUTING_KEY
        );
        assertDestination(
                new LeaveRequestCancelledIntegrationEvent(
                        date, "request-1", "staff-1", date, date, "APPROVED"
                ),
                RabbitTopologyConfiguration.CANCELLED_ROUTING_KEY
        );
    }

    private void assertDestination(RemoteEvent event, String routingKey) {
        RabbitOutboxRouter.Destination destination = router.resolve(event);
        assertEquals(RabbitTopologyConfiguration.LEAVE_EVENTS_EXCHANGE, destination.exchange());
        assertEquals(routingKey, destination.routingKey());
    }
}
