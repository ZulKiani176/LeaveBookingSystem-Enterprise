package uk.ac.staffs.leavebooking.notification.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestApprovedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestCancelledIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestRejectedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.LeaveRequestSubmittedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.SickLeaveRecordedIntegrationEvent;
import uk.ac.staffs.leavebooking.notification.application.dto.NotificationDTO;
import uk.ac.staffs.leavebooking.notification.application.mappers.NotificationJpaToDTOMapper;
import uk.ac.staffs.leavebooking.notification.infrastructure.entities.NotificationJpa;
import uk.ac.staffs.leavebooking.notification.infrastructure.repositories.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;

@Service
public class NotificationService {
    public static final String SOURCE_EVENT_ID_NOT_NULL = "Source event identity cannot be null";
    public static final String RECIPIENT_ID_NOT_EMPTY = "Notification recipient identity cannot be empty";
    public static final String LEAVE_REQUEST_ID_NOT_EMPTY = "Leave request identity cannot be empty";
    public static final String NOTIFICATION_TYPE_NOT_NULL = "Notification type cannot be null";
    public static final String MANAGER_PENDING_MESSAGE = "Leave request %s is awaiting your approval.";
    public static final String STAFF_APPROVED_MESSAGE = "Your leave request %s has been approved.";
    public static final String STAFF_REJECTED_MESSAGE = "Your leave request %s has been rejected.";
    public static final String STAFF_CANCELLED_MESSAGE = "Your leave request %s has been cancelled.";
    public static final String MANAGER_SICK_RECORDED_MESSAGE =
            "Sick leave %s has been recorded for a member of your team.";

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createManagerPendingNotification(LeaveRequestSubmittedIntegrationEvent event) {
        LeaveRequestSubmittedIntegrationEvent source = Objects.requireNonNull(event);
        create(
                source.id(),
                source.managerId(),
                source.leaveRequestId(),
                NotificationType.MANAGER_PENDING_LEAVE,
                MANAGER_PENDING_MESSAGE.formatted(source.leaveRequestId())
        );
    }

    @Transactional
    public void createManagerSickLeaveRecordedNotification(SickLeaveRecordedIntegrationEvent event) {
        SickLeaveRecordedIntegrationEvent source = Objects.requireNonNull(event);
        create(
                source.id(),
                source.managerId(),
                source.leaveRequestId(),
                NotificationType.MANAGER_SICK_LEAVE_RECORDED,
                MANAGER_SICK_RECORDED_MESSAGE.formatted(source.leaveRequestId())
        );
    }

    @Transactional
    public void createStaffApprovedNotification(LeaveRequestApprovedIntegrationEvent event) {
        LeaveRequestApprovedIntegrationEvent source = Objects.requireNonNull(event);
        create(
                source.id(),
                source.staffMemberId(),
                source.leaveRequestId(),
                NotificationType.STAFF_LEAVE_APPROVED,
                STAFF_APPROVED_MESSAGE.formatted(source.leaveRequestId())
        );
    }

    @Transactional
    public void createStaffRejectedNotification(LeaveRequestRejectedIntegrationEvent event) {
        LeaveRequestRejectedIntegrationEvent source = Objects.requireNonNull(event);
        create(
                source.id(),
                source.staffMemberId(),
                source.leaveRequestId(),
                NotificationType.STAFF_LEAVE_REJECTED,
                STAFF_REJECTED_MESSAGE.formatted(source.leaveRequestId())
        );
    }

    @Transactional
    public void createStaffCancelledNotification(LeaveRequestCancelledIntegrationEvent event) {
        LeaveRequestCancelledIntegrationEvent source = Objects.requireNonNull(event);
        create(
                source.id(),
                source.staffMemberId(),
                source.leaveRequestId(),
                NotificationType.STAFF_LEAVE_CANCELLED,
                STAFF_CANCELLED_MESSAGE.formatted(source.leaveRequestId())
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> findByRecipientId(String recipientId) {
        String validatedRecipientId = argumentNotEmpty(recipientId, RECIPIENT_ID_NOT_EMPTY);
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(validatedRecipientId)
                .stream()
                .map(NotificationJpaToDTOMapper::map)
                .toList();
    }

    private void create(
            Long sourceEventId,
            String recipientId,
            String leaveRequestId,
            NotificationType notificationType,
            String message
    ) {
        Long validatedSourceEventId = Objects.requireNonNull(
                sourceEventId,
                SOURCE_EVENT_ID_NOT_NULL
        );
        String validatedRecipientId = argumentNotEmpty(recipientId, RECIPIENT_ID_NOT_EMPTY);
        String validatedLeaveRequestId = argumentNotEmpty(
                leaveRequestId,
                LEAVE_REQUEST_ID_NOT_EMPTY
        );
        NotificationType validatedType = Objects.requireNonNull(
                notificationType,
                NOTIFICATION_TYPE_NOT_NULL
        );

        if (notificationRepository.existsBySourceEventId(validatedSourceEventId)) {
            return;
        }

        notificationRepository.save(new NotificationJpa(
                Identity.generateId().id(),
                validatedSourceEventId,
                validatedRecipientId,
                validatedLeaveRequestId,
                validatedType,
                message,
                Instant.now()
        ));
    }
}
