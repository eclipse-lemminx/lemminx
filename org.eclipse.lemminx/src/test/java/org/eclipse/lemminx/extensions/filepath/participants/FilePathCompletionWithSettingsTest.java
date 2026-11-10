/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * File path support completion test with user settings.
 *
 * Test folders are in
 * org.eclipse.lemminx/src/test/resources/filePathCompletion/
 */
public class FilePathCompletionWithSettingsTest extends AbstractFilePathCompletionTest {

	@Test
	public void empty() throws BadLocationException {
		String xml = "<a path=\"|\">";
		CompletionItem[] items = getCompletionItemList(0, 9, 9, "folderA", "folderB", "folderC", "NestedA", "main.xml",
				"main.xsd", "main.xsl", "main.dtd", "main.rnc");
		testCompletionFor(xml, 9, items);
	}

	@Test
	public void testFilePathCompletion() throws BadLocationException {
		String xml = "<a path=\"./|\">";
		CompletionItem[] items = getCompletionItemList(0, 11, 11, "folderA", "folderB", "folderC", "NestedA",
				"main.xml",
				"main.xsd", "main.xsl", "main.dtd", "main.rnc");
		testCompletionFor(xml, 9, items);
	}

	@Test
	public void testFilePathCompletionBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\".\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 11, 11, "folderA", "folderB", "folderC", "NestedA",
				"main.xml",
				"main.xsd", "main.xsl", "main.dtd", "main.rnc");
		testCompletionFor(xml, 9, items);
	}

	@Test
	public void testFilePathCompletionFolderA() throws BadLocationException {
		String xml = "<a path=\"./folderA/|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "xsdA1.xsd", "xsdA2.xsd");
		testCompletionFor(xml, items);
	}

	@Test
	public void testFilePathCompletionFolderABackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\".\\folderA\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "xsdA1.xsd", "xsdA2.xsd");
		testCompletionFor(xml, items);
	}

	@Test
	public void testFilePathCompletionFolderB() throws BadLocationException {
		String xml = "<a path=\"folderB/|\">";
		CompletionItem[] items = getCompletionItemList(0, 17, 17, "xsdB1.xsd", "xmlB1.xml", "dtdB1.dtd", "rncB1.rnc");
		testCompletionFor(xml, 4, items);
	}

	@Test
	public void testFilePathCompletionFolderBBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\"folderB\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 17, 17, "xsdB1.xsd", "xmlB1.xml", "dtdB1.dtd", "rncB1.rnc");
		testCompletionFor(xml, 4, items);
	}

	@Test
	public void testFilePathCompletionFolderBAbsolutePath() throws BadLocationException {
		String filePath = userDirForwardSlash + "/src/test/resources/filePathCompletion/folderB/"; // C:/.../src/test...
		int filePathLength = filePath.length();
		String xml = "<a path=\"" + filePath + "|\">";
		CompletionItem[] items = getCompletionItemList(0, 9 + filePathLength, 9 + filePathLength, "xsdB1.xsd",
				"xmlB1.xml", "dtdB1.dtd", "rncB1.rnc");
		testCompletionFor(xml, 4, items);
	}

	@Test
	public void testFilePathCompletionFolderBAbsolutePathBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String filePath = userDirBackSlash + "\\src\\test\\resources\\filePathCompletion\\folderB\\"; // C:\...\src\test...
		int filePathLength = filePath.length();
		String xml = "<a path=\"" + filePath + "|\">";
		CompletionItem[] items = getCompletionItemList(0, 9 + filePathLength, 9 + filePathLength, "xsdB1.xsd",
				"xmlB1.xml", "dtdB1.dtd", "rncB1.rnc");
		testCompletionFor(xml, 4, items);
	}

	@Test
	public void testFilePathCompletionFolderBAbsolutePathWithFileScheme() throws BadLocationException {
		String filePath = (isWindows ? FilesUtils.FILE_SCHEME + "/" : FilesUtils.FILE_SCHEME) + userDirForwardSlash
				+ "/src/test/resources/filePathCompletion/folderB/";
		int filePathLength = filePath.length();
		String xml = "<a path=\"" + filePath + "|\">";
		CompletionItem[] items = getCompletionItemList(0, 9 + filePathLength, 9 + filePathLength, "xsdB1.xsd",
				"xmlB1.xml", "dtdB1.dtd", "rncB1.rnc");
		testCompletionFor(xml, 4, items);
	}

	@Test
	public void testFilePathCompletionNestedA() throws BadLocationException {
		String xml = "<a path=\"./NestedA/|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "NestedB");
		testCompletionFor(xml, 1, items);
	}

	@Test
	public void testFilePathCompletionNestedABackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\"./NestedA\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "NestedB");
		testCompletionFor(xml, 1, items);
	}

	@Test
	public void testFilePathCompletionNestedBIncomplete() throws BadLocationException {
		String xml = "<a path=\"./NestedA/NestedB/ZZZ|\">";
		CompletionItem[] items = getCompletionItemList(0, 27, 30, "nestedXSD.xsd");
		testCompletionFor(xml, 1, items);
	}

	@Test
	public void testFilePathCompletionNestedBIncompleteBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\".\\NestedA\\NestedB\\ZZZ|\">";
		CompletionItem[] items = getCompletionItemList(0, 27, 30, "nestedXSD.xsd");
		testCompletionFor(xml, 1, items);
	}

	@Test
	public void testFilePathCompletionExtraTextInValue() throws BadLocationException {
		String xml = "<a path=\"NAMESPACE_IGNORE_ME NestedA/NestedB/|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionExtraTextInValueBackSlash() throws BadLocationException {
		String xml = "<a path=\"NAMESPACE_IGNORE_ME NestedA\\NestedB\\|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionBadFolder() throws BadLocationException {
		String xml = "<a path=\"NestedA/BAD_FOLDER/|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionBadFolderBackSlash() throws BadLocationException {
		String xml = "<a path=\"NestedA\\BAD_FOLDER\\|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionStartWithDotDot() throws BadLocationException {
		String xml = "<a path=\"../filePathCompletion/folderA/|\">";
		CompletionItem[] items = getCompletionItemList(0, 39, 39, "xsdA1.xsd", "xsdA2.xsd", "dtdA1.dtd", "dtdA2.dtd",
				"rncA1.rnc", "rngA1.rng");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void testFilePathCompletionStartWithDotDotBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\"..\\filePathCompletion\\folderA\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 39, 39, "xsdA1.xsd", "xsdA2.xsd", "dtdA1.dtd", "dtdA2.dtd",
				"rncA1.rnc", "rngA1.rng");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void testFilePathCompletionStartWithDot() throws BadLocationException {
		String xml = "<a path=\"./folderA/|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "xsdA1.xsd", "xsdA2.xsd", "dtdA1.dtd", "dtdA2.dtd",
				"rncA1.rnc", "rngA1.rng");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void testFilePathCompletionStartWithDotBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String xml = "<a path=\".\\folderA\\|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "xsdA1.xsd", "xsdA2.xsd", "dtdA1.dtd", "dtdA2.dtd",
				"rncA1.rnc", "rngA1.rng");
		testCompletionFor(xml, 6, items);
	}

	@Test
	public void testFilePathCompletionEndsWithFileAndSlash() throws BadLocationException {
		String xml = "<a path=\"./randomFile.aaa/|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionEndsWithFileAndBackSlash() throws BadLocationException {
		String xml = "<a path=\".\\randomFile.aaa\\|\">";
		testCompletionFor(xml, 0);
	}

	@Test
	public void testFilePathCompletionWithSpacesFolder() throws BadLocationException {
		String xml = "<a path=\"./folderC/|\">";
		CompletionItem[] items = getCompletionItemList(0, 19, 19, "a@b", "with%20spaces");
		testCompletionFor(xml, 2, items);
	}

	@Test
	public void testFilePathCompletionInsideSpecialChars() throws BadLocationException {
		String xml = "<a path=\"../|\">";
		String fileURI = "file://" + userDirForwardSlash + "/src/test/resources/filePathCompletion/folderC/a@b/foo.xml";
		CompletionItem[] items = getCompletionItemList(0, 12, 12, "a@b", "with%20spaces");
		testCompletionFor(xml, fileURI, 2, items);
	}

	@Disabled
	@Test
	public void testFilePathCompletionWithBrokenAbsoluteWindowsPath() throws BadLocationException {
		String xml = "<a path=\"C|\">";
		testCompletionFor(xml, 0);
		xml = "<a path=\"C:|\">";
		testCompletionFor(xml, 0);
		xml = "<a path=\"C\\\\|\">";
		testCompletionFor(xml, 0);
		xml = "<a path=\"C::|\">";
		testCompletionFor(xml, 0);
	}

	// Test with multiple file path

	@Test
	public void testFilePathCompletionExtraTextInValueAbsolute() throws BadLocationException {
		String filePath = userDirForwardSlash + "/src/test/resources/filePathCompletion/NestedA/NestedB/";
		int filePathLength = filePath.length();
		String xml = "<a paths=\"NAMESPACE_IGNORE_ME " + filePath + "|\">";
		CompletionItem[] items = getCompletionItemList(0, 30 + filePathLength, 30 + filePathLength, "nestedXSD.xsd");
		testCompletionFor(xml, 1, items);
	}

	@Test
	public void testFilePathCompletionExtraTextInValueAbsoluteBackSlash() throws BadLocationException {
		if (!isWindows) {
			return;
		}
		String filePath = userDirBackSlash + "\\src\\test\\resources\\filePathCompletion\\NestedA\\NestedB\\";
		int filePathLength = filePath.length();

		String xml = "<a path=\"NAMESPACE_IGNORE_ME " + filePath + "|\">";
		testCompletionFor(xml, 0);

		xml = "<a paths=\"NAMESPACE_IGNORE_ME " + filePath + "|\">";
		CompletionItem[] items = getCompletionItemList(0, 30 + filePathLength, 30 + filePathLength, "nestedXSD.xsd");
		testCompletionFor(xml, 1, items);
	}
	
	@Test
	public void beforeFirstPath() throws BadLocationException {
		String xml = "<a paths=\"| NestedA\">";
		CompletionItem[] items = getCompletionItemList(0, 10, 10, "folderA", "folderB", "folderC", "NestedA", "main.xml",
				"main.xsd", "main.xsl", "main.dtd", "main.rnc");
		testCompletionFor(xml, 9, items);
	}

	
	@Test
	public void afterFirstPath() throws BadLocationException {
		String xml = "<a paths=\"NestedA |\">";
		CompletionItem[] items = getCompletionItemList(0, 18, 18, "folderA", "folderB", "folderC", "NestedA", "main.xml",
				"main.xsd", "main.xsl", "main.dtd", "main.rnc");
		testCompletionFor(xml, 9, items);
	}

}