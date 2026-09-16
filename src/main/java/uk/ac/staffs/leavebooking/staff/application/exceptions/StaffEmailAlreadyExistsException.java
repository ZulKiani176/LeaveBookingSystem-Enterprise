package uk.ac.staffs.leavebooking.staff.application.exceptions;

public class StaffEmailAlreadyExistsException extends RuntimeException {
    public StaffEmailAlreadyExistsException(String email) {
        super("A staff member with email " + email + " already exists");
    }
}
