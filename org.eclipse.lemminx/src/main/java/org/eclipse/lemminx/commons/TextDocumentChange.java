/**
 *  Copyright (c) 2024 Angelo ZERR.
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
package org.eclipse.lemminx.commons;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

/**
 * Represents a text document change with pre-calculated offsets. The offsets
 * are calculated BEFORE the text is updated, so they are relative to the old
 * text content.
 */
public class TextDocumentChange {

	private final TextDocumentContentChangeEvent event;
	private final int startOffset;
	private final int oldLength;
	private final int newLength;

	/**
	 * Creates a new text document change.
	 * 
	 * @param event       the original change event
	 * @param startOffset the start offset in the old text (before update)
	 * @param oldLength   the length of text being replaced
	 * @param newLength   the length of the new text
	 */
	public TextDocumentChange(TextDocumentContentChangeEvent event, int startOffset, int oldLength, int newLength) {
		this.event = event;
		this.startOffset = startOffset;
		this.oldLength = oldLength;
		this.newLength = newLength;
	}

	/**
	 * Returns the original change event.
	 * 
	 * @return the original change event
	 */
	public TextDocumentContentChangeEvent getEvent() {
		return event;
	}

	/**
	 * Returns the start offset in the old text (before update).
	 * 
	 * @return the start offset
	 */
	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * Returns the length of text being replaced.
	 * 
	 * @return the old length
	 */
	public int getOldLength() {
		return oldLength;
	}

	/**
	 * Returns the length of the new text.
	 * 
	 * @return the new length
	 */
	public int getNewLength() {
		return newLength;
	}

	/**
	 * Returns the new text content.
	 * 
	 * @return the new text
	 */
	public String getText() {
		return event.getText();
	}
}
