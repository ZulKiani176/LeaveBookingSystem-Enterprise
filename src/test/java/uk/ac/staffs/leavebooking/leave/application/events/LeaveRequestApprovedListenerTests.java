package uk.ac.staffs.leavebooking.leave.application.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Request Approved Listener")
class LeaveRequestApprovedListenerTests {
    @Mock
    private LeaveAllowanceEventCoordinator allowanceCoordinator;

    @InjectMocks
    private LeaveRequestApprovedListener listener;

    @Test
    @DisplayName("An approval event delegates allowance deduction")
    void approvalDelegatesDeduction() {
        LeaveRequestApprovedEvent event = event();

        listener.handle(event);

        verify(allowanceCoordinator).deduct(event);
    }

    @Test
    @DisplayName("Approval coordination is registered before transaction commit")
    void approvalListenerUsesBeforeCommitPhase() throws Exception {
        Method method = LeaveRequestApprovedListener.class.getMethod(
                "handle",
                LeaveRequestApprovedEvent.class
        );
        TransactionalEventListener annotation = method.getAnnotation(
                TransactionalEventListener.class
        );

        assertEquals(TransactionPhase.BEFORE_COMMIT, annotation.phase());
    }

    private LeaveRequestApprovedEvent event() {
        return new LeaveRequestApprovedEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1",
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18)
        );
    }
}
