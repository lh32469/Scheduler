package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.ClassOffering;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RavenDB-backed implementation of {@link ClassOfferingRepository}.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RavenClassOfferingRepository implements ClassOfferingRepository {

  private final IDocumentStore documentStore;

  @Override
  public List<ClassOffering> list(int page, int size) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);
    int skip = (safePage - 1) * safeSize;

    try (IDocumentSession session = documentStore.openSession()) {
      List<ClassOffering> offerings = session
          .query(ClassOffering.class)
          .include("instructorId")
          .whereExists("slots")
          .whereNotEquals("slots", 0)
          .orderBy("schedule.start")
          .skip(skip)
          .take(safeSize)
          .toList();
      log.info("Fetched {} ClassOffering documents (page={}, size={})",
               offerings.size(),
               safePage,
               safeSize);

      for (ClassOffering offering : offerings) {
//        log.info("Found ClassOffering: {}", offering);
//        log.info("  Instructor: {}", offering.getInstructorId());

        // Already loaded into session via include above.
        // No request sent to DB.
        UserAccount instructor =
            session.load(UserAccount.class, offering.getInstructorId());

        if (Objects.nonNull(instructor)) {
          offering.setInstructorAccount(instructor);
        }
//        log.info("  Instructor: {}", instructor);
      }

      return offerings;
    } catch (Exception e) {
      log.error("Failed to list ClassOfferings: {}", e.toString());
      return Collections.emptyList();
    }
  }

  @Override
  public ClassOffering findById(String id) {
    try (IDocumentSession session = documentStore.openSession()) {
      ClassOffering found = session.load(ClassOffering.class, id);
      if (found == null) {
        log.info("ClassOffering not found for id={}", id);
      }
      return found;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load ClassOffering id=" + id + ": " + e.getMessage(),
                                      e);
    }
  }

  @Override
  public String save(ClassOffering offering) {
    try (IDocumentSession session = documentStore.openSession()) {

      session.store(offering);
      String id = session.advanced().getDocumentId(offering);

      session.saveChanges();
      log.info("Stored ClassOffering in RavenDB with id={}", id);
      return id;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to store ClassOffering via DocumentStore:" +
                                          " " + e.getMessage(),
                                      e);
    }
  }

  @Override
  public String save(IDocumentSession session, ClassOffering offering) {
    session.store(offering);
    String id = session.advanced().getDocumentId(offering);
    session.saveChanges();
    log.info("Stored ClassOffering in RavenDB with id={}", id);
    return id;
  }

}
