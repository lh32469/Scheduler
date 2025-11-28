package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.security.VerificationToken;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RavenVerificationTokenRepository {

  private final RavenDB ravenDB;

  public void save(VerificationToken token) {
    try (IDocumentSession session = ravenDB.openSession()) {
      session.store(token);
      session.saveChanges();
    }
  }

  public VerificationToken findByToken(String tokenValue) {
    try (IDocumentSession session = ravenDB.openSession()) {
      return session.query(VerificationToken.class)
                    .whereEquals("token", tokenValue)
                    .firstOrDefault();
    } catch (Exception e) {
      log.error("Failed to lookup VerificationToken: {}", e.toString());
      return null;
    }
  }

  public void delete(String id) {
    try (IDocumentSession session = ravenDB.openSession()) {
      VerificationToken vt = session.load(VerificationToken.class, id);
      if (vt != null) {
        session.delete(vt);
        session.saveChanges();
      }
    }
  }
}
