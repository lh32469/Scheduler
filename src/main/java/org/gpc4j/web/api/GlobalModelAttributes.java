package org.gpc4j.web.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Banner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice(basePackages = {"org.gpc4j.web"})
@RequiredArgsConstructor
public class GlobalModelAttributes {

  private final IDocumentSession session;

  @Value("${management.server.port}")
  int managementPort;

  @ModelAttribute
  public void addRequestToModel(HttpServletRequest request, Model model) {

    final String url = request.getRequestURL().toString();
    log.info("Request URL " + url);

    if (log.isTraceEnabled()) {
      log.trace(request.getRemoteAddr() + " "
                    + request.getRequestURL() + " "
                    + request.getMethod());
      log.trace("Model = " + model);
    }

    Banner banner = session
        .query(Banner.class)
        .firstOrDefault();

    model.addAttribute("banner", banner);
    model.addAttribute("request", request);
    log.debug("Domain name: " + request.getLocalName());

  }

}
