package uk.ac.staffs.leavebooking.common.events.integration;

public enum IntegrationLeaveStatus {
    PENDING,
    PENDING_HR_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED,
    RECORDED;

    public static IntegrationLeaveStatus from(Enum<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("Leave status cannot be null");
        }
        return valueOf(source.name());
    }

    public static IntegrationLeaveStatus from(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Leave status cannot be empty");
        }
        return valueOf(source.trim());
    }
}
