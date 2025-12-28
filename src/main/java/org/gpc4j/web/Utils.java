package org.gpc4j.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

import java.time.LocalDate;

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

  /**
   * Computes the date of the most recent Sunday relative to the current date.
   *
   * @return The {@link LocalDate} representing the most recent Sunday. If today is
   *         already Sunday, today's date is returned.
   */
  public static LocalDate getSunday() {
    LocalDate today = LocalDate.now();
    return today.minusDays(today.getDayOfWeek().getValue() - 1);
  }

}
