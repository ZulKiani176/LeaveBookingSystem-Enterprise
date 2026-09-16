package uk.ac.staffs.leavebooking.leave.ui.controllers;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.requests.AmendLeaveAllowanceRequest;
import uk.ac.staffs.leavebooking.leave.ui.requests.CreateLeaveAllowanceRequest;
import uk.ac.staffs.leavebooking.leave.ui.requests.CarryOverLeaveAllowanceRequest;
import uk.ac.staffs.leavebooking.leave.ui.commands.CarryOverLeaveAllowanceCommand;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff/{staffMemberId}/leave-allowances")
public class LeaveAllowanceController {
    private final ContextFacade contextFacade;

    public LeaveAllowanceController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @PostMapping
    @PreAuthorize("@leaveAccess.canManageAllowances(authentication)")
    public ResponseEntity<LeaveAllowanceDTO> createLeaveAllowance(
            @PathVariable String staffMemberId,
            @Valid @RequestBody CreateLeaveAllowanceRequest request
    ) {
        String allowanceId = contextFacade.createLeaveAllowance(new CreateLeaveAllowanceCommand(
                staffMemberId,
                request.businessYearStart(),
                request.businessYearEnd(),
                request.annualEntitlement()
        ));
        LeaveAllowanceDTO createdAllowance = contextFacade.findLeaveAllowance(
                staffMemberId,
                request.businessYearStart(),
                request.businessYearEnd()
        );
        URI location = URI.create(
                "/api/staff/" + staffMemberId + "/leave-allowances/year?start="
                        + request.businessYearStart() + "&end=" + request.businessYearEnd()
        );

        return ResponseEntity.created(location).body(createdAllowance);
    }

    @GetMapping
    @PreAuthorize("@leaveAccess.canReadAllowance(authentication, #staffMemberId)")
    public List<LeaveAllowanceDTO> findLeaveAllowancesByStaffMemberId(
            @PathVariable String staffMemberId
    ) {
        return contextFacade.findLeaveAllowancesByStaffMemberId(staffMemberId);
    }

    @GetMapping("/year")
    @PreAuthorize("@leaveAccess.canReadAllowance(authentication, #staffMemberId)")
    public LeaveAllowanceDTO findLeaveAllowance(
            @PathVariable String staffMemberId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearStart,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearEnd
    ) {
        return contextFacade.findLeaveAllowance(
                staffMemberId,
                businessYearStart,
                businessYearEnd
        );
    }

    @PatchMapping("/year")
    @PreAuthorize("@leaveAccess.canManageAllowances(authentication)")
    public LeaveAllowanceDTO amendLeaveAllowance(
            @PathVariable String staffMemberId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearStart,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearEnd,
            @Valid @RequestBody AmendLeaveAllowanceRequest request
    ) {
        contextFacade.amendLeaveAllowance(new AmendLeaveAllowanceCommand(
                staffMemberId,
                businessYearStart,
                businessYearEnd,
                request.newEntitlement()
        ));

        return contextFacade.findLeaveAllowance(
                staffMemberId,
                businessYearStart,
                businessYearEnd
        );
    }

    @PostMapping("/carry-over")
    @PreAuthorize("@leaveAccess.canManageAllowances(authentication)")
    public LeaveAllowanceDTO carryOverLeaveAllowance(
            @PathVariable String staffMemberId,
            @Valid @RequestBody CarryOverLeaveAllowanceRequest request
    ) {
        contextFacade.carryOverLeaveAllowance(new CarryOverLeaveAllowanceCommand(
                staffMemberId,
                request.sourceYearStart(),
                request.sourceYearEnd(),
                request.targetYearStart(),
                request.targetYearEnd()
        ));
        return contextFacade.findLeaveAllowance(
                staffMemberId,
                request.targetYearStart(),
                request.targetYearEnd()
        );
    }
}
