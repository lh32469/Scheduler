package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.repository.UserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * MVC page for instructors to see their own class offerings.
 */
@Slf4j
@Controller
@RequestMapping("/instructor/classes")
@RequiredArgsConstructor
public class InstructorClassesController {

  private final RavenDB ravenDB;
  private final UserRepository userRepository;

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @GetMapping
  public String listInstructorClasses(@RequestParam(name = "page", defaultValue = "1") int page,
                                      @RequestParam(name = "size", defaultValue = "50") int size,
                                      Authentication authentication,
                                      Model model) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 200);
    int skip = (safePage - 1) * safeSize;

    if (authentication == null || authentication.getName() == null) {
      return "redirect:/login";
    }

    String username = authentication.getName();
    UserAccount user = userRepository.findByUsername(username);
    if (user == null) {
      return "redirect:/login";
    }

    try (IDocumentSession session = ravenDB.openSession()) {
      List<ClassSchedule> schedules = session.query(ClassSchedule.class)
          .whereEquals("instructorId", user.getId())
          .orderBy("startWeek")
          .skip(skip)
          .take(safeSize + 1)
          .toList();

      boolean hasNext = schedules.size() > safeSize;
      if (hasNext) {
        schedules = schedules.subList(0, safeSize);
      }

      model.addAttribute("title", "My Classes");
      model.addAttribute("schedules", schedules);
      model.addAttribute("page", safePage);
      model.addAttribute("size", safeSize);
      model.addAttribute("hasNext", hasNext);

      log.info("Instructor {} has {} schedules.", username, schedules.size());
    }


    return "instructor/classes";
  }
}
