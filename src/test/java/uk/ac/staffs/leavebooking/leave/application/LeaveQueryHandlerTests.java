package uk.ac.staffs.leavebooking.leave.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveUsageSummaryDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Leave Query Handler")
class LeaveQueryHandlerTests {
    private static final LocalDate BUSINESS_YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate BUSINESS_YEAR_END = LocalDate.of(2027, 3, 31);

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveAllowanceRepository leaveAllowanceRepository;

    @InjectMocks
    private LeaveQueryHandler handler;

    @Test
    @DisplayName("A leave request is found by identity and mapped to a DTO")
    void findLeaveRequestByIdMapsResult() {
        when(leaveRequestRepository.findById("request-1"))
                .thenReturn(Optional.of(requestJpa("request-1", LeaveStatus.APPROVED)));

        LeaveRequestDTO result = handler.findLeaveRequestById("request-1");

        assertEquals("request-1", result.id());
        assertEquals("staff-1", result.staffMemberId());
        assertEquals("manager-1", result.managerId());
        assertEquals(LeaveStatus.APPROVED, result.status());
    }

    @Test
    @DisplayName("A missing leave request raises an application exception")
    void missingLeaveRequestFails() {
        when(leaveRequestRepository.findById("missing-request")).thenReturn(Optional.empty());

        Throwable exception = assertThrows(LeaveRequestNotFoundException.class, () ->
                handler.findLeaveRequestById("missing-request")
        );

        assertEquals("Leave request not found: missing-request", exception.getMessage());
    }

