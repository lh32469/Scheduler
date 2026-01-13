package org.gpc4j.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record Closed(LocalDate date,
                     List<Hours> hours) {

  public record Hours(LocalTime start, LocalTime end) {

  }

}
