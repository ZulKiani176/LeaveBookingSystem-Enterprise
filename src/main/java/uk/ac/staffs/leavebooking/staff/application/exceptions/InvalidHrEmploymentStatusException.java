package uk.ac.staffs.leavebooking.staff.application.exceptions;

public class InvalidHrEmploymentStatusException extends RuntimeException {
    public InvalidHrEmploymentStatusException(String employmentStatus) {
        super("Unsupported HR employment status: " + employmentStatus);
    }
}
