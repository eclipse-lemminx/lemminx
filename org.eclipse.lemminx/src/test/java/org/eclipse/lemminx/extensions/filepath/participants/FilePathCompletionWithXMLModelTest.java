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
package org.eclipse.lemminx.extensions.filepath.participants;

import static org.eclipse.lemminx.utils.platform.Platform.isWindows;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * File path support completion test with {@code <?xml-model href="..." ?>}.
 *
 * Test folders are in
 * org.eclipse.lemminx/src/test/resources/filePathCompletion/
 *
 * @see <a href="https://github.com/eclipse-lemminx/lemminx/issues/1307">issue
 *      1307</a>
 */
public class FilePathCompletionWithXMLModelTest extends AbstractFilePathCompletionTest {

	@Test
	public void empty() throws BadLocationException {
		String xml = "<?xml-model href=\"|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 18, 18, "folderA", "folderB", "folderC", "NestedA",
				"main.dtd", "main.xsd", "main.rnc");
		testCompletionFor(xml, 7, items);
	}

	@Test
	public void dotSlash() throws BadLocationException {
		String xml = "<?xml-model href=\"./|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 20, 20, "folderA", "folderB", "folderC", "NestedA",
				"main.dtd", "main.xsd", "main.rnc");
		testCompletionFor(xml, 7, items);
	}

	@Test
	public void dotSlashFollowingBySlash() throws BadLocationException {
		String xml = "<?xml-model href=\"./|/\"?>";
		CompletionItem[] items = getCompletionItemList(0, 20, 21, "folderA", "folderB", "folderC", "NestedA",
				"main.dtd", "main.xsd",  "main.rnc");
		testCompletionFor(xml, 7, items);
	}

	@Test
	public void backSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<?xml-model href=\".\\|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 20, 20, "folderA", "folderB", "folderC", "NestedA",
				"main.dtd", "main.xsd", "main.rnc");
		testCompletionFor(xml, 7, items);
	}

	@Test
	public void afterFolderA() throws BadLocationException {
		// folderA contains dtdA1.dtd, dtdA2.dtd, rncA1.rnc, rngA1.rng, xsdA1.xsd, xsdA2.xsd
		// all are schema files and must be proposed
		String xml = "<?xml-model href=\"./folderA/|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 28, 28, "dtdA1.dtd", "dtdA2.dtd", "rncA1.rnc",
				"rngA1.rng", "xsdA1.xsd", "xsdA2.xsd");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void afterFolderABackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<?xml-model href=\".\\folderA\\|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 28, 28, "dtdA1.dtd", "dtdA2.dtd", "rncA1.rnc",
				"rngA1.rng", "xsdA1.xsd", "xsdA2.xsd");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void afterFolderB() throws BadLocationException {
		// folderB contains dtdB1.dtd, xmlB1.xml, xsdB1.xsd
		// xmlB1.xml must be filtered out (only schema files are proposed)
		String xml = "<?xml-model href=\"./folderB/|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 28, 28, "dtdB1.dtd", "xsdB1.xsd", "rncB1.rnc");
		testCompletionFor(xml, 3, items);
	}

	@Test
	public void afterFolderBBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<?xml-model href=\".\\folderB\\|\"?>";
		CompletionItem[] items = getCompletionItemList(0, 28, 28, "dtdB1.dtd", "xsdB1.xsd", "rncB1.rnc");
		testCompletionFor(xml, 3, items);
	}

	@Test
	public void withOtherAttributes() throws BadLocationException {
		String xml = "<?xml-model href=\"./|\" type=\"application/xml\" schematypens=\"http://www.w3.org/2001/XMLSchema\"?>";
		CompletionItem[] items = getCompletionItemList(0, 20, 20, "folderA", "folderB", "folderC", "NestedA",
				"main.dtd", "main.xsd", "main.rnc");
		testCompletionFor(xml, 7, items);
	}

	@Test
	public void noCompletionOutsideHref() throws BadLocationException {
		String xml = "<?xml-model href=\"./foo.xsd\" type=\"|\"?>";
		testCompletionFor(xml, 0);
	}

}
