package org.gpc4j.web;

import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.repository.RavenDB;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest

public class UtilitiesIT {

  @Autowired
  RavenDB ravenDB;

  @Test
  void createBanner() {

    try (IDocumentSession session = ravenDB.openSession()) {
      Banner banner = new Banner();
      banner.setCompanyName("Company Name");
      banner.setTitle("Title");
      banner.setSubTitle("Subtitle");
      session.store(banner);
      session.saveChanges();
    }

  }

}
