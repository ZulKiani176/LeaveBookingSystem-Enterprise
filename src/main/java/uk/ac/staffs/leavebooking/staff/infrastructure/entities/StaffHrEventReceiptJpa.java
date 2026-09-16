package uk.ac.staffs.leavebooking.staff.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "staff_hr_event_receipt",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_staff_hr_event_receipt_source_type",
                columnNames = {"source_event_id", "event_type"}
        )
)
public class StaffHrEventReceiptJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_event_id", nullable = false)
    private Long sourceEventId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected StaffHrEventReceiptJpa() {
    }

    public StaffHrEventReceiptJpa(Long sourceEventId, String eventType, Instant processedAt) {
        this.sourceEventId = sourceEventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSourceEventId() {
        return sourceEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
