package org.gpc4j.web.repository;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class RavenDB {

  private final RavenDocumentStoreCache cache;
  private final HttpServletRequest request;

  @Value("${ravendb.database}")
  private String databaseName;

  public RavenDB(HttpServletRequest request,
                 RavenDocumentStoreCache cache) {

    this.cache = cache;
    this.request = request;
  }

  @PostConstruct
  public void postConstruct() {
    final String hostname = request.getHeader("X-Forwarded-Host");

    if (Objects.isNull(hostname)) {
      log.warn("X-Forwarded-Host header not found in request");
    } else {
      this.databaseName = hostname;
    }
    log.debug(this + ": " + databaseName);
  }

  public IDocumentStore getDocumentStore() {
    return cache.getDocumentStore(databaseName);
  }

  public IDocumentSession openSession() {
    return getDocumentStore().openSession();
  }

}
