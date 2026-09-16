package uk.ac.staffs.leavebooking.audit.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.audit.application.LeaveAuditService;
import uk.ac.staffs.leavebooking.common.events.RabbitTopologyConfiguration;
import uk.ac.staffs.leavebooking.common.events.integration.*;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
@RabbitListener(queues = RabbitTopologyConfiguration.LEAVE_AUDIT_QUEUE)
public class LeaveAuditRabbitListener {
    private final LeaveAuditService service;

    public LeaveAuditRabbitListener(LeaveAuditService service) {
        this.service = service;
    }

    @RabbitHandler public void receive(LeaveRequestSubmittedIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(LeaveRequestApprovedIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(LeaveRequestRejectedIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(LeaveRequestReferredForHrApprovalIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(LeaveRequestCancelledIntegrationEvent event) { service.record(event); }
    @RabbitHandler public void receive(SickLeaveRecordedIntegrationEvent event) { service.record(event); }
}
