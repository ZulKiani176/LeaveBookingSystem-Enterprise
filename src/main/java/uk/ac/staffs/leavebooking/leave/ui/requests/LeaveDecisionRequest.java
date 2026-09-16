package uk.ac.staffs.leavebooking.leave.ui.requests;

import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(
        @Size(max = 500, message = "Decision comment must not exceed 500 characters")
        String comment
) {
}
