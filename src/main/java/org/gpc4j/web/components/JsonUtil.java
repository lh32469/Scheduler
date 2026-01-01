package org.gpc4j.web.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("jsonUtil")
public class JsonUtil {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.registerModule(new JavaTimeModule());
  }

  public String toJson(Object obj) {
    try {
      String json = MAPPER.writeValueAsString(obj);
      if (log.isDebugEnabled()) {
        log.debug(json);
      }
      return json;

    } catch (JsonProcessingException e) {
      log.error("Error converting object to json: " + e.getMessage());
      return "{}";
    }
  }

}
