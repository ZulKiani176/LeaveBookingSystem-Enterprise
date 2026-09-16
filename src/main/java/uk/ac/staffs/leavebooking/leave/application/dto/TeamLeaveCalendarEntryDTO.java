package uk.ac.staffs.leavebooking.leave.application.dto;

import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;

import java.time.LocalDate;

public record TeamLeaveCalendarEntryDTO(
        String leaveRequestId,
        String staffMemberId,
        String firstName,
        String surname,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        LeaveDayPortion dayPortion,
        LeaveStatus status
) {
}
