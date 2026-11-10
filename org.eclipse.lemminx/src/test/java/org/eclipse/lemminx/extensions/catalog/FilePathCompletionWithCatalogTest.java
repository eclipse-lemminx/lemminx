/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.catalog;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.filepath.participants.AbstractFilePathCompletionTest;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * File path support completion test for XML catalog with
 * 
 * <ul>
 * <li>@uri</li>
 * </ul>
 *
 * Test folders are in
 * org.eclipse.lemminx/src/test/resources/filePathCompletion/
 */
public class FilePathCompletionWithCatalogTest extends AbstractFilePathCompletionTest {

	@Test
	public void uri() throws BadLocationException {
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "	<public publicId=\"\" uri=\"|\" />\n"
				+ "</catalog>";
		CompletionItem[] items = getCompletionItemList(1, 26, 26, "folderA", "folderB", "folderC", "NestedA",
				"main.xsd", "main.dtd", "main.xml", "main.xsl", "main.rnc");
		testCompletionFor(xml, 9, items);
	}
}