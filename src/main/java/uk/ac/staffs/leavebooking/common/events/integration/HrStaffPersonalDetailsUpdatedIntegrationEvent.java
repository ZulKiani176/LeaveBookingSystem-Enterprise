package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record HrStaffPersonalDetailsUpdatedIntegrationEvent(
        Long id,
        LocalDate occurredOn,
        String staffMemberId,
        String firstName,
        String surname,
        String email
) implements RemoteEvent {
    public static final String ID_NOT_NULL = "HR source event identity cannot be null";
    public static final String OCCURRED_ON_NOT_NULL = "Event occurrence date cannot be null";
    public static final String STAFF_MEMBER_ID_NOT_EMPTY = "Staff member identity cannot be empty";
    public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
    public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
    public static final String EMAIL_NOT_EMPTY = "Staff member email cannot be empty";

    public HrStaffPersonalDetailsUpdatedIntegrationEvent {
        id = argumentNotNull(id, ID_NOT_NULL);
        occurredOn = argumentNotNull(occurredOn, OCCURRED_ON_NOT_NULL);
        staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_NOT_EMPTY);
        firstName = argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
        surname = argumentNotEmpty(surname, SURNAME_NOT_EMPTY);
        email = argumentNotEmpty(email, EMAIL_NOT_EMPTY);
    }

    @Override
    public HrStaffPersonalDetailsUpdatedIntegrationEvent withId(Long newId) {
        return new HrStaffPersonalDetailsUpdatedIntegrationEvent(
                newId,
                occurredOn,
                staffMemberId,
                firstName,
                surname,
                email
        );
    }
}
