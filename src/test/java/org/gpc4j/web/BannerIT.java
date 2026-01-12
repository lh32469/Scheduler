package org.gpc4j.web;

import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Banner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class BannerIT {

  @Autowired
  IDocumentSession session;

  @Test
  void bannerTest() {
    Banner banner = session
        .query(Banner.class)
        .firstOrDefault();
    log.info(banner.toString());
  }

}
