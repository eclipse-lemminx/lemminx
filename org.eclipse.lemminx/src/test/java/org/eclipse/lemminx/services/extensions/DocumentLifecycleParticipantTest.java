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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.dom.DOMDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IDocumentLifecycleParticipant}
 */
public class DocumentLifecycleParticipantTest extends AbstractCacheBasedTest {

	private static class CaptureDocumentLifecycleCalls implements IDocumentLifecycleParticipant {

		private int didOpen;
		private int didChange;
		private int didSave;
		private int didClose;

		@Override
		public void didOpen(DOMDocument document) {
			if (document != null) {
				this.didOpen++;
			}
		}

		@Override
		public void didChange(DOMDocument document) {
			if (document != null) {
				this.didChange++;
			}
		}

		@Override
		public void didSave(DOMDocument document) {
			if (document != null) {
				this.didSave++;
			}
		}

		@Override
		public void didClose(DOMDocument document) {
			if (document != null) {
				this.didClose++;
			}
		}

		public int getDidOpen() {
			return didOpen;
		}

		public int getDidChange() {
			return didChange;
		}

		public int getDidSave() {
			return didSave;
		}

		public int getDidClose() {
			return didClose;
		}

	}

	private CaptureDocumentLifecycleCalls documentLifecycleParticipant;
	private MockXMLLanguageServer server;

	@BeforeEach
	public void initializeLanguageService() {
		this.server = new MockXMLLanguageServer();
		this.documentLifecycleParticipant = new CaptureDocumentLifecycleCalls();
		server.getXMLLanguageService().registerDocumentLifecycleParticipant(this.documentLifecycleParticipant);
	}

	@Test
	public void didOpen() {
		assertEquals(0, documentLifecycleParticipant.getDidOpen());
		server.didOpen("test.xml", "<foo ");
		waitUntil(() -> documentLifecycleParticipant.getDidOpen() >= 1);
		assertEquals(1, documentLifecycleParticipant.getDidOpen());
	}

	@Test
	public void didChange() {
		assertEquals(0, documentLifecycleParticipant.getDidChange());
		server.didOpen("test.xml", "<foo ");
		server.didChange("test.xml", Collections.emptyList());
		waitUntil(() -> documentLifecycleParticipant.getDidChange() >= 1);
		assertEquals(1, documentLifecycleParticipant.getDidChange());
	}

	@Test
	public void didSave() {
		assertEquals(0, documentLifecycleParticipant.getDidSave());
		server.didOpen("test.xml", "<foo ");
		server.didSave("test.xml");
		waitUntil(() -> documentLifecycleParticipant.getDidSave() >= 1);
		assertEquals(1, documentLifecycleParticipant.getDidSave());
	}

	@Test
	public void didClose() {
		assertEquals(0, documentLifecycleParticipant.getDidClose());
		server.didOpen("test.xml", "<foo ");
		server.didClose("test.xml");
		assertEquals(1, documentLifecycleParticipant.getDidClose());
	}

	private static void waitUntil(java.util.function.BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + 5000;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() >= deadline) {
				break;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
}
