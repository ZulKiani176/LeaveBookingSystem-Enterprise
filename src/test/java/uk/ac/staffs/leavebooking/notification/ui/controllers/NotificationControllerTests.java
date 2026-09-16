package uk.ac.staffs.leavebooking.notification.ui.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.notification.ContextFacade;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import uk.ac.staffs.leavebooking.identity.security.ControllerSecurityTestConfiguration;
import org.springframework.context.annotation.Import;
import uk.ac.staffs.leavebooking.notification.application.NotificationType;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(ControllerSecurityTestConfiguration.class)
@WithMockFirebaseUser
@DisplayName("Notification inbox API")
class NotificationControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContextFacade contextFacade;

    @Test
    @DisplayName("A recipient inbox returns notification JSON newest first")
    void recipientInboxReturnsJson() throws Exception {
        when(contextFacade.findNotificationsByRecipientId("staff-1")).thenReturn(List.of(
                new NotificationDTO(
                        "notification-1",
                        7L,
                        "staff-1",
                        "request-1",
                        NotificationType.STAFF_LEAVE_APPROVED,
                        "Your leave request request-1 has been approved.",
                        Instant.parse("2026-08-25T10:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/users/staff-1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("notification-1"))
                .andExpect(jsonPath("$[0].sourceEventId").value(7))
                .andExpect(jsonPath("$[0].recipientId").value("staff-1"))
                .andExpect(jsonPath("$[0].leaveRequestId").value("request-1"))
                .andExpect(jsonPath("$[0].type").value("STAFF_LEAVE_APPROVED"))
                .andExpect(jsonPath("$[0].message").value(
                        "Your leave request request-1 has been approved."
                ));

    }

    @Test
    @DisplayName("A recipient without notifications receives an empty JSON array")
    void emptyInboxReturnsEmptyArray() throws Exception {
        when(contextFacade.findNotificationsByRecipientId("staff-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/users/staff-1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
