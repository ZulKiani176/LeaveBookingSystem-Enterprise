package uk.ac.staffs.leavebooking.reporting.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.reporting.infrastructure.entities.LeaveReportingProjectionJpa;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveReportingProjectionRepository
        extends CrudRepository<LeaveReportingProjectionJpa, String> {
    List<LeaveReportingProjectionJpa>
    findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDate(
            LocalDate reportingEnd,
            LocalDate reportingStart
    );
}
