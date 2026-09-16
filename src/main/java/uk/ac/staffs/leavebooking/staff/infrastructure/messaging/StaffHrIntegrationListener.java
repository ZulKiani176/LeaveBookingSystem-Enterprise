package uk.ac.staffs.leavebooking.staff.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.common.events.RabbitTopologyConfiguration;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffPersonalDetailsUpdatedIntegrationEvent;
import uk.ac.staffs.leavebooking.staff.application.StaffHrIntegrationService;

@Component
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
@RabbitListener(queues = RabbitTopologyConfiguration.STAFF_HR_UPDATES_QUEUE)
public class StaffHrIntegrationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaffHrIntegrationListener.class);

    private final StaffHrIntegrationService integrationService;

    public StaffHrIntegrationListener(StaffHrIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @RabbitHandler
    public void receive(HrStaffMemberCreatedIntegrationEvent event) {
        LOGGER.info(
                "Received HR integration event type={} sourceEventId={}",
                event.getClass().getSimpleName(),
                event.id()
        );
        integrationService.process(event);
    }

    @RabbitHandler
    public void receive(HrStaffPersonalDetailsUpdatedIntegrationEvent event) {
        LOGGER.info(
                "Received HR integration event type={} sourceEventId={}",
                event.getClass().getSimpleName(),
                event.id()
        );
        integrationService.process(event);
    }
}
