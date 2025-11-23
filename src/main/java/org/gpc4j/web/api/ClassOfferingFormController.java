package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.repository.RavenDB;
import org.gpc4j.web.security.RavenUserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * MVC controller to create new ClassSchedule documents via Thymeleaf form,
 * keeping the existing /offerings/new endpoint and POST /offerings action.
 */
@Slf4j
@Controller
@RequestMapping("/offerings")
@RequiredArgsConstructor
public class ClassOfferingFormController {

  private final RavenDB ravenDB;
  private final RavenUserRepository userRepository;

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
    return "offerings/new";
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
        bindingResult.reject("auth.required", "You must be logged in to create a schedule.");
      }

      if (bindingResult.hasErrors()) {
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.schedule", bindingResult);
        redirectAttributes.addFlashAttribute("schedule", schedule);
        return "redirect:/offerings/new";
      }

      // Set the instructor to the current user
      String username = authentication.getName();
      UserAccount user = userRepository.findByUsername(username);
      if (user == null) {
        bindingResult.reject("user.notfound", "Authenticated user not found");
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.schedule", bindingResult);
        redirectAttributes.addFlashAttribute("schedule", schedule);
        return "redirect:/offerings/new";
      }

      schedule.setInstructorId(user.getId());

      String id;
      try (IDocumentSession session = ravenDB.openSession()) {
        session.store(schedule);
        session.saveChanges();
        id = schedule.getId();
      }

      redirectAttributes.addFlashAttribute("message", "Created schedule with id=" + id);
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/instructor/classes";
    } catch (Exception e) {
      log.error("Failed to create ClassSchedule", e);
      redirectAttributes.addFlashAttribute("message", "Failed to create schedule: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/offerings/new";
    }
  }
}
