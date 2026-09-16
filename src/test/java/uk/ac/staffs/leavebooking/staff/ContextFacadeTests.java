package uk.ac.staffs.leavebooking.staff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;
import uk.ac.staffs.leavebooking.staff.application.StaffApplicationService;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Staff public operations")
class ContextFacadeTests {
    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 4, 1);

    @Mock
    private StaffApplicationService service;

    @InjectMocks
    private ContextFacade facade;

    @Test
    @DisplayName("Creating staff returns the new staff identity")
    void createStaffMemberDelegates() {
        CreateStaffMemberDetails details = details();
        when(service.createStaffMember(details)).thenReturn("staff-1");

        String result = facade.createStaffMember(details);

        assertEquals("staff-1", result);
    }

    @Test
    @DisplayName("The selected staff member and new department are sent for update")
    void changeDepartmentDelegates() {
        facade.changeDepartment("staff-1", "Finance");

        verify(service).changeDepartment("staff-1", "Finance");
    }

    @Test
    @DisplayName("The new job role and start date are sent for update")
    void changeJobRoleDelegates() {
        LocalDate roleStartDate = LocalDate.of(2026, 8, 1);

        facade.changeJobRole("staff-1", "Team Leader", roleStartDate);

        verify(service).changeJobRole("staff-1", "Team Leader", roleStartDate);
    }

    @Test
    @DisplayName("Finding staff by identity returns their details")
    void findStaffMemberByIdDelegates() {
        StaffMemberDTO expected = dto();
        when(service.findStaffMemberById("staff-1")).thenReturn(expected);

        StaffMemberDTO result = facade.findStaffMemberById("staff-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding staff by manager returns the matching team")
    void findStaffByManagerIdDelegates() {
        List<StaffMemberDTO> expected = List.of(dto());
        when(service.findStaffByManagerId("manager-1")).thenReturn(expected);

        List<StaffMemberDTO> result = facade.findStaffByManagerId("manager-1");

        assertSame(expected, result);
    }

    @Test
    @DisplayName("Finding staff by department returns the matching list")
    void findStaffByDepartmentDelegates() {
        List<StaffMemberDTO> expected = List.of(dto());
        when(service.findStaffByDepartment("Engineering")).thenReturn(expected);

        List<StaffMemberDTO> result = facade.findStaffByDepartment("Engineering");

        assertSame(expected, result);
    }

    private CreateStaffMemberDetails details() {
        return new CreateStaffMemberDetails(
                "Ada",
                "Lovelace",
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                HIRE_DATE,
                "Level 2",
                "Permanent"
        );
    }

    private StaffMemberDTO dto() {
        return new StaffMemberDTO(
                "staff-1",
                "Ada",
                "Lovelace",
                "ada@example.com",
                HIRE_DATE,
                "Engineering",
                "manager-1",
                "Software Engineer",
                HIRE_DATE,
                "Level 2",
                "Permanent",
                EmploymentStatus.ACTIVE
        );
    }
}
