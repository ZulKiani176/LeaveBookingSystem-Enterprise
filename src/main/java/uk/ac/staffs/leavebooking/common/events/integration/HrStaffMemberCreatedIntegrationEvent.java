package uk.ac.staffs.leavebooking.common.events.integration;

import uk.ac.staffs.leavebooking.common.events.RemoteEvent;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public record HrStaffMemberCreatedIntegrationEvent(
        Long id,
        LocalDate occurredOn,
        String staffMemberId,
        String firstName,
        String surname,
        String email,
        LocalDate hireDate,
        String department,
        String managerId,
        String jobRole,
        LocalDate roleStartDate,
        String jobLevel,
        String employmentType,
        String employmentStatus
) implements RemoteEvent {
    public static final String ID_NOT_NULL = "HR source event identity cannot be null";
    public static final String OCCURRED_ON_NOT_NULL = "Event occurrence date cannot be null";
    public static final String STAFF_MEMBER_ID_NOT_EMPTY = "Staff member identity cannot be empty";
    public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
    public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
    public static final String EMAIL_NOT_EMPTY = "Staff member email cannot be empty";
    public static final String HIRE_DATE_NOT_NULL = "Staff member hire date cannot be null";
    public static final String DEPARTMENT_NOT_EMPTY = "Staff member department cannot be empty";
    public static final String MANAGER_ID_NOT_EMPTY = "Manager identity cannot be empty";
    public static final String JOB_ROLE_NOT_EMPTY = "Staff member job role cannot be empty";
    public static final String ROLE_START_DATE_NOT_NULL = "Job role start date cannot be null";
    public static final String JOB_LEVEL_NOT_EMPTY = "Staff member job level cannot be empty";
    public static final String EMPLOYMENT_TYPE_NOT_EMPTY = "Staff member employment type cannot be empty";
    public static final String EMPLOYMENT_STATUS_NOT_EMPTY = "Employment status cannot be empty";

    public HrStaffMemberCreatedIntegrationEvent {
        id = argumentNotNull(id, ID_NOT_NULL);
        occurredOn = argumentNotNull(occurredOn, OCCURRED_ON_NOT_NULL);
        staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_NOT_EMPTY);
        firstName = argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
        surname = argumentNotEmpty(surname, SURNAME_NOT_EMPTY);
        email = argumentNotEmpty(email, EMAIL_NOT_EMPTY);
        hireDate = argumentNotNull(hireDate, HIRE_DATE_NOT_NULL);
        department = argumentNotEmpty(department, DEPARTMENT_NOT_EMPTY);
        managerId = argumentNotEmpty(managerId, MANAGER_ID_NOT_EMPTY);
        jobRole = argumentNotEmpty(jobRole, JOB_ROLE_NOT_EMPTY);
        roleStartDate = argumentNotNull(roleStartDate, ROLE_START_DATE_NOT_NULL);
        jobLevel = argumentNotEmpty(jobLevel, JOB_LEVEL_NOT_EMPTY);
        employmentType = argumentNotEmpty(employmentType, EMPLOYMENT_TYPE_NOT_EMPTY);
        employmentStatus = argumentNotEmpty(employmentStatus, EMPLOYMENT_STATUS_NOT_EMPTY);
    }

    @Override
    public HrStaffMemberCreatedIntegrationEvent withId(Long newId) {
        return new HrStaffMemberCreatedIntegrationEvent(
                newId,
                occurredOn,
                staffMemberId,
                firstName,
                surname,
                email,
                hireDate,
                department,
                managerId,
                jobRole,
                roleStartDate,
                jobLevel,
                employmentType,
                employmentStatus
        );
    }
}
