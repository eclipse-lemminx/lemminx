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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.hover;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.AbstractRNCRequest;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;

/**
 * Encapsulates all context needed to compute RNC hover information.
 *
 * <p>
 * Created once per hover request, this class resolves the cursor offset, word
 * at the cursor, and document text. It also exposes client capabilities
 * (markdown support) via {@link ISharedSettingsRequest}.
 * </p>
 */
public class RNCHoverRequest extends AbstractRNCRequest {

	private final int offset;
	private final String word;

	/**
	 * Creates a new hover request.
	 *
	 * @param rncDocument    the parsed RNC document.
	 * @param position       the cursor position.
	 * @param sharedSettings the shared client settings.
	 * @throws BadLocationException if the position cannot be converted to an
	 *                              offset.
	 */
	public RNCHoverRequest(RNCDocument rncDocument, Position position, SharedSettings sharedSettings)
			throws BadLocationException {
		super(rncDocument, position, sharedSettings);
		TextDocument textDocument = getTextDocument();
		this.offset = textDocument.offsetAt(position);
		this.word = RNCPositionUtils.getWordAt(textDocument.getTextSequence(), offset);
	}

	public int getOffset() {
		return offset;
	}

	public String getWord() {
		return word;
	}

	public boolean canSupportMarkdown() {
		return canSupportMarkupKind(MarkupKind.MARKDOWN);
	}

	@Override
	public boolean canSupportMarkupKind(String kind) {
		return getSharedSettings().getHoverSettings().canSupportMarkupKind(kind);
	}
}
