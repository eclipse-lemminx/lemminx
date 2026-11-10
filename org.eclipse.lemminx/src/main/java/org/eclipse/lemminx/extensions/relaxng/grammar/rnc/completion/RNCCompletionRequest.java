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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.AbstractRNCRequest;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Encapsulates all context needed to compute RNC completion items.
 *
 * <p>
 * Created once per completion request, this class resolves the cursor offset,
 * document text, line separator, and replacement range. It also exposes client
 * capabilities (snippet support, trigger-suggest, markdown) via
 * {@link ISharedSettingsRequest}.
 * </p>
 *
 * <p>
 * Use case: {@link RNCCompletions#doComplete} creates a request and passes it
 * to the various {@code collect*} methods, avoiding the need to thread
 * individual parameters through every call.
 * </p>
 */
public class RNCCompletionRequest extends AbstractRNCRequest {

	private final int offset;
	private final String lineSeparator;
	private final Range replaceRange;

	/**
	 * Creates a new completion request.
	 *
	 * @param rncDocument    the parsed RNC document.
	 * @param position       the cursor position.
	 * @param sharedSettings the shared client settings.
	 * @throws BadLocationException if the position cannot be converted to an
	 *                              offset.
	 */
	public RNCCompletionRequest(RNCDocument rncDocument, Position position, SharedSettings sharedSettings)
			throws BadLocationException {
		super(rncDocument, position, sharedSettings);
		TextDocument textDocument = getTextDocument();
		this.offset = textDocument.offsetAt(position);
		this.lineSeparator = textDocument.lineDelimiter(0);
		this.replaceRange = computeReplaceRange();
	}

	public int getOffset() {
		return offset;
	}

	public String getLineSeparator() {
		return lineSeparator;
	}

	public Range getReplaceRange() {
		return replaceRange;
	}

	public boolean isSnippetsSupported() {
		return getSharedSettings().getCompletionSettings().isCompletionSnippetsSupported();
	}

	public boolean isTriggerSuggestSupported() {
		return getSharedSettings().isTriggerSuggestCommandSupport();
	}

	@Override
	public boolean canSupportMarkupKind(String kind) {
		return getSharedSettings().getCompletionSettings().canSupportMarkupKind(kind);
	}

	private Range computeReplaceRange() throws BadLocationException {
		CharSequence text = getText();
		int start = offset;
		while (start > 0 && RNCPositionUtils.isIdentifierChar(text.charAt(start - 1))) {
			start--;
		}
		TextDocument textDocument = getTextDocument();
		Position startPos = textDocument.positionAt(start);
		Position endPos = textDocument.positionAt(offset);
		return new Range(startPos, endPos);
	}
}
