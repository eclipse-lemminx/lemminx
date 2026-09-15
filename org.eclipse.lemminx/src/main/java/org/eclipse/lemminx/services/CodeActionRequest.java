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
package org.eclipse.lemminx.services;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.LineIndentInfo;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

/**
 * Code action request implementation.
 * 
 * @author Angelo ZERR
 *
 */
public class CodeActionRequest extends BaseCodeActionRequest implements ICodeActionRequest {

	private final Diagnostic diagnostic;

	private final Range range;

	private XMLElementGenerator generator;

	private LineIndentInfo indentInfo;

	public CodeActionRequest(Diagnostic diagnostic, Range range, DOMDocument document,
			IComponentProvider componentProvider, SharedSettings sharedSettings) {
		super(document, componentProvider, sharedSettings);
		this.diagnostic = diagnostic;
		this.range = range;
	}

	@Override
	public Diagnostic getDiagnostic() {
		return diagnostic;
	}

	@Override
	public Range getRange() {
		return range;
	}

	public XMLElementGenerator getXMLGenerator() throws BadLocationException {
		if (generator == null) {
			generator = new XMLElementGenerator(getSharedSettings(),
					getSharedSettings().getCompletionSettings().isAutoCloseTags(),
					getLineIndentInfo().getWhitespacesIndent(), getLineIndentInfo().getLineDelimiter(),
					false, Integer.MAX_VALUE, null);
		}
		return generator;
	}

	public LineIndentInfo getLineIndentInfo() throws BadLocationException {
		if (indentInfo == null) {
			int lineNumber = getRange().getStart().getLine();
			indentInfo = getDocument().getLineIndentInfo(lineNumber);
		}
		return indentInfo;
	}

}
