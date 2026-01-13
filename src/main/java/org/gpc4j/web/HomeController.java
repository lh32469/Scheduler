package org.gpc4j.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Closed;
import org.gpc4j.web.dto.Holiday;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.repository.ClassScheduleRepository;
import org.gpc4j.web.services.EtagChangePublisher;
import org.gpc4j.web.services.EtagWatchdog;
import org.gpc4j.web.services.HolidayService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gpc4j.web.configs.RavenConfig.DB_NAME;

/**
 * MVC controller that renders the home page using Thymeleaf.
 * Keeps REST API controllers intact; this simply serves a server-rendered view.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

  private final ClassScheduleRepository classScheduleRepository;
  private final EtagChangePublisher etagChangePublisher;
  private final IDocumentSession session;
  private final EtagWatchdog etagWatchdog;
  private final HolidayService holidayService;

  @GetMapping(value = "/etag-updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeToEtagUpdates(HttpServletRequest request) {

    String databaseName = (String) request.getAttribute(DB_NAME);
    log.debug("Using database: {}", databaseName);
    return etagChangePublisher.subscribe(databaseName);
  }

//  @GetMapping("/etag")
//  @ResponseBody
//  public String getETag() {
//    LocalDate sunday = getSunday();
//    List<ScheduledClass> classes =
//        classScheduleRepository.getClassesDuring(sunday, sunday.plusWeeks(4));
//    String eta = generateETag(classes);
//    log.info("ETag: {}", eta);
//    return eta;
//  }

  @GetMapping({"/"})
  public String home(
      @CookieValue(name = "userTimezone", defaultValue = "UTC") String timezone,
      @RequestParam(name = "page", required = false, defaultValue = "1") int page,
      @RequestParam(name = "size", required = false, defaultValue = "100") int size,
      @RequestParam(name = "month", required = false) String monthParam,
      Authentication authentication,
      Model model,
      HttpServletResponse response
  ) {

    CacheControl cacheControl = CacheControl
        .maxAge(Duration.of(15, ChronoUnit.MINUTES));

    response.setHeader("Cache-Control", cacheControl.getHeaderValue());

    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);

    log.debug("Authentication: " + authentication);
    log.debug("Timezone: " + timezone);

    // Build Month Calendar data (selected month or current month)
    YearMonth yearMonth;
    try {
      if (StringUtils.hasText(monthParam)) {
        yearMonth = YearMonth.parse(monthParam);
      } else {
        yearMonth = YearMonth.now();
      }
    } catch (Exception e) {
      // Fallback to current month if parsing fails
      yearMonth = YearMonth.now();
    }

    List<ScheduledClass> classes =
        classScheduleRepository.getClassesForMonth(yearMonth, ZoneId.of(timezone));

    List<Holiday> holidays = holidayService.getHolidays(yearMonth.getYear());

    // Get days closed
    Map<LocalDate, Closed> closedDays =
        session.query(Closed.class)
               .whereBetween("date",
                             yearMonth.atDay(1),
                             yearMonth.atEndOfMonth())
               .toList()
               .stream()
               .collect(Collectors.toMap(Closed::date, Function.identity()));

    log.info("Days Closed for " + yearMonth + ": " + closedDays);

    // Filter out classes during Closed hours
    classes = classes.stream()
                     .filter(clss -> {
                       LocalDateTime classStartTime = clss.getStart();
                       LocalDate classStartDate = classStartTime.toLocalDate();
                       Closed closed = closedDays.get(classStartDate);

                       if (closed == null) {
                         return true;
                       }

                       return closed.hours()
                                    .stream()
                                    .noneMatch(hours ->
                                                   classStartTime.isAfter(
                                                       hours.start()
                                                            .atDate(classStartDate))
                                                       &&
                                                       classStartTime.isBefore(
                                                           hours.end()
                                                                .atDate(classStartDate)));
                     })
                     .collect(Collectors.toList());

    // Transform into a lightweight event map for the client (ISO date + display fields)
    DateTimeFormatter isoDate = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
    DateTimeFormatter time24 = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    DateTimeFormatter time12 = java.time.format.DateTimeFormatter.ofPattern("h:mm a");

    List<Map<String, Object>> calendarEvents =
        classes.stream().map(sc -> {
          Map<String, Object> m = new java.util.HashMap<>();
          LocalDateTime start = sc.getStart();
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

    log.debug("calendarEvents size: " + calendarEvents.size() + " for " + yearMonth);

    DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");

    LocalDate firstOfMonth = yearMonth.atDay(1);

    model.addAttribute("calendarMonthLabel", firstOfMonth.format(monthFmt));
    model.addAttribute("calendarMonthStart", firstOfMonth);
    model.addAttribute("calendarMonthEnd", yearMonth.atEndOfMonth());
    model.addAttribute("calendarEvents", calendarEvents);
    model.addAttribute("holidays", holidays);

    // Month navigation params for UI
    String currentMonthParam = yearMonth.toString(); // yyyy-MM
    String prevMonthParam = yearMonth.minusMonths(1).toString();
    String nextMonthParam = yearMonth.plusMonths(1).toString();
    model.addAttribute("monthParam", currentMonthParam);
    model.addAttribute("prevMonthParam", prevMonthParam);
    model.addAttribute("nextMonthParam", nextMonthParam);

    model.addAttribute("classes", classes);
    model.addAttribute("classesETag", etagWatchdog.getEtag(session));
    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);

    log.info("classesETag: " + model.getAttribute("classesETag"));

    return "index"; // resolved from src/main/resources/templates/index.html
  }

}
