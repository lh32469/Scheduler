package org.gpc4j.web.repository;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;

/**
 * Abstraction for accessing RavenDB document stores and sessions.
 * Two implementations are provided, selected by Spring profiles:
 * - Default (active when profile 'k8s' is NOT active): resolves
 * database per request  host.
 * - K8s (active when profile 'k8s' IS active): uses configured database name.
 */
public interface RavenDB {

  IDocumentStore getDocumentStore();

  IDocumentSession openSession();

}
