package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.repository.UserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Public page that lists all instructors and their profile information.
 */
@Slf4j
@Controller
@RequestMapping("/instructors")
@RequiredArgsConstructor
public class InstructorsController {

  private final UserRepository userRepository;
  private final RavenDB ravenDB;

  @GetMapping
  public String listInstructors(Model model,
                                @RequestParam(name = "classType", required = false) ClassType selectedClassType,
                                @RequestParam(name = "serviceType", required = false) ServiceType selectedServiceType) {

    List<UserAccount> instructors = userRepository.findAllInstructors();

    // Map of instructorId -> distinct ClassTypes taught based on current ClassSchedules
    Map<String, Set<ClassType>> instructorClassTypes = new HashMap<>();
    Map<String, Set<ServiceType>> instructorServiceTypes = new HashMap<>();

    try (IDocumentSession session = ravenDB.openSession()) {

      // Collect instructor IDs
      List<String> instructorIds = instructors.stream()
                                              .map(UserAccount::getId)
                                              .toList();

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

    instructors.stream()
               .filter(instructor -> Objects.nonNull(instructor.getServiceTypes()))
               .forEach(instructor -> instructorServiceTypes
                   .computeIfAbsent(instructor.getId(), k ->
                       EnumSet.noneOf(ServiceType.class))
                   .addAll(instructor.getServiceTypes())
               );

    log.debug("instructorClassTypes " + instructorClassTypes);
    log.debug("instructorServiceTypes " + instructorServiceTypes);

    // If a class type filter is provided, filter the instructors list accordingly
    if (selectedClassType != null) {
      instructors = instructors.stream()
                               .filter(i -> {
                                 Set<ClassType> types =
                                     instructorClassTypes.get(i.getId());
                                 return types != null && types.contains(selectedClassType);
                               })
                               .toList();
    }

    model.addAttribute("title", selectedClassType == null ? "Instructors"
        : ("Instructors — " + selectedClassType.name()))
    ;

    // If a Service type filter is provided, filter the instructors list accordingly
    if (selectedServiceType != null) {
      instructors = instructors.stream()
                               .filter(i -> {
                                 Set<ServiceType> types =
                                     instructorServiceTypes.get(i.getId());
                                 return types != null && types.contains(
                                     selectedServiceType);
                               })
                               .toList();
    }

    model.addAttribute("title", selectedClassType == null ? "Instructors"
        : ("Instructors — " + selectedClassType.name()))
    ;

    model.addAttribute("instructors", instructors);
    model.addAttribute("instructorClassTypes", instructorClassTypes);
    model.addAttribute("selectedClassType", selectedClassType);
    model.addAttribute("allClassTypes",
                       instructorClassTypes.values()
                                           .stream()
                                           .flatMap(Set::stream)
                                           .distinct()
                                           .toList());
    model.addAttribute("selectedServiceType", selectedServiceType);
    // Only show distinct service types available to instructors
    model.addAttribute("allServiceTypes",
                       instructorServiceTypes.values()
                                             .stream()
                                             .flatMap(Set::stream)
                                             .distinct()
                                             .toList());
    return "instructors/index";
  }

}
