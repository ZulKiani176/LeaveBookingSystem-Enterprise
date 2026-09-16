package uk.ac.staffs.leavebooking.reporting;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveStatus;
import uk.ac.staffs.leavebooking.common.events.integration.IntegrationLeaveType;
import uk.ac.staffs.leavebooking.reporting.application.LeaveReportingService;
import uk.ac.staffs.leavebooking.reporting.application.dto.LeaveReportingDTO;

import java.time.LocalDate;
import java.util.List;

@Component("reportingContextFacade")
public class ContextFacade {
    private final LeaveReportingService service;

    public ContextFacade(LeaveReportingService service) {
        this.service = service;
    }

    public List<LeaveReportingDTO> find(
            LocalDate startDate,
            LocalDate endDate,
            IntegrationLeaveType leaveType,
            IntegrationLeaveStatus status
    ) {
        return service.find(startDate, endDate, leaveType, status);
    }
}
