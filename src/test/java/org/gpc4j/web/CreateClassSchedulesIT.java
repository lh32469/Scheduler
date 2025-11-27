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
public class CreateClassSchedulesIT {

  @Value("${ravendb.database}")
  private String databaseName;

  @Autowired
  RavenDocumentStoreCache cache;

  @Autowired
  PasswordEncoder passwordEncoder;

  IDocumentSession session;

  private List<UserAccount> instructors;

  private UserAccount getRandomInstructor() {
    return instructors.get((int) (Math.random() * instructors.size()));
  }

  /**
   * Select an instructor whose profile best matches the schedule's class type or name.
   * Falls back to a random instructor if no good match is found.
   */
  private UserAccount getInstructorFor(ClassSchedule schedule) {
    if (instructors == null || instructors.isEmpty()) {
      // Just in case setup didn't populate
      return getRandomInstructor();
    }

    String className =
        schedule.getClassName() != null ? schedule.getClassName().toLowerCase() : "";
    ClassType type = schedule.getClassType();

    // Build keyword set based on type and className
    java.util.Set<String> keywords = new java.util.HashSet<>();
    if (type != null) {
      switch (type) {
        case yoga ->
            keywords.addAll(java.util.List.of("yoga", "vinyasa", "hatha", "mindfulness"));
        case pilates -> keywords.add("pilates");
        case spin -> keywords.addAll(java.util.List.of("spin", "cycling"));
        case cardio -> keywords.addAll(java.util.List.of("cardio",
                                                         "hiit",
                                                         "high-intensity",
                                                         "boxing",
                                                         "kickboxing",
                                                         "martial"));
        case strength -> keywords.addAll(java.util.List.of("strength",
                                                           "conditioning",
                                                           "crossfit",
                                                           "body pump"));
        case dance -> keywords.addAll(java.util.List.of("dance", "zumba", "ballet"));
        case meditation -> keywords.addAll(java.util.List.of("meditation",
                                                             "mindfulness",
                                                             "tai chi",
                                                             "yoga"));
        case stretch ->
            keywords.addAll(java.util.List.of("stretch", "flexibility", "senior"));
      }
    }

    // Class name hints
    if (className.contains("zumba") || className.contains("dance")) {
      keywords.addAll(java.util.List.of("zumba", "dance"));
    }
    if (className.contains("boxing") || className.contains("kickboxing")) {
      keywords.addAll(java.util.List.of("boxing", "kickboxing", "martial"));
    }
    if (className.contains("crossfit") || className.contains("body pump") || className.contains(
        "circuit") || className.contains("bootcamp")) {
      keywords.addAll(java.util.List.of("crossfit", "strength", "conditioning"));
    }
    if (className.contains("pilates")) {
      keywords.add("pilates");
    }
    if (className.contains("yoga") || className.contains("tai chi") || className.contains(
        "meditation") || className.contains("mindful")) {
      keywords.addAll(java.util.List.of("yoga", "meditation", "mindfulness", "tai chi"));
    }
    if (className.contains("aqua") || className.contains("swim") || className.contains(
        "pool")) {
      keywords.addAll(java.util.List.of("swim", "swimming"));
    }

    // Try to find matching instructors by profile keywords
    java.util.List<UserAccount> matches = instructors.stream()
                                                     .filter(u -> u.getProfile() != null)
                                                     .filter(u -> {
                                                       String p =
                                                           u.getProfile().toLowerCase();
                                                       for (String kw : keywords) {
                                                         if (p.contains(kw)) {
                                                           return true;
                                                         }
                                                       }
                                                       return false;
                                                     })
                                                     .toList();

    if (!matches.isEmpty()) {
      return matches.get((int) (Math.random() * matches.size()));
    }

    // No match found, fallback to random
    return getRandomInstructor();
  }

  private String getRandomLocation() {
    return "Studio " + ((int) (Math.random() * 5) + 1);
  }

  @BeforeEach
  void setUp() {
    session = cache.getDocumentStore(databaseName)
                   .openSession();

    instructors = session.query(UserAccount.class)
                         .whereEquals("roles", "ROLE_INSTRUCTOR")
                         .toList();
  }

  @AfterEach
  void tearDown() {
    session.saveChanges();
    session.close();
  }

  @Test
  void createYogaClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Power Yoga");
    schedule.setClassDescription(
        "Build strength and flexibility with breath-focused flows.");
    schedule.setClassType(ClassType.yoga);
    schedule.setLevel("Intermediate");
    schedule.setStartWeek(LocalDate.of(2025, 11, 30));
    schedule.setNumberOfWeeks(4);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(9, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(75);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/1-A");
  }

  @Test
  void createPilatesClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Core Pilates");
    schedule.setClassDescription(
        "Improve core strength and posture with classic Pilates mat work.");
    schedule.setClassType(ClassType.pilates);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(10, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/2-A");
  }

