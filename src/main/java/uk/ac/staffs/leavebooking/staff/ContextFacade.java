package uk.ac.staffs.leavebooking.staff;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.staff.application.StaffApplicationService;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;

import java.time.LocalDate;
import java.util.List;

@Component("staffContextFacade")
public class ContextFacade {
    private final StaffApplicationService staffApplicationService;

    public ContextFacade(StaffApplicationService staffApplicationService) {
        this.staffApplicationService = staffApplicationService;
    }

    public String createStaffMember(CreateStaffMemberDetails details) {
        return staffApplicationService.createStaffMember(details);
    }

    public void changeDepartment(String staffMemberId, String newDepartment) {
        staffApplicationService.changeDepartment(staffMemberId, newDepartment);
    }

    public void changeJobRole(
            String staffMemberId,
            String newJobRole,
            LocalDate newRoleStartDate
    ) {
        staffApplicationService.changeJobRole(staffMemberId, newJobRole, newRoleStartDate);
    }

    public StaffMemberDTO findStaffMemberById(String staffMemberId) {
        return staffApplicationService.findStaffMemberById(staffMemberId);
    }

    public List<StaffMemberDTO> findStaffByManagerId(String managerId) {
        return staffApplicationService.findStaffByManagerId(managerId);
    }

    public List<StaffMemberDTO> findStaffByDepartment(String department) {
        return staffApplicationService.findStaffByDepartment(department);
    }
}
