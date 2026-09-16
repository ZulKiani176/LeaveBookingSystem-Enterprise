package uk.ac.staffs.leavebooking.common.events;

import java.time.LocalDate;

public interface Event {
    Long id();

    LocalDate occurredOn();

    Event withId(Long id);
}
