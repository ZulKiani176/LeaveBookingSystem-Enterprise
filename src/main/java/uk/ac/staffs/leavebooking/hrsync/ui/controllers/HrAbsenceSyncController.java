package uk.ac.staffs.leavebooking.hrsync.ui.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.hrsync.ContextFacade;
import uk.ac.staffs.leavebooking.hrsync.application.dto.HrAbsenceSyncDTO;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/hr/absences")
public class HrAbsenceSyncController {
    private final ContextFacade contextFacade;

    public HrAbsenceSyncController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<HrAbsenceSyncDTO> find(
            @RequestParam(required = false) String staffMemberId
    ) {
        return contextFacade.find(staffMemberId);
    }
}