    @Test
    @DisplayName("Every request for a staff member is mapped")
    void staffMemberRequestListMapsAllRecords() {
        when(leaveRequestRepository.findByStaffMemberId("staff-1")).thenReturn(List.of(
                requestJpa("request-1", LeaveStatus.PENDING),
                requestJpa("request-2", LeaveStatus.APPROVED)
        ));

        List<LeaveRequestDTO> results = handler.findLeaveRequestsByStaffMemberId("staff-1");

        assertEquals(2, results.size());
        assertEquals(
                Set.of("request-1", "request-2"),
                results.stream().map(LeaveRequestDTO::id).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("Filtering by staff and status returns the matching requests")
    void staffAndStatusFilteringMapsResults() {
        when(leaveRequestRepository.findByStaffMemberIdAndStatus("staff-1", LeaveStatus.PENDING))
                .thenReturn(List.of(requestJpa("request-1", LeaveStatus.PENDING)));

        List<LeaveRequestDTO> results = handler.findLeaveRequestsByStaffMemberIdAndStatus(
                "staff-1",
                LeaveStatus.PENDING
        );

        assertEquals(1, results.size());
        assertEquals(LeaveStatus.PENDING, results.getFirst().status());
    }

    @Test
    @DisplayName("Status-only filtering maps every matching request")
    void statusOnlyFilteringMapsResults() {
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING)).thenReturn(List.of(
                requestJpa("request-1", LeaveStatus.PENDING),
                requestJpa("request-2", LeaveStatus.PENDING)
        ));

        List<LeaveRequestDTO> results = handler.findLeaveRequestsByStatus(LeaveStatus.PENDING);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(dto -> dto.status() == LeaveStatus.PENDING));
    }

    @Test
    @DisplayName("HR outstanding requests contain every request pending HR approval")
    void hrOutstandingRequestsMapEveryHrPendingRecord() {
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR_APPROVAL)).thenReturn(List.of(
                requestJpa("request-1", LeaveStatus.PENDING_HR_APPROVAL),
                requestJpa("request-2", LeaveStatus.PENDING_HR_APPROVAL)
        ));

        List<LeaveRequestDTO> results = handler.findOutstandingHrLeaveRequests();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(
                request -> request.status() == LeaveStatus.PENDING_HR_APPROVAL
        ));
    }

    @Test
    @DisplayName("No requests pending HR approval produces an empty list")
    void emptyHrOutstandingResultReturnsEmptyList() {
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR_APPROVAL))
                .thenReturn(List.of());

        List<LeaveRequestDTO> results = handler.findOutstandingHrLeaveRequests();

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("An empty repository result becomes an empty list")
    void emptyRepositoryResultReturnsEmptyList() {
        when(leaveRequestRepository.findByStaffMemberId("staff-1")).thenReturn(List.of());

        List<LeaveRequestDTO> results = handler.findLeaveRequestsByStaffMemberId("staff-1");

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Manager outstanding requests query only the local pending read model")
    void managerOutstandingRequestsMapPendingResults() {
        when(leaveRequestRepository.findByManagerIdAndStatus("manager-1", LeaveStatus.PENDING))
                .thenReturn(List.of(requestJpa("request-1", LeaveStatus.PENDING)));

        List<LeaveRequestDTO> results =
                handler.findOutstandingLeaveRequestsByManagerId("manager-1");

        assertEquals(1, results.size());
        assertEquals("manager-1", results.getFirst().managerId());
        assertEquals(LeaveStatus.PENDING, results.getFirst().status());
    }

    @Test
    @DisplayName("A manager with no outstanding requests receives an empty list")
    void emptyManagerOutstandingRequestsReturnEmptyList() {
        when(leaveRequestRepository.findByManagerIdAndStatus("manager-1", LeaveStatus.PENDING))
                .thenReturn(List.of());

        List<LeaveRequestDTO> results =
                handler.findOutstandingLeaveRequestsByManagerId("manager-1");

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Manager reporting dates delegate to the inclusive overlap repository query")
    void managerReportingDatesUseOverlapQuery() {
        LocalDate reportingStart = LocalDate.of(2026, 8, 1);
        LocalDate reportingEnd = LocalDate.of(2026, 8, 31);
        when(leaveRequestRepository
                .findByManagerIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        "manager-1",
                        LeaveStatus.PENDING,
                        reportingEnd,
                        reportingStart
                )).thenReturn(List.of(requestJpa("request-1", LeaveStatus.PENDING)));

        List<LeaveRequestDTO> results = handler.findOutstandingLeaveRequestsByManagerId(
                "manager-1",
                reportingStart,
                reportingEnd
        );

        assertEquals(List.of("request-1"), results.stream().map(LeaveRequestDTO::id).toList());
    }

    @Test
    @DisplayName("A reversed manager reporting range is rejected before repository access")
    void reversedManagerReportingRangeIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                handler.findOutstandingLeaveRequestsByManagerId(
                        "manager-1",
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 8, 1)
                )
        );

        assertEquals(LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START, exception.getMessage());
    }

    @Test
    @DisplayName("An exact-year allowance maps every query field")
    void exactYearAllowanceMapsEveryField() {
        whenExactAllowanceLookupReturns(allowanceJpa(
                "allowance-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                30,
                22
        ));

        LeaveAllowanceDTO result = handler.findLeaveAllowance(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertEquals("allowance-1", result.id());
        assertEquals("staff-1", result.staffMemberId());
        assertEquals("Ada", result.firstName());
        assertEquals("Lovelace", result.surname());
        assertEquals("manager-1", result.managerId());
        assertEquals(BUSINESS_YEAR_START, result.businessYearStart());
        assertEquals(BUSINESS_YEAR_END, result.businessYearEnd());
        assertEquals(30, result.annualEntitlement());
        assertEquals(java.math.BigDecimal.valueOf(22), result.remainingDays());
    }

    @Test
    @DisplayName("Used allowance days are derived in the query DTO")
    void usedDaysAreDerivedInAllowanceDto() {
        whenExactAllowanceLookupReturns(allowanceJpa(
                "allowance-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END,
                30,
                22
        ));

        LeaveAllowanceDTO result = handler.findLeaveAllowance(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertEquals(java.math.BigDecimal.valueOf(8), result.usedDays());
    }

    @Test
    @DisplayName("A missing exact-year allowance raises an application exception")
    void missingExactYearAllowanceFails() {
        when(leaveAllowanceRepository.findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(Optional.empty());

        assertThrows(LeaveAllowanceNotFoundException.class, () ->
                handler.findLeaveAllowance("staff-1", BUSINESS_YEAR_START, BUSINESS_YEAR_END)
        );
    }

    @Test
    @DisplayName("Multiple yearly allowances for one staff member are mapped")
    void multipleYearlyAllowancesAreMapped() {
        LocalDate secondYearStart = LocalDate.of(2027, 4, 1);
        LocalDate secondYearEnd = LocalDate.of(2028, 3, 31);
        when(leaveAllowanceRepository.findByStaffMemberId("staff-1")).thenReturn(List.of(
                allowanceJpa("allowance-1", BUSINESS_YEAR_START, BUSINESS_YEAR_END, 25, 20),
                allowanceJpa("allowance-2", secondYearStart, secondYearEnd, 30, 30)
        ));

        List<LeaveAllowanceDTO> results = handler.findLeaveAllowancesByStaffMemberId("staff-1");

        assertEquals(2, results.size());
        assertEquals(
                Set.of(BUSINESS_YEAR_START, secondYearStart),
                results.stream().map(LeaveAllowanceDTO::businessYearStart).collect(Collectors.toSet())
        );
    }

    @Test
    @DisplayName("System-wide usage totals are derived across the exact business year")
    void systemWideUsageTotalsAreDerived() {
        when(leaveAllowanceRepository.findByBusinessYearStartAndBusinessYearEnd(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(List.of(
                allowanceJpa("allowance-1", BUSINESS_YEAR_START, BUSINESS_YEAR_END, 25, 20),
                allowanceJpa("allowance-2", BUSINESS_YEAR_START, BUSINESS_YEAR_END, 30, 18)
        ));

        LeaveUsageSummaryDTO result = handler.findSystemWideUsage(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertEquals(BUSINESS_YEAR_START, result.businessYearStart());
        assertEquals(BUSINESS_YEAR_END, result.businessYearEnd());
        assertEquals(2, result.staffCount());
        assertEquals(java.math.BigDecimal.valueOf(55), result.totalEntitlement());
        assertEquals(java.math.BigDecimal.valueOf(38), result.totalRemainingDays());
        assertEquals(java.math.BigDecimal.valueOf(17), result.totalUsedDays());
    }

    @Test
    @DisplayName("An empty business year returns a zero-valued usage summary")
    void emptyBusinessYearReturnsZeroUsageSummary() {
        when(leaveAllowanceRepository.findByBusinessYearStartAndBusinessYearEnd(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(List.of());

        LeaveUsageSummaryDTO result = handler.findSystemWideUsage(
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        );

        assertEquals(0, result.staffCount());
        assertEquals(java.math.BigDecimal.ZERO, result.totalEntitlement());
        assertEquals(java.math.BigDecimal.ZERO, result.totalRemainingDays());
        assertEquals(java.math.BigDecimal.ZERO, result.totalUsedDays());
    }

    @Test
    @DisplayName("A reversed usage reporting range is rejected")
    void reversedUsageReportingRangeIsRejected() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                handler.findSystemWideUsage(BUSINESS_YEAR_END, BUSINESS_YEAR_START)
        );

        assertEquals(LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START, exception.getMessage());
    }

    private LeaveRequestJpa requestJpa(String id, LeaveStatus status) {
        return new LeaveRequestJpa(
                id,
                "staff-1",
                "manager-1",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 14),
                "Summer holiday",
                LeaveType.ANNUAL,
                status
        );
    }

    private LeaveAllowanceJpa allowanceJpa(
            String id,
            LocalDate businessYearStart,
            LocalDate businessYearEnd,
            int annualEntitlement,
            int remainingDays
    ) {
        return new LeaveAllowanceJpa(
                id,
                "staff-1",
                "Ada",
                "Lovelace",
                "manager-1",
                businessYearStart,
                businessYearEnd,
                annualEntitlement,
                remainingDays
        );
    }

    private void whenExactAllowanceLookupReturns(LeaveAllowanceJpa allowanceJpa) {
        when(leaveAllowanceRepository.findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                "staff-1",
                BUSINESS_YEAR_START,
                BUSINESS_YEAR_END
        )).thenReturn(Optional.of(allowanceJpa));
    }
}
