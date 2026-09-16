package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message queues and routing")
class RabbitTopologyConfigurationTests {
    @Test
    @DisplayName("HR referrals reach the operational reporting queue")
    void referralsReachReporting() {
        assertEquals(Set.of(
                RabbitTopologyConfiguration.SUBMITTED_ROUTING_KEY,
                RabbitTopologyConfiguration.APPROVED_ROUTING_KEY,
                RabbitTopologyConfiguration.REJECTED_ROUTING_KEY,
                RabbitTopologyConfiguration.CANCELLED_ROUTING_KEY,
                RabbitTopologyConfiguration.SICK_RECORDED_ROUTING_KEY,
                RabbitTopologyConfiguration.REFERRED_HR_ROUTING_KEY
        ), routes(new RabbitTopologyConfiguration().leaveNotificationTopology(),
                RabbitTopologyConfiguration.LEAVE_REPORTING_QUEUE));
    }
    @Test
    @DisplayName("Leave topology declares four independent business queues and four DLQs")
    void leaveTopologyContainsExpectedBrokerResources() {
        Declarables topology = new RabbitTopologyConfiguration().leaveNotificationTopology();

        Set<String> exchanges = topology.getDeclarablesByType(DirectExchange.class).stream()
                .map(DirectExchange::getName)
                .collect(Collectors.toSet());
        Set<String> queues = queueNames(topology);

        assertEquals(Set.of(
                RabbitTopologyConfiguration.LEAVE_EVENTS_EXCHANGE,
                RabbitTopologyConfiguration.DEAD_LETTER_EXCHANGE
        ), exchanges);
        assertEquals(Set.of(
                RabbitTopologyConfiguration.LEAVE_NOTIFICATIONS_QUEUE,
                RabbitTopologyConfiguration.LEAVE_REPORTING_QUEUE,
                RabbitTopologyConfiguration.LEAVE_AUDIT_QUEUE,
                RabbitTopologyConfiguration.LEAVE_HR_SYNC_QUEUE,
                "leave.notifications.dlq",
                "leave.reporting.dlq",
                "leave.audit.dlq",
                "leave.hr-sync.dlq"
        ), queues);
        assertTrue(topology.getDeclarablesByType(Queue.class).stream().allMatch(Queue::isDurable));
    }

    @Test
    @DisplayName("Leave consumers have routing keys matching their independent responsibilities")
    void leaveBindingsMatchSubscriberResponsibilities() {
        Declarables topology = new RabbitTopologyConfiguration().leaveNotificationTopology();
        Set<String> allLeaveLifecycleRoutes = Set.of(
                RabbitTopologyConfiguration.SUBMITTED_ROUTING_KEY,
                RabbitTopologyConfiguration.APPROVED_ROUTING_KEY,
                RabbitTopologyConfiguration.REJECTED_ROUTING_KEY,
                RabbitTopologyConfiguration.CANCELLED_ROUTING_KEY,
                RabbitTopologyConfiguration.SICK_RECORDED_ROUTING_KEY
        );

        assertEquals(allLeaveLifecycleRoutes, routes(topology,
                RabbitTopologyConfiguration.LEAVE_NOTIFICATIONS_QUEUE));
        assertEquals(Set.of(
                RabbitTopologyConfiguration.SUBMITTED_ROUTING_KEY,
                RabbitTopologyConfiguration.APPROVED_ROUTING_KEY,
                RabbitTopologyConfiguration.REJECTED_ROUTING_KEY,
                RabbitTopologyConfiguration.CANCELLED_ROUTING_KEY,
                RabbitTopologyConfiguration.SICK_RECORDED_ROUTING_KEY,
                RabbitTopologyConfiguration.REFERRED_HR_ROUTING_KEY
        ), routes(topology, RabbitTopologyConfiguration.LEAVE_AUDIT_QUEUE));
        assertEquals(Set.of(
                RabbitTopologyConfiguration.APPROVED_ROUTING_KEY,
                RabbitTopologyConfiguration.CANCELLED_ROUTING_KEY,
                RabbitTopologyConfiguration.SICK_RECORDED_ROUTING_KEY
        ), routes(topology, RabbitTopologyConfiguration.LEAVE_HR_SYNC_QUEUE));
    }

    @Test
    @DisplayName("Every Leave business queue has a separately bound durable DLQ")
    void leaveDeadLetterQueuesAreBound() {
        Declarables topology = new RabbitTopologyConfiguration().leaveNotificationTopology();

        for (String businessQueue : Set.of(
                RabbitTopologyConfiguration.LEAVE_NOTIFICATIONS_QUEUE,
                RabbitTopologyConfiguration.LEAVE_REPORTING_QUEUE,
                RabbitTopologyConfiguration.LEAVE_AUDIT_QUEUE,
                RabbitTopologyConfiguration.LEAVE_HR_SYNC_QUEUE
        )) {
            String dlq = RabbitTopologyConfiguration.deadLetterQueueName(businessQueue);
            assertEquals(Set.of(dlq), routes(topology, dlq));
        }
    }

    @Test
    @DisplayName("HR topology declares its business queue and separately bound DLQ")
    void hrTopologyContainsExpectedBrokerResources() {
        Declarables topology = new RabbitTopologyConfiguration().staffHrUpdateTopology();

        assertEquals(Set.of(
                RabbitTopologyConfiguration.HR_EVENTS_EXCHANGE,
                RabbitTopologyConfiguration.DEAD_LETTER_EXCHANGE
        ), topology.getDeclarablesByType(DirectExchange.class).stream()
                .map(DirectExchange::getName)
                .collect(Collectors.toSet()));
        assertEquals(Set.of(
                RabbitTopologyConfiguration.STAFF_HR_UPDATES_QUEUE,
                "staff.hr-updates.dlq"
        ), queueNames(topology));
        assertEquals(Set.of(
                RabbitTopologyConfiguration.HR_STAFF_CREATED_ROUTING_KEY,
                RabbitTopologyConfiguration.HR_PERSONAL_DETAILS_UPDATED_ROUTING_KEY
        ), routes(topology, RabbitTopologyConfiguration.STAFF_HR_UPDATES_QUEUE));
        assertEquals(Set.of("staff.hr-updates.dlq"), routes(topology, "staff.hr-updates.dlq"));
    }

    private Set<String> queueNames(Declarables topology) {
        return topology.getDeclarablesByType(Queue.class).stream()
                .map(Queue::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> routes(Declarables topology, String destination) {
        return topology.getDeclarablesByType(Binding.class).stream()
                .filter(binding -> destination.equals(binding.getDestination()))
                .map(Binding::getRoutingKey)
                .collect(Collectors.toSet());
    }
}
