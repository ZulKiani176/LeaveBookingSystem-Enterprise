package uk.ac.staffs.leavebooking.leave.application;

import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.leave.application.dto.TeamLeaveCalendarEntryDTO;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamLeaveCalendarService {
    private final uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;
    private final LeaveRequestRepository leaveRequestRepository;

    public TeamLeaveCalendarService(
            uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade,
            LeaveRequestRepository leaveRequestRepository
    ) {
        this.staffContextFacade = staffContextFacade;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public List<TeamLeaveCalendarEntryDTO> find(
            String managerId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Team-calendar start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Team-calendar end date cannot be before start date"
            );
        }
        Map<String, StaffMemberDTO> currentTeam = staffContextFacade
                .findStaffByManagerId(managerId)
                .stream()
                .collect(Collectors.toMap(StaffMemberDTO::id, Function.identity()));
        if (currentTeam.isEmpty()) {
            return List.of();
        }
        return leaveRequestRepository
                .findByStaffMemberIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        List.copyOf(currentTeam.keySet()), endDate, startDate
                )
                .stream()
                .filter(request -> request.getStatus() != uk.ac.staffs.leavebooking.leave.domain.LeaveStatus.REJECTED
                        && request.getStatus() != uk.ac.staffs.leavebooking.leave.domain.LeaveStatus.CANCELLED)
                .map(request -> {
                    StaffMemberDTO staff = currentTeam.get(request.getStaffMemberId());
                    return new TeamLeaveCalendarEntryDTO(
                            request.getId(),
                            request.getStaffMemberId(),
                            staff.firstName(),
                            staff.surname(),
                            request.getStartDate(),
                            request.getEndDate(),
                            request.getLeaveType(),
                            request.getDayPortion(),
                            request.getStatus()
                    );
                })
                .toList();
    }
}
