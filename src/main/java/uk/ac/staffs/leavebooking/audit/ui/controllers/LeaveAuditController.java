package uk.ac.staffs.leavebooking.audit.ui.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.audit.ContextFacade;
import uk.ac.staffs.leavebooking.audit.application.dto.LeaveAuditDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/audit/leave")
public class LeaveAuditController {
    private final ContextFacade contextFacade;

    public LeaveAuditController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<LeaveAuditDTO> find(
            @RequestParam(required = false) String leaveRequestId,
            @RequestParam(required = false) String staffMemberId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return contextFacade.find(leaveRequestId, staffMemberId, startDate, endDate);
    }
}
