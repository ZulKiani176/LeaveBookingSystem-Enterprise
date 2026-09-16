package uk.ac.staffs.leavebooking.leave.application.events;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;

@Component
public class LeaveRequestApprovedListener {
    private final LeaveAllowanceEventCoordinator allowanceCoordinator;

    public LeaveRequestApprovedListener(LeaveAllowanceEventCoordinator allowanceCoordinator) {
        this.allowanceCoordinator = allowanceCoordinator;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(LeaveRequestApprovedEvent event) {
        allowanceCoordinator.deduct(event);
    }
}
