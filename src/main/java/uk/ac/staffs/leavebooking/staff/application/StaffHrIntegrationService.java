package uk.ac.staffs.leavebooking.staff.application;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.RemoteEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffMemberCreatedIntegrationEvent;
import uk.ac.staffs.leavebooking.common.events.integration.HrStaffPersonalDetailsUpdatedIntegrationEvent;
import uk.ac.staffs.leavebooking.staff.application.exceptions.InvalidHrEmploymentStatusException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.application.mappers.StaffMemberDomainToJpaMapper;
import uk.ac.staffs.leavebooking.staff.application.mappers.StaffMemberJpaToDomainMapper;
import uk.ac.staffs.leavebooking.staff.domain.EmploymentStatus;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffHrEventReceiptJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffHrEventReceiptRepository;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.Instant;
import java.util.Objects;

@Service
public class StaffHrIntegrationService {
    public static final String HR_CREATED_EVENT_NOT_NULL = "HR staff-created event cannot be null";
    public static final String HR_UPDATED_EVENT_NOT_NULL =
            "HR staff personal-details event cannot be null";

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffHrIntegrationService.class);

    private final StaffMemberRepository staffMemberRepository;
    private final StaffHrEventReceiptRepository receiptRepository;

    public StaffHrIntegrationService(
            StaffMemberRepository staffMemberRepository,
            StaffHrEventReceiptRepository receiptRepository
    ) {
        this.staffMemberRepository = staffMemberRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional
    public void process(HrStaffMemberCreatedIntegrationEvent event) {
        Objects.requireNonNull(event, HR_CREATED_EVENT_NOT_NULL);
        if (alreadyProcessed(event)) {
            return;
        }
        if (staffMemberRepository.existsById(event.staffMemberId())) {
            throw new StaffMemberAlreadyExistsException(event.staffMemberId());
        }

        EmploymentStatus status = parseEmploymentStatus(event.employmentStatus());
        StaffMember staffMember = StaffMember.createFromExternalHr(
                Identity.of(event.staffMemberId()),
                new FullName(event.firstName(), event.surname()),
                event.email(),
                event.hireDate(),
                event.department(),
                event.managerId(),
                event.jobRole(),
                event.roleStartDate(),
                event.jobLevel(),
                event.employmentType(),
                status
        );
        if (staffMemberRepository.existsByEmail(staffMember.email())) {
            throw new StaffEmailAlreadyExistsException(staffMember.email());
        }

        staffMemberRepository.save(StaffMemberDomainToJpaMapper.map(staffMember));
        storeReceipt(event);
        LOGGER.info(
                "Processed HR integration event type={} sourceEventId={} staffMemberId={}",
                event.getClass().getSimpleName(),
                event.id(),
                event.staffMemberId()
        );
    }

    @Transactional
    public void process(HrStaffPersonalDetailsUpdatedIntegrationEvent event) {
        Objects.requireNonNull(event, HR_UPDATED_EVENT_NOT_NULL);
        if (alreadyProcessed(event)) {
            return;
        }

        StaffMemberJpa storedStaffMember = staffMemberRepository.findById(event.staffMemberId())
                .orElseThrow(() -> new StaffMemberNotFoundException(event.staffMemberId()));
        staffMemberRepository.findByEmail(event.email())
                .filter(owner -> !owner.getId().equals(event.staffMemberId()))
                .ifPresent(owner -> {
                    throw new StaffEmailAlreadyExistsException(event.email());
                });

        StaffMember staffMember = StaffMemberJpaToDomainMapper.map(storedStaffMember);
        staffMember.changePersonalDetails(
                new FullName(event.firstName(), event.surname()),
                event.email()
        );
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.map(staffMember));
        storeReceipt(event);
        LOGGER.info(
                "Processed HR integration event type={} sourceEventId={} staffMemberId={}",
                event.getClass().getSimpleName(),
                event.id(),
                event.staffMemberId()
        );
    }

    private boolean alreadyProcessed(RemoteEvent event) {
        String eventType = event.getClass().getSimpleName();
        boolean processed = receiptRepository.existsBySourceEventIdAndEventType(
                event.id(),
                eventType
        );
        if (processed) {
            LOGGER.info(
                    "Ignoring duplicate HR integration event type={} sourceEventId={}",
                    eventType,
                    event.id()
            );
        }
        return processed;
    }

    private void storeReceipt(RemoteEvent event) {
        receiptRepository.save(new StaffHrEventReceiptJpa(
                event.id(),
                event.getClass().getSimpleName(),
                Instant.now()
        ));
    }

    private EmploymentStatus parseEmploymentStatus(String employmentStatus) {
        try {
            return EmploymentStatus.valueOf(employmentStatus);
        } catch (IllegalArgumentException exception) {
            throw new InvalidHrEmploymentStatusException(employmentStatus);
        }
    }
}
