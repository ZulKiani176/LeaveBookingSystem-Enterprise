package uk.ac.staffs.leavebooking.staff.application;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.common.FullName;
import uk.ac.staffs.leavebooking.common.Identity;
import uk.ac.staffs.leavebooking.staff.application.dto.StaffMemberDTO;
import uk.ac.staffs.leavebooking.staff.application.dto.CreateStaffMemberDetails;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffEmailAlreadyExistsException;
import uk.ac.staffs.leavebooking.staff.application.exceptions.StaffMemberNotFoundException;
import uk.ac.staffs.leavebooking.staff.application.mappers.StaffMemberDomainToJpaMapper;
import uk.ac.staffs.leavebooking.staff.application.mappers.StaffMemberJpaToDTOMapper;
import uk.ac.staffs.leavebooking.staff.application.mappers.StaffMemberJpaToDomainMapper;
import uk.ac.staffs.leavebooking.staff.domain.StaffMember;
import uk.ac.staffs.leavebooking.staff.infrastructure.repositories.StaffMemberRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class StaffApplicationService {
    public static final String CREATE_STAFF_DETAILS_NOT_NULL =
            "Create staff member details cannot be null";

    private final StaffMemberRepository staffMemberRepository;

    public StaffApplicationService(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    @Transactional
    public String createStaffMember(CreateStaffMemberDetails details) {
        Objects.requireNonNull(details, CREATE_STAFF_DETAILS_NOT_NULL);

        Identity<StaffMember> staffMemberId = Identity.generateId();
        StaffMember staffMember = new StaffMember(
                staffMemberId,
                new FullName(details.firstName(), details.surname()),
                details.email(),
                details.hireDate(),
                details.department(),
                details.managerId(),
                details.jobRole(),
                details.roleStartDate(),
                details.jobLevel(),
                details.employmentType()
        );

        if (staffMemberRepository.existsByEmail(staffMember.email())) {
            throw new StaffEmailAlreadyExistsException(staffMember.email());
        }

        staffMemberRepository.save(StaffMemberDomainToJpaMapper.map(staffMember));
        return staffMemberId.id();
    }

    @Transactional
    public void changeDepartment(String staffMemberId, String newDepartment) {
        StaffMember staffMember = findStaffMemberDomain(staffMemberId);
        staffMember.changeDepartment(newDepartment);
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.map(staffMember));
    }

    @Transactional
    public void changeJobRole(
            String staffMemberId,
            String newJobRole,
            LocalDate newRoleStartDate
    ) {
        StaffMember staffMember = findStaffMemberDomain(staffMemberId);
        staffMember.changeJobRole(newJobRole, newRoleStartDate);
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.map(staffMember));
    }

    public StaffMemberDTO findStaffMemberById(String staffMemberId) {
        return staffMemberRepository.findById(staffMemberId)
                .map(StaffMemberJpaToDTOMapper::map)
                .orElseThrow(() -> new StaffMemberNotFoundException(staffMemberId));
    }

    public List<StaffMemberDTO> findStaffByManagerId(String managerId) {
        return staffMemberRepository.findByManagerId(managerId).stream()
                .map(StaffMemberJpaToDTOMapper::map)
                .toList();
    }

    public List<StaffMemberDTO> findStaffByDepartment(String department) {
        return staffMemberRepository.findByDepartment(department).stream()
                .map(StaffMemberJpaToDTOMapper::map)
                .toList();
    }

    private StaffMember findStaffMemberDomain(String staffMemberId) {
        return staffMemberRepository.findById(staffMemberId)
                .map(StaffMemberJpaToDomainMapper::map)
                .orElseThrow(() -> new StaffMemberNotFoundException(staffMemberId));
    }
}
