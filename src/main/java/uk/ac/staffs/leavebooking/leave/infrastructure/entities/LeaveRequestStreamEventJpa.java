package uk.ac.staffs.leavebooking.leave.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "leave_request_event_stream",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_leave_request_stream_sequence",
                columnNames = {"aggregate_id", "sequence_number"}
        )
)
public class LeaveRequestStreamEventJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "event_body", nullable = false, length = 65000)
    private String eventBody;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    protected LeaveRequestStreamEventJpa() {
    }

    public LeaveRequestStreamEventJpa(
            String aggregateId,
            long sequenceNumber,
            String eventType,
            String eventBody,
            LocalDate occurredOn
    ) {
        this.aggregateId = aggregateId;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.eventBody = eventBody;
        this.occurredOn = occurredOn;
    }

    public Long getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getEventType() { return eventType; }
    public String getEventBody() { return eventBody; }
    public LocalDate getOccurredOn() { return occurredOn; }
}
