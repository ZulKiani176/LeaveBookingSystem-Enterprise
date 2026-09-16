package uk.ac.staffs.leavebooking.leave.application.events;

import org.springframework.stereotype.Component;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceJpaToDomainMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;

import java.util.List;

@Component
public class LeaveAllowanceEventCoordinator {
    public static final String MULTIPLE_APPLICABLE_ALLOWANCES =
            "Multiple leave allowances contain the requested leave period";

    private final LeaveAllowanceRepository leaveAllowanceRepository;

    public LeaveAllowanceEventCoordinator(LeaveAllowanceRepository leaveAllowanceRepository) {
        this.leaveAllowanceRepository = leaveAllowanceRepository;
    }

    public void deduct(LeaveRequestApprovedEvent event) {
        LeavePeriod leavePeriod = new LeavePeriod(event.startDate(), event.endDate());
        LeaveAllowanceJpa stored = findApplicableAllowance(event.staffMemberId(), leavePeriod);
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.map(stored);
        allowance.deduct(new uk.ac.staffs.leavebooking.leave.domain.LeaveDays(
                event.chargedLeaveDays()
        ));
        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.map(
                allowance,
                stored.getVersion()
        ));
    }

    public void restore(LeaveRequestCancelledEvent event) {
        LeavePeriod leavePeriod = new LeavePeriod(event.startDate(), event.endDate());
        LeaveAllowanceJpa stored = findApplicableAllowance(event.staffMemberId(), leavePeriod);
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.map(stored);
        allowance.restore(new uk.ac.staffs.leavebooking.leave.domain.LeaveDays(
                event.chargedLeaveDays()
        ));
        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.map(
                allowance,
                stored.getVersion()
        ));
    }

    private LeaveAllowanceJpa findApplicableAllowance(
            String staffMemberId,
            LeavePeriod leavePeriod
    ) {
        List<LeaveAllowanceJpa> applicableAllowances = leaveAllowanceRepository
                .findByStaffMemberId(staffMemberId)
                .stream()
                .filter(allowance -> !leavePeriod.startDate().isBefore(
                        allowance.getBusinessYearStart()
                ) && !leavePeriod.endDate().isAfter(allowance.getBusinessYearEnd()))
                .toList();

        if (applicableAllowances.isEmpty()) {
            throw LeaveAllowanceNotFoundException.forLeavePeriod(
                    staffMemberId,
                    leavePeriod.startDate(),
                    leavePeriod.endDate()
            );
        }
        if (applicableAllowances.size() > 1) {
            throw new InvalidLeaveAllowanceException(MULTIPLE_APPLICABLE_ALLOWANCES);
        }
        return applicableAllowances.getFirst();
    }
}
