package org.gpc4j.web.repository;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Objects;

@Slf4j
@Component
@RequestScope
public class RavenDB {

  private final RavenDocumentStoreCache cache;
  private final HttpServletRequest request;

  @Value("${ravendb.database}")
  private String databaseName;

  @Value("${management.server.port}")
  int managementPort;

  public RavenDB(HttpServletRequest request,
                 RavenDocumentStoreCache cache) {

    this.cache = cache;
    this.request = request;

    log.info(request.getRequestURL() + " " + request.getServerPort());

  }

  @PostConstruct
  public void postConstruct() {

    // Ignore calls to management port
    if (managementPort == request.getServerPort()) {
      return;
    }

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
