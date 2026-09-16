package uk.ac.staffs.leavebooking.leave.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "public_holiday")
public class PublicHolidayJpa {
    @Id
    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected PublicHolidayJpa() {
    }

    public PublicHolidayJpa(LocalDate date, String name) {
        this.date = date;
        this.name = name;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
