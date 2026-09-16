package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import uk.ac.staffs.leavebooking.notification.infrastructure.messaging.NotificationRabbitListener;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = "rabbitmq.enabled=false")
@DisplayName("Running without RabbitMQ")
class RabbitDisabledIntegrationTests {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Disabled broker mode does not create publishers, consumers or topology")
    void disabledModeCreatesNoActiveBrokerComponents() {
        assertFalse(applicationContext.containsBeanDefinition("remoteOutboxListener"));
        assertFalse(applicationContext.containsBeanDefinition("notificationRabbitListener"));
        assertFalse(applicationContext.containsBeanDefinition("leaveNotificationTopology"));
        assertFalse(applicationContext.getBeansOfType(RemoteOutboxListener.class).containsKey(
                "remoteOutboxListener"
        ));
        assertFalse(applicationContext.getBeansOfType(NotificationRabbitListener.class).containsKey(
                "notificationRabbitListener"
        ));
    }
}
