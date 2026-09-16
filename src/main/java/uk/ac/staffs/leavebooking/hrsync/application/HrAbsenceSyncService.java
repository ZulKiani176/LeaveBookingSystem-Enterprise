package uk.ac.staffs.leavebooking.hrsync.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.hrsync.application.dto.HrAbsenceSyncDTO;
import uk.ac.staffs.leavebooking.hrsync.domain.HrAbsenceSyncAction;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.entities.HrAbsenceSyncJpa;
import uk.ac.staffs.leavebooking.hrsync.infrastructure.repositories.HrAbsenceSyncRepository;

import java.util.List;
import java.util.Objects;

@Service
public class HrAbsenceSyncService {
    private final HrAbsenceSyncRepository repository;

    public HrAbsenceSyncService(HrAbsenceSyncRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(LeaveRequestApprovedIntegrationEvent event) {
        if (event.leaveType() != IntegrationLeaveType.ANNUAL) {
            return;
        }
        save(new HrAbsenceSyncJpa(
                requiredId(event.id()), event.leaveRequestId(), event.staffMemberId(),
                HrAbsenceSyncAction.ANNUAL_LEAVE_APPROVED, event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn()
        ));
    }

    @Transactional
    public void record(LeaveRequestCancelledIntegrationEvent event) {
        boolean annualCancellation = event.leaveType() == IntegrationLeaveType.ANNUAL
                && IntegrationLeaveStatus.APPROVED.name().equals(event.previousStatus());
        boolean sickCancellation = event.leaveType() == IntegrationLeaveType.SICK
                && IntegrationLeaveStatus.RECORDED.name().equals(event.previousStatus());
        if (!annualCancellation && !sickCancellation) {
            return;
        }
        save(new HrAbsenceSyncJpa(
                requiredId(event.id()), event.leaveRequestId(), event.staffMemberId(),
                sickCancellation ? HrAbsenceSyncAction.SICK_LEAVE_CANCELLED
                        : HrAbsenceSyncAction.APPROVED_LEAVE_CANCELLED, event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn()
        ));
    }

    @Transactional
    public void record(SickLeaveRecordedIntegrationEvent event) {
        save(new HrAbsenceSyncJpa(
                requiredId(event.id()), event.leaveRequestId(), event.staffMemberId(),
                HrAbsenceSyncAction.SICK_LEAVE_RECORDED, event.leaveType(),
                event.startDate(), event.endDate(), event.dayPortion(),
                event.chargedLeaveDays(), event.status(), event.occurredOn()
        ));
    }

    @Transactional(readOnly = true)
    public List<HrAbsenceSyncDTO> find(String staffMemberId) {
        List<HrAbsenceSyncJpa> records = staffMemberId == null || staffMemberId.isBlank()
                ? repository.findAllByOrderBySourceEventId()
                : repository.findByStaffMemberIdOrderBySourceEventId(staffMemberId.trim());
        return records.stream().map(HrAbsenceSyncService::map).toList();
    }

    private void save(HrAbsenceSyncJpa record) {
        if (!repository.existsById(record.getSourceEventId())) {
            repository.save(record);
        }
    }

    private static Long requiredId(Long sourceEventId) {
        return Objects.requireNonNull(sourceEventId, "Source event identity cannot be null");
    }

    private static HrAbsenceSyncDTO map(HrAbsenceSyncJpa item) {
        return new HrAbsenceSyncDTO(
                item.getSourceEventId(), item.getLeaveRequestId(), item.getStaffMemberId(),
                item.getSyncAction(), item.getLeaveType(), item.getStartDate(),
                item.getEndDate(), item.getDayPortion(), item.getChargedLeaveDays(),
                item.getStatus(), item.getOccurredOn()
        );
    }
}
