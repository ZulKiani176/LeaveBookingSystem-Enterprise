package uk.ac.staffs.leavebooking.leave.infrastructure.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.PublicHolidayJpa;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PublicHolidayRepository extends CrudRepository<PublicHolidayJpa, LocalDate> {
    List<PublicHolidayJpa> findByDateBetweenOrderByDate(LocalDate startDate, LocalDate endDate);
}
