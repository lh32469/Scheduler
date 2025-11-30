package org.gpc4j.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassOffering;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.security.UserAccount;
import org.gpc4j.web.repository.ClassOfferingRepository;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.services.ScheduleClassesService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * MVC controller that renders the home page using Thymeleaf.
 * Keeps REST API controllers intact; this simply serves a server-rendered view.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ClassOfferingRepository classOfferingRepository;

  private final ScheduleClassesService classesService;

  @GetMapping({"/"})
  public String home(
      @RequestParam(name = "page", required = false, defaultValue = "1") int page,
      @RequestParam(name = "size", required = false, defaultValue = "100") int size,
      @RequestParam(name = "filter", required = false) String filter,
      @RequestParam(name = "month", required = false) String monthParam,
      @RequestParam(name = "instructor", required = false) String instructorId,
      Authentication authentication,
      Model model
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);

    log.debug("Authentication: " + authentication);


    LocalDate sunday = getSunday();
    LocalDate fourWeeksFromNow = sunday.plusWeeks(4);
    List<ScheduledClass> classes =
        classesService.getScheduledClasses(sunday, fourWeeksFromNow);

    if (filter != null && !filter.isBlank()) {
      String f = filter.trim();
      classes = classes.stream()
                       .filter(c -> c.getClassType()
                                     .name()
                                     .equalsIgnoreCase(f))
                       .toList();
    }

    // Optional filter: only show classes for a specific instructor (by instructor UserAccount id)
    if (instructorId != null && !instructorId.isBlank()) {
      String target = instructorId.trim();
      classes = classes.stream()
                       .filter(c -> c.getInstructorAccount() != null
                           && target.equals(c.getInstructorAccount().getId()))
                       .toList();

      // Try to determine the instructor's display name from the resulting classes
      String instructorName = classes.stream()
                                     .map(ScheduledClass::getInstructorAccount)
                                     .filter(Objects::nonNull)
                                     .map(UserAccount::getName)
                                     .findFirst()
                                     .orElse(null);
      model.addAttribute("instructorName", instructorName);
    }

    // Build Month Calendar data (selected month or current month)
    java.time.YearMonth ym;
    try {
      if (monthParam != null && !monthParam.isBlank()) {
        ym = java.time.YearMonth.parse(monthParam);
      } else {
        ym = java.time.YearMonth.now();
      }
    } catch (Exception e) {
      // Fallback to current month if parsing fails
      ym = java.time.YearMonth.now();
    }

    LocalDate firstOfMonth = ym.atDay(1);
    LocalDate firstOfNextMonth = ym.plusMonths(1).atDay(1);
    List<ScheduledClass> monthClasses = classesService.getScheduledClasses(firstOfMonth, firstOfNextMonth);

    // Apply the same filters to the calendar data
    if (filter != null && !filter.isBlank()) {
      String f = filter.trim();
      monthClasses = monthClasses.stream()
          .filter(c -> c.getClassType() != null && c.getClassType().name().equalsIgnoreCase(f))
          .toList();
    }
    if (instructorId != null && !instructorId.isBlank()) {
      String target = instructorId.trim();
      monthClasses = monthClasses.stream()
          .filter(c -> c.getInstructorAccount() != null && target.equals(c.getInstructorAccount().getId()))
          .toList();
    }

    // Transform into a lightweight event map for the client (ISO date + display fields)
    java.time.format.DateTimeFormatter isoDate = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
    java.time.format.DateTimeFormatter time24 = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    java.time.format.DateTimeFormatter time12 = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
    java.util.List<java.util.Map<String, Object>> calendarEvents = monthClasses.stream().map(sc -> {
      java.util.Map<String, Object> m = new java.util.HashMap<>();
      java.time.LocalDateTime start = sc.getStart();
      m.put("date", start.toLocalDate().format(isoDate));
      m.put("time", start.toLocalTime().format(time24));
      m.put("timeDisplay", start.toLocalTime().format(time12));
      m.put("title", sc.getClassName());
      m.put("type", sc.getClassType() != null ? sc.getClassType().name() : "");
      m.put("location", sc.getLocation());
      m.put("instructor", sc.getInstructorAccount() != null ? sc.getInstructorAccount().getName() : "");
      // Extra details for calendar hover popup
      m.put("level", sc.getLevel());
      m.put("duration", sc.getDuration());
      m.put("slots", sc.getSlots());
      m.put("description", sc.getClassDescription());
      m.put("id", sc.getId());
      return m;
    }).toList();

    java.time.format.DateTimeFormatter monthFmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy");

    model.addAttribute("calendarMonthLabel", firstOfMonth.format(monthFmt));
    model.addAttribute("calendarMonthStart", firstOfMonth);
    model.addAttribute("calendarMonthEnd", firstOfNextMonth.minusDays(1));
    model.addAttribute("calendarEvents", calendarEvents);

    // Month navigation params for UI
    String currentMonthParam = ym.toString(); // yyyy-MM
    String prevMonthParam = ym.minusMonths(1).toString();
    String nextMonthParam = ym.plusMonths(1).toString();
    model.addAttribute("monthParam", currentMonthParam);
    model.addAttribute("prevMonthParam", prevMonthParam);
    model.addAttribute("nextMonthParam", nextMonthParam);

    model.addAttribute("classes", classes);
    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);

    return "index"; // resolved from src/main/resources/templates/index.html
  }

  LocalDate getSunday() {
    LocalDate today = LocalDate.now();
    return today.minusDays(today.getDayOfWeek().getValue() - 1);
  }

}
