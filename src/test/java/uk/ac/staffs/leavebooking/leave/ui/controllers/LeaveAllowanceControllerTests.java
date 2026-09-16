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
import uk.ac.staffs.leavebooking.leave.application.dto.LeaveAllowanceDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(LeaveAllowanceController.class)
@Import({GlobalExceptionHandler.class, ControllerSecurityTestConfiguration.class})
@WithMockFirebaseUser
@DisplayName("Leave allowance API")
class LeaveAllowanceControllerTests {
    private static final String STAFF_ID = "staff-1";
    private static final LocalDate YEAR_START = LocalDate.of(2026, 4, 1);
    private static final LocalDate YEAR_END = LocalDate.of(2027, 3, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContextFacade contextFacade;

    @Nested
    @DisplayName("Creating an allowance")
    class CreatingAllowances {
        @Test
        @DisplayName("Creating an allowance returns 201 with its details and a link")
        void validAllowanceCreationReturnsCreatedResource() throws Exception {
            CreateLeaveAllowanceCommand command = new CreateLeaveAllowanceCommand(
                    STAFF_ID,
                    YEAR_START,
                    YEAR_END,
                    25
            );
            when(contextFacade.createLeaveAllowance(command)).thenReturn("allowance-1");
            when(contextFacade.findLeaveAllowance(STAFF_ID, YEAR_START, YEAR_END))
                    .thenReturn(allowanceDto(25, 25));

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateRequestJson()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(
                            "Location",
                            "/api/staff/staff-1/leave-allowances/year"
                                    + "?start=2026-04-01&end=2027-03-31"
                    ))
                    .andExpect(jsonPath("$.id").value("allowance-1"))
                    .andExpect(jsonPath("$.firstName").value("Ada"))
                    .andExpect(jsonPath("$.surname").value("Lovelace"))
                    .andExpect(jsonPath("$.managerId").value("manager-1"))
                    .andExpect(jsonPath("$.annualEntitlement").value(25))
                    .andExpect(jsonPath("$.remainingDays").value(25));

            verify(contextFacade).createLeaveAllowance(command);
        }

        @Test
        @DisplayName("Missing allowance details return 400")
        void missingAllowanceCreationFieldsAreRejected() throws Exception {
            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A negative starting entitlement returns 400")
        void negativeInitialEntitlementIsRejected() throws Exception {
            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateRequestJson().replace("25", "-1")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value(
                            "annualEntitlement: Annual entitlement cannot be negative"
                    ));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A second allowance for the same staff member and year returns 409")
        void duplicateAllowanceReturnsConflict() throws Exception {
            CreateLeaveAllowanceCommand command = new CreateLeaveAllowanceCommand(
                    STAFF_ID,
                    YEAR_START,
                    YEAR_END,
                    25
            );
            doThrow(new LeaveAllowanceAlreadyExistsException(STAFF_ID, YEAR_START, YEAR_END))
                    .when(contextFacade).createLeaveAllowance(command);

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateRequestJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("LEAVE_ALLOWANCE_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.path").value(
                            "/api/staff/staff-1/leave-allowances"
                    ));
        }

        @Test
        @DisplayName("Creating an allowance for missing staff returns 404")
        void missingStaffReturnsNotFound() throws Exception {
            CreateLeaveAllowanceCommand command = new CreateLeaveAllowanceCommand(
                    STAFF_ID,
                    YEAR_START,
                    YEAR_END,
                    25
            );
            doThrow(new StaffMemberNotFoundException(STAFF_ID))
                    .when(contextFacade).createLeaveAllowance(command);

            mockMvc.perform(post("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCreateRequestJson()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STAFF_MEMBER_NOT_FOUND"));

        }
    }

    @Nested
    @DisplayName("Finding yearly allowances")
    class FindingAllowances {
        @Test
        @DisplayName("A staff member's yearly allowances are returned as a list")
        void findStaffAllowancesReturnsList() throws Exception {
            when(contextFacade.findLeaveAllowancesByStaffMemberId(STAFF_ID))
                    .thenReturn(List.of(allowanceDto(25, 20)));

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("allowance-1"))
                    .andExpect(jsonPath("$[0].usedDays").value(5));
        }

