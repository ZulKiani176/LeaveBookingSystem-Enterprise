package uk.ac.staffs.leavebooking.leave.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.common.events.DomainEventManager;
import uk.ac.staffs.leavebooking.common.events.Event;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.leave.application.exceptions.InvalidCarryOverException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.OverlappingLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceCarryOverJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.PublicHolidayJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceCarryOverRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;
import uk.ac.staffs.leavebooking.leave.ui.commands.CarryOverLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave balances, sickness and working days")
class LeaveApplicationServiceTask17Tests {
    private static final LocalDate DATE = LocalDate.parse("2026-08-24");
    private static final LocalDate SOURCE_START = LocalDate.parse("2025-04-01");
    private static final LocalDate SOURCE_END = LocalDate.parse("2026-03-31");
    private static final LocalDate TARGET_START = LocalDate.parse("2026-04-01");
    private static final LocalDate TARGET_END = LocalDate.parse("2027-03-31");

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveAllowanceRepository leaveAllowanceRepository;
    @Mock private uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;
    @Mock private DomainEventManager domainEventManager;
    @Mock private PublicHolidayRepository publicHolidayRepository;
    @Mock private LeaveRequestEventStore leaveRequestEventStore;
    @Mock private LeaveAllowanceCarryOverRepository carryOverRepository;
    @Mock private uk.ac.staffs.leavebooking.leave.infrastructure.LeaveSubmissionLock submissionLock;

