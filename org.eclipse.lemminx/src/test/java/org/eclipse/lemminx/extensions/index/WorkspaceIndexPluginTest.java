/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lemminx.services.extensions.IWatchedFilesContext;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WorkspaceIndexPlugin} behavior — fix coverage for
 * didChangeWatchedFiles disabled guard and doSave disable/enable transitions.
 */
public class WorkspaceIndexPluginTest {

	@TempDir
	Path tempDir;

	private WorkspaceIndexPlugin plugin;
	private WorkspaceIndex workspaceIndex;

	@BeforeEach
	public void setUp() throws Exception {
		plugin = new WorkspaceIndexPlugin();
		workspaceIndex = getIndex(plugin);
	}

	// Fix: didChangeWatchedFiles — Created/Changed events must NOT index when plugin is disabled

	@Test
	public void testDidChangeWatchedFiles_created_doesNotIndexWhenDisabled() throws Exception {
		setEnabled(plugin, false);
		Path xml = writeFile("order.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<order/>");

		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Created)), NO_OP_CONTEXT);

		assertEquals(0, workspaceIndex.size(),
				"Created event must not index file when plugin is disabled");
	}

	@Test
	public void testDidChangeWatchedFiles_changed_doesNotIndexWhenDisabled() throws Exception {
		setEnabled(plugin, false);
		Path xml = writeFile("order.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<order/>");

		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Changed)), NO_OP_CONTEXT);

		assertEquals(0, workspaceIndex.size(),
				"Changed event must not index file when plugin is disabled");
	}

	// Fix: didChangeWatchedFiles — Deleted events must STILL remove from index even when disabled

	@Test
	public void testDidChangeWatchedFiles_deleted_removesFromIndexEvenWhenDisabled() throws Exception {
		// Pre-populate index (simulating previously indexed file)
		Path xml = writeFile("order.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<order/>");
		workspaceIndex.indexFile(xml); // index directly, bypassing plugin enabled check
		assertEquals(1, workspaceIndex.size());

		// Disable the plugin AFTER indexing
		setEnabled(plugin, false);

		// Deleted event must still remove the file
		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Deleted)), NO_OP_CONTEXT);

		assertEquals(0, workspaceIndex.size(),
				"Deleted event must remove file even when plugin is disabled");
		assertTrue(workspaceIndex.findReferencingFiles("schema.xsd").isEmpty());
	}

	// Fix: didChangeWatchedFiles — Created/Changed events DO index when plugin is enabled (regression)

	@Test
	public void testDidChangeWatchedFiles_created_indexesWhenEnabled() throws Exception {
		setEnabled(plugin, true);
		Path xml = writeFile("order.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<order/>");

		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Created)), NO_OP_CONTEXT);

		assertEquals(1, workspaceIndex.size(),
				"Created event must index file when plugin is enabled");
		assertFalse(workspaceIndex.findReferencingFiles("schema.xsd").isEmpty());
	}

	// Fix: requestCodeLensRefresh must only be called when grammar references are found

	@Test
	public void testDidChangeWatchedFiles_created_requestsRefreshOnlyWhenGrammarRefsFound() throws Exception {
		setEnabled(plugin, true);

		// File WITH grammar reference → refresh must be requested
		Path withRef = writeFile("with-ref.xml", "<?xml-model href=\"schema.xsd\"?>\n<root/>");
		TrackingContext ctxWithRef = new TrackingContext();
		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(withRef.toUri().toString(), FileChangeType.Created)), ctxWithRef);
		assertTrue(ctxWithRef.codeLensRefreshRequested,
				"Refresh must be requested when file contains grammar references");

		// File WITHOUT grammar reference → no refresh
		Path noRef = writeFile("no-ref.xml", "<root/>");
		TrackingContext ctxNoRef = new TrackingContext();
		plugin.didChangeWatchedFiles(List.of(
				new FileEvent(noRef.toUri().toString(), FileChangeType.Created)), ctxNoRef);
		assertFalse(ctxNoRef.codeLensRefreshRequested,
				"Refresh must NOT be requested when file has no grammar references");
	}

	// --- updateCatalogPaths: catalog eviction vs workspace-root containment ---

	@Test
	public void testUpdateCatalogPaths_doesNotEvictCatalogInsideWorkspaceRoot() throws Exception {
		// catalog.xml is inside tempDir (the workspace root)
		Path catalog = writeFile("catalog.xml",
				"<?xml version=\"1.0\"?>\n<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\"/>");
		workspaceIndex.indexFile(catalog);
		assertEquals(1, workspaceIndex.size());

		// Workspace folder = tempDir; catalog is under it
		setWorkspaceFolders(plugin, List.of(asWorkspaceFolder(tempDir)));
		setCurrentCatalogPaths(plugin, new String[]{catalog.toUri().toString()});

		// Remove catalog from xml.catalogs — it's still under the workspace root
		invokeUpdateCatalogPaths(plugin, new String[0]);

		assertEquals(1, workspaceIndex.size(),
				"Catalog inside workspace root must stay indexed when removed from xml.catalogs");
	}

	@Test
	public void testUpdateCatalogPaths_evictsCatalogOutsideWorkspaceRoot() throws Exception {
		// catalog.xml is inside tempDir, but workspace folders is empty → "outside"
		Path catalog = writeFile("catalog.xml",
				"<?xml version=\"1.0\"?>\n<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\"/>");
		workspaceIndex.indexFile(catalog);
		assertEquals(1, workspaceIndex.size());

		// No workspace folders and no rootUri → catalog is not under any workspace root
		setWorkspaceFolders(plugin, List.of());
		setCurrentCatalogPaths(plugin, new String[]{catalog.toUri().toString()});

		invokeUpdateCatalogPaths(plugin, new String[0]);

		assertEquals(0, workspaceIndex.size(),
				"Catalog outside all workspace roots must be evicted when removed from xml.catalogs");
	}

	// --- Helpers ---

	/** No-op context for tests that don't need to observe the refresh flags. */
	private static final IWatchedFilesContext NO_OP_CONTEXT = new IWatchedFilesContext() {
		@Override public void requestCodeLensRefresh() {}
		@Override public boolean isCodeLensRefreshRequested() { return false; }
		@Override public void requestDiagnosticsRefresh() {}
		@Override public boolean isDiagnosticsRefreshRequested() { return false; }
	};

	/** Tracking context that records whether code lens or diagnostics refresh was requested. */
	private static class TrackingContext implements IWatchedFilesContext {
		boolean codeLensRefreshRequested;
		boolean diagnosticsRefreshRequested;
		@Override public void requestCodeLensRefresh() { codeLensRefreshRequested = true; }
		@Override public boolean isCodeLensRefreshRequested() { return codeLensRefreshRequested; }
		@Override public void requestDiagnosticsRefresh() { diagnosticsRefreshRequested = true; }
		@Override public boolean isDiagnosticsRefreshRequested() { return diagnosticsRefreshRequested; }
	}

	private static WorkspaceIndex getIndex(WorkspaceIndexPlugin plugin) throws Exception {
		Field f = WorkspaceIndexPlugin.class.getDeclaredField("workspaceIndex");
		f.setAccessible(true);
		return (WorkspaceIndex) f.get(plugin);
	}

	private static void setEnabled(WorkspaceIndexPlugin plugin, boolean enabled) throws Exception {
		Field f = WorkspaceIndexPlugin.class.getDeclaredField("enabled");
		f.setAccessible(true);
		f.set(plugin, enabled);
	}

	private static void setWorkspaceFolders(WorkspaceIndexPlugin plugin,
			List<WorkspaceFolder> folders) throws Exception {
		Field f = WorkspaceIndexPlugin.class.getDeclaredField("workspaceFolders");
		f.setAccessible(true);
		f.set(plugin, folders);
	}

	private static void setCurrentCatalogPaths(WorkspaceIndexPlugin plugin,
			String[] paths) throws Exception {
		Field f = WorkspaceIndexPlugin.class.getDeclaredField("currentCatalogPaths");
		f.setAccessible(true);
		f.set(plugin, paths);
	}

	private static void invokeUpdateCatalogPaths(WorkspaceIndexPlugin plugin,
			String[] newPaths) throws Exception {
		Method m = WorkspaceIndexPlugin.class.getDeclaredMethod("updateCatalogPaths", String[].class);
		m.setAccessible(true);
		m.invoke(plugin, (Object) newPaths);
	}

	private static WorkspaceFolder asWorkspaceFolder(Path dir) {
		WorkspaceFolder folder = new WorkspaceFolder();
		folder.setUri(dir.toUri().toString());
		return folder;
	}

	private Path writeFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}

}
