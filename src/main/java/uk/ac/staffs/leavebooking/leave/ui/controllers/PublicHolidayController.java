package uk.ac.staffs.leavebooking.leave.ui.controllers;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.dto.PublicHolidayDTO;
import uk.ac.staffs.leavebooking.leave.ui.requests.CreatePublicHolidayRequest;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public-holidays")
public class PublicHolidayController {
    private final ContextFacade contextFacade;

    public PublicHolidayController(ContextFacade contextFacade) {
        this.contextFacade = contextFacade;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicHolidayDTO> create(
            @Valid @RequestBody CreatePublicHolidayRequest request
    ) {
        PublicHolidayDTO created = contextFacade.createPublicHoliday(request.date(), request.name());
        return ResponseEntity.created(URI.create("/api/public-holidays/" + created.date()))
                .body(created);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PublicHolidayDTO> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return contextFacade.findPublicHolidays(startDate, endDate);
    }

    @DeleteMapping("/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        contextFacade.deletePublicHoliday(date);
        return ResponseEntity.noContent().build();
    }
}
