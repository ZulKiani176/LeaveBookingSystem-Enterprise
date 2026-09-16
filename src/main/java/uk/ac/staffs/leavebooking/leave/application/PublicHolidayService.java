package uk.ac.staffs.leavebooking.leave.application;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.ac.staffs.leavebooking.leave.application.dto.PublicHolidayDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayNotFoundException;
import uk.ac.staffs.leavebooking.leave.domain.PublicHoliday;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.PublicHolidayJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class PublicHolidayService {
    private final PublicHolidayRepository repository;

    public PublicHolidayService(PublicHolidayRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PublicHolidayDTO create(LocalDate date, String name) {
        PublicHoliday holiday = new PublicHoliday(date, name);
        if (repository.existsById(holiday.date())) {
            throw new PublicHolidayAlreadyExistsException(holiday.date());
        }
        return map(repository.save(new PublicHolidayJpa(holiday.date(), holiday.name())));
    }

    public List<PublicHolidayDTO> findBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Public holiday start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Public holiday end date cannot be before start date");
        }
        return repository.findByDateBetweenOrderByDate(startDate, endDate).stream()
                .map(PublicHolidayService::map)
                .toList();
    }

    @Transactional
    public void delete(LocalDate date) {
        if (!repository.existsById(date)) {
            throw new PublicHolidayNotFoundException(date);
        }
        repository.deleteById(date);
    }

    private static PublicHolidayDTO map(PublicHolidayJpa holiday) {
        return new PublicHolidayDTO(holiday.getDate(), holiday.getName());
    }
}
