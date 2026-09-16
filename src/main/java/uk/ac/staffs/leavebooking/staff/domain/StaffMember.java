package uk.ac.staffs.leavebooking.staff.domain;

import uk.ac.staffs.leavebooking.common.AggregateRoot;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.staff.domain.exceptions.InvalidStaffMemberException;

import java.time.LocalDate;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;
import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotNull;

public class StaffMember extends AggregateRoot<StaffMember> {
    public static final String FULL_NAME_NOT_NULL = "Staff member full name cannot be null";
    public static final String EMAIL_NOT_EMPTY = "Staff member email cannot be empty";
    public static final String HIRE_DATE_NOT_NULL = "Staff member hire date cannot be null";
    public static final String DEPARTMENT_NOT_EMPTY = "Staff member department cannot be empty";
    public static final String MANAGER_ID_NOT_EMPTY = "Manager identity cannot be empty";
    public static final String JOB_ROLE_NOT_EMPTY = "Staff member job role cannot be empty";
    public static final String ROLE_START_DATE_NOT_NULL = "Job role start date cannot be null";
    public static final String JOB_LEVEL_NOT_EMPTY = "Staff member job level cannot be empty";
    public static final String EMPLOYMENT_TYPE_NOT_EMPTY = "Staff member employment type cannot be empty";
    public static final String EMPLOYMENT_STATUS_NOT_NULL = "Employment status cannot be null";
    public static final String ROLE_START_BEFORE_HIRE =
            "Job role start date cannot be before the staff member hire date";

    private FullName fullName;
    private String email;
    private final LocalDate hireDate;
    private String department;
    private final String managerId;
    private String jobRole;
    private LocalDate roleStartDate;
    private final String jobLevel;
    private final String employmentType;
    private final EmploymentStatus employmentStatus;

    public StaffMember(
            Identity<StaffMember> id,
            FullName fullName,
            String email,
            LocalDate hireDate,
            String department,
            String managerId,
            String jobRole,
            LocalDate roleStartDate,
            String jobLevel,
            String employmentType
    ) {
        this(
                id,
                fullName,
                email,
                hireDate,
                department,
                managerId,
                jobRole,
                roleStartDate,
                jobLevel,
                employmentType,
                EmploymentStatus.ACTIVE
        );
    }

    private StaffMember(
            Identity<StaffMember> id,
            FullName fullName,
            String email,
            LocalDate hireDate,
            String department,
            String managerId,
            String jobRole,
            LocalDate roleStartDate,
            String jobLevel,
            String employmentType,
            EmploymentStatus employmentStatus
    ) {
        super(id);
        this.fullName = argumentNotNull(fullName, FULL_NAME_NOT_NULL);
        this.email = argumentNotEmpty(email, EMAIL_NOT_EMPTY);
        this.hireDate = argumentNotNull(hireDate, HIRE_DATE_NOT_NULL);
        this.department = argumentNotEmpty(department, DEPARTMENT_NOT_EMPTY);
        this.managerId = argumentNotEmpty(managerId, MANAGER_ID_NOT_EMPTY);
        this.jobRole = argumentNotEmpty(jobRole, JOB_ROLE_NOT_EMPTY);
        this.roleStartDate = validateRoleStartDate(roleStartDate, this.hireDate);
        this.jobLevel = argumentNotEmpty(jobLevel, JOB_LEVEL_NOT_EMPTY);
        this.employmentType = argumentNotEmpty(employmentType, EMPLOYMENT_TYPE_NOT_EMPTY);
        this.employmentStatus = argumentNotNull(employmentStatus, EMPLOYMENT_STATUS_NOT_NULL);
    }

    public static StaffMember reconstitute(
            Identity<StaffMember> id,
            FullName fullName,
            String email,
            LocalDate hireDate,
            String department,
            String managerId,
            String jobRole,
            LocalDate roleStartDate,
            String jobLevel,
            String employmentType,
            EmploymentStatus employmentStatus
    ) {
        return new StaffMember(
                id,
                fullName,
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

    public static StaffMember createFromExternalHr(
            Identity<StaffMember> id,
            FullName fullName,
            String email,
            LocalDate hireDate,
            String department,
            String managerId,
            String jobRole,
            LocalDate roleStartDate,
            String jobLevel,
            String employmentType,
            EmploymentStatus employmentStatus
    ) {
        return new StaffMember(
                id,
                fullName,
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

    public FullName fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public LocalDate hireDate() {
        return hireDate;
    }

    public String department() {
        return department;
    }

    public String managerId() {
        return managerId;
    }

    public String jobRole() {
        return jobRole;
    }

    public LocalDate roleStartDate() {
        return roleStartDate;
    }

    public String jobLevel() {
        return jobLevel;
    }

    public String employmentType() {
        return employmentType;
    }

    public EmploymentStatus employmentStatus() {
        return employmentStatus;
    }

    public void changeDepartment(String newDepartment) {
        String validatedDepartment = argumentNotEmpty(newDepartment, DEPARTMENT_NOT_EMPTY);
        department = validatedDepartment;
    }

    public void changeJobRole(String newJobRole, LocalDate newRoleStartDate) {
        String validatedJobRole = argumentNotEmpty(newJobRole, JOB_ROLE_NOT_EMPTY);
        LocalDate validatedRoleStartDate = validateRoleStartDate(newRoleStartDate, hireDate);

        jobRole = validatedJobRole;
        roleStartDate = validatedRoleStartDate;
    }

    public void changePersonalDetails(FullName newFullName, String newEmail) {
        FullName validatedFullName = argumentNotNull(newFullName, FULL_NAME_NOT_NULL);
        String validatedEmail = argumentNotEmpty(newEmail, EMAIL_NOT_EMPTY);

        fullName = validatedFullName;
        email = validatedEmail;
    }

    private static LocalDate validateRoleStartDate(LocalDate roleStartDate, LocalDate hireDate) {
        LocalDate validatedRoleStartDate = argumentNotNull(
                roleStartDate,
                ROLE_START_DATE_NOT_NULL
        );
        if (validatedRoleStartDate.isBefore(hireDate)) {
            throw new InvalidStaffMemberException(ROLE_START_BEFORE_HIRE);
        }
        return validatedRoleStartDate;
    }
}
