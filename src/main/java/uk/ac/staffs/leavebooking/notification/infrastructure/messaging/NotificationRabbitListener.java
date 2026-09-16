package uk.ac.staffs.leavebooking.notification.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.common.events.RabbitTopologyConfiguration;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.notification.application.NotificationService;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
@RabbitListener(queues = RabbitTopologyConfiguration.LEAVE_NOTIFICATIONS_QUEUE)
public class NotificationRabbitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRabbitListener.class);

    private final NotificationService notificationService;

    public NotificationRabbitListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitHandler
    public void receive(LeaveRequestSubmittedIntegrationEvent event) {
        LOGGER.info("Received submitted leave integration event {}", event.id());
        notificationService.createManagerPendingNotification(event);
    }

    @RabbitHandler
    public void receive(SickLeaveRecordedIntegrationEvent event) {
        LOGGER.info("Received recorded sick-leave integration event {}", event.id());
        notificationService.createManagerSickLeaveRecordedNotification(event);
    }

    @RabbitHandler
    public void receive(LeaveRequestApprovedIntegrationEvent event) {
        LOGGER.info("Received approved leave integration event {}", event.id());
        notificationService.createStaffApprovedNotification(event);
    }

    @RabbitHandler
    public void receive(LeaveRequestRejectedIntegrationEvent event) {
        LOGGER.info("Received rejected leave integration event {}", event.id());
        notificationService.createStaffRejectedNotification(event);
    }

    @RabbitHandler
    public void receive(LeaveRequestCancelledIntegrationEvent event) {
        LOGGER.info("Received cancelled leave integration event {}", event.id());
        notificationService.createStaffCancelledNotification(event);
    }
}