        @Test
        @DisplayName("A staff member with no allowances receives an empty list")
        void emptyStaffAllowancesReturnsEmptyArray() throws Exception {
            when(contextFacade.findLeaveAllowancesByStaffMemberId(STAFF_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances", STAFF_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        @DisplayName("A yearly allowance includes entitlement, remaining days and used days")
        void findExactBusinessYearAllowanceReturnsDto() throws Exception {
            when(contextFacade.findLeaveAllowance(STAFF_ID, YEAR_START, YEAR_END))
                    .thenReturn(allowanceDto(25, 20));

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.staffMemberId").value(STAFF_ID))
                    .andExpect(jsonPath("$.firstName").value("Ada"))
                    .andExpect(jsonPath("$.surname").value("Lovelace"))
                    .andExpect(jsonPath("$.managerId").value("manager-1"))
                    .andExpect(jsonPath("$.businessYearStart").value("2026-04-01"))
                    .andExpect(jsonPath("$.businessYearEnd").value("2027-03-31"))
                    .andExpect(jsonPath("$.annualEntitlement").value(25))
                    .andExpect(jsonPath("$.remainingDays").value(20))
                    .andExpect(jsonPath("$.usedDays").value(5));
        }

        @Test
        @DisplayName("A missing yearly allowance returns 404")
        void missingExactBusinessYearAllowanceReturnsNotFound() throws Exception {
            when(contextFacade.findLeaveAllowance(STAFF_ID, YEAR_START, YEAR_END))
                    .thenThrow(new LeaveAllowanceNotFoundException(STAFF_ID, YEAR_START, YEAR_END));

            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("LEAVE_ALLOWANCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(
                            "Leave allowance not found for staff member staff-1 "
                                    + "and business year 2026-04-01 to 2027-03-31"
                    ))
                    .andExpect(jsonPath("$.path").value(
                            "/api/staff/staff-1/leave-allowances/year"
                    ));
        }
    }

    @Nested
    @DisplayName("Changing an allowance")
    class AmendingAllowances {
        @Test
        @DisplayName("Changing entitlement returns the updated allowance")
        void amendEntitlementReturnsUpdatedAllowance() throws Exception {
            when(contextFacade.findLeaveAllowance(STAFF_ID, YEAR_START, YEAR_END))
                    .thenReturn(allowanceDto(30, 25));

            mockMvc.perform(patch("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newEntitlement\":30}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.annualEntitlement").value(30))
                    .andExpect(jsonPath("$.remainingDays").value(25))
                    .andExpect(jsonPath("$.usedDays").value(5));

            verify(contextFacade).amendLeaveAllowance(new AmendLeaveAllowanceCommand(
                    STAFF_ID,
                    YEAR_START,
                    YEAR_END,
                    30
            ));
        }

        @Test
        @DisplayName("A missing new entitlement returns 400")
        void missingNewEntitlementIsRejected() throws Exception {
            mockMvc.perform(patch("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value(
                            "newEntitlement: New entitlement is required"
                    ));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("A negative new entitlement returns 400")
        void negativeNewEntitlementIsRejected() throws Exception {
            mockMvc.perform(patch("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newEntitlement\":-1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value(
                            "newEntitlement: New entitlement cannot be negative"
                    ));

            verifyNoInteractions(contextFacade);
        }

        @Test
        @DisplayName("An entitlement that would invalidate used leave returns 409")
        void domainInvalidEntitlementAmendmentReturnsConflict() throws Exception {
            AmendLeaveAllowanceCommand command = new AmendLeaveAllowanceCommand(
                    STAFF_ID,
                    YEAR_START,
                    YEAR_END,
                    4
            );
            doThrow(new InvalidLeaveAllowanceException(
                    LeaveAllowance.ENTITLEMENT_BELOW_USED_DAYS
            )).when(contextFacade).amendLeaveAllowance(command);

            mockMvc.perform(patch("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", YEAR_START.toString())
                            .queryParam("end", YEAR_END.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newEntitlement\":4}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.code").value("INVALID_LEAVE_ALLOWANCE_STATE"))
                    .andExpect(jsonPath("$.message").value(
                            LeaveAllowance.ENTITLEMENT_BELOW_USED_DAYS
                    ))
                    .andExpect(jsonPath("$.path").value(
                            "/api/staff/staff-1/leave-allowances/year"
                    ));
        }
    }

    @Nested
    @DisplayName("Checking business-year dates")
    class ReportingDates {
        @Test
        @DisplayName("An invalid business-year date returns 400")
        void malformedBusinessYearDateReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("start", "not-a-date")
                            .queryParam("end", YEAR_END.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value(
                            "A request parameter has an invalid value"
                    ));
        }

        @Test
        @DisplayName("A missing business-year date returns 400")
        void missingBusinessYearParameterReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/staff/{staffMemberId}/leave-allowances/year", STAFF_ID)
                            .queryParam("end", YEAR_END.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("The request is invalid"));
        }
    }

    private LeaveAllowanceDTO allowanceDto(int entitlement, int remainingDays) {
        return new LeaveAllowanceDTO(
                "allowance-1",
                STAFF_ID,
                "Ada",
                "Lovelace",
                "manager-1",
                YEAR_START,
                YEAR_END,
                entitlement,
                remainingDays,
                entitlement - remainingDays
        );
    }

    private String validCreateRequestJson() {
        return """
                {
                  "businessYearStart": "2026-04-01",
                  "businessYearEnd": "2027-03-31",
                  "annualEntitlement": 25
                }
                """;
    }
}
