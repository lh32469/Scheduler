package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import org.gpc4j.web.repository.RavenUserRepository;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Public page that lists all instructors and their profile information.
 */
@Controller
@RequestMapping("/instructors")
@RequiredArgsConstructor
public class InstructorsController {

  private final RavenUserRepository userRepository;

  @GetMapping
  public String listInstructors(Model model) {
    List<UserAccount> instructors = userRepository.findAllInstructors();
    model.addAttribute("title", "Instructors");
    model.addAttribute("instructors", instructors);
    return "instructors/index";
  }
}