    private LeaveApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LeaveApplicationService(
                leaveRequestRepository,
                leaveAllowanceRepository,
                staffContextFacade,
                domainEventManager,
                publicHolidayRepository,
                leaveRequestEventStore,
                carryOverRepository,
                submissionLock
        );
    }

    @Test
    @DisplayName("A sickness report is stored as recorded with zero annual charge")
    void sicknessIsRecordedWithoutAnnualCharge() {
        prepareRequestCreation();

        service.requestLeave(new RequestLeaveCommand(
                "staff-1", DATE, DATE.plusDays(2), "Migraine",
                LeaveType.SICK, LeaveDayPortion.FULL_DAY
        ));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(LeaveStatus.RECORDED, saved.getStatus());
        assertEquals(new BigDecimal("0.0"), saved.getChargedLeaveDays());
        assertEquals(LeaveDayPortion.FULL_DAY, saved.getDayPortion());
        assertEquals(1, capturedDispatchedEvents().stream()
                .filter(SickLeaveRecordedIntegrationEvent.class::isInstance).count());
    }

    @Test
    @DisplayName("A valid morning annual request stores an exact half-day charge")
    void morningAnnualLeaveStoresHalfDayCharge() {
        prepareRequestCreation();

        service.requestLeave(new RequestLeaveCommand(
                "staff-1", DATE, DATE, "Appointment",
                LeaveType.ANNUAL, LeaveDayPortion.MORNING
        ));

        LeaveRequestJpa saved = captureSavedRequest();
        assertEquals(new BigDecimal("0.5"), saved.getChargedLeaveDays());
        assertEquals(LeaveDayPortion.MORNING, saved.getDayPortion());
    }

    @Test
    @DisplayName("Working-day calculation excludes weekend and configured holiday dates")
    void annualChargeExcludesWeekendAndPublicHoliday() {
        LocalDate friday = LocalDate.parse("2026-08-28");
        LocalDate monday = LocalDate.parse("2026-08-31");
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staff());
        when(publicHolidayRepository.findByDateBetweenOrderByDate(friday, monday))
                .thenReturn(List.of(new PublicHolidayJpa(monday, "Bank holiday")));
        when(leaveRequestRepository
                .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), any(), any()
                )).thenReturn(List.of());

        service.requestLeave(new RequestLeaveCommand(
                "staff-1", friday, monday, "Long weekend", LeaveType.ANNUAL
        ));

        assertEquals(new BigDecimal("1.0"), captureSavedRequest().getChargedLeaveDays());
    }

    @Test
    @DisplayName("Annual leave with no working date creates no persistence or events")
    void nonWorkingAnnualLeaveHasNoSideEffects() {
        LocalDate saturday = LocalDate.parse("2026-08-22");
        LocalDate sunday = LocalDate.parse("2026-08-23");
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staff());
        when(publicHolidayRepository.findByDateBetweenOrderByDate(saturday, sunday))
                .thenReturn(List.of());

        assertThrows(InvalidLeaveRequestException.class, () -> service.requestLeave(
                new RequestLeaveCommand(
                        "staff-1", saturday, sunday, "Weekend", LeaveType.ANNUAL
                )
        ));

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(leaveRequestEventStore, domainEventManager);
    }

    @Test
    @DisplayName("An overlapping active session creates no request or event")
    void overlappingSessionHasNoSideEffects() {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staff());
        when(publicHolidayRepository.findByDateBetweenOrderByDate(DATE, DATE))
                .thenReturn(List.of());
        LeaveRequestJpa existing = requestJpa(LeaveDayPortion.MORNING);
        when(leaveRequestRepository
                .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), eq(DATE), eq(DATE)
                )).thenReturn(List.of(existing));

        assertThrows(OverlappingLeaveRequestException.class, () -> service.requestLeave(
                new RequestLeaveCommand(
                        "staff-1", DATE, DATE, "Duplicate morning",
                        LeaveType.ANNUAL, LeaveDayPortion.MORNING
                )
        ));

        verify(leaveRequestRepository, never()).save(any());
        verifyNoInteractions(leaveRequestEventStore, domainEventManager);
    }

    @Test
    @DisplayName("Complementary morning and afternoon sessions may coexist")
    void complementarySessionsMayCoexist() {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staff());
        when(publicHolidayRepository.findByDateBetweenOrderByDate(DATE, DATE))
                .thenReturn(List.of());
        when(leaveRequestRepository
                .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), eq(DATE), eq(DATE)
                )).thenReturn(List.of(requestJpa(LeaveDayPortion.MORNING)));

        service.requestLeave(new RequestLeaveCommand(
                "staff-1", DATE, DATE, "Afternoon",
                LeaveType.ANNUAL, LeaveDayPortion.AFTERNOON
        ));

        assertEquals(LeaveDayPortion.AFTERNOON, captureSavedRequest().getDayPortion());
    }

    @Test
    @DisplayName("Carry-over is capped at five days and persisted separately from base entitlement")
    void carryOverIsCappedAtFiveDays() {
        prepareCarryOver(allowance("source", SOURCE_START, SOURCE_END, "8.0", "8.0"));

        service.carryOverLeaveAllowance(carryCommand());

        LeaveAllowanceJpa savedTarget = captureSavedAllowance();
        assertEquals(new BigDecimal("25.0"), savedTarget.getBaseEntitlement());
        assertEquals(new BigDecimal("5.0"), savedTarget.getCarriedOverDays());
        assertEquals(new BigDecimal("30.0"), savedTarget.getRemainingLeaveDays());
        ArgumentCaptor<LeaveAllowanceCarryOverJpa> receipt =
                ArgumentCaptor.forClass(LeaveAllowanceCarryOverJpa.class);
        verify(carryOverRepository).save(receipt.capture());
        assertEquals(new BigDecimal("5.0"), receipt.getValue().getCarriedDays());
    }

    @Test
    @DisplayName("Fractional source balance is carried over exactly")
    void fractionalCarryOverIsExact() {
        prepareCarryOver(allowance("source", SOURCE_START, SOURCE_END, "25.0", "3.5"));

        service.carryOverLeaveAllowance(carryCommand());

        assertEquals(new BigDecimal("3.5"), captureSavedAllowance().getCarriedOverDays());
    }

    @Test
    @DisplayName("A repeated carry-over operation does not mutate target or create another receipt")
    void repeatedCarryOverIsIdempotent() {
        LeaveAllowanceJpa source = allowance("source", SOURCE_START, SOURCE_END, "25.0", "8.0");
        LeaveAllowanceJpa target = allowance("target", TARGET_START, TARGET_END, "25.0", "25.0");
        when(leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        "staff-1", SOURCE_START, SOURCE_END
                )).thenReturn(Optional.of(source));
        when(leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        "staff-1", TARGET_START, TARGET_END
                )).thenReturn(Optional.of(target));
        when(carryOverRepository.existsBySourceAllowanceIdAndTargetAllowanceId("source", "target"))
                .thenReturn(true);

        service.carryOverLeaveAllowance(carryCommand());

        verify(leaveAllowanceRepository, never()).save(any());
        verify(carryOverRepository, never()).save(any());
    }

    @Test
    @DisplayName("Non-consecutive business years are rejected before repository access")
    void nonConsecutiveYearsAreRejectedEarly() {
        CarryOverLeaveAllowanceCommand command = new CarryOverLeaveAllowanceCommand(
                "staff-1", SOURCE_START, SOURCE_END,
                TARGET_START.plusDays(1), TARGET_END.plusDays(1)
        );

        assertThrows(InvalidCarryOverException.class, () ->
                service.carryOverLeaveAllowance(command)
        );

        verifyNoInteractions(leaveAllowanceRepository, carryOverRepository);
    }

    private void prepareRequestCreation() {
        when(staffContextFacade.findStaffMemberById("staff-1")).thenReturn(staff());
        when(publicHolidayRepository.findByDateBetweenOrderByDate(any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository
                .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        any(), any(), any(), any()
                )).thenReturn(List.of());
    }

    private void prepareCarryOver(LeaveAllowanceJpa source) {
        LeaveAllowanceJpa target = allowance("target", TARGET_START, TARGET_END, "25.0", "25.0");
        when(leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        "staff-1", SOURCE_START, SOURCE_END
                )).thenReturn(Optional.of(source));
        when(leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        "staff-1", TARGET_START, TARGET_END
                )).thenReturn(Optional.of(target));
        when(carryOverRepository.existsBySourceAllowanceIdAndTargetAllowanceId("source", "target"))
                .thenReturn(false);
    }

    private LeaveRequestJpa captureSavedRequest() {
        ArgumentCaptor<LeaveRequestJpa> captor = ArgumentCaptor.forClass(LeaveRequestJpa.class);
        verify(leaveRequestRepository).save(captor.capture());
        return captor.getValue();
    }

    private LeaveAllowanceJpa captureSavedAllowance() {
        ArgumentCaptor<LeaveAllowanceJpa> captor = ArgumentCaptor.forClass(LeaveAllowanceJpa.class);
        verify(leaveAllowanceRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Event> capturedDispatchedEvents() {
        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainEventManager).manageDomainEvents(
                eq(LeaveApplicationService.SOURCE_CONTEXT), captor.capture()
        );
        return captor.getValue();
    }

    private LeaveRequestJpa requestJpa(LeaveDayPortion portion) {
        return new LeaveRequestJpa(
                "existing", "staff-1", "manager-1", DATE, DATE,
                "Existing absence", LeaveType.ANNUAL, portion,
                new BigDecimal("0.5"), LeaveStatus.PENDING, null, 0L
        );
    }

    private LeaveAllowanceJpa allowance(
            String id, LocalDate start, LocalDate end, String base, String remaining
    ) {
        return new LeaveAllowanceJpa(
                id, "staff-1", "Ada", "Lovelace", "manager-1",
                start, end, new BigDecimal(base), BigDecimal.ZERO,
                new BigDecimal(remaining), 0L
        );
    }

    private CarryOverLeaveAllowanceCommand carryCommand() {
        return new CarryOverLeaveAllowanceCommand(
                "staff-1", SOURCE_START, SOURCE_END, TARGET_START, TARGET_END
        );
    }

    private StaffMemberDTO staff() {
        return new StaffMemberDTO(
                "staff-1", "Ada", "Lovelace", "ada@example.com",
                LocalDate.parse("2025-01-01"), "Engineering", "manager-1",
                "Developer", LocalDate.parse("2025-01-01"), "Senior",
                "Permanent", EmploymentStatus.ACTIVE
        );
    }
}
