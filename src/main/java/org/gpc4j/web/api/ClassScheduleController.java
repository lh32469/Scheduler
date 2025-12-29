package org.gpc4j.web.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.repository.UserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.gpc4j.web.configs.CacheConfig.CLASS_LIST;

/**
 * Controller for handling class schedule management tasks such as creating,
 * editing, and updating class schedules. Provides endpoints only accessible
 * to users with roles 'ADMIN' and 'INSTRUCTOR'.
 */
@Slf4j
@Controller
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ClassScheduleController {

  private final IDocumentSession session;
  private final UserRepository userRepository;
  private final CacheManager cacheManager;

  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @GetMapping("/new")
  public String newOfferingForm(Model model) {
    if (!model.containsAttribute("schedule")) {
      ClassSchedule schedule = new ClassSchedule();
      // Default startWeek to upcoming Sunday
      LocalDate today = LocalDate.now();
      LocalDate nextSunday = today.plusDays((7 - today.getDayOfWeek().getValue()) % 7L);
      if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
        nextSunday = today.plusWeeks(1); // default to next week if today is Sunday
      }
      schedule.setStartWeek(nextSunday);
      schedule.setNumberOfWeeks(5);
      schedule.setDuration(60);
      schedule.setSlots(10);
      schedule.setLevel("All Levels");
      model.addAttribute("schedule", schedule);
    }
    model.addAttribute("title", "Schedule New Class");
    model.addAttribute("isEdit", false);
    return "schedules/new";
  }

  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @PostMapping
  public String createOffering(@ModelAttribute("schedule") ClassSchedule schedule,
                               BindingResult bindingResult,
                               Authentication authentication,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
    try {
      if (authentication == null || authentication.getName() == null) {
        bindingResult.reject("auth.required",
                             "You must be logged in to create a schedule.");
      }

      if (bindingResult.hasErrors()) {
        redirectAttributes.addFlashAttribute(
            "org.springframework.validation.BindingResult.schedule",
            bindingResult);
        redirectAttributes.addFlashAttribute("schedule", schedule);
        return "redirect:/schedules/new";
      }

      // Set the instructor to the current user
      String username = authentication.getName();
      UserAccount user = userRepository.findByUsername(username);
      if (user == null) {
        bindingResult.reject("user.notfound", "Authenticated user not found");
        redirectAttributes.addFlashAttribute(
            "org.springframework.validation.BindingResult.schedule",
            bindingResult);
        redirectAttributes.addFlashAttribute("schedule", schedule);
        return "redirect:/schedules/new";
      }

      schedule.setInstructorId(user.getId());

      session.store(schedule);
      session.saveChanges();
      String id = schedule.getId();

      Cache cache = cacheManager.getCache(CLASS_LIST);
      if (cache != null) {
        cache.clear();
      }

      redirectAttributes.addFlashAttribute("message", "Created schedule with id=" + id);
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/instructor/classes";
    } catch (Exception e) {
      log.error("Failed to create ClassSchedule", e);
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to create schedule: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/schedules/new";
    }
  }

  /**
   * Edit form for an existing ClassSchedule.
   */
  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @GetMapping("/ClassSchedules/{id}/edit")
  public String editOfferingForm(@PathVariable("id") String id,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

    id = "ClassSchedules/" + id;
    log.info("Editing ClassSchedule with ID: " + id);

    ClassSchedule schedule = session.load(ClassSchedule.class, id);
    if (schedule == null) {
      redirectAttributes.addFlashAttribute("message", "Schedule not found");
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/instructor/classes";
    }

    // Ownership / role check
    String username = authentication != null ? authentication.getName() : null;
    UserAccount user =
        (username != null) ? userRepository.findByUsername(username) : null;
    boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                                                              .anyMatch(a -> a.getAuthority()
                                                                              .equals(
                                                                                  "ROLE_ADMIN"));
    if (user == null || (!isAdmin && !schedule.getInstructorId().equals(user.getId()))) {
      redirectAttributes.addFlashAttribute("message",
                                           "You are not allowed to edit this schedule.");
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/instructor/classes";
    }

    model.addAttribute("schedule", schedule);
    model.addAttribute("title", "Edit Class");
    model.addAttribute("isEdit", true);
    return "schedules/new"; // reuse the same template with edit mode

  }

  /**
   * Persist changes to an existing ClassSchedule.
   */
  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @PostMapping("/ClassSchedules/{id}")
  public String updateOffering(@PathVariable("id") String id,
                               @ModelAttribute("schedule") ClassSchedule form,
                               BindingResult bindingResult,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

    id = "ClassSchedules/" + id;
    log.info("Editing ClassSchedule with ID: " + id);

    try {
      ClassSchedule schedule = session.load(ClassSchedule.class, id);
      if (schedule == null) {
        redirectAttributes.addFlashAttribute("message", "Schedule not found");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/instructor/classes";
      }

      String username = authentication != null ? authentication.getName() : null;
      UserAccount user =
          (username != null) ? userRepository.findByUsername(username) : null;
      boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                                                                .anyMatch(a -> a.getAuthority()
                                                                                .equals(
                                                                                    "ROLE_ADMIN"));
      if (user == null || (!isAdmin && !schedule.getInstructorId()
                                                .equals(user.getId()))) {
        redirectAttributes.addFlashAttribute("message",
                                             "You are not allowed to edit this schedule" +
                                                 ".");
        redirectAttributes.addFlashAttribute("messageType", "error");
        return "redirect:/instructor/classes";
      }

      // Basic validation (mirror create)
      if (form.getStartWeek() == null || form.getClassStartTime() == null ||
          form.getDaysOfWeek() == null || form.getDaysOfWeek().isEmpty()) {
        redirectAttributes.addFlashAttribute("message",
                                             "Please provide Start Week, Start Time, " +
                                                 "and select at least one day.");
        redirectAttributes.addFlashAttribute("messageType", "error");
        redirectAttributes.addFlashAttribute("schedule", form);
        redirectAttributes.addFlashAttribute("isEdit", true);
        return "redirect:/schedules/" + id + "/edit";
      }

      // Copy mutable fields from form to loaded entity
      schedule.setClassName(form.getClassName());
      schedule.setClassDescription(form.getClassDescription());
      schedule.setClassType(form.getClassType());
      schedule.setLevel(form.getLevel());
      schedule.setStartWeek(form.getStartWeek());
      schedule.setNumberOfWeeks(form.getNumberOfWeeks());
      schedule.setDaysOfWeek(form.getDaysOfWeek());
      schedule.setClassStartTime(form.getClassStartTime());
      schedule.setDuration(form.getDuration());
      schedule.setSlots(form.getSlots());
      schedule.setLocation(form.getLocation());
      // preserve instructorId

      session.saveChanges();

      redirectAttributes.addFlashAttribute("message",
                                           "Updated schedule for '" + schedule.getClassName() + "'.");
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/instructor/classes";
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("message",
                                           "Failed to update: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/instructor/classes";
    }
  }

}
