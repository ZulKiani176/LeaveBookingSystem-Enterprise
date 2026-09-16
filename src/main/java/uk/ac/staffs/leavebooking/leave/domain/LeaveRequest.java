package uk.ac.staffs.leavebooking.leave.domain;

import uk.ac.staffs.leavebooking.common.AggregateRoot;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.LocalEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestReferredForHrApprovalIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveDayPortion;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestReferredForHrApprovalEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestRejectedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestSubmittedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.SickLeaveRecordedEvent;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;

import java.time.LocalDate;
import java.util.List;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public class LeaveRequest extends AggregateRoot<LeaveRequest> {
    public static final String STAFF_MEMBER_ID_NOT_EMPTY = "Staff member identity cannot be empty";
    public static final String MANAGER_ID_NOT_EMPTY = "Manager identity cannot be empty";
    public static final String LEAVE_PERIOD_NOT_NULL = "Leave period cannot be null";
    public static final String REASON_NOT_EMPTY = "Leave reason cannot be empty";
    public static final String LEAVE_TYPE_NOT_NULL = "Leave type cannot be null";
    public static final String DAY_PORTION_NOT_NULL = "Leave day portion cannot be null";
    public static final String CHARGED_LEAVE_DAYS_NOT_NULL = "Charged leave days cannot be null";
    public static final String STATUS_NOT_NULL = "Leave status cannot be null";
    public static final String DECISION_COMMENT_TOO_LONG =
            "Decision comment must not exceed 500 characters";
    public static final String ANNUAL_CHARGE_NOT_POSITIVE =
            "Annual leave must have a positive leave charge";
    public static final String SICK_CHARGE_NOT_ZERO = "Sick leave must not consume annual allowance";
    public static final String SICK_MUST_BE_FULL_DAY = "Sick leave must use the full-day portion";
    public static final String HALF_DAY_SINGLE_DATE = "Half-day leave must start and end on the same date";
    public static final String REQUEST_CANNOT_BE_APPROVED = "Only pending leave requests can be approved";
    public static final String REQUEST_CANNOT_BE_REJECTED = "Only pending leave requests can be rejected";
    public static final String REQUEST_CANNOT_BE_REFERRED_FOR_HR_APPROVAL =
            "Only pending leave requests can be referred for HR approval";
    public static final String REQUEST_CANNOT_BE_APPROVED_BY_HR =
            "Only leave requests pending HR approval can be approved by HR";
    public static final String REQUEST_CANNOT_BE_REJECTED_BY_HR =
            "Only leave requests pending HR approval can be rejected by HR";
    public static final String REQUEST_CANNOT_BE_CANCELLED =
            "Only pending, HR-pending, approved or recorded leave requests can be cancelled";
    public static final String EVENT_STREAM_EMPTY = "Leave request event stream cannot be empty";

    private String staffMemberId;
    private String managerId;
    private LeavePeriod leavePeriod;
    private String reason;
    private LeaveType leaveType;
    private LeaveDayPortion dayPortion;
    private LeaveDays chargedLeaveDays;
    private LeaveStatus status;
    private String decisionComment;
    private long streamVersion;

    private LeaveRequest(Identity<LeaveRequest> id) {
        super(id);
    }

    public static LeaveRequest create(
            Identity<LeaveRequest> id,
            String staffMemberId,
            String managerId,
            LeavePeriod leavePeriod,
            String reason,
            LeaveType leaveType
    ) {
        LeavePeriod validatedPeriod = argumentNotNull(leavePeriod, LEAVE_PERIOD_NOT_NULL);
        LeaveDays legacyCharge = leaveType == LeaveType.SICK
                ? LeaveDays.zero()
                : LeaveDays.of(validatedPeriod.calendarDays());
        return create(
                id, staffMemberId, managerId, validatedPeriod, reason, leaveType,
                LeaveDayPortion.FULL_DAY, legacyCharge
        );
    }

    public static LeaveRequest create(
            Identity<LeaveRequest> id,
            String staffMemberId,
            String managerId,
            LeavePeriod leavePeriod,
            String reason,
            LeaveType leaveType,
            LeaveDayPortion dayPortion,
            LeaveDays chargedLeaveDays
    ) {
        LeaveRequest request = new LeaveRequest(id);
        request.validateInitialState(
                staffMemberId, managerId, leavePeriod, reason, leaveType,
                dayPortion, chargedLeaveDays
        );
        LocalDate occurredOn = LocalDate.now();
        if (leaveType == LeaveType.SICK) {
            SickLeaveRecordedEvent local = new SickLeaveRecordedEvent(
                    occurredOn, id.id(), staffMemberId, managerId,
                    leavePeriod.startDate(), leavePeriod.endDate(), reason
            );
            request.apply(local);
            request.addDomainEvent(local);
            request.addDomainEvent(new SickLeaveRecordedIntegrationEvent(
                    occurredOn, id.id(), staffMemberId, managerId,
                    leavePeriod.startDate(), leavePeriod.endDate()
            ));
        } else {
            LeaveRequestSubmittedEvent local = new LeaveRequestSubmittedEvent(
                    occurredOn, id.id(), staffMemberId, managerId,
                    leavePeriod.startDate(), leavePeriod.endDate(), reason,
                    leaveType, dayPortion, chargedLeaveDays.value()
            );
            request.apply(local);
            request.addDomainEvent(local);
            request.addDomainEvent(new LeaveRequestSubmittedIntegrationEvent(
                    occurredOn, id.id(), staffMemberId, managerId,
                    leavePeriod.startDate(), leavePeriod.endDate(),
                    IntegrationLeaveType.from(leaveType),
                    IntegrationLeaveDayPortion.from(dayPortion),
                    chargedLeaveDays.value()
            ));
        }
        return request;
    }

    public static LeaveRequest replay(
            Identity<LeaveRequest> id,
            List<? extends LocalEvent> historicalEvents
    ) {
        if (historicalEvents == null || historicalEvents.isEmpty()) {
            throw new IllegalArgumentException(EVENT_STREAM_EMPTY);
        }
        LeaveRequest request = new LeaveRequest(id);
        historicalEvents.forEach(request::apply);
        request.streamVersion = historicalEvents.size();
        return request;
    }

    public static LeaveRequest reconstitute(
            Identity<LeaveRequest> id,
            String staffMemberId,
            String managerId,
            LeavePeriod leavePeriod,
            String reason,
            LeaveType leaveType,
            LeaveStatus status
    ) {
        LeaveDays charge = leaveType == LeaveType.SICK
                ? LeaveDays.zero()
                : LeaveDays.of(leavePeriod.calendarDays());
        return reconstitute(
                id, staffMemberId, managerId, leavePeriod, reason, leaveType,
                LeaveDayPortion.FULL_DAY, charge, status, null
        );
    }

    public static LeaveRequest reconstitute(
            Identity<LeaveRequest> id,
            String staffMemberId,
            String managerId,
            LeavePeriod leavePeriod,
            String reason,
            LeaveType leaveType,
            LeaveDayPortion dayPortion,
            LeaveDays chargedLeaveDays,
            LeaveStatus status,
            String decisionComment
    ) {
        LeaveRequest request = new LeaveRequest(id);
        request.validateInitialState(
                staffMemberId, managerId, leavePeriod, reason, leaveType,
                dayPortion, chargedLeaveDays
        );
        request.staffMemberId = staffMemberId.trim();
        request.managerId = managerId.trim();
        request.leavePeriod = leavePeriod;
        request.reason = reason.trim();
        request.leaveType = leaveType;
        request.dayPortion = dayPortion;
        request.chargedLeaveDays = chargedLeaveDays;
        request.status = argumentNotNull(status, STATUS_NOT_NULL);
        request.decisionComment = normaliseDecisionComment(decisionComment);
        return request;
    }

    public String staffMemberId() { return staffMemberId; }
    public String managerId() { return managerId; }
    public LeavePeriod leavePeriod() { return leavePeriod; }
    public String reason() { return reason; }
    public LeaveType leaveType() { return leaveType; }
    public LeaveDayPortion dayPortion() { return dayPortion; }
    public LeaveDays chargedLeaveDays() { return chargedLeaveDays; }
    public LeaveStatus status() { return status; }
    public String decisionComment() { return decisionComment; }
    public long streamVersion() { return streamVersion; }

    public void approve() { approveWithComment(null); }

    public void approveWithComment(String comment) {
        String validatedComment = normaliseDecisionComment(comment);
        requireStatus(LeaveStatus.PENDING, REQUEST_CANNOT_BE_APPROVED);
        recordApproved(validatedComment);
    }

    public void reject() { rejectWithComment(null); }

    public void rejectWithComment(String comment) {
        String validatedComment = normaliseDecisionComment(comment);
        requireStatus(LeaveStatus.PENDING, REQUEST_CANNOT_BE_REJECTED);
        recordRejected(validatedComment);
    }

    public void referForHrApproval() {
        requireStatus(LeaveStatus.PENDING, REQUEST_CANNOT_BE_REFERRED_FOR_HR_APPROVAL);
        LocalDate occurredOn = LocalDate.now();
        LeaveRequestReferredForHrApprovalEvent event = new LeaveRequestReferredForHrApprovalEvent(
                occurredOn, id().id(), staffMemberId
        );
        apply(event);
        addDomainEvent(event);
        addDomainEvent(new LeaveRequestReferredForHrApprovalIntegrationEvent(
                null, occurredOn, id().id(), staffMemberId,
                IntegrationLeaveStatus.PENDING, IntegrationLeaveStatus.PENDING_HR_APPROVAL,
                leavePeriod.startDate(), leavePeriod.endDate(),
                IntegrationLeaveType.from(leaveType), IntegrationLeaveDayPortion.from(dayPortion),
                chargedLeaveDays.value()
        ));
    }

    public void approveByHr() { approveByHrWithComment(null); }

    public void approveByHrWithComment(String comment) {
        String validatedComment = normaliseDecisionComment(comment);
        requireStatus(LeaveStatus.PENDING_HR_APPROVAL, REQUEST_CANNOT_BE_APPROVED_BY_HR);
        recordApproved(validatedComment);
    }

    public void rejectByHr() { rejectByHrWithComment(null); }

    public void rejectByHrWithComment(String comment) {
        String validatedComment = normaliseDecisionComment(comment);
        requireStatus(LeaveStatus.PENDING_HR_APPROVAL, REQUEST_CANNOT_BE_REJECTED_BY_HR);
        recordRejected(validatedComment);
    }

    public void cancel() {
        if (status != LeaveStatus.PENDING
                && status != LeaveStatus.PENDING_HR_APPROVAL
                && status != LeaveStatus.APPROVED
                && status != LeaveStatus.RECORDED) {
            throw new InvalidLeaveRequestStateException(REQUEST_CANNOT_BE_CANCELLED);
        }
        LeaveStatus previousStatus = status;
        LocalDate occurredOn = LocalDate.now();
        LeaveRequestCancelledEvent local = new LeaveRequestCancelledEvent(
                occurredOn, id().id(), staffMemberId, leavePeriod.startDate(),
                leavePeriod.endDate(), previousStatus, chargedLeaveDays.value()
        );
        apply(local);
        addDomainEvent(local);
        addDomainEvent(new LeaveRequestCancelledIntegrationEvent(
                occurredOn, id().id(), staffMemberId, leavePeriod.startDate(),
                leavePeriod.endDate(), previousStatus.name(),
                IntegrationLeaveType.from(leaveType),
                IntegrationLeaveDayPortion.from(dayPortion),
                chargedLeaveDays.value()
        ));
    }

    private void recordApproved(String comment) {
        LeaveStatus previousStatus = status;
        LocalDate occurredOn = LocalDate.now();
        LeaveRequestApprovedEvent local = new LeaveRequestApprovedEvent(
                occurredOn, id().id(), staffMemberId, leavePeriod.startDate(),
                leavePeriod.endDate(), chargedLeaveDays.value(), comment
        );
        apply(local);
        addDomainEvent(local);
        addDomainEvent(new LeaveRequestApprovedIntegrationEvent(
                occurredOn, id().id(), staffMemberId,
                leavePeriod.startDate(), leavePeriod.endDate(),
                IntegrationLeaveType.from(leaveType),
                IntegrationLeaveDayPortion.from(dayPortion),
                chargedLeaveDays.value(), IntegrationLeaveStatus.from(previousStatus)
        ));
    }

    private void recordRejected(String comment) {
        LeaveStatus previousStatus = status;
        LocalDate occurredOn = LocalDate.now();
        LeaveRequestRejectedEvent local = new LeaveRequestRejectedEvent(
                occurredOn, id().id(), staffMemberId, comment
        );
        apply(local);
        addDomainEvent(local);
        addDomainEvent(new LeaveRequestRejectedIntegrationEvent(
                occurredOn, id().id(), staffMemberId,
                leavePeriod.startDate(), leavePeriod.endDate(),
                IntegrationLeaveType.from(leaveType),
                IntegrationLeaveDayPortion.from(dayPortion),
                chargedLeaveDays.value(), IntegrationLeaveStatus.from(previousStatus)
        ));
    }

    private void apply(LocalEvent event) {
        if (event instanceof LeaveRequestSubmittedEvent submitted) {
            staffMemberId = submitted.staffMemberId();
            managerId = submitted.managerId();
            leavePeriod = new LeavePeriod(submitted.startDate(), submitted.endDate());
            reason = submitted.reason();
            leaveType = submitted.leaveType();
            dayPortion = submitted.dayPortion();
            chargedLeaveDays = new LeaveDays(submitted.chargedLeaveDays());
            status = LeaveStatus.PENDING;
            decisionComment = null;
        } else if (event instanceof SickLeaveRecordedEvent sick) {
            staffMemberId = sick.staffMemberId();
            managerId = sick.managerId();
            leavePeriod = new LeavePeriod(sick.startDate(), sick.endDate());
            reason = sick.reason();
            leaveType = LeaveType.SICK;
            dayPortion = LeaveDayPortion.FULL_DAY;
            chargedLeaveDays = LeaveDays.zero();
            status = LeaveStatus.RECORDED;
            decisionComment = null;
        } else if (event instanceof LeaveRequestReferredForHrApprovalEvent) {
            status = LeaveStatus.PENDING_HR_APPROVAL;
        } else if (event instanceof LeaveRequestApprovedEvent approved) {
            status = LeaveStatus.APPROVED;
            decisionComment = approved.decisionComment();
        } else if (event instanceof LeaveRequestRejectedEvent rejected) {
            status = LeaveStatus.REJECTED;
            decisionComment = rejected.decisionComment();
        } else if (event instanceof LeaveRequestCancelledEvent) {
            status = LeaveStatus.CANCELLED;
        } else {
            throw new IllegalArgumentException("Unsupported LeaveRequest event: " + event.getClass().getName());
        }
    }

    private void validateInitialState(
            String staffMemberId,
            String managerId,
            LeavePeriod leavePeriod,
            String reason,
            LeaveType leaveType,
            LeaveDayPortion dayPortion,
            LeaveDays chargedLeaveDays
    ) {
        argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_NOT_EMPTY);
        argumentNotEmpty(managerId, MANAGER_ID_NOT_EMPTY);
        LeavePeriod period = argumentNotNull(leavePeriod, LEAVE_PERIOD_NOT_NULL);
        argumentNotEmpty(reason, REASON_NOT_EMPTY);
        LeaveType type = argumentNotNull(leaveType, LEAVE_TYPE_NOT_NULL);
        LeaveDayPortion portion = argumentNotNull(dayPortion, DAY_PORTION_NOT_NULL);
        LeaveDays charge = argumentNotNull(chargedLeaveDays, CHARGED_LEAVE_DAYS_NOT_NULL);
        if (type == LeaveType.SICK) {
            if (portion != LeaveDayPortion.FULL_DAY) {
                throw new InvalidLeaveRequestException(SICK_MUST_BE_FULL_DAY);
            }
            if (!charge.isZero()) {
                throw new InvalidLeaveRequestException(SICK_CHARGE_NOT_ZERO);
            }
        } else {
            if (!charge.isPositive()) {
                throw new InvalidLeaveRequestException(ANNUAL_CHARGE_NOT_POSITIVE);
            }
            if (portion.isHalfDay() && !period.startDate().equals(period.endDate())) {
                throw new InvalidLeaveRequestException(HALF_DAY_SINGLE_DATE);
            }
        }
    }

    private void requireStatus(LeaveStatus expected, String message) {
        if (status != expected) {
            throw new InvalidLeaveRequestStateException(message);
        }
    }

    private static String normaliseDecisionComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.length() > 500) {
            throw new InvalidLeaveRequestException(DECISION_COMMENT_TOO_LONG);
        }
        return trimmed;
    }
}
