package uk.ac.staffs.leavebooking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.ac.staffs.leavebooking.common.dto.ErrorResponse;
import uk.ac.staffs.leavebooking.identity.exceptions.AuthenticationFailedException;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.identity.exceptions.IdentityRegistrationException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveAllowanceException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestStateException;
import uk.ac.staffs.leavebooking.leave.domain.exceptions.InvalidLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.OverlappingLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.ConcurrentLeaveModificationException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.InvalidCarryOverException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.domain.exceptions.InvalidStaffMemberException;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    public static final String LEAVE_REQUEST_NOT_FOUND = "LEAVE_REQUEST_NOT_FOUND";
    public static final String LEAVE_ALLOWANCE_NOT_FOUND = "LEAVE_ALLOWANCE_NOT_FOUND";
    public static final String LEAVE_ALLOWANCE_ALREADY_EXISTS = "LEAVE_ALLOWANCE_ALREADY_EXISTS";
    public static final String INVALID_LEAVE_REQUEST_STATE = "INVALID_LEAVE_REQUEST_STATE";
    public static final String INVALID_LEAVE_ALLOWANCE_STATE = "INVALID_LEAVE_ALLOWANCE_STATE";
    public static final String STAFF_MEMBER_NOT_FOUND = "STAFF_MEMBER_NOT_FOUND";
    public static final String STAFF_EMAIL_ALREADY_EXISTS = "STAFF_EMAIL_ALREADY_EXISTS";
    public static final String INVALID_STAFF_MEMBER_STATE = "INVALID_STAFF_MEMBER_STATE";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String IDENTITY_EMAIL_ALREADY_EXISTS = "IDENTITY_EMAIL_ALREADY_EXISTS";
    public static final String IDENTITY_REGISTRATION_FAILED = "IDENTITY_REGISTRATION_FAILED";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";
    public static final String OVERLAPPING_LEAVE_REQUEST = "OVERLAPPING_LEAVE_REQUEST";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    public static final String PUBLIC_HOLIDAY_ALREADY_EXISTS = "PUBLIC_HOLIDAY_ALREADY_EXISTS";
    public static final String PUBLIC_HOLIDAY_NOT_FOUND = "PUBLIC_HOLIDAY_NOT_FOUND";
    public static final String INVALID_CARRY_OVER = "INVALID_CARRY_OVER";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        String principal = request.getUserPrincipal() == null
                ? "anonymous"
                : request.getUserPrincipal().getName();
        LOG.warn(
                "SECURITY outcome=403 method={} uri={} principal={} remoteIp={}",
                request.getMethod(),
                request.getRequestURI(),
                principal,
                request.getRemoteAddr()
        );
        return errorResponse(
                HttpStatus.FORBIDDEN,
                ACCESS_DENIED,
                "Access is denied",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Authentication failed for request {}", request.getRequestURI());
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                AUTHENTICATION_FAILED,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IdentityEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleIdentityEmailAlreadyExists(
            IdentityEmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                IDENTITY_EMAIL_ALREADY_EXISTS,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IdentityRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleIdentityRegistration(
            IdentityRegistrationException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                IDENTITY_REGISTRATION_FAILED,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(LeaveRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLeaveRequestNotFound(
            LeaveRequestNotFoundException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Leave request lookup failed: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.NOT_FOUND,
                LEAVE_REQUEST_NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(LeaveAllowanceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLeaveAllowanceNotFound(
            LeaveAllowanceNotFoundException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Leave allowance lookup failed: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.NOT_FOUND,
                LEAVE_ALLOWANCE_NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(LeaveAllowanceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleLeaveAllowanceAlreadyExists(
            LeaveAllowanceAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Duplicate leave allowance rejected: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.CONFLICT,
                LEAVE_ALLOWANCE_ALREADY_EXISTS,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidLeaveRequestStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveRequestState(
            InvalidLeaveRequestStateException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Invalid leave request transition: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.CONFLICT,
                INVALID_LEAVE_REQUEST_STATE,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidLeaveRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveRequest(
            InvalidLeaveRequestException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(OverlappingLeaveRequestException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingLeaveRequest(
            OverlappingLeaveRequestException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                OVERLAPPING_LEAVE_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({ConcurrentLeaveModificationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConcurrentModification(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                CONCURRENT_MODIFICATION,
                ConcurrentLeaveModificationException.MESSAGE,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidCarryOverException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCarryOver(
            InvalidCarryOverException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                INVALID_CARRY_OVER,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(PublicHolidayAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePublicHolidayAlreadyExists(
            PublicHolidayAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                PUBLIC_HOLIDAY_ALREADY_EXISTS,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(PublicHolidayNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePublicHolidayNotFound(
            PublicHolidayNotFoundException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                PUBLIC_HOLIDAY_NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidLeaveAllowanceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveAllowanceState(
            InvalidLeaveAllowanceException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Invalid leave allowance operation: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.CONFLICT,
                INVALID_LEAVE_ALLOWANCE_STATE,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(StaffMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStaffMemberNotFound(
            StaffMemberNotFoundException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Staff member lookup failed: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.NOT_FOUND,
                STAFF_MEMBER_NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(StaffEmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleStaffEmailAlreadyExists(
            StaffEmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Duplicate staff email rejected: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.CONFLICT,
                STAFF_EMAIL_ALREADY_EXISTS,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidStaffMemberException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStaffMemberState(
            InvalidStaffMemberException exception,
            HttpServletRequest request
    ) {
        LOG.warn("Invalid staff member operation: {}", exception.getMessage());
        return errorResponse(
                HttpStatus.CONFLICT,
                INVALID_STAFF_MEMBER_STATE,
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(Objects::nonNull)
                .sorted()
                .distinct()
                .collect(Collectors.joining("; "));

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                message.isBlank() ? "Validation failed" : message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                Objects.requireNonNullElse(exception.getMessage(), "The request is invalid"),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOG.error("Unexpected error processing {}", request.getRequestURI(), exception);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_MESSAGE,
                request.getRequestURI()
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .sorted((left, right) -> left.getField().compareTo(right.getField()))
                .map(this::fieldErrorMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        return frameworkErrorResponse(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                message.isBlank() ? "Validation failed" : message,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkErrorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                "A request parameter has an invalid value",
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return frameworkErrorResponse(
                HttpStatus.BAD_REQUEST,
                INVALID_REQUEST,
                "The request body is missing or malformed",
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        HttpStatus resolvedStatus = httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus;

        if (resolvedStatus.is5xxServerError()) {
            LOG.error("Unexpected framework error processing {}", requestPath(request), exception);
            return frameworkErrorResponse(
                    resolvedStatus,
                    INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR_MESSAGE,
                    request
            );
        }

        return frameworkErrorResponse(
                resolvedStatus,
                frameworkErrorCode(resolvedStatus),
                frameworkErrorMessage(resolvedStatus),
                request
        );
    }

    private String fieldErrorMessage(FieldError error) {
        return error.getField() + ": "
                + Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value");
    }

    private String frameworkErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED";
            case NOT_ACCEPTABLE -> "NOT_ACCEPTABLE";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            default -> INVALID_REQUEST;
        };
    }

    private String frameworkErrorMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "The requested resource was not found";
            case METHOD_NOT_ALLOWED -> "The HTTP method is not supported for this resource";
            case NOT_ACCEPTABLE -> "The requested response format is not supported";
            case UNSUPPORTED_MEDIA_TYPE -> "The request content type is not supported";
            default -> "The request is invalid";
        };
    }

    private ResponseEntity<Object> frameworkErrorResponse(
            HttpStatus status,
            String code,
            String message,
            WebRequest request
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                requestPath(request)
        ));
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                path
        ));
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }

        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
