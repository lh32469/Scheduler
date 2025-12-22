package org.gpc4j.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.repository.ClassScheduleRepository;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.security.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * MVC controller that renders the home page using Thymeleaf.
 * Keeps REST API controllers intact; this simply serves a server-rendered view.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ClassScheduleRepository classScheduleRepository;

  private final RavenDB ravenDB;

  @GetMapping({"/"})
  public String home(
      @CookieValue(name = "userTimezone", defaultValue = "UTC") String timezone,
      @RequestParam(name = "page", required = false, defaultValue = "1") int page,
      @RequestParam(name = "size", required = false, defaultValue = "100") int size,
      @RequestParam(name = "type", required = false) String classType,
      @RequestParam(name = "month", required = false) String monthParam,
      @RequestParam(name = "instructor", required = false) String instructorId,
      Authentication authentication,
      Model model
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);

    log.debug("Authentication: " + authentication);
    log.debug("Timezone: " + timezone);
    log.debug("instructorId: " + instructorId);

    LocalDate sunday = getSunday();
    LocalDate fourWeeksFromNow = sunday.plusWeeks(4);
    List<ScheduledClass> classes;

    classes = classScheduleRepository.listClassesForPeriod(sunday, fourWeeksFromNow,
                                                           classType, instructorId);

    if (StringUtils.hasText(instructorId)) {
      ravenDB.getRepository(UserAccount.class)
             .findById("UserAccounts/" + instructorId)
             .ifPresent(acct -> model.addAttribute("instructorName", acct.getName()));
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

    // Transform into a lightweight event map for the client (ISO date + display fields)
    java.time.format.DateTimeFormatter isoDate =
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
    java.time.format.DateTimeFormatter time24 =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    java.time.format.DateTimeFormatter time12 =
        java.time.format.DateTimeFormatter.ofPattern("h:mm a");
    java.util.List<java.util.Map<String, Object>> calendarEvents =
        classes.stream().map(sc -> {
          java.util.Map<String, Object> m = new java.util.HashMap<>();
          java.time.LocalDateTime start = sc.getStart();
          m.put("date", start.toLocalDate().format(isoDate));
          m.put("time", start.toLocalTime().format(time24));
          m.put("timeDisplay", start.toLocalTime().format(time12));
          m.put("title", sc.getClassName());
          m.put("type", sc.getClassType() != null ? sc.getClassType().name() : "");
          m.put("location", sc.getLocation());
          m.put("instructor",
                sc.getInstructorAccount() != null ?
                    sc.getInstructorAccount().getName() :
                    "");
          // Extra details for calendar hover popup
          m.put("level", sc.getLevel());
          m.put("duration", sc.getDuration());
          m.put("slots", sc.getSlots());
          m.put("description", sc.getClassDescription());
          m.put("id", sc.getId());
          return m;
        }).toList();

    java.time.format.DateTimeFormatter monthFmt =
        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy");

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
