package uk.ac.staffs.leavebooking.common.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
public class RabbitTopologyConfiguration {
    public static final String LEAVE_EVENTS_EXCHANGE = "leave.events";
    public static final String HR_EVENTS_EXCHANGE = "hr.events";
    public static final String DEAD_LETTER_EXCHANGE = "dead-letter.events";

    public static final String LEAVE_NOTIFICATIONS_QUEUE = "leave.notifications";
    public static final String LEAVE_REPORTING_QUEUE = "leave.reporting";
    public static final String LEAVE_AUDIT_QUEUE = "leave.audit";
    public static final String LEAVE_HR_SYNC_QUEUE = "leave.hr-sync";
    public static final String STAFF_HR_UPDATES_QUEUE = "staff.hr-updates";

    public static final String SUBMITTED_ROUTING_KEY = "leave.request.submitted";
    public static final String APPROVED_ROUTING_KEY = "leave.request.approved";
    public static final String REJECTED_ROUTING_KEY = "leave.request.rejected";
    public static final String CANCELLED_ROUTING_KEY = "leave.request.cancelled";
    public static final String SICK_RECORDED_ROUTING_KEY = "leave.sick.recorded";
    public static final String REFERRED_HR_ROUTING_KEY = "leave.request.referred-hr";
    public static final String HR_STAFF_CREATED_ROUTING_KEY = "hr.staff.created";
    public static final String HR_PERSONAL_DETAILS_UPDATED_ROUTING_KEY =
            "hr.staff.personal-details-updated";

    public static String deadLetterQueueName(String businessQueue) {
        return businessQueue + ".dlq";
    }

    @Bean
    Declarables leaveNotificationTopology() {
        DirectExchange exchange = new DirectExchange(LEAVE_EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue notifications = durableQueue(LEAVE_NOTIFICATIONS_QUEUE);
        Queue reporting = durableQueue(LEAVE_REPORTING_QUEUE);
        Queue audit = durableQueue(LEAVE_AUDIT_QUEUE);
        Queue hrSync = durableQueue(LEAVE_HR_SYNC_QUEUE);
        Queue notificationsDlq = deadLetterQueue(LEAVE_NOTIFICATIONS_QUEUE);
        Queue reportingDlq = deadLetterQueue(LEAVE_REPORTING_QUEUE);
        Queue auditDlq = deadLetterQueue(LEAVE_AUDIT_QUEUE);
        Queue hrSyncDlq = deadLetterQueue(LEAVE_HR_SYNC_QUEUE);

        return new Declarables(
                exchange,
                deadLetterExchange,
                notifications,
                reporting,
                audit,
                hrSync,
                notificationsDlq,
                reportingDlq,
                auditDlq,
                hrSyncDlq,
                BindingBuilder.bind(notifications).to(exchange).with(SUBMITTED_ROUTING_KEY),
                BindingBuilder.bind(notifications).to(exchange).with(APPROVED_ROUTING_KEY),
                BindingBuilder.bind(notifications).to(exchange).with(REJECTED_ROUTING_KEY),
                BindingBuilder.bind(notifications).to(exchange).with(CANCELLED_ROUTING_KEY),
                BindingBuilder.bind(notifications).to(exchange).with(SICK_RECORDED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(SUBMITTED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(APPROVED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(REJECTED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(CANCELLED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(SICK_RECORDED_ROUTING_KEY),
                BindingBuilder.bind(reporting).to(exchange).with(REFERRED_HR_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(SUBMITTED_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(APPROVED_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(REJECTED_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(CANCELLED_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(SICK_RECORDED_ROUTING_KEY),
                BindingBuilder.bind(audit).to(exchange).with(REFERRED_HR_ROUTING_KEY),
                BindingBuilder.bind(hrSync).to(exchange).with(APPROVED_ROUTING_KEY),
                BindingBuilder.bind(hrSync).to(exchange).with(CANCELLED_ROUTING_KEY),
                BindingBuilder.bind(hrSync).to(exchange).with(SICK_RECORDED_ROUTING_KEY),
                deadLetterBinding(notificationsDlq, deadLetterExchange),
                deadLetterBinding(reportingDlq, deadLetterExchange),
                deadLetterBinding(auditDlq, deadLetterExchange),
                deadLetterBinding(hrSyncDlq, deadLetterExchange)
        );
    }

    @Bean
    Declarables staffHrUpdateTopology() {
        DirectExchange exchange = new DirectExchange(HR_EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue queue = durableQueue(STAFF_HR_UPDATES_QUEUE);
        Queue deadLetterQueue = deadLetterQueue(STAFF_HR_UPDATES_QUEUE);

        return new Declarables(
                exchange,
                deadLetterExchange,
                queue,
                deadLetterQueue,
                BindingBuilder.bind(queue).to(exchange).with(HR_STAFF_CREATED_ROUTING_KEY),
                BindingBuilder.bind(queue).to(exchange)
                        .with(HR_PERSONAL_DETAILS_UPDATED_ROUTING_KEY),
                deadLetterBinding(deadLetterQueue, deadLetterExchange)
        );
    }

    private static Queue durableQueue(String name) {
        return QueueBuilder.durable(name).build();
    }

    private static Queue deadLetterQueue(String businessQueue) {
        return QueueBuilder.durable(deadLetterQueueName(businessQueue)).build();
    }

    private static Binding deadLetterBinding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queue.getName());
    }
}
