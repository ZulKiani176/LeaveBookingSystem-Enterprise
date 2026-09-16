package uk.ac.staffs.leavebooking.staff.ui.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.staff.ContextFacade;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.ui.requests.ChangeDepartmentRequest;
import uk.ac.staffs.leavebooking.staff.ui.requests.ChangeJobRoleRequest;
import uk.ac.staffs.leavebooking.staff.ui.requests.CreateStaffMemberRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StaffMemberController {
    private final ContextFacade contextFacade;

    public StaffMemberController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @PostMapping("/staff")
    @PreAuthorize("@staffAccess.isAdmin(authentication)")
    public ResponseEntity<StaffMemberDTO> createStaffMember(
            @Valid @RequestBody CreateStaffMemberRequest request
    ) {
        String staffMemberId = contextFacade.createStaffMember(new CreateStaffMemberDetails(
                request.firstName(),
                request.surname(),
                request.email(),
                request.hireDate(),
                request.department(),
                request.managerId(),
                request.jobRole(),
                request.roleStartDate(),
                request.jobLevel(),
                request.employmentType()
        ));
        StaffMemberDTO createdStaffMember = contextFacade.findStaffMemberById(staffMemberId);

        return ResponseEntity
                .created(URI.create("/api/staff/" + staffMemberId))
                .body(createdStaffMember);
    }

    @GetMapping("/staff/{staffMemberId}")
    @PreAuthorize("@staffAccess.canReadStaffMember(authentication, #staffMemberId)")
    public StaffMemberDTO findStaffMemberById(@PathVariable String staffMemberId) {
        return contextFacade.findStaffMemberById(staffMemberId);
    }

    @GetMapping("/managers/{managerId}/staff")
    @PreAuthorize("@staffAccess.canReadManagerTeam(authentication, #managerId)")
    public List<StaffMemberDTO> findStaffByManagerId(@PathVariable String managerId) {
        return contextFacade.findStaffByManagerId(managerId);
    }

    @GetMapping("/staff")
    @PreAuthorize("@staffAccess.isAdmin(authentication)")
    public List<StaffMemberDTO> findStaffByDepartment(@RequestParam String department) {
        return contextFacade.findStaffByDepartment(department);
    }

    @PatchMapping("/staff/{staffMemberId}/department")
    @PreAuthorize("@staffAccess.isAdmin(authentication)")
    public StaffMemberDTO changeDepartment(
            @PathVariable String staffMemberId,
            @Valid @RequestBody ChangeDepartmentRequest request
    ) {
        contextFacade.changeDepartment(staffMemberId, request.department());
        return contextFacade.findStaffMemberById(staffMemberId);
    }

    @PatchMapping("/staff/{staffMemberId}/job-role")
    @PreAuthorize("@staffAccess.isAdmin(authentication)")
    public StaffMemberDTO changeJobRole(
            @PathVariable String staffMemberId,
            @Valid @RequestBody ChangeJobRoleRequest request
    ) {
        contextFacade.changeJobRole(
                staffMemberId,
                request.jobRole(),
                request.roleStartDate()
        );
        return contextFacade.findStaffMemberById(staffMemberId);
    }
}
