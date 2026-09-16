package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import tools.jackson.databind.json.JsonMapper;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Message JSON conversion")
class RabbitMessageConverterConfigurationTests {
    @Test
    @DisplayName("Trusted integration events round-trip through the Rabbit JSON converter")
    void integrationEventRoundTrips() {
        MessageConverter converter = converter();
        LeaveRequestRejectedIntegrationEvent event =
                new LeaveRequestRejectedIntegrationEvent(
                        7L,
                        LocalDate.of(2026, 8, 25),
                        "request-1",
                        "staff-1"
                );

        Message message = converter.toMessage(event, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertEquals(event, assertInstanceOf(
                LeaveRequestRejectedIntegrationEvent.class,
                restored
        ));
    }

    @Test
    @DisplayName("Rabbit deserialisation does not use an unrestricted trusted-package wildcard")
    void trustedPackageIsRestricted() {
        assertNotEquals(
                "*",
                RabbitMessageConverterConfiguration.TRUSTED_INTEGRATION_EVENT_PACKAGE
        );
        MessageConverter converter = converter();
        Message message = converter.toMessage(
                new UntrustedPayload("data"),
                new MessageProperties()
        );

        assertThrows(IllegalArgumentException.class, () -> converter.fromMessage(message));
    }

    @Test
    @DisplayName("External HR contracts round-trip within the restricted trusted package")
    void hrIntegrationEventRoundTrips() {
        MessageConverter converter = converter();
        HrStaffMemberCreatedIntegrationEvent event = new HrStaffMemberCreatedIntegrationEvent(
                81L,
                LocalDate.of(2026, 8, 25),
                "staff-81",
                "Ada",
                "Lovelace",
                "ada@example.com",
                LocalDate.of(2024, 1, 1),
                "Engineering",
                "manager-1",
                "Developer",
                LocalDate.of(2024, 1, 1),
                "Senior",
                "Permanent",
                "ACTIVE"
        );

        Message message = converter.toMessage(event, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertEquals(event, assertInstanceOf(HrStaffMemberCreatedIntegrationEvent.class, restored));
    }

    private MessageConverter converter() {
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        return new RabbitMessageConverterConfiguration()
                .rabbitJsonMessageConverter(jsonMapper);
    }

    private record UntrustedPayload(String value) {
    }
}
