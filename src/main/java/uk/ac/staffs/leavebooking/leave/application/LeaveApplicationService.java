package uk.ac.staffs.leavebooking.leave.application;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.common.events.DomainEventManager;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveAllowanceNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.LeaveRequestNotFoundException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.OverlappingLeaveRequestException;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveAllowanceJpaToDomainMapper;
import uk.ac.staffs.leavebooking.leave.application.mappers.LeaveRequestDomainToJpaMapper;
import uk.ac.staffs.leavebooking.leave.domain.LeaveAllowance;
import uk.ac.staffs.leavebooking.leave.domain.BusinessYear;
import uk.ac.staffs.leavebooking.leave.domain.LeavePeriod;
import uk.ac.staffs.leavebooking.leave.domain.LeaveRequest;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDays;
import uk.ac.staffs.leavebooking.leave.domain.LeaveDayPortion;
import uk.ac.staffs.leavebooking.leave.domain.LeaveOverlapPolicy;
import uk.ac.staffs.leavebooking.leave.domain.LeaveStatus;
import uk.ac.staffs.leavebooking.leave.domain.WorkingDayCalculator;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveRequestJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.LeaveRequestEventStore;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceCarryOverRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.LeaveAllowanceCarryOverJpa;
import uk.ac.staffs.leavebooking.leave.application.exceptions.InvalidCarryOverException;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveAllowanceRepository;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.LeaveRequestRepository;
import uk.ac.staffs.leavebooking.leave.ui.commands.AmendLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ApproveLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CancelLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CreateLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.ReferLeaveRequestForHrApprovalCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectHrLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RejectLeaveRequestCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.RequestLeaveCommand;
import uk.ac.staffs.leavebooking.leave.ui.commands.CarryOverLeaveAllowanceCommand;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;

import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.staffs.leavebooking.common.DomainAssertions.argumentNotEmpty;

@Service
public class LeaveApplicationService {
    public static final String SOURCE_CONTEXT = "LeaveManagement";
    public static final String REQUEST_LEAVE_COMMAND_NOT_NULL = "Request leave command cannot be null";
    public static final String APPROVE_COMMAND_NOT_NULL = "Approve leave request command cannot be null";
    public static final String REJECT_COMMAND_NOT_NULL = "Reject leave request command cannot be null";
    public static final String CANCEL_COMMAND_NOT_NULL = "Cancel leave request command cannot be null";
    public static final String REFER_FOR_HR_COMMAND_NOT_NULL =
            "Refer leave request for HR approval command cannot be null";
    public static final String HR_APPROVE_COMMAND_NOT_NULL =
            "HR approve leave request command cannot be null";
    public static final String HR_REJECT_COMMAND_NOT_NULL =
            "HR reject leave request command cannot be null";
    public static final String AMEND_ALLOWANCE_COMMAND_NOT_NULL = "Amend leave allowance command cannot be null";
    public static final String CREATE_ALLOWANCE_COMMAND_NOT_NULL = "Create leave allowance command cannot be null";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveAllowanceRepository leaveAllowanceRepository;
    private final uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade;
    private final DomainEventManager domainEventManager;
    private final PublicHolidayRepository publicHolidayRepository;
    private final WorkingDayCalculator workingDayCalculator = new WorkingDayCalculator();
    private final LeaveRequestEventStore leaveRequestEventStore;
    private final LeaveAllowanceCarryOverRepository carryOverRepository;
    private final uk.ac.staffs.leavebooking.leave.infrastructure.LeaveSubmissionLock submissionLock;

    public LeaveApplicationService(
            LeaveRequestRepository leaveRequestRepository,
            LeaveAllowanceRepository leaveAllowanceRepository,
            uk.ac.staffs.leavebooking.staff.ContextFacade staffContextFacade,
            DomainEventManager domainEventManager,
            PublicHolidayRepository publicHolidayRepository,
            LeaveRequestEventStore leaveRequestEventStore,
            LeaveAllowanceCarryOverRepository carryOverRepository,
            uk.ac.staffs.leavebooking.leave.infrastructure.LeaveSubmissionLock submissionLock
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveAllowanceRepository = leaveAllowanceRepository;
        this.staffContextFacade = staffContextFacade;
        this.domainEventManager = domainEventManager;
        this.publicHolidayRepository = publicHolidayRepository;
        this.leaveRequestEventStore = leaveRequestEventStore;
        this.carryOverRepository = carryOverRepository;
        this.submissionLock = submissionLock;
    }

