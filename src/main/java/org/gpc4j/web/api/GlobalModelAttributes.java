package org.gpc4j.web.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.repository.RavenDB;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

  private final RavenDB ravenDB;

  @ModelAttribute
  public void addRequestToModel(HttpServletRequest request, Model model) {
    model.addAttribute("request", request);
    log.debug("Domain name: " + request.getLocalName());

    try (IDocumentSession session = ravenDB.openSession()) {
      Banner banner = session.query(Banner.class)
                             .firstOrDefault();

      model.addAttribute("banner", banner);
    }

  }

}
