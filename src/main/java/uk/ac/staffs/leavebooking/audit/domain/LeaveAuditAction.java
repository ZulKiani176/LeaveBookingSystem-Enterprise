package uk.ac.staffs.leavebooking.audit.domain;

public enum LeaveAuditAction {
    SUBMITTED,
    APPROVED,
    HR_APPROVED,
    REJECTED,
    HR_REJECTED,
    REFERRED_FOR_HR_APPROVAL,
    CANCELLED,
    SICK_LEAVE_RECORDED
}
