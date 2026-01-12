package org.gpc4j.web.dto;

import java.time.LocalDate;

public record Holiday(LocalDate date,
                      String name) {

}
