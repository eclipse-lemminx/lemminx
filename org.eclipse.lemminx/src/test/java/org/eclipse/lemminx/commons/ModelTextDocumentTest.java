/*******************************************************************************
* Copyright (c) 2026 Aleksandar Kurtakov and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Aleksandar Kurtakov - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModelTextDocument}.
 */
public class ModelTextDocumentTest {

	private static final String OLD_TEXT = "FAIL";

	private static final String NEW_TEXT = "<plugin></";

	/**
	 * A parse of the old content which completes after the document has changed
	 * must not be cached as the model of the new content.
	 */
	@Test
	public void modelParsedBeforeChangeIsNotReused() throws Exception {
		CountDownLatch parsingOldText = new CountDownLatch(1);
		CountDownLatch documentChanged = new CountDownLatch(1);
		ModelTextDocument<String, Void> document = new ModelTextDocument<>(
				new TextDocumentItem("file:///test.xml", "xml", 1, OLD_TEXT), (doc, cancelChecker) -> {
					String text = doc.getText();
					if (OLD_TEXT.equals(text)) {
						// Simulate a parse which is still running when the document changes
						parsingOldText.countDown();
						try {
							documentChanged.await(5, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					return text;
				});
		// The XML language server uses incremental text document synchronization
		document.setIncremental(true);

		// Parse the old content asynchronously, like the validation triggered by didOpen
		CompletableFuture<String> oldParse = CompletableFuture.supplyAsync(document::getModel);
		assertTrue(parsingOldText.await(5, TimeUnit.SECONDS), "Parse of old content not started");

		// Change the document like TextDocuments#onDidChangeTextDocument does
		document.setVersion(2);
		document.update(Collections.singletonList(new TextDocumentContentChangeEvent(NEW_TEXT)));
		documentChanged.countDown();
		oldParse.get(5, TimeUnit.SECONDS);

		assertNull(document.getExistingModel(), "Model of the old content must not be reused");
		assertEquals(NEW_TEXT, document.getModel());
		assertEquals(NEW_TEXT, document.getExistingModel());
	}

}
