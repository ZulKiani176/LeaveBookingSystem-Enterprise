package uk.ac.staffs.leavebooking.hrsync.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.common.events.RabbitTopologyConfiguration;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.hrsync.application.HrAbsenceSyncService;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
@RabbitListener(queues = RabbitTopologyConfiguration.LEAVE_HR_SYNC_QUEUE)
public class HrAbsenceSyncRabbitListener {
    private final HrAbsenceSyncService service;

    public HrAbsenceSyncRabbitListener(HrAbsenceSyncService service) {
        this.service = service;
    }

    @RabbitHandler public void receive(LeaveRequestApprovedIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(LeaveRequestCancelledIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(SickLeaveRecordedIntegrationEvent event) { service.record(event); }
}
