package uk.ac.staffs.leavebooking.staff.application.exceptions;

public class StaffMemberNotFoundException extends RuntimeException {
    public StaffMemberNotFoundException(String staffMemberId) {
        super("Staff member not found: " + staffMemberId);
    }
}
