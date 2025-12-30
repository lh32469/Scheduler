package org.gpc4j.web.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Objects;

@Slf4j
@Configuration
public class RavenConfig {

  public static final String DB_NAME = "RavenDB";

  /**
   * Represents the RavenDB server URL configuration. This is used to initialize
   * the connection to a RavenDB instance by providing the base server URL.
   * <p>
   * The URL is typically provided through an external configuration, such as
   * environment variables, property files, or application context, to enable
   * dynamic configuration and deployment flexibility.
   * <p>
   * Example configurations might include domain names or IP addresses in the
   * format "http://<hostname>:<port>" or "https://<hostname>:<port>".
   */
  private final String url;

  /**
   * Default databaseName
   */
  private final String defaultDatabaseName;

  public RavenConfig(@Value("${ravendb.database}") String dbName,
                     @Value("${ravendb.url}") String url) {
    this.defaultDatabaseName = dbName;
    this.url = url;
  }

  @Bean
  public DocumentStore documentStore() {
    DocumentStore store = new DocumentStore(url, null);

    // Configure Jackson ObjectMapper for proper DateTime handling
    ObjectMapper mapper = store.getConventions().getEntityMapper();
    mapper.registerModule(new JavaTimeModule());

    log.info("DocumentStore created");
    store.initialize();
    return store;
  }

  /**
   * Creates and returns an {@code IDocumentSession} scoped to the current request.
   * The session is tied to the database derived from the "X-Forwarded-Host" header
   * if present in the incoming HTTP request. If the header is missing, it defaults
   * to a predefined database name.
   *
   * @param store   The {@code DocumentStore} instance used to open a session.
   * @param request The {@code HttpServletRequest} containing information about the
   *                incoming HTTP request, including headers.
   * @return A scoped {@code IDocumentSession} instance for interacting with the
   * configured RavenDB database.
   */
  @Bean(destroyMethod = "close")
  @RequestScope
  public IDocumentSession session(DocumentStore store,
                                  HttpServletRequest request) {

    final String url = request.getRequestURL().toString();
    log.debug("Request URL " + url);

    String databaseName = defaultDatabaseName;

    // Get the Ingress hostname forwarded from nginx front-end.
    final String hostname = request.getHeader("X-Forwarded-Host");

    if (Objects.isNull(hostname)) {
      log.debug("X-Forwarded-Host header not found in request");
    } else {
      databaseName = hostname;
    }

    log.debug("Database Name " + databaseName);
    request.setAttribute(DB_NAME, databaseName);
    IDocumentSession session = store.openSession(databaseName);

    // For transactions
    session.advanced().setUseOptimisticConcurrency(true);

    return session;
  }

}
