package uk.ac.staffs.leavebooking;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "rabbitmq.enabled=false",
        "firebase.enabled=false",
        "rate-limit.enabled=false",
        "logging.file.name=target/test-logs/persistent-logging.log"
})
@DisplayName("Saved application logs")
class PersistentLoggingIntegrationTests {
    private static final String MARKER = "PERSISTENT_LOGGING_SMOKE_TEST";

    @Autowired private Environment environment;

    @Test
    @DisplayName("Application events are saved in a log file with size and history limits")
    void configuredRollingFileRetainsApplicationEvents() throws Exception {
        LoggerFactory.getLogger(PersistentLoggingIntegrationTests.class).info(MARKER);
        Path logFile = Path.of(environment.getRequiredProperty("logging.file.name"));

        assertTrue(Files.exists(logFile));
        assertTrue(Files.readString(logFile).contains(MARKER));
        assertEquals("10MB", environment.getProperty(
                "logging.logback.rollingpolicy.max-file-size"
        ));
        assertEquals("7", environment.getProperty(
                "logging.logback.rollingpolicy.max-history"
        ));
        assertEquals("70MB", environment.getProperty(
                "logging.logback.rollingpolicy.total-size-cap"
        ));
    }
}
