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
package org.eclipse.lemminx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.settings.XMLGeneralClientSettings;
import org.eclipse.lemminx.settings.XMLWorkspaceIndexSettings;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileRename;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link XMLWorkspaceService}: workspace/willRenameFiles
 * and workspace/didChangeWatchedFiles updating the bloom index.
 */
public class XMLWorkspaceServiceTest {

	@TempDir
	Path tempDir;

	private MockXMLLanguageServer server;
	private XMLWorkspaceService workspaceService;

	@BeforeEach
	public void setUp() {
		server = new MockXMLLanguageServer();
		workspaceService = (XMLWorkspaceService) server.getWorkspaceService();
		// Enable the workspace index (disabled by default; must be opted in via settings).
		// Storing this as a SETTINGS save context before initializeIfNeeded() means it is
		// replayed to each extension when it registers, so WorkspaceIndexPlugin sees enabled=true.
		XMLGeneralClientSettings settings = new XMLGeneralClientSettings();
		XMLWorkspaceIndexSettings indexSettings = new XMLWorkspaceIndexSettings();
		indexSettings.setEnabled(true);
		settings.setWorkspaceIndex(indexSettings);
		server.getXMLLanguageService().doSave(new XMLAssert.SettingsSaveContext(settings));
	}

	// --- willRenameFiles ---

	@Test
	public void testWillRenameFiles_updatesReferences() throws IOException, ExecutionException, InterruptedException {
		Path xml = writeFile("order.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>");
		Path oldSchema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
		Path newSchema = tempDir.resolve("types.xsd");

		WorkspaceIndex index = server.getWorkspaceIndex();
		index.indexFile(xml);
		index.indexFile(oldSchema);

		RenameFilesParams params = new RenameFilesParams(List.of(
				new FileRename(oldSchema.toUri().toString(), newSchema.toUri().toString())));

		WorkspaceEdit edit = workspaceService.willRenameFiles(params).get();

		assertNotNull(edit);
		Map<String, List<TextEdit>> changes = edit.getChanges();
		assertEquals(1, changes.size());
		List<TextEdit> edits = changes.get(xml.toUri().toString());
		assertNotNull(edits);
		assertEquals(1, edits.size());
		assertEquals("types.xsd", edits.get(0).getNewText());
	}

	@Test
	public void testWillRenameFiles_noReferences_returnsNull()
			throws IOException, ExecutionException, InterruptedException {
		Path xml = writeFile("empty.xml", "<?xml version=\"1.0\"?><root/>");
		Path oldSchema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
		Path newSchema = tempDir.resolve("types.xsd");

		WorkspaceIndex index = server.getWorkspaceIndex();
		index.indexFile(xml);
		index.indexFile(oldSchema);

		RenameFilesParams params = new RenameFilesParams(List.of(
				new FileRename(oldSchema.toUri().toString(), newSchema.toUri().toString())));

		WorkspaceEdit edit = workspaceService.willRenameFiles(params).get();
		assertNull(edit);
	}

	// --- didChangeWatchedFiles: index update ---

	@Test
	public void testDidChangeWatchedFiles_created_addsToIndex() throws IOException {
		Path xml = writeFile("new.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<root/>");

		WorkspaceIndex index = server.getWorkspaceIndex();
		assertEquals(0, index.size());

		DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Created)));
		workspaceService.didChangeWatchedFiles(params);

		assertEquals(1, index.size());
		List<String> refs = index.findReferencingFiles("schema.xsd");
		assertEquals(1, refs.size());
	}

	@Test
	public void testDidChangeWatchedFiles_deleted_removesFromIndex() throws IOException {
		Path xml = writeFile("order.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>");

		WorkspaceIndex index = server.getWorkspaceIndex();
		index.indexFile(xml);
		assertEquals(1, index.size());

		DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Deleted)));
		workspaceService.didChangeWatchedFiles(params);

		assertEquals(0, index.size());
	}

	@Test
	public void testDidChangeWatchedFiles_changed_updatesIndex() throws IOException {
		Path xml = writeFile("order.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"old.xsd\"?>\n" +
				"<order/>");

		WorkspaceIndex index = server.getWorkspaceIndex();
		index.indexFile(xml);
		assertEquals(1, index.findReferencingFiles("old.xsd").size());
		assertEquals(0, index.findReferencingFiles("new.xsd").size());

		// Overwrite the file with a new reference
		Files.writeString(xml,
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"new.xsd\"?>\n" +
				"<order/>");

		DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(List.of(
				new FileEvent(xml.toUri().toString(), FileChangeType.Changed)));
		workspaceService.didChangeWatchedFiles(params);

		assertEquals(0, index.findReferencingFiles("old.xsd").size());
		assertEquals(1, index.findReferencingFiles("new.xsd").size());
	}

	private Path writeFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}
}
