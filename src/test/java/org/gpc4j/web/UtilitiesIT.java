package org.gpc4j.web;

import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.ClassType;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.dto.ClassSchedule;
import org.gpc4j.web.dto.ScheduledClass;
import org.gpc4j.web.repository.RavenDocumentStoreCache;
import org.gpc4j.web.security.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Slf4j
@SpringBootTest
public class UtilitiesIT {

  @Value("${ravendb.database}")
  private String databaseName;

  @Autowired
  RavenDocumentStoreCache cache;

  @Autowired
  PasswordEncoder passwordEncoder;

  IDocumentSession session;

  @BeforeEach
  void setUp() {
    session = cache.getDocumentStore(databaseName)
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
    admin.setPasswordHash(passwordEncoder.encode("admin"));
    admin.setEnabled(true);
    admin.setAccountNonLocked(true);
    admin.setRoles(List.of("ROLE_ADMIN"));

    session.store(admin, "Admin/1-A");
  }

  @Test
  void createInstructorRachel() {

    UserAccount account = new UserAccount();
    account.setUsername("rachel@example.com");
    account.setName("Rachel Green");
    account.setProfile("Ballet-trained instructor combining barre techniques " +
                                     "with strength training.");
    account.setPasswordHash(passwordEncoder.encode("rachel"));

    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));

    session.store(account, "UserAccounts/1-A");
  }

  @Test
  void createInstructorAlex() {

    UserAccount account = new UserAccount();
    account.setUsername("alex@example.com");
    account.setName("Alex Johnson");
    account.setProfile("Former competitive boxer teaching proper " +
                                     "technique and fitness through boxing");

    account.setPasswordHash(passwordEncoder.encode("alex"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));

    session.store(account, "UserAccounts/2-A");
  }

//  @Test
//  void createOneClass() {
//
//    ClassOffering offering = new ClassOffering();
//    offering.setClassName("Demo Class");
//    offering.setLevel("Beginner");
//    offering.setSlots(10);
//    offering.setClassType(ClassType.cardio);
//    offering.setInstructorId("Instructor/1-A");
//
//    ClassOffering.Schedule schedule = new ClassOffering.Schedule();
//    schedule.setStartWeek(LocalDate.now());
//    schedule.setNumberOfWeeks(2);
//    schedule.setTime(LocalTime.of(10, 0));
//    schedule.setDuration(60);
//    schedule.setDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
//    offering.setSchedule(schedule);
//
//    session.store(offering);
//  }

  @Test
  void createClassSchedule1A() {

    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Morning Flow Yoga");
    schedule.setClassType(ClassType.yoga);
    schedule.setLevel("Beginner");

    schedule.setStartWeek(LocalDate.of(2025, 11, 30));
    schedule.setNumberOfWeeks(2);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(10, 0));
    schedule.setInstructorId("UserAccounts/1-A");

    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation("Studio 1-A");

    session.store(schedule, "ClassSchedule/1-A");
  }

  @Test
  void createClassSchedule2A() {

    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Boxing Class");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("Advanced");

    schedule.setStartWeek(LocalDate.of(2025, 11, 30));
    schedule.setNumberOfWeeks(2);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(10, 0));
    schedule.setInstructorId("UserAccounts/2-A");

    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation("Studio 2");

    session.store(schedule, "ClassSchedule/2-A");
  }

  @Test
  void createPerpetualClassSchedule3A() {

    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Latin Dance Fitness");
    schedule.setClassType(ClassType.dance);
    schedule.setLevel("Advanced");

    schedule.setStartWeek(LocalDate.of(2025, 11, 30));
    schedule.setNumberOfWeeks(0);  // Perpetual schedule
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(13, 0));
    schedule.setInstructorId("UserAccounts/1-A");

    schedule.setDuration(60);
    schedule.setLocation("Studio 1");
    schedule.setSlots(-1);

    session.store(schedule, "ClassSchedule/3-A");
  }

  @Test
  void getClassesFromSchedule() {
    ClassSchedule schedule = session.load(ClassSchedule.class, "ClassSchedule/1-A");
//     System.out.println(schedule);
    log.info(schedule.toString());

    LocalDate startDate = LocalDate.of(2025, 11, 30);
    LocalDate endDate = LocalDate.of(2025, 12, 14);
    List<ScheduledClass> classes =
        schedule.getScheduledClasses(startDate, endDate);

    for (ScheduledClass aClass : classes) {
      log.info("Class: {}", aClass);
    }

  }

}
