package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RavenUserRepository {

  private final IDocumentStore documentStore;

  public UserAccount findByUsername(String username) {
    log.info("Finding UserAccount by username={}", username);
    try (IDocumentSession session = documentStore.openSession()) {
      UserAccount user = session.query(UserAccount.class)
                                .whereEquals("username", username)
                                .firstOrDefault();
      log.info("Found UserAccount by username={}: {}", username, user);
      return user;
    } catch (Exception e) {
      log.error("Failed to query UserAccount by username={}: {}", username, e.toString());
      return null;
    }
  }

  public String save(UserAccount user) {
    try (IDocumentSession session = documentStore.openSession()) {
      session.store(user);
      session.saveChanges();
      String id = session.advanced().getDocumentId(user);
      log.info("Stored UserAccount id={} username={}", id, user.getUsername());
      return id;
    }
  }

  public long count() {
    try (IDocumentSession session = documentStore.openSession()) {
      List<UserAccount> users = session.query(UserAccount.class).take(1).toList();
      if (users.isEmpty()) return 0L;
      // Use a rough estimate by querying index stats if needed; here we just check existence.
      return 1L;
    }
  }
}
