package uk.ac.staffs.leavebooking.common.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "event_store")
public class EventStoreJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "event_body", nullable = false, length = 65000)
    private String eventBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusOfMessageDelivery status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected EventStoreJpa() {
    }

    public EventStoreJpa(
            Long id,
            LocalDate occurredOn,
            String eventType,
            String eventBody,
            StatusOfMessageDelivery status,
            int retryCount
    ) {
        this.id = id;
        this.occurredOn = occurredOn;
        this.eventType = eventType;
        this.eventBody = eventBody;
        this.status = status;
        this.retryCount = retryCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventBody() {
        return eventBody;
    }

    public void setEventBody(String eventBody) {
        this.eventBody = eventBody;
    }

    public StatusOfMessageDelivery getStatus() {
        return status;
    }

    public void setStatus(StatusOfMessageDelivery status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
