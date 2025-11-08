package org.gpc4j.web.repository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Default RavenDB implementation active when the 'k8s' profile is NOT active.
 * Resolves the database name from the incoming request's host so that
 * multi-tenant hosts map to their own RavenDB databases.
 */
@Slf4j
@Component
@RequestScope
@Profile("!k8s")
public class DefaultRavenDB implements RavenDB {

  private final RavenDocumentStoreCache cache;

  @Value("${ravendb.database}")
  private String databaseName;

  public DefaultRavenDB(HttpServletRequest request,
                        RavenDocumentStoreCache cache) {
    this.cache = cache;
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
