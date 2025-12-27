package org.gpc4j.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.util.DigestUtils;

public class Utils {

  static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.registerModule(new JavaTimeModule());
  }

  @SneakyThrows
  public static String generateETag(Object model) {
    String json = MAPPER.writeValueAsString(model);
    return DigestUtils.md5DigestAsHex(json.getBytes());
  }

}
