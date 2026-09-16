package uk.ac.staffs.leavebooking.leave.ui.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.staffs.leavebooking.GlobalExceptionHandler;
import uk.ac.staffs.leavebooking.identity.security.WithMockFirebaseUser;
import uk.ac.staffs.leavebooking.identity.security.ControllerSecurityTestConfiguration;
import uk.ac.staffs.leavebooking.leave.ContextFacade;
import uk.ac.staffs.leavebooking.leave.application.LeaveQueryHandler;
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveRequestDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.LeaveType;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveRequestController.class)
@Import({GlobalExceptionHandler.class, ControllerSecurityTestConfiguration.class})
@WithMockFirebaseUser
@DisplayName("Leave request API")
class LeaveRequestControllerTests {
    private static final String STAFF_ID = "staff-1";
    private static final String REQUEST_ID = "request-1";
    private static final String VALID_REQUEST_JSON = """
            {
              "startDate": "2026-09-14",
              "endDate": "2026-09-18",
              "reason": "Annual leave",
              "leaveType": "ANNUAL"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContextFacade contextFacade;

    @Nested
    @DisplayName("Requesting leave")
    class RequestingLeave {
        @Test
        @DisplayName("A valid request returns 201 with its details and a link")
        void validRequestLeaveReturnsCreatedResource() throws Exception {
            LeaveRequestDTO created = requestDto(LeaveStatus.PENDING);
            when(contextFacade.requestLeave(any(RequestLeaveCommand.class))).thenReturn(REQUEST_ID);
            when(contextFacade.findLeaveRequestById(REQUEST_ID)).thenReturn(created);

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/leave-requests/" + REQUEST_ID))
                    .andExpect(jsonPath("$.id").value(REQUEST_ID))
                    .andExpect(jsonPath("$.staffMemberId").value(STAFF_ID))
                    .andExpect(jsonPath("$.managerId").value("manager-1"))
                    .andExpect(jsonPath("$.startDate").value("2026-09-14"))
                    .andExpect(jsonPath("$.endDate").value("2026-09-18"))
                    .andExpect(jsonPath("$.reason").value("Annual leave"))
                    .andExpect(jsonPath("$.leaveType").value("ANNUAL"))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            var commandCaptor = org.mockito.ArgumentCaptor.forClass(RequestLeaveCommand.class);
            verify(contextFacade).requestLeave(commandCaptor.capture());
            RequestLeaveCommand command = commandCaptor.getValue();
            assertEquals(STAFF_ID, command.staffMemberId());
            assertEquals(LocalDate.of(2026, 9, 14), command.startDate());
            assertEquals(LocalDate.of(2026, 9, 18), command.endDate());
            assertEquals("Annual leave", command.reason());
            assertEquals(LeaveType.ANNUAL, command.leaveType());
        }

        @Test
        @DisplayName("A blank reason returns 400")
        void blankReasonIsRejected() throws Exception {
            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_JSON.replace("Annual leave", "   ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("reason: Reason is required"))
                    .andExpect(jsonPath("$.path").value("/api/staff/staff-1/leave-requests"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A reason longer than 500 characters returns 400")
        void oversizedReasonIsRejected() throws Exception {
            String body = VALID_REQUEST_JSON.replace("Annual leave", "a".repeat(501));

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value(
                            "reason: Reason must not exceed 500 characters"
                    ));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A missing start date returns 400")
        void missingStartDateIsRejected() throws Exception {
            String body = VALID_REQUEST_JSON.replace("\"startDate\": \"2026-09-14\",", "");

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("startDate: Start date is required"));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A missing end date returns 400")
        void missingEndDateIsRejected() throws Exception {
            String body = VALID_REQUEST_JSON.replace("\"endDate\": \"2026-09-18\",", "");

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("endDate: End date is required"));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A missing leave type returns 400")
        void missingLeaveTypeIsRejected() throws Exception {
            String body = """
                    {
                      "startDate": "2026-09-14",
                      "endDate": "2026-09-18",
                      "reason": "Annual leave"
                    }
                    """;

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("leaveType: Leave type is required"));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("Invalid JSON returns 400")
        void malformedJsonIsRejected() throws Exception {
            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"startDate\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(
                            "The request body is missing or malformed"
                    ));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("An end date before the start date returns 400")
        void reversedLeavePeriodIsRejectedByDomain() throws Exception {
            when(contextFacade.requestLeave(any(RequestLeaveCommand.class)))
                    .thenThrow(new IllegalArgumentException(LeavePeriod.END_DATE_NOT_BEFORE_START));
            String body = VALID_REQUEST_JSON
                    .replace("2026-09-14", "2026-09-19")
                    .replace("2026-09-18", "2026-09-18");

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(LeavePeriod.END_DATE_NOT_BEFORE_START));

        }
    }

    @Nested
    @DisplayName("Finding leave requests")
    class FindingRequests {
        @Test
        @DisplayName("Finding an existing request returns 200 with its details")
        void findLeaveRequestByIdReturnsDto() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID)).thenReturn(requestDto(LeaveStatus.PENDING));

            mockMvc.perform(get("/api/leave-requests/{requestId}", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(REQUEST_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("A missing request returns 404 with a helpful error")
        void missingLeaveRequestReturnsNotFound() throws Exception {
            when(contextFacade.findLeaveRequestById("missing"))
                    .thenThrow(new LeaveRequestNotFoundException("missing"));

            mockMvc.perform(get("/api/leave-requests/{requestId}", "missing"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("LEAVE_REQUEST_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Leave request not found: missing"))
                    .andExpect(jsonPath("$.path").value("/api/leave-requests/missing"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Staff leave requests are returned as a list")
        void findStaffLeaveRequestsReturnsList() throws Exception {
            when(contextFacade.findLeaveRequestsByStaffMemberId(STAFF_ID))
                    .thenReturn(List.of(requestDto(LeaveStatus.PENDING)));

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-requests", STAFF_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(REQUEST_ID));
        }

        @Test
        @DisplayName("A staff member with no requests receives an empty list")
        void emptyStaffLeaveRequestsReturnsEmptyArray() throws Exception {
            when(contextFacade.findLeaveRequestsByStaffMemberId(STAFF_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-requests", STAFF_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("Filtering by status returns the matching staff requests")
        void staffStatusFilterDelegatesToFilteredQuery() throws Exception {
            when(contextFacade.findLeaveRequestsByStaffMemberIdAndStatus(
                    STAFF_ID,
                    LeaveStatus.PENDING
            )).thenReturn(List.of(requestDto(LeaveStatus.PENDING)));

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .queryParam("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

        }

        @Test
        @DisplayName("An unknown status returns 400")
        void invalidStatusReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-requests", STAFF_ID)
                            .queryParam("status", "UNKNOWN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("A request parameter has an invalid value"))
                    .andExpect(jsonPath("$.path").value("/api/staff/staff-1/leave-requests"));

        }

        @Test
        @DisplayName("Filtering company requests by status returns the matching requests")
        void findLeaveRequestsByStatusDelegatesToFacade() throws Exception {
            when(contextFacade.findLeaveRequestsByStatus(LeaveStatus.PENDING))
                    .thenReturn(List.of(requestDto(LeaveStatus.PENDING)));

            mockMvc.perform(get("/api/leave-requests").queryParam("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

        }
    }

    @Nested
    @DisplayName("Viewing a manager's outstanding requests")
    class ManagerRequests {
        @Test
        @DisplayName("A manager receives their team's pending requests")
        void managerOutstandingRequestsReturnList() throws Exception {
            when(contextFacade.findOutstandingLeaveRequestsByManagerId("manager-1"))
                    .thenReturn(List.of(requestDto(LeaveStatus.PENDING)));

            mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", "manager-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].managerId").value("manager-1"))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

        }

        @Test
        @DisplayName("A manager with no pending requests receives an empty list")
        void emptyManagerOutstandingRequestsReturnEmptyArray() throws Exception {
            when(contextFacade.findOutstandingLeaveRequestsByManagerId("manager-1"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", "manager-1"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("A manager can filter pending requests by a date range")
        void managerReportingWindowDelegates() throws Exception {
            LocalDate start = LocalDate.of(2026, 9, 1);
            LocalDate end = LocalDate.of(2026, 9, 30);
            when(contextFacade.findOutstandingLeaveRequestsByManagerId("manager-1", start, end))
                    .thenReturn(List.of(requestDto(LeaveStatus.PENDING)));

            mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", "manager-1")
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(REQUEST_ID));

        }

        @Test
        @DisplayName("Providing only one reporting date returns 400")
        void partialManagerReportingWindowIsRejected() throws Exception {
            mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", "manager-1")
                            .queryParam("start", "2026-09-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveQueryHandler.REPORTING_DATES_REQUIRED
                    ));

        }

        @Test
        @DisplayName("A reporting end date before its start returns 400")
        void reversedManagerReportingWindowIsRejected() throws Exception {
            LocalDate start = LocalDate.of(2026, 9, 30);
            LocalDate end = LocalDate.of(2026, 9, 1);
            when(contextFacade.findOutstandingLeaveRequestsByManagerId("manager-1", start, end))
                    .thenThrow(new IllegalArgumentException(
                            LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START
                    ));

            mockMvc.perform(get("/api/managers/{managerId}/leave-requests/outstanding", "manager-1")
                            .queryParam("start", start.toString())
                            .queryParam("end", end.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveQueryHandler.REPORTING_END_NOT_BEFORE_START
                    ));
        }
    }

    @Nested
    @DisplayName("Approving, rejecting and cancelling leave")
    class ReviewingRequests {
        @Test
        @DisplayName("Approving leave returns 200 with the approved request")
        void approveReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID)).thenReturn(requestDto(LeaveStatus.APPROVED));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/approve", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(contextFacade).approveLeaveRequest(new ApproveLeaveRequestCommand(REQUEST_ID));
        }

        @Test
        @DisplayName("An invalid approval returns 409")
        void invalidApprovalReturnsConflict() throws Exception {
            doThrow(new InvalidLeaveRequestStateException(LeaveRequest.REQUEST_CANNOT_BE_APPROVED))
                    .when(contextFacade)
                    .approveLeaveRequest(new ApproveLeaveRequestCommand(REQUEST_ID));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/approve", REQUEST_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.code").value("INVALID_LEAVE_REQUEST_STATE"))
                    .andExpect(jsonPath("$.message").value(LeaveRequest.REQUEST_CANNOT_BE_APPROVED))
                    .andExpect(jsonPath("$.path").value("/api/leave-requests/request-1/approve"))
                    .andExpect(jsonPath("$.timestamp").exists());

        }

        @Test
        @DisplayName("Rejecting leave returns 200 with the rejected request")
        void rejectReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID)).thenReturn(requestDto(LeaveStatus.REJECTED));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/reject", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(contextFacade).rejectLeaveRequest(new RejectLeaveRequestCommand(REQUEST_ID));
        }

        @Test
        @DisplayName("Cancelling leave returns 200 with the cancelled request")
        void cancelReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID)).thenReturn(requestDto(LeaveStatus.CANCELLED));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/cancel", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(contextFacade).cancelLeaveRequest(new CancelLeaveRequestCommand(REQUEST_ID));
        }
    }

    @Nested
    @DisplayName("Reviewing leave with HR")
    class HrReview {
        @Test
        @DisplayName("Referring leave to HR returns 200 with the request awaiting HR review")
        void referToHrReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID))
                    .thenReturn(requestDto(LeaveStatus.PENDING_HR_APPROVAL));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/refer-to-hr", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING_HR_APPROVAL"));

            verify(contextFacade).referLeaveRequestForHrApproval(
                    new ReferLeaveRequestForHrApprovalCommand(REQUEST_ID)
            );
        }

        @Test
        @DisplayName("An invalid HR referral returns 409")
        void invalidHrReferralReturnsConflict() throws Exception {
            doThrow(new InvalidLeaveRequestStateException(
                    LeaveRequest.REQUEST_CANNOT_BE_REFERRED_FOR_HR_APPROVAL
            )).when(contextFacade).referLeaveRequestForHrApproval(
                    new ReferLeaveRequestForHrApprovalCommand(REQUEST_ID)
            );

            mockMvc.perform(patch("/api/leave-requests/{requestId}/refer-to-hr", REQUEST_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_LEAVE_REQUEST_STATE"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveRequest.REQUEST_CANNOT_BE_REFERRED_FOR_HR_APPROVAL
                    ));

        }

        @Test
        @DisplayName("HR approval returns 200 with the approved request")
        void hrApproveReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID))
                    .thenReturn(requestDto(LeaveStatus.APPROVED));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-approve", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(contextFacade).approveLeaveRequestByHr(
                    new ApproveHrLeaveRequestCommand(REQUEST_ID)
            );
        }

        @Test
        @DisplayName("An invalid HR approval returns 409")
        void invalidHrApprovalReturnsConflict() throws Exception {
            doThrow(new InvalidLeaveRequestStateException(
                    LeaveRequest.REQUEST_CANNOT_BE_APPROVED_BY_HR
            )).when(contextFacade).approveLeaveRequestByHr(
                    new ApproveHrLeaveRequestCommand(REQUEST_ID)
            );

            mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-approve", REQUEST_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_LEAVE_REQUEST_STATE"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveRequest.REQUEST_CANNOT_BE_APPROVED_BY_HR
                    ));

        }

        @Test
        @DisplayName("HR rejection returns 200 with the rejected request")
        void hrRejectReturnsUpdatedRequest() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID))
                    .thenReturn(requestDto(LeaveStatus.REJECTED));

            mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-reject", REQUEST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            verify(contextFacade).rejectLeaveRequestByHr(
                    new RejectHrLeaveRequestCommand(REQUEST_ID)
            );
        }

        @Test
        @DisplayName("An invalid HR rejection returns 409")
        void invalidHrRejectionReturnsConflict() throws Exception {
            doThrow(new InvalidLeaveRequestStateException(
                    LeaveRequest.REQUEST_CANNOT_BE_REJECTED_BY_HR
            )).when(contextFacade).rejectLeaveRequestByHr(
                    new RejectHrLeaveRequestCommand(REQUEST_ID)
            );

            mockMvc.perform(patch("/api/leave-requests/{requestId}/hr-reject", REQUEST_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_LEAVE_REQUEST_STATE"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveRequest.REQUEST_CANNOT_BE_REJECTED_BY_HR
                    ));

        }

        @Test
        @DisplayName("HR receives the requests awaiting its review")
        void hrOutstandingRequestsReturnList() throws Exception {
            when(contextFacade.findOutstandingHrLeaveRequests()).thenReturn(List.of(
                    requestDto(LeaveStatus.PENDING_HR_APPROVAL)
            ));

            mockMvc.perform(get("/api/leave-requests/hr/outstanding"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(REQUEST_ID))
                    .andExpect(jsonPath("$[0].status").value("PENDING_HR_APPROVAL"));

        }

        @Test
        @DisplayName("HR receives an empty list when no requests need review")
        void emptyHrOutstandingRequestsReturnEmptyArray() throws Exception {
            when(contextFacade.findOutstandingHrLeaveRequests()).thenReturn(List.of());

            mockMvc.perform(get("/api/leave-requests/hr/outstanding"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Nested
    @DisplayName("Unexpected errors")
    class UnexpectedErrors {
        @Test
        @DisplayName("An unexpected failure returns 500 without exposing private details")
        void unexpectedFailureReturnsSafeInternalServerError() throws Exception {
            when(contextFacade.findLeaveRequestById(REQUEST_ID))
                    .thenThrow(new RuntimeException("database password secret"));

            mockMvc.perform(get("/api/leave-requests/{requestId}", REQUEST_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                    .andExpect(jsonPath("$.path").value("/api/leave-requests/request-1"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(content().string(not(containsString("database password secret"))));
        }
    }

    private LeaveRequestDTO requestDto(LeaveStatus status) {
        return new LeaveRequestDTO(
                REQUEST_ID,
                STAFF_ID,
                "manager-1",
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 18),
                "Annual leave",
                LeaveType.ANNUAL,
                status
        );
    }
}
