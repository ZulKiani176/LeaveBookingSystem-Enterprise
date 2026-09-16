package uk.ac.staffs.leavebooking.leave.application;

import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestAccessView;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceJpaToDTOMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestJpaToDTOMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Service
public class LeaveQueryHandler {
    public static final String REPORTING_DATES_REQUIRED =
            "Both reporting start and end dates must be provided";
    public static final String REPORTING_END_NOT_BEFORE_START =
            "Reporting end date cannot be before the start date";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveAllowanceRepository leaveAllowanceRepository;

    public LeaveQueryHandler(
            LeaveRequestRepository leaveRequestRepository,
            LeaveAllowanceRepository leaveAllowanceRepository
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveAllowanceRepository = leaveAllowanceRepository;
    }

    public LeaveRequestDTO findLeaveRequestById(String requestId) {
        return leaveRequestRepository.findById(requestId)
                .map(LeaveRequestJpaToDTOMapper::map)
                .orElseThrow(() -> new LeaveRequestNotFoundException(requestId));
    }

    public LeaveRequestAccessView findLeaveRequestAccessView(String requestId) {
        return leaveRequestRepository.findById(requestId)
                .map(request -> new LeaveRequestAccessView(
                        request.getStaffMemberId(),
                        request.getManagerId(),
                        request.getLeaveType().name(),
                        request.getStatus().name()
                ))
                .orElseThrow(() -> new LeaveRequestNotFoundException(requestId));
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStaffMemberId(String staffMemberId) {
        return leaveRequestRepository.findByStaffMemberId(staffMemberId).stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStaffMemberIdAndStatus(
            String staffMemberId,
            LeaveStatus status
    ) {
        return leaveRequestRepository.findByStaffMemberIdAndStatus(staffMemberId, status).stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public List<LeaveRequestDTO> findLeaveRequestsByStatus(LeaveStatus status) {
        return leaveRequestRepository.findByStatus(status).stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public List<LeaveRequestDTO> findOutstandingHrLeaveRequests() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR_APPROVAL).stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public List<LeaveRequestDTO> findOutstandingLeaveRequestsByManagerId(String managerId) {
        return leaveRequestRepository
                .findByManagerIdAndStatus(managerId, LeaveStatus.PENDING)
                .stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public List<LeaveRequestDTO> findOutstandingLeaveRequestsByManagerId(
            String managerId,
            LocalDate reportingStart,
            LocalDate reportingEnd
    ) {
        validateReportingRange(reportingStart, reportingEnd);
        return leaveRequestRepository
                .findByManagerIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        managerId,
                        LeaveStatus.PENDING,
                        reportingEnd,
                        reportingStart
                )
                .stream()
                .map(LeaveRequestJpaToDTOMapper::map)
                .toList();
    }

    public LeaveAllowanceDTO findLeaveAllowance(
            String staffMemberId,
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        return leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        staffMemberId,
                        businessYearStart,
                        businessYearEnd
                )
                .map(LeaveAllowanceJpaToDTOMapper::map)
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(
                        staffMemberId,
                        businessYearStart,
                        businessYearEnd
                ));
    }

    public List<LeaveAllowanceDTO> findLeaveAllowancesByStaffMemberId(String staffMemberId) {
        return leaveAllowanceRepository.findByStaffMemberId(staffMemberId).stream()
                .map(LeaveAllowanceJpaToDTOMapper::map)
                .toList();
    }

    public LeaveUsageSummaryDTO findSystemWideUsage(
            LocalDate businessYearStart,
            LocalDate businessYearEnd
    ) {
        validateReportingRange(businessYearStart, businessYearEnd);
        var allowances = leaveAllowanceRepository
                .findByBusinessYearStartAndBusinessYearEnd(
                        businessYearStart,
                        businessYearEnd
                );
        BigDecimal totalEntitlement = allowances.stream()
                .map(allowance -> allowance.getTotalEntitlement())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemainingDays = allowances.stream()
                .map(allowance -> allowance.getRemainingLeaveDays())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LeaveUsageSummaryDTO(
                businessYearStart,
                businessYearEnd,
                allowances.size(),
                totalEntitlement,
                totalRemainingDays,
                totalEntitlement.subtract(totalRemainingDays)
        );
    }

    private void validateReportingRange(LocalDate reportingStart, LocalDate reportingEnd) {
        if (reportingStart == null || reportingEnd == null) {
            throw new IllegalArgumentException(REPORTING_DATES_REQUIRED);
        }
        if (reportingEnd.isBefore(reportingStart)) {
            throw new IllegalArgumentException(REPORTING_END_NOT_BEFORE_START);
        }
    }
}
