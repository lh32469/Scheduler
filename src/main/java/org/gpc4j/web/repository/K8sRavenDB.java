package org.gpc4j.web.repository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RavenDB implementation used when the 'k8s' Spring profile is active.
 * Uses a single configured database name instead of deriving it from the request host.
 */
@Slf4j
@Component
@Profile("k8s")
public class K8sRavenDB implements RavenDB {

  private final RavenDocumentStoreCache cache;
  private final String databaseName;

  public K8sRavenDB(HttpServletRequest request,
                    RavenDocumentStoreCache cache) {

    String hostname = request.getHeader("X-Forwarded-Host");
    log.debug("X-Forwarded-Host: {}", hostname);

    this.cache = cache;
    this.databaseName = hostname;
    if (log.isDebugEnabled()) {
      log.debug(this + ": " + databaseName);
    }
  }

  @Override
  public IDocumentStore getDocumentStore() {
    return cache.getDocumentStore(databaseName);
  }

  @Override
  public IDocumentSession openSession() {
    return getDocumentStore().openSession();
  }


}