    @Transactional
    public String requestLeave(RequestLeaveCommand command) {
        Objects.requireNonNull(command, REQUEST_LEAVE_COMMAND_NOT_NULL);

        String staffMemberId = argumentNotEmpty(
                command.staffMemberId(),
                LeaveRequest.STAFF_MEMBER_ID_NOT_EMPTY
        );
        StaffMemberDTO staffMember = staffContextFacade.findStaffMemberById(staffMemberId);
        LeavePeriod leavePeriod = new LeavePeriod(command.startDate(), command.endDate());
        LeaveDayPortion dayPortion = command.dayPortion() == null
                ? LeaveDayPortion.FULL_DAY
                : command.dayPortion();
        Set<java.time.LocalDate> publicHolidays = publicHolidayRepository
                .findByDateBetweenOrderByDate(leavePeriod.startDate(), leavePeriod.endDate())
                .stream()
                .map(holiday -> holiday.getDate())
                .collect(Collectors.toUnmodifiableSet());
        LeaveDays chargedLeaveDays = workingDayCalculator.calculate(
                command.leaveType(), leavePeriod, dayPortion, publicHolidays
        );
        submissionLock.acquire(staffMemberId);
        rejectOverlap(staffMemberId, leavePeriod, dayPortion);
        Identity<LeaveRequest> leaveRequestId = Identity.generateId();
        LeaveRequest leaveRequest = LeaveRequest.create(
                leaveRequestId,
                staffMemberId,
                staffMember.managerId(),
                leavePeriod,
                command.reason(),
                command.leaveType(),
                dayPortion,
                chargedLeaveDays
        );

        saveAndDispatch(leaveRequest, null);
        return leaveRequestId.id();
    }

    @Transactional
    public String createLeaveAllowance(CreateLeaveAllowanceCommand command) {
        Objects.requireNonNull(command, CREATE_ALLOWANCE_COMMAND_NOT_NULL);

        String staffMemberId = argumentNotEmpty(
                command.staffMemberId(),
                LeaveAllowance.STAFF_MEMBER_ID_NOT_EMPTY
        );
        StaffMemberDTO staffMember = staffContextFacade.findStaffMemberById(staffMemberId);
        FullName fullName = new FullName(staffMember.firstName(), staffMember.surname());
        BusinessYear businessYear = new BusinessYear(
                command.businessYearStart(),
                command.businessYearEnd()
        );

        if (leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        staffMemberId,
                        command.businessYearStart(),
                        command.businessYearEnd()
                )
                .isPresent()) {
            throw new LeaveAllowanceAlreadyExistsException(
                    staffMemberId,
                    command.businessYearStart(),
                    command.businessYearEnd()
            );
        }

