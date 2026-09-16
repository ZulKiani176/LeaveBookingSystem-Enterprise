package uk.ac.staffs.leavebooking.common.events;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMessageConverterConfiguration {
    public static final String TRUSTED_INTEGRATION_EVENT_PACKAGE =
            "uk.ac.staffs.leavebooking.common.events.integration";

    @Bean
    MessageConverter rabbitJsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(
                jsonMapper,
                TRUSTED_INTEGRATION_EVENT_PACKAGE
        );
    }
}
