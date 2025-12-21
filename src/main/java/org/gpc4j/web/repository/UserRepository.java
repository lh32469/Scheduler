package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.security.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

  private final RavenDB ravenDB;

  public UserAccount findByUsername(String username) {
    log.info("Finding UserAccount by username={}", username);
    try (IDocumentSession session = ravenDB.openSession()) {
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

  public List<UserAccount> findAllInstructors() {
    log.info("Finding All Instructors");
    try (IDocumentSession session = ravenDB.openSession()) {
      List<UserAccount> instructors = session.query(UserAccount.class)
                                             .whereIn("roles",
                                                      List.of("ROLE_INSTRUCTOR"))
                                             .toList();
      log.info("Found {} instructors", instructors.size());
      return instructors;
    } catch (Exception e) {
      log.error("Failed to query instructors: {}", e.toString());
      return List.of();
    }

  }

  public UserAccount findById(String id) {
    try (IDocumentSession session = ravenDB.openSession()) {
      return session.load(UserAccount.class, id);
    } catch (Exception e) {
      log.error("Failed to load UserAccount by id={}: {}", id, e.toString());
      return null;
    }
  }

  public List<UserAccount> findUsers(List<String> ids) {
    log.info("Finding users with ids: " + ids);

    try (IDocumentSession session = ravenDB.openSession()) {
      List<UserAccount> users = session.query(UserAccount.class)
                                       .whereIn("id", ids)
                                       .toList();
      log.info("Found {} Users", users.size());
      return users;
    } catch (Exception e) {
      log.error("Failed to query Users: {}", e.toString());
      return List.of();
    }

  }

  public String save(UserAccount user) {
    try (IDocumentSession session = ravenDB.openSession()) {
      session.store(user);
      session.saveChanges();
      String id = session.advanced().getDocumentId(user);
      log.info("Stored UserAccount id={} username={}", id, user.getUsername());
      return id;
    }
  }

  public long count() {
    try (IDocumentSession session = ravenDB.openSession()) {
      List<UserAccount> users = session.query(UserAccount.class).take(1).toList();
      if (users.isEmpty()) {
        return 0L;
      }
      // Use a rough estimate by querying index stats if needed; here we just check
      // existence.
      return 1L;
    }
  }

}
