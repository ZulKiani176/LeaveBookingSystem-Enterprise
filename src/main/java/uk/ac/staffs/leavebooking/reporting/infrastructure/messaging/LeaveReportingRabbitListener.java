package uk.ac.staffs.leavebooking.reporting.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.common.events.RabbitTopologyConfiguration;
import uk.ac.staffs.leavebooking.common.events.integration.*;
import uk.ac.staffs.leavebooking.reporting.application.LeaveReportingService;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
@RabbitListener(queues = RabbitTopologyConfiguration.LEAVE_REPORTING_QUEUE)
public class LeaveReportingRabbitListener {
    private final LeaveReportingService service;

    public LeaveReportingRabbitListener(LeaveReportingService service) {
        this.service = service;
    }

    @RabbitHandler public void receive(LeaveRequestSubmittedIntegrationEvent event) {
        service.apply(event);
    }
    @RabbitHandler public void receive(SickLeaveRecordedIntegrationEvent event) {
        service.apply(event);
    }
    @RabbitHandler public void receive(LeaveRequestApprovedIntegrationEvent event) {
        service.apply(event);
    }
    @RabbitHandler public void receive(LeaveRequestRejectedIntegrationEvent event) {
        service.apply(event);
    }
    @RabbitHandler public void receive(LeaveRequestCancelledIntegrationEvent event) {
        service.apply(event);
    }
    @RabbitHandler public void receive(LeaveRequestReferredForHrApprovalIntegrationEvent event) {
        service.apply(event);
    }
}
