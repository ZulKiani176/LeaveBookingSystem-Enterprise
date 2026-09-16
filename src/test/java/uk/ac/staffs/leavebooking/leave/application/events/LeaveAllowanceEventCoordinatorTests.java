package uk.ac.staffs.leavebooking.leave.application.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestApprovedEvent;
import uk.ac.staffs.leavebooking.leave.domain.events.LeaveRequestCancelledEvent;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Allowance Event Coordinator")
class LeaveAllowanceEventCoordinatorTests {
    private static final LocalDate LEAVE_START = LocalDate.of(2026, 9, 14);
    private static final LocalDate LEAVE_END = LocalDate.of(2026, 9, 18);

    @Mock
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @InjectMocks
    private LeaveAllowanceEventCoordinator coordinator;

    @Test
    @DisplayName("Approval deducts inclusive calendar days from the containing allowance")
    void approvalDeductsCalendarDays() {
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1"))
                .thenReturn(List.of(allowance("allowance-1", 25, 25)));

        coordinator.deduct(approvedEvent());

        assertEquals(20, captureSavedAllowance().getRemainingDays());
    }

    @Test
    @DisplayName("Approved cancellation restores inclusive calendar days")
    void approvedCancellationRestoresCalendarDays() {
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1"))
                .thenReturn(List.of(allowance("allowance-1", 25, 20)));

        coordinator.restore(cancelledEvent());

        assertEquals(25, captureSavedAllowance().getRemainingDays());
    }

    @Test
    @DisplayName("No allowance containing the complete leave period fails clearly")
    void missingApplicableAllowanceFails() {
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1")).thenReturn(List.of());

        Throwable exception = assertThrows(LeaveAllowanceNotFoundException.class, () ->
                coordinator.deduct(approvedEvent())
        );

        assertEquals(
                "No leave allowance for staff member staff-1 contains leave period "
                        + "2026-09-14 to 2026-09-18",
                exception.getMessage()
        );
        verify(leaveAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Multiple containing allowances fail rather than selecting arbitrarily")
    void multipleApplicableAllowancesFail() {
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1")).thenReturn(List.of(
                allowance("allowance-1", 25, 25),
                new LeaveAllowanceJpa(
                        "allowance-2",
                        "staff-1",
                        "Ada",
                        "Lovelace",
                        "manager-1",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        25,
                        25
                )
        ));

        Throwable exception = assertThrows(InvalidLeaveAllowanceException.class, () ->
                coordinator.deduct(approvedEvent())
        );

        assertEquals(
                LeaveAllowanceEventCoordinator.MULTIPLE_APPLICABLE_ALLOWANCES,
                exception.getMessage()
        );
        verify(leaveAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Insufficient allowance propagates the domain failure without saving")
    void insufficientAllowanceIsNotSaved() {
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1"))
                .thenReturn(List.of(allowance("allowance-1", 25, 4)));

        assertThrows(InvalidLeaveAllowanceException.class, () ->
                coordinator.deduct(approvedEvent())
        );

        verify(leaveAllowanceRepository, never()).save(any());
    }

    private LeaveRequestApprovedEvent approvedEvent() {
        return new LeaveRequestApprovedEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1",
                LEAVE_START,
                LEAVE_END
        );
    }

    private LeaveRequestCancelledEvent cancelledEvent() {
        return new LeaveRequestCancelledEvent(
                LocalDate.of(2026, 8, 25),
                "request-1",
                "staff-1",
                LEAVE_START,
                LEAVE_END,
                LeaveStatus.APPROVED
        );
    }

    private LeaveAllowanceJpa allowance(String id, int entitlement, int remaining) {
        return new LeaveAllowanceJpa(
                id,
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 3, 31),
                entitlement,
                remaining
        );
    }

    private LeaveAllowanceJpa captureSavedAllowance() {
        ArgumentCaptor<LeaveAllowanceJpa> captor = ArgumentCaptor.forClass(LeaveAllowanceJpa.class);
        verify(leaveAllowanceRepository).save(captor.capture());
        return captor.getValue();
    }
}
