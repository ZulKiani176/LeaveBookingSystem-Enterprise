package uk.ac.staffs.leavebooking.common.events;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "rabbitmq", name = "enabled", havingValue = "true")
public class RabbitConsumerConfiguration {
    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            DeadLetterMessageRecoverer recoverer,
            @Value("${rabbitmq.consumer.retry.max-attempts:3}") int maxAttempts,
            @Value("${rabbitmq.consumer.retry.initial-delay:500}") long initialDelay,
            @Value("${rabbitmq.consumer.retry.multiplier:2.0}") double multiplier,
            @Value("${rabbitmq.consumer.retry.max-delay:2000}") long maxDelay
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setDefaultRequeueRejected(false);
        Advice retryAdvice = RetryInterceptorBuilder.stateless()
                .maxRetries(Math.max(0, maxAttempts - 1))
                .backOffOptions(initialDelay, multiplier, maxDelay)
                .recoverer(recoverer)
                .build();
        factory.setAdviceChain(retryAdvice);
        return factory;
    }
}
