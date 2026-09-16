package uk.ac.staffs.leavebooking.staff.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;

@Entity
@Table(name = "staff_member")
public class StaffMemberJpa {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "first_name", nullable = false, length = 20)
    private String firstName;

    @Column(name = "surname", nullable = false, length = 20)
    private String surname;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "manager_id", nullable = false, length = 36)
    private String managerId;

    @Column(name = "job_role", nullable = false, length = 100)
    private String jobRole;

    @Column(name = "role_start_date", nullable = false)
    private LocalDate roleStartDate;

    @Column(name = "job_level", nullable = false, length = 50)
    private String jobLevel;

    @Column(name = "employment_type", nullable = false, length = 50)
    private String employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    protected StaffMemberJpa() {
    }

    public StaffMemberJpa(
            String id,
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
            EmploymentStatus employmentStatus
    ) {
        this.id = id;
        this.firstName = firstName;
        this.surname = surname;
        this.email = email;
        this.hireDate = hireDate;
        this.department = department;
        this.managerId = managerId;
        this.jobRole = jobRole;
        this.roleStartDate = roleStartDate;
        this.jobLevel = jobLevel;
        this.employmentType = employmentType;
        this.employmentStatus = employmentStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public LocalDate getRoleStartDate() {
        return roleStartDate;
    }

    public void setRoleStartDate(LocalDate roleStartDate) {
        this.roleStartDate = roleStartDate;
    }

    public String getJobLevel() {
        return jobLevel;
    }

    public void setJobLevel(String jobLevel) {
        this.jobLevel = jobLevel;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }
}
