/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.extensions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Collections;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IWorkspaceServiceParticipant}
 */
public class WorkspaceServiceParticipantTest extends AbstractCacheBasedTest {

	private static class CaptureWokspaceServiceCalls implements IWorkspaceServiceParticipant {

		public DidChangeWorkspaceFoldersParams didChangeWorkspaceFolders;

		@Override
		public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
			this.didChangeWorkspaceFolders = params;
		}
	}

	private CaptureWokspaceServiceCalls workspaceServiceParticipant;
	private XMLLanguageServer server;

	@BeforeEach
	public void initializeLanguageService() {
		this.server = new MockXMLLanguageServer();
		this.workspaceServiceParticipant = new CaptureWokspaceServiceCalls();
		server.getXMLLanguageService().registerWorkspaceServiceParticipant(this.workspaceServiceParticipant);
	}

	@Test
	public void testWorkspaceFolders() {
		DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams(new WorkspaceFoldersChangeEvent(
				Collections.singletonList(new WorkspaceFolder("added", "added")),
				Collections.singletonList(new WorkspaceFolder("removed", "removed"))));
		server.getWorkspaceService().didChangeWorkspaceFolders(params);
		assertArrayEquals(new String[] { "added" }, workspaceServiceParticipant.didChangeWorkspaceFolders.getEvent().getAdded().stream().map(WorkspaceFolder::getUri).toArray(String[]::new));
		assertArrayEquals(new String[] { "removed" }, workspaceServiceParticipant.didChangeWorkspaceFolders.getEvent().getRemoved().stream().map(WorkspaceFolder::getUri).toArray(String[]::new));
	}
}
