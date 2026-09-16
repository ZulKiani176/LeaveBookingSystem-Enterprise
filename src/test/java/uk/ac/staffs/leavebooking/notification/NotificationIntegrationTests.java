package uk.ac.staffs.leavebooking.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import uk.ac.staffs.leavebooking.common.events.EventStoreJpa;
import uk.ac.staffs.leavebooking.common.events.EventStoreRepository;
import uk.ac.staffs.leavebooking.common.events.EventStoreService;
import uk.ac.staffs.leavebooking.common.events.StatusOfMessageDelivery;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.notification.application.NotificationService;
import uk.ac.staffs.leavebooking.notification.infrastructure.messaging.NotificationRabbitListener;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "rabbitmq.enabled=false")
@AutoConfigureMockMvc
@WithMockFirebaseUser
@DisplayName("Notification Integration")
class NotificationIntegrationTests {
    @Autowired
    private EventStoreService eventStoreService;

    @Autowired
    private EventStoreRepository eventStoreRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearPersistence() {
        notificationRepository.deleteAll();
        eventStoreRepository.deleteAll();
    }

    @Test
    @DisplayName("Direct broker consumption is idempotent and visible through the inbox API")
    void directConsumptionPersistsOneVisibleNotification() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 25);
        LeaveRequestSubmittedIntegrationEvent event =
                new LeaveRequestSubmittedIntegrationEvent(
                        date,
                        "request-1",
                        "staff-1",
                        "manager-1",
                        date,
                        date
                );
        EventStoreJpa stored = eventStoreService.append(event);
        LeaveRequestSubmittedIntegrationEvent delivered = event.withId(stored.getId());
        NotificationRabbitListener listener = new NotificationRabbitListener(notificationService);

        listener.receive(delivered);
        listener.receive(delivered);

        assertEquals(1, notificationRepository.count());
        assertEquals(StatusOfMessageDelivery.PENDING,
                eventStoreRepository.findById(stored.getId()).orElseThrow().getStatus());
        mockMvc.perform(get("/api/users/manager-1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceEventId").value(stored.getId()))
                .andExpect(jsonPath("$[0].recipientId").value("manager-1"))
                .andExpect(jsonPath("$[0].leaveRequestId").value("request-1"))
                .andExpect(jsonPath("$[0].type").value("MANAGER_PENDING_LEAVE"));
    }
}
