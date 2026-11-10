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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;

/**
 * Base class for RNC features request.
 */
public abstract class AbstractRNCRequest implements ISharedSettingsRequest {

	private final RNCDocument rncDocument;
	private final int offset;
	private final TextDocument textDocument;
	private final SharedSettings sharedSettings;

	/**
	 * Creates a new completion request.
	 *
	 * @param rncDocument    the parsed RNC document.
	 * @param position       the cursor position.
	 * @param sharedSettings the shared client settings.
	 * @throws BadLocationException if the position cannot be converted to an
	 *                              offset.
	 */
	public AbstractRNCRequest(RNCDocument rncDocument, Position position, SharedSettings sharedSettings)
			throws BadLocationException {
		this.rncDocument = rncDocument;
		this.sharedSettings = sharedSettings;
		this.textDocument = rncDocument.getTextDocument();
		this.offset = textDocument.offsetAt(position);
	}

	public RNCDocument getRNCDocument() {
		return rncDocument;
	}

	public int getOffset() {
		return offset;
	}

	public CharSequence getText() {
		return textDocument.getTextSequence();
	}

	public TextDocument getTextDocument() {
		return textDocument;
	}

	public boolean isSnippetsSupported() {
		return sharedSettings.getCompletionSettings().isCompletionSnippetsSupported();
	}

	@Override
	public SharedSettings getSharedSettings() {
		return sharedSettings;
	}

}
