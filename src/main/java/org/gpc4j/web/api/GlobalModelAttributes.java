package org.gpc4j.web.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
public class GlobalModelAttributes {

  @ModelAttribute
  public void addRequestToModel(HttpServletRequest request, Model model) {
    model.addAttribute("request", request);
    log.debug("Domain name: " + request.getLocalName());
  }

}
