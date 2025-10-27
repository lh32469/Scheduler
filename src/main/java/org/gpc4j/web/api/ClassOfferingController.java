package org.gpc4j.web.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.repository.ClassOfferingRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller to fetch ClassOffering documents from RavenDB.
 * <p>
 * Endpoint: GET /api/offerings
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/offerings", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ClassOfferingController {

  private final ClassOfferingRepository repository;

  /**
   * Returns a list of ClassOffering documents from RavenDB.
   *
   * @param page 1-based page index (optional, default 1)
   * @param size page size (optional, default 100, max 500)
   */
  @GetMapping
  public List<ClassOffering> list(
      @RequestParam(name = "page", required = false, defaultValue = "1") int page,
      @RequestParam(name = "size", required = false, defaultValue = "100") int size
  ) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(Math.max(1, size), 500);
    int skip = (safePage - 1) * safeSize;

    return repository.list(skip, safeSize);

  }

}
