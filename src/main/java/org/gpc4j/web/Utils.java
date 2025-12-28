package org.gpc4j.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

@Slf4j
public class Utils {

  static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.registerModule(new JavaTimeModule());
  }

  @SneakyThrows
  public static String generateETag(Object model) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    String json = MAPPER.writeValueAsString(model);
    String etag = DigestUtils.md5DigestAsHex(json.getBytes());
    stopWatch.stop();
    log.debug("generateETag took {} ms", stopWatch.getTotalTimeMillis());
    return etag;
  }

}
