package uk.ac.staffs.leavebooking.reporting.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.reporting.application.LeaveReportingService;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Leave Reporting Rabbit Listener")
class LeaveReportingRabbitListenerTests {
    private static final LocalDate OCCURRED_ON = LocalDate.of(2026, 8, 26);
    private static final LocalDate START_DATE = LocalDate.of(2026, 9, 14);
    private static final LocalDate END_DATE = LocalDate.of(2026, 9, 18);

    private LeaveReportingService service;
    private LeaveReportingRabbitListener listener;

    @BeforeEach
    void setUp() {
        service = mock(LeaveReportingService.class);
        listener = new LeaveReportingRabbitListener(service);
    }

    @Test
    @DisplayName("Submitted leave is delegated to the reporting projection")
    void submittedLeaveIsDelegated() {
        var event = new LeaveRequestSubmittedIntegrationEvent(
                1L, OCCURRED_ON, "request-1", "staff-1", "manager-1",
                START_DATE, END_DATE
        );

        listener.receive(event);

        verify(service).apply(event);
    }

    @Test
    @DisplayName("Recorded sickness is delegated to the reporting projection")
    void recordedSicknessIsDelegated() {
        var event = new SickLeaveRecordedIntegrationEvent(
                OCCURRED_ON, "request-1", "staff-1", "manager-1",
                START_DATE, END_DATE
        ).withId(2L);

        listener.receive(event);

        verify(service).apply(event);
    }

    @Test
    @DisplayName("Approved leave is delegated to the reporting projection")
    void approvedLeaveIsDelegated() {
        var event = new LeaveRequestApprovedIntegrationEvent(
                3L, OCCURRED_ON, "request-1", "staff-1", START_DATE, END_DATE
        );

        listener.receive(event);

        verify(service).apply(event);
    }

    @Test
    @DisplayName("Rejected leave is delegated to the reporting projection")
    void rejectedLeaveIsDelegated() {
        var event = new LeaveRequestRejectedIntegrationEvent(
                4L, OCCURRED_ON, "request-1", "staff-1"
        );

        listener.receive(event);

        verify(service).apply(event);
    }

    @Test
    @DisplayName("Cancelled leave is delegated to the reporting projection")
    void cancelledLeaveIsDelegated() {
        var event = new LeaveRequestCancelledIntegrationEvent(
                5L, OCCURRED_ON, "request-1", "staff-1",
                START_DATE, END_DATE, "APPROVED"
        );

        listener.receive(event);

        verify(service).apply(event);
    }
}
