/**
 *  Copyright (c) 2026 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.services.format;

/**
 * Shared indentation support for XML formatting.
 *
 * <p>
 * Manages a reusable {@link StringBuilder} and formatting options (tab size,
 * insert spaces, line delimiter) to generate indent strings.
 * </p>
 *
 * <p>
 * {@link XMLFormatterDocument} extends this class to inherit indentation
 * methods. {@link XMLFormatterOnType} creates an instance to reuse the same
 * logic.
 * </p>
 */
public class XMLFormatterIndent {

	private final int tabSize;
	private final boolean insertSpaces;
	private final String lineDelimiter;
	private final StringBuilder indentBuilder;

	public XMLFormatterIndent(int tabSize, boolean insertSpaces, String lineDelimiter) {
		this.tabSize = tabSize;
		this.insertSpaces = insertSpaces;
		this.lineDelimiter = lineDelimiter;
		this.indentBuilder = new StringBuilder(400);
	}

	public int getTabSize() {
		return tabSize;
	}

	public boolean isInsertSpaces() {
		return insertSpaces;
	}

	public String getLineDelimiter() {
		return lineDelimiter;
	}

	public String getIndentSpaces(int level, boolean addLineSeparator) {
		indentBuilder.setLength(0);
		if (addLineSeparator) {
			indentBuilder.append(lineDelimiter);
		}
		appendIndent(level, indentBuilder);
		return indentBuilder.toString();
	}

	public String getIndentSpacesWithMultiNewLines(int level, int newLineCount) {
		indentBuilder.setLength(0);
		while (newLineCount != 0) {
			indentBuilder.append(lineDelimiter);
			newLineCount--;
		}
		appendIndent(level, indentBuilder);
		return indentBuilder.toString();
	}

	public String getIndentSpacesWithOffsetSpaces(int spaceCount, boolean addLineSeparator) {
		indentBuilder.setLength(0);
		if (addLineSeparator) {
			indentBuilder.append(lineDelimiter);
		}
		int spaceOffset = spaceCount % tabSize;
		appendIndent(spaceCount / tabSize, indentBuilder);
		appendSpaceIndent(spaceOffset, indentBuilder);
		return indentBuilder.toString();
	}

	public String createIndent(int indentLevel) {
		indentBuilder.setLength(0);
		appendIndent(indentLevel, indentBuilder);
		return indentBuilder.toString();
	}

	public String createSpaceIndent(int spaceCount) {
		indentBuilder.setLength(0);
		appendSpaceIndent(spaceCount, indentBuilder);
		return indentBuilder.toString();
	}

	private void appendIndent(int level, StringBuilder sb) {
		for (int i = 0; i < level; i++) {
			if (insertSpaces) {
				for (int j = 0; j < tabSize; j++) {
					sb.append(' ');
				}
			} else {
				sb.append('\t');
			}
		}
	}

	private void appendSpaceIndent(int spaceCount, StringBuilder sb) {
		for (int i = 0; i < spaceCount; i++) {
			sb.append(' ');
		}
	}
}
