package org.gpc4j.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.ClassOffering;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.repository.ClassOfferingRepository;
import org.gpc4j.web.repository.RavenDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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


  private final RavenDB ravenDB;

  @GetMapping({"/"})
  public String home(
      @RequestParam(name = "page", required = false, defaultValue = "1") int page,
      @RequestParam(name = "size", required = false, defaultValue = "100") int size,
      @RequestParam(name = "filter", required = false) String filter,
      Authentication authentication,
      Model model
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);

    log.info("" + authentication);

    List<ClassOffering> offerings =
        classOfferingRepository.list(safePage, safeSize);

    if (filter != null) {
      offerings = offerings.stream()
                           .filter(o -> Objects.equals(
                               o.getClassType()
                                .toLowerCase(), filter.toLowerCase()))
                           .toList();
    }

    Banner banner;

    try (IDocumentSession session = ravenDB.openSession()) {
      banner = session.query(Banner.class)
                  .firstOrDefault();
    }

    model.addAttribute("offerings", offerings);
    model.addAttribute("banner", banner);

    model.addAttribute("page", safePage);
    model.addAttribute("size", safeSize);

    return "index"; // resolved from src/main/resources/templates/index.html
  }

}
