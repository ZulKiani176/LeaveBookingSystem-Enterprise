package uk.ac.staffs.leavebooking.staff.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.staff.infrastructure.entities.StaffMemberJpa;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMemberRepository extends CrudRepository<StaffMemberJpa, String> {
    Optional<StaffMemberJpa> findByEmail(String email);

    List<StaffMemberJpa> findByManagerId(String managerId);

    List<StaffMemberJpa> findByDepartment(String department);

    boolean existsByEmail(String email);
}