        Identity<LeaveAllowance> leaveAllowanceId = Identity.generateId();
        LeaveAllowance leaveAllowance = new LeaveAllowance(
                leaveAllowanceId,
                staffMemberId,
                fullName,
                staffMember.managerId(),
                businessYear,
                command.baseEntitlement()
        );

        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.map(leaveAllowance));
        return leaveAllowanceId.id();
    }

    @Transactional
    public void approveLeaveRequest(ApproveLeaveRequestCommand command) {
        Objects.requireNonNull(command, APPROVE_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().approveWithComment(command.comment());
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void rejectLeaveRequest(RejectLeaveRequestCommand command) {
        Objects.requireNonNull(command, REJECT_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().rejectWithComment(command.comment());
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void cancelLeaveRequest(CancelLeaveRequestCommand command) {
        Objects.requireNonNull(command, CANCEL_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().cancel();
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void referLeaveRequestForHrApproval(ReferLeaveRequestForHrApprovalCommand command) {
        Objects.requireNonNull(command, REFER_FOR_HR_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().referForHrApproval();
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void approveLeaveRequestByHr(ApproveHrLeaveRequestCommand command) {
        Objects.requireNonNull(command, HR_APPROVE_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().approveByHrWithComment(command.comment());
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void rejectLeaveRequestByHr(RejectHrLeaveRequestCommand command) {
        Objects.requireNonNull(command, HR_REJECT_COMMAND_NOT_NULL);

        LoadedLeaveRequest loaded = findLeaveRequest(command.leaveRequestId());
        loaded.request().rejectByHrWithComment(command.comment());
        saveAndDispatch(loaded.request(), loaded.version());
    }

    @Transactional
    public void amendLeaveAllowance(AmendLeaveAllowanceCommand command) {
        Objects.requireNonNull(command, AMEND_ALLOWANCE_COMMAND_NOT_NULL);

        var storedAllowance = leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        command.staffMemberId(),
                        command.businessYearStart(),
                        command.businessYearEnd()
                )
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(
                        command.staffMemberId(),
                        command.businessYearStart(),
                        command.businessYearEnd()
                ));

        LeaveAllowance leaveAllowance = LeaveAllowanceJpaToDomainMapper.map(storedAllowance);
        leaveAllowance.amendBaseEntitlement(new LeaveDays(command.newEntitlement()));
        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.map(
                leaveAllowance,
                storedAllowance.getVersion()
        ));
    }

    @Transactional
    public void carryOverLeaveAllowance(CarryOverLeaveAllowanceCommand command) {
        Objects.requireNonNull(command, "Carry-over command cannot be null");
        BusinessYear sourceYear = new BusinessYear(
                command.sourceYearStart(), command.sourceYearEnd()
        );
        BusinessYear targetYear = new BusinessYear(
                command.targetYearStart(), command.targetYearEnd()
        );
        if (!sourceYear.endDate().plusDays(1).equals(targetYear.startDate())) {
            throw new InvalidCarryOverException(
                    "Source and target business years must be consecutive"
            );
        }
        var sourceJpa = leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        command.staffMemberId(), sourceYear.startDate(), sourceYear.endDate()
                )
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(
                        command.staffMemberId(), sourceYear.startDate(), sourceYear.endDate()
                ));
        var targetJpa = leaveAllowanceRepository
                .findByStaffMemberIdAndBusinessYearStartAndBusinessYearEnd(
                        command.staffMemberId(), targetYear.startDate(), targetYear.endDate()
                )
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(
                        command.staffMemberId(), targetYear.startDate(), targetYear.endDate()
                ));
        if (carryOverRepository.existsBySourceAllowanceIdAndTargetAllowanceId(
                sourceJpa.getId(), targetJpa.getId()
        )) {
            return;
        }

        LeaveAllowance source = LeaveAllowanceJpaToDomainMapper.map(sourceJpa);
        LeaveAllowance target = LeaveAllowanceJpaToDomainMapper.map(targetJpa);
        LeaveDays carriedDays = source.remainingLeaveDays().min(LeaveAllowance.MAX_CARRY_OVER);
        target.setCarryOver(carriedDays);
        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.map(
                target, targetJpa.getVersion()
        ));
        carryOverRepository.save(new LeaveAllowanceCarryOverJpa(
                command.staffMemberId(),
                sourceJpa.getId(),
                targetJpa.getId(),
                carriedDays.value(),
                java.time.LocalDate.now()
        ));
    }

    private LoadedLeaveRequest findLeaveRequest(String leaveRequestId) {
        LeaveRequest request = leaveRequestEventStore.load(leaveRequestId);
        Long projectionVersion = leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequestJpa::getVersion)
                .orElseThrow(() -> new LeaveRequestNotFoundException(leaveRequestId));
        return new LoadedLeaveRequest(request, projectionVersion);
    }

    private void saveAndDispatch(LeaveRequest leaveRequest, Long version) {
        leaveRequestEventStore.append(leaveRequest);
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.map(leaveRequest, version));
        if (leaveRequest.domainEventsExist()) {
            domainEventManager.manageDomainEvents(
                    SOURCE_CONTEXT,
                    leaveRequest.listOfDomainEvents()
            );
            leaveRequest.clearDomainEvents();
        }
    }

    private void rejectOverlap(
            String staffMemberId,
            LeavePeriod requestedPeriod,
            LeaveDayPortion requestedPortion
    ) {
        List<LeaveStatus> blockingStatuses = List.of(
                LeaveStatus.PENDING,
                LeaveStatus.PENDING_HR_APPROVAL,
                LeaveStatus.APPROVED,
                LeaveStatus.RECORDED
        );
        boolean conflict = leaveRequestRepository
                .findByStaffMemberIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        staffMemberId,
                        blockingStatuses,
                        requestedPeriod.endDate(),
                        requestedPeriod.startDate()
                )
                .stream()
                .anyMatch(existing -> LeaveOverlapPolicy.conflicts(
                        requestedPeriod,
                        requestedPortion,
                        new LeavePeriod(existing.getStartDate(), existing.getEndDate()),
                        existing.getDayPortion()
                ));
        if (conflict) {
            throw new OverlappingLeaveRequestException();
        }
    }

    private record LoadedLeaveRequest(LeaveRequest request, Long version) {
    }
}
