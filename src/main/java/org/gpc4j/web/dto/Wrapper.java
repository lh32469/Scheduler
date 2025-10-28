package org.gpc4j.web.dto;

import lombok.Data;
import org.gpc4j.web.api.ClassOffering;

@Data
public class Wrapper {

  private ClassOffering offering = new ClassOffering();
  private String level = "default";

}
