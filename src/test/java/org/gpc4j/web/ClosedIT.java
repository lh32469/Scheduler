package org.gpc4j.web;

import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.dto.Closed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@SpringBootTest
public class ClosedIT {

  @Autowired
  IDocumentSession session;

  @Test
  void closedMLK() {
    // Create a list of business hours for that day
    List<Closed.Hours> hours =
        List.of(new Closed.Hours(LocalTime.of(9, 0), LocalTime.of(17, 0)));

    LocalDate mlk = LocalDate.of(2026, 1, 19);
    Closed closed = new Closed(mlk, hours);
    session.store(closed);
    session.saveChanges();
  }

  @Test
  void closedEntireDay() {
    // Create a list of business hours for that day
    List<Closed.Hours> hours =
        List.of(new Closed.Hours(LocalTime.of(9, 0), LocalTime.of(17, 0)));

    LocalDate date = LocalDate.of(2026, 5, 25);
    Closed closed = new Closed(date, hours);
    session.store(closed);
    session.saveChanges();
  }

}
