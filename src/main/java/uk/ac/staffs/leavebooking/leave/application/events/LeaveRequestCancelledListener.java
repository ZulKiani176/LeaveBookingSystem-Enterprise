package uk.ac.staffs.leavebooking.leave.application.events;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;

@Component
public class LeaveRequestCancelledListener {
    private final LeaveAllowanceEventCoordinator allowanceCoordinator;

    public LeaveRequestCancelledListener(LeaveAllowanceEventCoordinator allowanceCoordinator) {
        this.allowanceCoordinator = allowanceCoordinator;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(LeaveRequestCancelledEvent event) {
        if (event.previousStatus() == LeaveStatus.APPROVED) {
            allowanceCoordinator.restore(event);
        }
    }
}
