package org.gpc4j.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.api.ClassOffering;
import org.gpc4j.web.dto.ScheduledClass;
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

    if (filter != null) {
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
    }

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
