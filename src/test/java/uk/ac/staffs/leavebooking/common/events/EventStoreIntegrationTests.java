package uk.ac.staffs.leavebooking.common.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("Event history in the database")
class EventStoreIntegrationTests {
    @Autowired
    private EventStoreService eventStoreService;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEventStore() {
        eventStoreRepository.deleteAll();
    }

    @Test
    @DisplayName("An appended event survives H2 reload with valid JSON and its event date")
    void eventSurvivesDatabaseRoundTrip() throws Exception {
        LocalDate occurredOn = LocalDate.of(2026, 8, 25);
        Event event = new LeaveRequestSubmittedEvent(
                occurredOn,
                "request-1",
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18)
        );

        EventStoreJpa appended = eventStoreService.append(event);
        EventStoreJpa restored = eventStoreRepository.findById(appended.getId()).orElseThrow();

        assertNotNull(restored.getId());
        assertEquals(occurredOn, restored.getOccurredOn());
        assertEquals("LeaveRequestSubmittedEvent", restored.getEventType());
        assertEquals(StatusOfMessageDelivery.LOCAL, restored.getStatus());
        assertEquals(0, restored.getRetryCount());
        JsonNode body = assertDoesNotThrow(() -> objectMapper.readTree(restored.getEventBody()));
        assertEquals("request-1", body.get("leaveRequestId").asString());
        assertEquals("staff-1", body.get("staffMemberId").asString());
        assertEquals("manager-1", body.get("managerId").asString());
        assertEquals("2026-09-14", body.get("startDate").asString());
        assertEquals("2026-09-18", body.get("endDate").asString());
        assertEquals(1, eventStoreRepository.count());
    }

    @Test
    @DisplayName("The schema contains the Spring Modulith event-publication registry")
    void modulithEventPublicationRegistryExists() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EVENT_PUBLICATION'",
                Integer.class
        );
        var columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EVENT_PUBLICATION'",
                String.class
        );

        assertEquals(1, tableCount);
        assertTrue(columns.containsAll(List.of(
                "ID",
                "COMPLETION_DATE",
                "EVENT_TYPE",
                "LISTENER_ID",
                "PUBLICATION_DATE",
                "SERIALIZED_EVENT",
                "STATUS",
                "COMPLETION_ATTEMPTS",
                "LAST_RESUBMISSION_DATE"
        )));
    }
}
