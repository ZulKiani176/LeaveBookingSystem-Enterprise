package uk.ac.staffs.leavebooking.reporting.ui.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.reporting.ContextFacade;
import uk.ac.staffs.leavebooking.reporting.application.dto.LeaveReportingDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/leave")
public class LeaveReportingController {
    private final ContextFacade contextFacade;

    public LeaveReportingController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<LeaveReportingDTO> find(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) IntegrationLeaveType leaveType,
            @RequestParam(required = false) IntegrationLeaveStatus status
    ) {
        return contextFacade.find(startDate, endDate, leaveType, status);
    }
}
