package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gpc4j.web.repository.ClassOfferingRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * MVC controller to create new ClassOffering documents via Thymeleaf form.
 */
@Slf4j
@Controller
@RequestMapping("/offerings")
@RequiredArgsConstructor
public class ClassOfferingFormController {

  private final ClassOfferingRepository repository;
  private final RavenUserRepository userRepository;

  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @GetMapping("/new")
  public String newOfferingForm(Model model) {
    if (!model.containsAttribute("offering")) {
      ClassOffering offering = new ClassOffering();
      ClassOffering.Schedule schedule = new ClassOffering.Schedule();
      schedule.setStart(LocalDateTime.now().plusDays(1));
      schedule.setDuration("60 min");
      offering.setSchedule(schedule);
      offering.setSlots(10);
      offering.setLevel("All Levels");
      model.addAttribute("offering", offering);
    }
    model.addAttribute("title", "Create Class Offering");
    return "offerings/new";
  }

  @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
  @PostMapping
  public String createOffering(@ModelAttribute("offering") ClassOffering offering,
                               BindingResult bindingResult,
                               Authentication authentication,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
    try {
      if (authentication == null || authentication.getName() == null) {
        bindingResult.reject("auth.required", "You must be logged in to create an offering.");
      }

      if (bindingResult.hasErrors()) {
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.offering", bindingResult);
        redirectAttributes.addFlashAttribute("offering", offering);
        return "redirect:/offerings/new";
      }

      // Set the instructor to the current user
      String username = authentication.getName();
      UserAccount user = userRepository.findByUsername(username);
      if (user == null) {
        bindingResult.reject("user.notfound", "Authenticated user not found");
        redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.offering", bindingResult);
        redirectAttributes.addFlashAttribute("offering", offering);
        return "redirect:/offerings/new";
      }

      offering.setInstructorId(user.getId());
      // Save current domain/host
      offering.setDomain(request.getLocalName());

      String id = repository.save(offering);
      redirectAttributes.addFlashAttribute("message", "Created offering with id=" + id);
      redirectAttributes.addFlashAttribute("messageType", "info");
      return "redirect:/";
    } catch (Exception e) {
      log.error("Failed to create ClassOffering", e);
      redirectAttributes.addFlashAttribute("message", "Failed to create offering: " + e.getMessage());
      redirectAttributes.addFlashAttribute("messageType", "error");
      return "redirect:/offerings/new";
    }
  }
}
