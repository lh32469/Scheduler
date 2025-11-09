package org.gpc4j.web.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * The {@code RavenDocumentStoreCache} class is responsible for managing RavenDB
 * DocumentStore instances and provides a caching mechanism for efficient usage.
 * It uses Spring's {@code @Cacheable} annotation to store and retrieve instances
 * of DocumentStore for a given database name.
 * <p>
 * The database URL is specified through the external configuration property
 * {@code ravendb.url}.
 * <p>
 * This class also ensures proper setup of the Jackson {@code ObjectMapper} for
 * correct handling of Java {@code java.time} types.
 * <p>
 * Logging is utilized to provide runtime feedback when DocumentStore instances
 * are created and initialized.
 */
@Slf4j
@Component
public class RavenDocumentStoreCache {

  @Value("${ravendb.url}")
  private String url;

  @Cacheable(value = "documentStore", key = "#databaseName")
  public IDocumentStore getDocumentStore(String databaseName) {
    DocumentStore store = new DocumentStore(url, databaseName);

    // Configure Jackson ObjectMapper for proper DateTime handling
    ObjectMapper mapper = store.getConventions().getEntityMapper();
    mapper.registerModule(new JavaTimeModule());

    log.info("DocumentStore created for databaseName={}", databaseName);
    store.initialize();
    return store;
  }

}
