package org.gpc4j.web;

import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.ClassOffering;
import org.gpc4j.web.api.ClassType;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.repository.RavenDocumentStoreCache;
import org.gpc4j.web.security.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest

public class UtilitiesIT {

  @Autowired
  RavenDocumentStoreCache cache;

  IDocumentSession session;

  @BeforeEach
  void setUp() {
    session = cache.getDocumentStore("demo.fithub.mcquarrie.cc")
                   .openSession();
  }

  @AfterEach
  void tearDown() {
    session.saveChanges();
    session.close();
  }

  @Test
  void createBanner() {

    Banner banner = new Banner();
    banner.setCompanyName("Company Name");
    banner.setTitle("Title");
    banner.setSubTitle("Subtitle");

    session.store(banner);
  }

  @Test
  void createAdmin() {

    UserAccount admin = new UserAccount();
    admin.setUsername("admin");
    admin.setName("Administrator");
    admin.setPasswordHash("$2a$10$TghwWcKwesb9pANYNDHZOOYomgkyoCX1fnTFX3g53SZLldibWGch");
    admin.setUsername("admin");
    admin.setEnabled(true);
    admin.setAccountNonLocked(true);
    admin.setRoles(List.of("ROLE_ADMIN"));

    session.store(admin);
  }

  @Test
  void createOneClass() {

    ClassOffering offering = new ClassOffering();
    offering.setClassName("Demo Class");
    offering.setLevel("Beginner");
    offering.setSlots(10);
    offering.setClassType(ClassType.cardio);
    offering.setInstructorId("UserAccounts/1-A");

    ClassOffering.Schedule schedule = new ClassOffering.Schedule();
    schedule.setStart(LocalDateTime.now().plusDays(7));
    offering.setSchedule(schedule);

    session.store(offering);
  }

}
