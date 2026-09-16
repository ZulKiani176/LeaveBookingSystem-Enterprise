package uk.ac.staffs.leavebooking.leave.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.dto.TeamLeaveCalendarEntryDTO;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Team Leave Calendar Service")
class TeamLeaveCalendarServiceTests {
    private static final LocalDate START = LocalDate.parse("2026-08-01");
    private static final LocalDate END = LocalDate.parse("2026-08-31");

    @Mock private uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;
    @Mock private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("The team calendar is based on the manager's current Staff context assignments")
    void calendarUsesCurrentTeam() {
        when(staffContextFacade.findStaffByManagerId("manager-current"))
                .thenReturn(List.of(staff("staff-current", "Current", "Member")));
        when(leaveRequestRepository
                .findByStaffMemberIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        List.of("staff-current"), END, START
                )).thenReturn(List.of(request(
                        "request-1", "staff-current", "manager-historical",
                        LeaveType.ANNUAL, LeaveStatus.APPROVED, "Private reason"
                )));

        List<TeamLeaveCalendarEntryDTO> result = service().find(
                "manager-current", START, END
        );

        assertEquals(1, result.size());
        assertEquals("Current", result.getFirst().firstName());
        assertEquals("staff-current", result.getFirst().staffMemberId());
    }

    @Test
    @DisplayName("Sickness is visible in the calendar without exposing its reason")
    void sicknessCalendarEntryIsPrivacySafe() {
        when(staffContextFacade.findStaffByManagerId("manager-1"))
                .thenReturn(List.of(staff("staff-1", "Ada", "Lovelace")));
        when(leaveRequestRepository
                .findByStaffMemberIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        List.of("staff-1"), END, START
                )).thenReturn(List.of(request(
                        "sick-1", "staff-1", "manager-1",
                        LeaveType.SICK, LeaveStatus.RECORDED, "Sensitive diagnosis"
                )));

        TeamLeaveCalendarEntryDTO result = service().find("manager-1", START, END).getFirst();

        assertEquals(LeaveType.SICK, result.leaveType());
        assertEquals(LeaveStatus.RECORDED, result.status());
        assertFalse(List.of(TeamLeaveCalendarEntryDTO.class.getRecordComponents()).stream()
                .anyMatch(component -> component.getName().equals("reason")));
    }

    @Test
    @DisplayName("Rejected and cancelled requests are excluded from workforce planning")
    void inactiveRequestsAreExcluded() {
        when(staffContextFacade.findStaffByManagerId("manager-1"))
                .thenReturn(List.of(staff("staff-1", "Ada", "Lovelace")));
        when(leaveRequestRepository
                .findByStaffMemberIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        List.of("staff-1"), END, START
                )).thenReturn(List.of(
                        request("rejected", "staff-1", "manager-1", LeaveType.ANNUAL,
                                LeaveStatus.REJECTED, "Reason"),
                        request("cancelled", "staff-1", "manager-1", LeaveType.SICK,
                                LeaveStatus.CANCELLED, "Private")
                ));

        assertEquals(List.of(), service().find("manager-1", START, END));
    }

    @Test
    @DisplayName("A manager with no current staff receives an empty calendar")
    void emptyCurrentTeamAvoidsLeaveQuery() {
        when(staffContextFacade.findStaffByManagerId("manager-1")).thenReturn(List.of());

        assertEquals(List.of(), service().find("manager-1", START, END));

    }

    @Test
    @DisplayName("A reversed calendar window is rejected")
    void reversedWindowIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                service().find("manager-1", END, START)
        );

    }

    private TeamLeaveCalendarService service() {
        return new TeamLeaveCalendarService(staffContextFacade, leaveRequestRepository);
    }

    private StaffMemberDTO staff(String id, String firstName, String surname) {
        return new StaffMemberDTO(
                id, firstName, surname, id + "@example.com", LocalDate.parse("2025-01-01"),
                "Engineering", "manager-1", "Developer", LocalDate.parse("2025-01-01"),
                "Senior", "Permanent", EmploymentStatus.ACTIVE
        );
    }

    private LeaveRequestJpa request(
            String id, String staffId, String managerId, LeaveType type,
            LeaveStatus status, String reason
    ) {
        return new LeaveRequestJpa(
                id, staffId, managerId, LocalDate.parse("2026-08-10"),
                LocalDate.parse("2026-08-12"), reason, type, status
        );
    }
}
