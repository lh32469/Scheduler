package org.gpc4j.web.repository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.annotation.RequestScope;

/**
 * The RavenDB class provides access to RavenDB sessions and document stores.
 * <p>
 * This class is a Spring component with a request scope lifecycle. It uses
 * the database name from the HttpServletRequest to interact with the RavenDB
 * document store. The class facilitates the retrieval of an {@code IDocumentStore}
 * instance and the creation of {@code IDocumentSession} instances for the given
 * database.
 * <p>
 * Dependencies:
 * - RavenDocumentStoreCache: Used to retrieve and cache instances of
 * {@code IDocumentStore}.
 * - HttpServletRequest: Provides the local name used as the database name for
 * document store retrieval.
 * <p>
 * Logging:
 * - Logs a message during initialization to indicate the target database name.
 * <p>
 * Thread Safety:
 * - This class is thread-safe within the request scope as each HTTP request
 * creates a new instance.
 * <p>
 * Methods:
 * - getDocumentStore(): Returns the IDocumentStore for the current request's
 * database.
 * - openSession(): Opens a new IDocumentSession from the document store.
 */
@Slf4j
@Component
@RequestScope
public class RavenDB {

  private final String databaseName;

  private final RavenDocumentStoreCache cache;

  public RavenDB(HttpServletRequest request,
                 RavenDocumentStoreCache cache) {

    String hostname = request.getHeader("X-Forwarded-Host");
    log.info("X-Forwarded-Host: {}", hostname);

    // Fall back to Host header if X-Forwarded-Host isn't set
    if (hostname == null || hostname.isEmpty()) {
      hostname = request.getHeader("Host");
      log.info("Host: {}", hostname);
    }

    log.info("Request came through: {}", hostname);

    this.cache = cache;
    databaseName = request.getLocalName();
    if (log.isDebugEnabled()) {
      log.debug(this + ": " + request.getLocalName());
    }
  }

  public IDocumentStore getDocumentStore() {
    return cache.getDocumentStore(databaseName);
  }

  public IDocumentSession openSession() {
    return getDocumentStore().openSession();
  }

}
