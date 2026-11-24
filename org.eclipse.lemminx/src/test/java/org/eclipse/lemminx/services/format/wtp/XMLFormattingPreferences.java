/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format.wtp;

import org.eclipse.lemminx.settings.XMLFormattingOptions;
/**
 * {@link XMLFormattingOptions} extension to emulate XMLFormattingPreferences from WTP XML Editor.
 *
 */
public class XMLFormattingPreferences extends XMLFormattingOptions {

	public static final String PRESERVE = "PRESERVE";
	public static final String COLLAPSE = "COLLAPSE";

	public XMLFormattingPreferences() {
		// Line width: [72]
		super.setMaxLineWidth(72);
		// [] Split multiple attributes each on new line
		super.setSplitAttributes(SplitAttributes.preserve);
		// [] Align final bracket in multi-line element tags
		// ???
		// [] Preserve whitespace in tags with PCDATA contents
		// ??
		// [] Clear all blank lines
		setClearAllBlankLines(false);
		// [x] Format comments
		super.setFormatComments(true);
		// [x] Join lines
		super.setJoinCommentLines(true);
		// [x] Insert whitespace before closing empty end-tags
		super.setSpaceBeforeEmptyCloseTag(true);
		// (x) Indent using tabs
		super.setInsertSpaces(false);
		// Indentation size: [1]
		super.setTabSize(1);
		// Use inferred grammar in absence of DTD/Schema
		// ??
	}

	public void setClearAllBlankLines(boolean clearAllBlankLines) {
		if (clearAllBlankLines) {
			super.setPreservedNewlines(0);
		} else {
			// Lemminx doesn't provide this settings, but it requires to set a max number
			// lines with preservedNewlines
			super.setPreservedNewlines(2);
		}
	}

	public void setIndentMultipleAttributes(boolean indentMultipleAttributes) {
		if (indentMultipleAttributes) {
			super.setSplitAttributes(SplitAttributes.splitNewLine);
		} else {
			super.setSplitAttributes(SplitAttributes.preserve);
		}
	}

	public void setFormatCommentText(boolean formatCommentText) {
		super.setFormatComments(formatCommentText);
	}

	public void setPCDataWhitespaceStrategy(String whiteSpaceStrategy) {
		// ???
	}

	public void setAlignFinalBracket(boolean alignFinalBracket) {
		// ???
	}

}
