package uk.ac.staffs.leavebooking.staff.domain.exceptions;

public class InvalidStaffMemberException extends RuntimeException {
    public InvalidStaffMemberException(String message) {
        super(message);
    }
}
