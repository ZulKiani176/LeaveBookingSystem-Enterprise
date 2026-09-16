package uk.ac.staffs.leavebooking.audit;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.audit.application.LeaveAuditService;
import uk.ac.staffs.leavebooking.audit.application.dto.LeaveAuditDTO;

import java.time.LocalDate;
import java.util.List;

@Component("auditContextFacade")
public class ContextFacade {
    private final LeaveAuditService service;

    public ContextFacade(LeaveAuditService service) {
        this.service = service;
    }

    public List<LeaveAuditDTO> find(
            String leaveRequestId,
            String staffMemberId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return service.find(leaveRequestId, staffMemberId, startDate, endDate);
    }
}
