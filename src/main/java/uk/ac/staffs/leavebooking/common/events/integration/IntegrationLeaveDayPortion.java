package uk.ac.staffs.leavebooking.common.events.integration;

public enum IntegrationLeaveDayPortion {
    FULL_DAY,
    MORNING,
    AFTERNOON;

    public static IntegrationLeaveDayPortion from(Enum<?> source) {
        if (source == null) {
            throw new IllegalArgumentException("Leave day portion cannot be null");
        }
        return valueOf(source.name());
    }
}
