package uk.ac.staffs.leavebooking.notification.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uk.ac.staffs.leavebooking.notification.application.NotificationType;

import java.time.Instant;

@Entity
@Table(name = "notification")
public class NotificationJpa {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private Long sourceEventId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Column(name = "leave_request_id", nullable = false, length = 36)
    private String leaveRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationJpa() {
    }

    public NotificationJpa(
            String id,
            Long sourceEventId,
            String recipientId,
            String leaveRequestId,
            NotificationType notificationType,
            String message,
            Instant createdAt
    ) {
        this.id = id;
        this.sourceEventId = sourceEventId;
        this.recipientId = recipientId;
        this.leaveRequestId = leaveRequestId;
        this.notificationType = notificationType;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(Long sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getLeaveRequestId() {
        return leaveRequestId;
    }

    public void setLeaveRequestId(String leaveRequestId) {
        this.leaveRequestId = leaveRequestId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
