package uk.ac.staffs.leavebooking.reporting.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.staffs.leavebooking.common.events.integration.*;
import uk.ac.staffs.leavebooking.reporting.application.dto.LeaveReportingDTO;
import uk.ac.staffs.leavebooking.reporting.infrastructure.entities.LeaveReportingProjectionJpa;
import uk.ac.staffs.leavebooking.reporting.infrastructure.repositories.LeaveReportingProjectionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class LeaveReportingService {
    private final LeaveReportingProjectionRepository repository;

    public LeaveReportingService(LeaveReportingProjectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(LeaveRequestSubmittedIntegrationEvent event) {
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional
    public void apply(SickLeaveRecordedIntegrationEvent event) {
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional
    public void apply(LeaveRequestApprovedIntegrationEvent event) {
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional
    public void apply(LeaveRequestRejectedIntegrationEvent event) {
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional
    public void apply(LeaveRequestCancelledIntegrationEvent event) {
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional
    public void apply(LeaveRequestReferredForHrApprovalIntegrationEvent event) {
        if (event.startDate() != null) {
            upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), event.leaveType(),
                    event.startDate(), event.endDate(), event.dayPortion(),
                    event.chargedLeaveDays(), event.status(), event.occurredOn());
            return;
        }
        var existing = repository.findById(event.leaveRequestId())
                .orElseThrow(() -> new IllegalStateException(
                        "The submission projection is required for a legacy HR referral"
                ));
        upsert(event.id(), event.leaveRequestId(), event.staffMemberId(), existing.getLeaveType(),
                existing.getStartDate(), existing.getEndDate(), existing.getDayPortion(),
                existing.getChargedLeaveDays(), event.status(), event.occurredOn());
    }

    @Transactional(readOnly = true)
    public List<LeaveReportingDTO> find(
            LocalDate startDate,
            LocalDate endDate,
            IntegrationLeaveType leaveType,
            IntegrationLeaveStatus status
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Reporting start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Reporting end date cannot be before start date");
        }
        return repository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
                        endDate, startDate
                )
                .stream()
                .filter(item -> leaveType == null || item.getLeaveType() == leaveType)
                .filter(item -> status == null || item.getStatus() == status)
                .map(LeaveReportingService::map)
                .toList();
    }

    private void upsert(
            Long sourceEventId, String requestId, String staffId,
            IntegrationLeaveType type, LocalDate start, LocalDate end,
            IntegrationLeaveDayPortion portion, BigDecimal chargedDays,
            IntegrationLeaveStatus status, LocalDate occurredOn
    ) {
        Long eventId = Objects.requireNonNull(sourceEventId, "Source event identity cannot be null");
        var existingProjection = repository.findById(requestId);
        if (existingProjection.isPresent()
                && existingProjection.get().getSourceEventId() >= eventId) {
            return;
        }
        LeaveReportingProjectionJpa projection = existingProjection
                .orElseGet(() -> new LeaveReportingProjectionJpa(
                        requestId, eventId, staffId, type, start, end,
                        portion, chargedDays, status, occurredOn
                ));
        projection.setSourceEventId(eventId);
        projection.setStaffMemberId(staffId);
        projection.setLeaveType(type);
        projection.setStartDate(start);
        projection.setEndDate(end);
        projection.setDayPortion(portion);
        projection.setChargedLeaveDays(chargedDays);
        projection.setStatus(status);
        projection.setOccurredOn(occurredOn);
        repository.save(projection);
    }

    private static LeaveReportingDTO map(LeaveReportingProjectionJpa item) {
        return new LeaveReportingDTO(
                item.getLeaveRequestId(), item.getSourceEventId(), item.getStaffMemberId(),
                item.getLeaveType(), item.getStartDate(), item.getEndDate(),
                item.getDayPortion(), item.getChargedLeaveDays(), item.getStatus(),
                item.getOccurredOn()
        );
    }
}
