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
public class CreateInstructorsIT {

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
  void createInstructorJohnSmith() {
    UserAccount account = new UserAccount();
    account.setUsername("john.smith@example.com");
    account.setName("John Smith");
    account.setProfile(
        "Certified yoga instructor with 10 years of experience in Vinyasa and Hatha " +
            "yoga");
    account.setPasswordHash(passwordEncoder.encode("john"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/3-A");
  }

  @Test
  void createInstructorSarahWilson() {
    UserAccount account = new UserAccount();
    account.setUsername("sarah.wilson@example.com");
    account.setName("Sarah Wilson");
    account.setProfile(
        "Professional dance instructor specializing in contemporary and jazz");
    account.setPasswordHash(passwordEncoder.encode("sarah"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/4-A");
  }

  @Test
  void createInstructorMikeJohnson() {
    UserAccount account = new UserAccount();
    account.setUsername("mike.johnson@example.com");
    account.setName("Mike Johnson");
    account.setProfile(
        "CrossFit certified trainer with expertise in strength and conditioning");
    account.setPasswordHash(passwordEncoder.encode("mike"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/5-A");
  }

  @Test
  void createInstructorEmilyDavis() {
    UserAccount account = new UserAccount();
    account.setUsername("emily.davis@example.com");
    account.setName("Emily Davis");
    account.setProfile("Pilates instructor focusing on core strength and flexibility");
    account.setPasswordHash(passwordEncoder.encode("emily"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/6-A");
  }

  @Test
  void createInstructorDavidBrown() {
    UserAccount account = new UserAccount();
    account.setUsername("david.brown@example.com");
    account.setName("David Brown");
    account.setProfile("Martial arts expert teaching self-defense and fitness");
    account.setPasswordHash(passwordEncoder.encode("david"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/7-A");
  }

  @Test
  void createInstructorLisaAnderson() {
    UserAccount account = new UserAccount();
    account.setUsername("lisa.anderson@example.com");
    account.setName("Lisa Anderson");
    account.setProfile(
        "Specialized in high-intensity interval training and cardio workouts");
    account.setPasswordHash(passwordEncoder.encode("lisa"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/8-A");
  }

  @Test
  void createInstructorJamesWilson() {
    UserAccount account = new UserAccount();
    account.setUsername("james.wilson@example.com");
    account.setName("James Wilson");
    account.setProfile("Swimming instructor with competitive background");
    account.setPasswordHash(passwordEncoder.encode("james"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/9-A");
  }

  @Test
  void createInstructorMariaGarcia() {
    UserAccount account = new UserAccount();
    account.setUsername("maria.garcia@example.com");
    account.setName("Maria Garcia");
    account.setProfile("Zumba instructor specializing in Latin dance fitness");
    account.setPasswordHash(passwordEncoder.encode("maria"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/10-A");
  }

  @Test
  void createInstructorRobertTaylor() {
    UserAccount account = new UserAccount();
    account.setUsername("robert.taylor@example.com");
    account.setName("Robert Taylor");
    account.setProfile("Personal trainer focusing on weight management and nutrition");
    account.setPasswordHash(passwordEncoder.encode("robert"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/11-A");
  }

  @Test
  void createInstructorJenniferMoore() {
    UserAccount account = new UserAccount();
    account.setUsername("jennifer.moore@example.com");
    account.setName("Jennifer Moore");
    account.setProfile("Meditation and mindfulness coach with yoga background");
    account.setPasswordHash(passwordEncoder.encode("jennifer"));
    account.setEnabled(true);
    account.setAccountNonLocked(true);
    account.setRoles(List.of("ROLE_INSTRUCTOR"));
    session.store(account, "UserAccounts/12-A");
  }

}
