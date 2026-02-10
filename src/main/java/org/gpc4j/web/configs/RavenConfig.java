package org.gpc4j.web.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.ServerOperationExecutor;
import org.gpc4j.web.dto.Banner;
import org.gpc4j.web.security.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class RavenConfig {

  public static final String DB_NAME = "RavenDB";

  private final PasswordEncoder passwordEncoder;

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
  private final List<String> urls;

  /**
   * Default databaseName
   */
  private final String defaultDatabaseName;

  public RavenConfig(PasswordEncoder passwordEncoder,
                     @Value("${ravendb.database}") String dbName,
                     @Value("${ravendb.urls}") List<String> urls) {
    this.passwordEncoder = passwordEncoder;
    this.defaultDatabaseName = dbName;
    this.urls = urls;
    log.info("URLs: {}", urls);
  }

  @Bean
  public DocumentStore documentStore() {
    DocumentStore store = new DocumentStore(urls.toArray(new String[0]), null);

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
   * <p>
   * This method also checks if the target database exists and creates it if not.
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
    try (ServerOperationExecutor server = store.maintenance().server()) {
      DatabaseRecord record =
          server.send(new GetDatabaseRecordOperation(databaseName));
      if (record == null) {
        log.info("Database '{}' not found. Creating...", databaseName);

        DatabaseRecord newRecord = new DatabaseRecord(databaseName);

        // Set up topology with all 3 nodes in the database group
        DatabaseTopology topology = new DatabaseTopology();
//        // All 3 nodes in the group
//        topology.setMembers(Arrays.asList("A", "B"));
//        // But only 2 will have data at any time
        topology.setReplicationFactor(2);
        newRecord.setTopology(topology);

        // Enable expiration
        ExpirationConfiguration expirationConfig = new ExpirationConfiguration();
        expirationConfig.setDisabled(false);
        // License limit of 36 hours in seconds
        expirationConfig.setDeleteFrequencyInSec(129600L);
        newRecord.setExpiration(expirationConfig);

        server.send(new CreateDatabaseOperation(newRecord, 2));
        log.info("Database '{}' created successfully.", databaseName);

        try (IDocumentSession session = store.openSession(databaseName)) {
          // Create default Admin and Banner
          UserAccount admin = new UserAccount();
          admin.setUsername("admin");
          admin.setName("Administrator");
          admin.setPasswordHash(passwordEncoder.encode("lh32469"));
          admin.setEnabled(true);
          admin.setAccountNonLocked(true);
          admin.setRoles(List.of("ROLE_ADMIN"));

          session.store(admin, "Admin/1-A");

          Banner banner = new Banner();
          banner.setCompanyName("Your Company");
          banner.setTitle("Description here");
          banner.setSubTitle("Subtitle here");
          banner.setTabTitle("Scheduler");

          session.store(banner, "Banners/1-A");

          session.saveChanges();
        }
      } else {
        log.debug("Database '{}' already exists.", databaseName);
      }
    } catch (Exception e) {
      log.error("Error while checking/creating database '{}': {}",
                databaseName,
                e.getMessage(),
                e);
      throw new RuntimeException("Failed to ensure database exists: "
                                     + databaseName, e);
    }

    IDocumentSession session = store.openSession(databaseName);

    // For transactions
    session.advanced().setUseOptimisticConcurrency(true);

    return session;
  }

}
