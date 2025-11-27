package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.repository.RavenUserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public page that lists all instructors and their profile information.
 */
@Slf4j
@Controller
@RequestMapping("/instructors")
@RequiredArgsConstructor
public class InstructorsController {

  private final RavenUserRepository userRepository;
  private final RavenDB ravenDB;

  @GetMapping
  public String listInstructors(Model model) {
    List<UserAccount> instructors = userRepository.findAllInstructors();

    // Map of instructorId -> distinct ClassTypes taught based on current ClassSchedules
    Map<String, Set<ClassType>> instructorClassTypes = new HashMap<>();
    try (IDocumentSession session = ravenDB.openSession()) {
      // Collect instructor IDs
      List<String> instructorIds = new LinkedList<>();
      for (UserAccount ua : instructors) {
        if (ua.getId() != null) {
          instructorIds.add(ua.getId());
        }
      }

      log.debug("instructorIds " + instructorIds);

      if (!instructorIds.isEmpty()) {
        List<ClassSchedule> schedules = session.query(ClassSchedule.class)
                                               .whereIn("instructorId", instructorIds)
                                               .toList();

        for (ClassSchedule cs : schedules) {
          if (cs.getInstructorId() == null || cs.getClassType() == null) {
            continue;
          }
          instructorClassTypes
              .computeIfAbsent(cs.getInstructorId(), k ->
                  EnumSet.noneOf(ClassType.class))
              .add(cs.getClassType());
        }
      }
    }

    log.debug("instructorClassTypes " + instructorClassTypes);

    model.addAttribute("title", "Instructors");
    model.addAttribute("instructors", instructors);
    model.addAttribute("instructorClassTypes", instructorClassTypes);
    return "instructors/index";
  }

}
