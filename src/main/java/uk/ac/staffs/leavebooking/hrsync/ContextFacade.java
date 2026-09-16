package uk.ac.staffs.leavebooking.hrsync;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.hrsync.application.HrAbsenceSyncService;
import uk.ac.staffs.leavebooking.hrsync.application.dto.HrAbsenceSyncDTO;

import java.util.List;

@Component("hrSyncContextFacade")
public class ContextFacade {
    private final HrAbsenceSyncService service;

    public ContextFacade(HrAbsenceSyncService service) {
        this.service = service;
    }

    public List<HrAbsenceSyncDTO> find(String staffMemberId) {
        return service.find(staffMemberId);
    }
}
