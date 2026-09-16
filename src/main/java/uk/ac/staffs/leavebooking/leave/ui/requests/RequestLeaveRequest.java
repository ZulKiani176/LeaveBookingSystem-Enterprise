package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;

import java.time.LocalDate;

public record RequestLeaveRequest(
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @NotNull(message = "Leave type is required")
        LeaveType leaveType,

        LeaveDayPortion dayPortion
) {
    public RequestLeaveRequest {
        dayPortion = dayPortion == null ? LeaveDayPortion.FULL_DAY : dayPortion;
    }

    public RequestLeaveRequest(
            LocalDate startDate, LocalDate endDate, String reason, LeaveType leaveType
    ) {
        this(startDate, endDate, reason, leaveType, LeaveDayPortion.FULL_DAY);
    }
}
