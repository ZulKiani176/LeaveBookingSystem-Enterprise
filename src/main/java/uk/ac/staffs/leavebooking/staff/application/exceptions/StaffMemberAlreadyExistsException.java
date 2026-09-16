package uk.ac.staffs.leavebooking.staff.application.exceptions;

public class StaffMemberAlreadyExistsException extends RuntimeException {
    public StaffMemberAlreadyExistsException(String staffMemberId) {
        super("Staff member already exists: " + staffMemberId);
    }
}
