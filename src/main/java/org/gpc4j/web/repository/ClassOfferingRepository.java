package org.gpc4j.web.repository;

import net.ravendb.client.documents.session.IDocumentSession;
import org.gpc4j.web.api.ClassOffering;

import java.util.List;

/**
 * Repository abstraction for querying and persisting {@link ClassOffering} documents.
 */
public interface ClassOfferingRepository {

  /**
   * Return a paged list of {@link ClassOffering} documents.
   *
   * @param page 1-based page index
   * @param size page size (max implementation-dependent)
   * @return list of offerings for the given page
   */
  List<ClassOffering> list(int page, int size);

  /**
   * Load a single newOffering by its RavenDB id.
   *
   * @param id RavenDB document id
   * @return the newOffering or null if not found
   */
  ClassOffering findById(String id);

  /**
   * Persist the given {@link ClassOffering}.
   * If the newOffering contains a non-null {@code customerInfo.bookingId}, it will be used as the id.
   * Otherwise RavenDB will generate an id.
   *
   * @param offering the document to store
   * @return the stored document id
   */
  String save(ClassOffering offering);

  /**
   * Saves the given {@link ClassOffering} document to the provided RavenDB session.
   * This method leverages the specified {@code IDocumentSession} to persist the
   * document. If the {@link ClassOffering} contains a {@code customerInfo.bookingId},
   * it will be used as the document ID. Otherwise, RavenDB will generate the ID.
   *
   * @param session the RavenDB session to use for persisting the document
   * @param offering the {@link ClassOffering} to be saved
   * @return the ID of the persisted document
   */
  String save(IDocumentSession session, ClassOffering offering);
}
