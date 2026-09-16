package uk.ac.staffs.leavebooking.leave.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.staffs.leavebooking.leave.application.dto.PublicHolidayDTO;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayAlreadyExistsException;
import uk.ac.staffs.leavebooking.leave.application.exceptions.PublicHolidayNotFoundException;
import uk.ac.staffs.leavebooking.leave.infrastructure.entities.PublicHolidayJpa;
import uk.ac.staffs.leavebooking.leave.infrastructure.repositories.PublicHolidayRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Public Holiday Service")
class PublicHolidayServiceTests {
    private static final LocalDate DATE = LocalDate.parse("2026-08-31");

    @Mock private PublicHolidayRepository repository;

    @Test
    @DisplayName("Creating a public holiday trims and persists its name")
    void createPersistsValidatedHoliday() {
        PublicHolidayService service = service();
        when(repository.existsById(DATE)).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(PublicHolidayJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublicHolidayDTO result = service.create(DATE, "  Summer bank holiday  ");

        ArgumentCaptor<PublicHolidayJpa> saved = ArgumentCaptor.forClass(PublicHolidayJpa.class);
        verify(repository).save(saved.capture());
        assertEquals("Summer bank holiday", saved.getValue().getName());
        assertEquals(new PublicHolidayDTO(DATE, "Summer bank holiday"), result);
    }

    @Test
    @DisplayName("A duplicate holiday date is rejected before save")
    void duplicateDateIsRejected() {
        PublicHolidayService service = service();
        when(repository.existsById(DATE)).thenReturn(true);

        assertThrows(
                PublicHolidayAlreadyExistsException.class,
                () -> service.create(DATE, "Summer bank holiday")
        );

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Range query returns holidays in repository order")
    void rangeQueryMapsResults() {
        PublicHolidayService service = service();
        LocalDate end = DATE.plusDays(7);
        when(repository.findByDateBetweenOrderByDate(DATE, end)).thenReturn(List.of(
                new PublicHolidayJpa(DATE, "First"),
                new PublicHolidayJpa(end, "Second")
        ));

        List<PublicHolidayDTO> result = service.findBetween(DATE, end);

        assertEquals(List.of(
                new PublicHolidayDTO(DATE, "First"),
                new PublicHolidayDTO(end, "Second")
        ), result);
    }

    @Test
    @DisplayName("A reversed range is rejected")
    void reversedRangeIsRejected() {
        PublicHolidayService service = service();

        assertThrows(IllegalArgumentException.class, () ->
                service.findBetween(DATE, DATE.minusDays(1))
        );

    }

    @Test
    @DisplayName("Deleting an existing public holiday uses its date identity")
    void deleteExistingHoliday() {
        PublicHolidayService service = service();
        when(repository.existsById(DATE)).thenReturn(true);

        service.delete(DATE);

        verify(repository).deleteById(DATE);
    }

    @Test
    @DisplayName("Deleting a missing public holiday returns a domain-facing not-found error")
    void deleteMissingHolidayFails() {
        PublicHolidayService service = service();
        when(repository.existsById(DATE)).thenReturn(false);

        assertThrows(PublicHolidayNotFoundException.class, () -> service.delete(DATE));

        verify(repository, never()).deleteById(DATE);
    }

    private PublicHolidayService service() {
        return new PublicHolidayService(repository);
    }
}
