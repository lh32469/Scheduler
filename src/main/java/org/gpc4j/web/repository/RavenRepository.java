package org.gpc4j.web.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.session.IDocumentSession;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RavenRepository<T> {

  private final RavenDB ravenDB;
  private final Class<T> entityClass;

  public Optional<T> findById(String id) {

    try (IDocumentSession session = ravenDB.openSession()) {
      return Optional.ofNullable(session.load(entityClass, id));
    } catch (Exception e) {
      log.error("Failed to load {} by id={}", entityClass.getSimpleName(), id, e);
      return Optional.empty();
    }
  }

}
