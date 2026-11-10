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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCompletionFor;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

public class RNCIncludeCompletionTest {

	private static final String userDir = FilesUtils.encodePath(System.getProperty("user.dir"));
	private static final String userDirForwardSlash = userDir.replace("\\", "/");

	private static String getFileUri(String fileName) {
		return "file://" + userDirForwardSlash + "/src/test/resources/filePathCompletion/" + fileName;
	}

	@Test
	public void includeEmptyLiteral() throws BadLocationException {
		String value = "include \"|\"";
		CompletionItem[] items = {
				c("folderA", te(0, 9, 0, 9, "folderA"), "folderA"),
				c("folderB", te(0, 9, 0, 9, "folderB"), "folderB"),
				c("main.rnc", te(0, 9, 0, 9, "main.rnc"), "main.rnc")
		};
		testCompletionFor(value, getFileUri("main.rnc"), false, null, items);
	}

	@Test
	public void includeWithPartialPath() throws BadLocationException {
		String value = "include \"folderA/|\"";
		CompletionItem[] items = {
				c("rncA1.rnc", te(0, 17, 0, 17, "rncA1.rnc"), "rncA1.rnc")
		};
		testCompletionFor(value, getFileUri("main.rnc"), false, null, items);
	}

	@Test
	public void includeOnlyShowsRncFiles() throws BadLocationException {
		String value = "include \"folderA/|\"";
		testCompletionFor(value, getFileUri("main.rnc"), false, 1,
				c("rncA1.rnc", te(0, 17, 0, 17, "rncA1.rnc"), "rncA1.rnc"));
	}

	@Test
	public void includeShowsFoldersAndRncFiles() throws BadLocationException {
		String value = "include \"folderB/|\"";
		testCompletionFor(value, getFileUri("main.rnc"), false, 1,
				c("rncB1.rnc", te(0, 17, 0, 17, "rncB1.rnc"), "rncB1.rnc"));
	}

	@Test
	public void includeAtRootLevel() throws BadLocationException {
		String value = "include \"|\"";
		testCompletionFor(value, getFileUri("main.rnc"), false, null,
				c("main.rnc", te(0, 9, 0, 9, "main.rnc"), "main.rnc"));
	}

	@Test
	public void includeAfterContent() throws BadLocationException {
		String value = "start = element root { text }" + LS
				+ "include \"|\"";
		int line = 1;
		testCompletionFor(value, getFileUri("main.rnc"), false, null,
				c("main.rnc", te(line, 9, line, 9, "main.rnc"), "main.rnc"));
	}

	@Test
	public void includeReplacesExistingText() throws BadLocationException {
		String value = "include \"mai|\"";
		testCompletionFor(value, getFileUri("main.rnc"), false, null,
				c("main.rnc", te(0, 9, 0, 12, "main.rnc"), "main.rnc"));
	}

	@Test
	public void includeFiltersOnlyRncNotXsd() throws BadLocationException {
		String value = "include \"folderA/|\"";
		testCompletionFor(value, getFileUri("main.rnc"), false, 1);
	}
}