  @Test
  void createSpinClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("High Intensity Spin");
    schedule.setClassDescription("High-energy indoor cycling with intervals and climbs.");
    schedule.setClassType(ClassType.spin);
    schedule.setLevel("Advanced");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    schedule.setClassStartTime(LocalTime.of(17, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(25);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/3-A");
  }

  @Test
  void createZumbaClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Zumba Dance");
    schedule.setClassDescription("Dance-based cardio workout set to upbeat Latin " +
                                     "rhythms.");
    schedule.setClassType(ClassType.dance);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY,
                                  DayOfWeek.THURSDAY,
                                  DayOfWeek.SATURDAY));
    schedule.setClassStartTime(LocalTime.of(18, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/4-A");
  }

  @Test
  void createBoxingClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Boxing Fitness");
    schedule.setClassDescription(
        "Boxing-inspired cardio conditioning with combos and mitt work.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("Intermediate");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(4);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(19, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/5-A");
  }

  @Test
  void createStretchClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Dynamic Stretching");
    schedule.setClassDescription("Gentle flexibility session to increase range of " +
                                     "motion.");
    schedule.setClassType(ClassType.stretch);
    schedule.setLevel("Beginner");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(8, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/6-A");
  }

  @Test
  void createHIITClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("HIIT Training");
    schedule.setClassDescription(
        "Short intense intervals with active recovery to boost cardio fitness.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("Advanced");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(7, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(20);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/7-A");
  }

  @Test
  void createMeditationClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Mindful Meditation");
    schedule.setClassDescription(
        "Guided mindfulness and breathing for relaxation and focus.");
    schedule.setClassType(ClassType.meditation);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(12);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(7, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(30);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/8-A");
  }

  @Test
  void createCrossFitClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("CrossFit Training");
    schedule.setClassDescription(
        "Functional strength circuits combining lifts and metabolic conditioning.");
    schedule.setClassType(ClassType.strength);
    schedule.setLevel("Advanced");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY,
                                  DayOfWeek.THURSDAY,
                                  DayOfWeek.SATURDAY));
    schedule.setClassStartTime(LocalTime.of(6, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(15);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/9-A");
  }

  @Test
  void createKickboxingClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Kickboxing");
    schedule.setClassDescription(
        "Strike, kick, and condition in a fast-paced martial fitness class.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("Intermediate");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    schedule.setClassStartTime(LocalTime.of(18, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(20);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/10-A");
  }

  @Test
  void createBodyPumpClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Body Pump");
    schedule.setClassDescription(
        "Full-body barbell workout targeting all major muscle groups.");
    schedule.setClassType(ClassType.strength);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(9, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/11-A");
  }

  @Test
  void createBellyDanceClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Belly Dance");
    schedule.setClassDescription(
        "Learn foundational belly dance movements for fun and fitness.");
    schedule.setClassType(ClassType.dance);
    schedule.setLevel("Beginner");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    schedule.setClassStartTime(LocalTime.of(19, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/12-A");
  }

  @Test
  void createTaiChiClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Tai Chi");
    schedule.setClassDescription(
        "Slow, flowing tai chi forms to improve balance and calm.");
    schedule.setClassType(ClassType.meditation);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(12);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY,
                                  DayOfWeek.THURSDAY,
                                  DayOfWeek.SATURDAY));
    schedule.setClassStartTime(LocalTime.of(8, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/13-A");
  }

  @Test
  void createCircuitTrainingClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Circuit Training");
    schedule.setClassDescription("Rotating stations for strength and cardio " +
                                     "conditioning.");
    schedule.setClassType(ClassType.strength);
    schedule.setLevel("Intermediate");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(12, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(20);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/14-A");
  }

  @Test
  void createAquaAerobicsClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Aqua Aerobics");
    schedule.setClassDescription(
        "Low-impact water workout that builds endurance and strength.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(11, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(-1);
    schedule.setLocation("Pool Area");
    session.store(schedule, "ClassSchedules/15-A");
  }

  @Test
  void createBalletFitClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Ballet Fit");
    schedule.setClassDescription(
        "Ballet-inspired conditioning to improve strength and posture.");
    schedule.setClassType(ClassType.dance);
    schedule.setLevel("Beginner");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    schedule.setClassStartTime(LocalTime.of(16, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(15);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/16-A");
  }

  @Test
  void createSeniorFitnessClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Senior Fitness");
    schedule.setClassDescription(
        "Low-impact mobility, balance, and strength for active seniors.");
    schedule.setClassType(ClassType.stretch);
    schedule.setLevel("Beginner");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(12);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(10, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(45);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/17-A");
  }

  @Test
  void createMorningBootcampClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Morning Bootcamp");
    schedule.setClassDescription(
        "Early-morning total-body bootcamp with drills and circuits.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("Advanced");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(6);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY,
                                  DayOfWeek.WEDNESDAY,
                                  DayOfWeek.FRIDAY));
    schedule.setClassStartTime(LocalTime.of(6, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/18-A");
  }

  @Test
  void createMartialArtsClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Martial Arts");
    schedule.setClassDescription("Fundamentals of striking and movement for all levels.");
    schedule.setClassType(ClassType.cardio);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 2));
    schedule.setNumberOfWeeks(12);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(20, 0));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(20);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/19-A");
  }

  @Test
  void createPreNatalYogaClass() {
    ClassSchedule schedule = new ClassSchedule();
    schedule.setClassName("Pre-natal Yoga");
    schedule.setClassDescription(
        "Safe, supportive yoga to strengthen and relax during pregnancy.");
    schedule.setClassType(ClassType.yoga);
    schedule.setLevel("All Levels");
    schedule.setStartWeek(LocalDate.of(2025, 12, 1));
    schedule.setNumberOfWeeks(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY));
    schedule.setClassStartTime(LocalTime.of(11, 30));
    schedule.setInstructorId(getInstructorFor(schedule).getId());
    schedule.setDuration(60);
    schedule.setSlots(-1);
    schedule.setLocation(getRandomLocation());
    session.store(schedule, "ClassSchedules/20-A");
  }

}
