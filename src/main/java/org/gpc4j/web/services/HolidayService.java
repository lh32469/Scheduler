package org.gpc4j.web.services;

import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.dto.Holiday;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class HolidayService {

  @Cacheable(value = "holidays")
  public List<Holiday> getHolidays(int year) {
    List<Holiday> holidays = new LinkedList<>();

    // New Year's Day - January 1
    holidays.add(createHoliday("New Year's Day", LocalDate.of(year, Month.JANUARY, 1)));

    // Martin Luther King Jr. Day - Third Monday in January
    holidays.add(createHoliday("Martin Luther King Jr. Day",
                               LocalDate.of(year, Month.JANUARY, 1)
                                        .with(TemporalAdjusters.dayOfWeekInMonth(3,
                                                                                 DayOfWeek.MONDAY))));

    // Presidents' Day - Third Monday in February
    holidays.add(createHoliday("Presidents' Day",
                               LocalDate.of(year, Month.FEBRUARY, 1)
                                        .with(TemporalAdjusters.dayOfWeekInMonth(3,
                                                                                 DayOfWeek.MONDAY))));

    // Memorial Day - Last Monday in May
    holidays.add(createHoliday("Memorial Day",
                               LocalDate.of(year, Month.MAY, 1)
                                        .with(TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY))));

    // Juneteenth - June 19
    holidays.add(createHoliday("Juneteenth", LocalDate.of(year, Month.JUNE, 19)));

    // Independence Day - July 4
    holidays.add(createHoliday("Independence Day", LocalDate.of(year, Month.JULY, 4)));

    // Labor Day - First Monday in September
    holidays.add(createHoliday("Labor Day",
                               LocalDate.of(year, Month.SEPTEMBER, 1)
                                        .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))));

    // Columbus Day - Second Monday in October
    holidays.add(createHoliday("Columbus Day",
                               LocalDate.of(year, Month.OCTOBER, 1)
                                        .with(TemporalAdjusters.dayOfWeekInMonth(2,
                                                                                 DayOfWeek.MONDAY))));

    // Veterans Day - November 11
    holidays.add(createHoliday("Veterans Day", LocalDate.of(year, Month.NOVEMBER, 11)));

    // Thanksgiving Day - Fourth Thursday in November
    holidays.add(createHoliday("Thanksgiving Day",
                               LocalDate.of(year, Month.NOVEMBER, 1)
                                        .with(TemporalAdjusters
                                                  .dayOfWeekInMonth(4,
                                                                    DayOfWeek.THURSDAY))));

    // Christmas Day - December 25
    holidays.add(createHoliday("Christmas Day", LocalDate.of(year, Month.DECEMBER, 25)));

    log.info("Retrieved {} US Federal Holidays for year {}", holidays.size(), year);

    return holidays;
  }

  private Holiday createHoliday(String name, LocalDate date) {
    return new Holiday(date, name);
  }

  public static void main(String[] args) {
    new HolidayService()
        .getHolidays(2026)
        .forEach(System.out::println);
  }

}
