package uk.ac.staffs.leavebooking.leave.ui.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leave-allowances/usage")
public class LeaveUsageController {
    private final ContextFacade contextFacade;

    public LeaveUsageController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @GetMapping
    @PreAuthorize("@leaveAccess.canManageAllowances(authentication)")
    public LeaveUsageSummaryDTO findSystemWideUsage(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearStart,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessYearEnd
    ) {
        return contextFacade.findSystemWideUsage(businessYearStart, businessYearEnd);
    }
}
