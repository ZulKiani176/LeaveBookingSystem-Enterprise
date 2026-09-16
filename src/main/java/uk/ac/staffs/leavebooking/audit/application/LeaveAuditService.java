package uk.ac.staffs.leavebooking.audit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.staffs.leavebooking.audit.application.dto.LeaveAuditDTO;
import uk.ac.staffs.leavebooking.audit.domain.LeaveAuditAction;
import uk.ac.staffs.leavebooking.audit.infrastructure.entities.LeaveAuditJpa;
import uk.ac.staffs.leavebooking.audit.infrastructure.repositories.LeaveAuditRepository;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestReferredForHrApprovalIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class LeaveAuditService {
    private final LeaveAuditRepository repository;

    public LeaveAuditService(LeaveAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(LeaveRequestSubmittedIntegrationEvent event) {
        save(event.id(), event.leaveRequestId(), event.staffMemberId(),
                LeaveAuditAction.SUBMITTED, null, event.status(), null, event.occurredOn());
    }

    @Transactional
    public void record(LeaveRequestApprovedIntegrationEvent event) {
        LeaveAuditAction action = event.previousStatus()
                == IntegrationLeaveStatus.PENDING_HR_APPROVAL
                ? LeaveAuditAction.HR_APPROVED
                : LeaveAuditAction.APPROVED;
        save(event.id(), event.leaveRequestId(), event.staffMemberId(), action,
                event.previousStatus(), event.status(), null, event.occurredOn());
    }

    @Transactional
    public void record(LeaveRequestRejectedIntegrationEvent event) {
        LeaveAuditAction action = event.previousStatus()
                == IntegrationLeaveStatus.PENDING_HR_APPROVAL
                ? LeaveAuditAction.HR_REJECTED
                : LeaveAuditAction.REJECTED;
        save(event.id(), event.leaveRequestId(), event.staffMemberId(), action,
                event.previousStatus(), event.status(), null, event.occurredOn());
    }

    @Transactional
    public void record(LeaveRequestReferredForHrApprovalIntegrationEvent event) {
        save(event.id(), event.leaveRequestId(), event.staffMemberId(),
                LeaveAuditAction.REFERRED_FOR_HR_APPROVAL, event.previousStatus(),
                event.status(), null, event.occurredOn());
    }

    @Transactional
    public void record(LeaveRequestCancelledIntegrationEvent event) {
        save(event.id(), event.leaveRequestId(), event.staffMemberId(),
                LeaveAuditAction.CANCELLED,
                IntegrationLeaveStatus.from(event.previousStatus()),
                event.status(), null, event.occurredOn());
    }

    @Transactional
    public void record(SickLeaveRecordedIntegrationEvent event) {
        save(event.id(), event.leaveRequestId(), event.staffMemberId(),
                LeaveAuditAction.SICK_LEAVE_RECORDED, null,
                event.status(), null, event.occurredOn());
    }

    @Transactional(readOnly = true)
    public List<LeaveAuditDTO> find(
            String leaveRequestId,
            String staffMemberId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("Audit start and end dates must be supplied together");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Audit end date cannot be before start date");
        }
        List<LeaveAuditJpa> records = select(leaveRequestId, staffMemberId);
        return records.stream()
                .filter(item -> staffMemberId == null || item.getStaffMemberId().equals(staffMemberId))
                .filter(item -> startDate == null || !item.getOccurredOn().isBefore(startDate))
                .filter(item -> endDate == null || !item.getOccurredOn().isAfter(endDate))
                .map(LeaveAuditService::map)
                .toList();
    }

    private List<LeaveAuditJpa> select(String leaveRequestId, String staffMemberId) {
        if (leaveRequestId != null && !leaveRequestId.isBlank()) {
            return repository.findByLeaveRequestIdOrderBySourceEventId(leaveRequestId.trim());
        }
        if (staffMemberId != null && !staffMemberId.isBlank()) {
            return repository.findByStaffMemberIdOrderBySourceEventId(staffMemberId.trim());
        }
        return repository.findAllByOrderBySourceEventId();
    }

    private void save(
            Long sourceEventId,
            String leaveRequestId,
            String staffMemberId,
            LeaveAuditAction action,
            IntegrationLeaveStatus previousStatus,
            IntegrationLeaveStatus newStatus,
            String decisionComment,
            LocalDate occurredOn
    ) {
        Long id = Objects.requireNonNull(sourceEventId, "Source event identity cannot be null");
        if (repository.existsById(id)) {
            return;
        }
        repository.save(new LeaveAuditJpa(
                id, leaveRequestId, staffMemberId, action, previousStatus,
                newStatus, decisionComment, occurredOn
        ));
    }

    private static LeaveAuditDTO map(LeaveAuditJpa item) {
        return new LeaveAuditDTO(
                item.getSourceEventId(), item.getLeaveRequestId(), item.getStaffMemberId(),
                item.getAction(), item.getPreviousStatus(), item.getNewStatus(),
                item.getDecisionComment(), item.getOccurredOn()
        );
    }
}
