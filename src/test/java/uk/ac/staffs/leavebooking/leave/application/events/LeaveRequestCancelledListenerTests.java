package uk.ac.staffs.leavebooking.leave.application.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Request Cancelled Listener")
class LeaveRequestCancelledListenerTests {
    @Mock
    private LeaveAllowanceEventCoordinator allowanceCoordinator;

    @InjectMocks
    private LeaveRequestCancelledListener listener;

    @Test
    @DisplayName("Cancellation after approval delegates allowance restoration")
    void approvedCancellationDelegatesRestoration() {
        LeaveRequestCancelledEvent event = event(LeaveStatus.APPROVED);

        listener.handle(event);

        verify(allowanceCoordinator).restore(event);
    }

    @ParameterizedTest(name = "{displayName} (case {index})")
    @EnumSource(value = LeaveStatus.class, names = {"PENDING", "PENDING_HR_APPROVAL"})
    @DisplayName("Cancelling leave that was not approved does not restore allowance")
    void nonApprovedCancellationDoesNotRestore(LeaveStatus previousStatus) {
        listener.handle(event(previousStatus));

        verifyNoInteractions(allowanceCoordinator);
    }

    @Test
    @DisplayName("Cancellation coordination is registered before transaction commit")
    void cancellationListenerUsesBeforeCommitPhase() throws Exception {
        Method method = LeaveRequestCancelledListener.class.getMethod(
                "handle",
                LeaveRequestCancelledEvent.class
        );
        TransactionalEventListener annotation = method.getAnnotation(
                TransactionalEventListener.class
        );

        assertEquals(TransactionPhase.BEFORE_COMMIT, annotation.phase());
    }

    private LeaveRequestCancelledEvent event(LeaveStatus previousStatus) {
        return new LeaveRequestCancelledEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1",
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18),
                previousStatus
        );
    }
}
