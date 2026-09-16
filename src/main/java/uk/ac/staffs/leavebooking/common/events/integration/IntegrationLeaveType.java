package uk.ac.staffs.leavebooking.common.events.integration;

public enum IntegrationLeaveType {
    ANNUAL,
    SICK;

    public static IntegrationLeaveType from(Enum<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("Leave type cannot be null");
        }
        return valueOf(source.name());
    }
}
