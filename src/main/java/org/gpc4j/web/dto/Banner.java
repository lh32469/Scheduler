package org.gpc4j.web.dto;

import lombok.Data;

@Data
public class Banner {

  private String companyName;
  /**
   * Title on URL tab in browser.
   */
  private String tabTitle;
  /**
   * Title on main page.
   */
  private String title;
  private String subTitle;

}
