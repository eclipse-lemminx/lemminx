/**
 *  Copyright (c) 2018, 2023 Angelo ZERR.
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

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;

/**
 * Text document extends LSP4j {@link TextDocumentItem} to provide methods to
 * retrieve position.
 *
 */
public class TextDocument extends TextDocumentItem {

	private static final Logger LOGGER = Logger.getLogger(TextDocument.class.getName());

	private final Object lock = new Object();

	private static String DEFAULT_DELIMTER = System.lineSeparator();

	private ILineTracker lineTracker;

	private volatile boolean disposed;

	private boolean incremental;

	private CharSequence textSequence;
	private boolean textDirty;

	public TextDocument(TextDocumentItem document) {
		this(document.getText(), document.getUri());
		super.setVersion(document.getVersion());
		super.setLanguageId(document.getLanguageId());
	}

	public TextDocument(String text, String uri) {
		super.setUri(uri);
		setText(text);
	}

	@Override
	public void setText(String text) {
		this.textSequence = text;
		this.textDirty = true;
	}

	/**
	 * Returns the text content as a {@link String}. Prefer
	 * {@link #getTextSequence()} which avoids costly string materialization when
	 * the document is backed by a {@link StringBuilder}.
	 *
	 * @deprecated Use {@link #getTextSequence()} instead to avoid allocating a full
	 *             String copy of the document text.
	 */
	@Deprecated
	@Override
	public String getText() {
		if (textDirty) {
			super.setText(textSequence.toString());
			textDirty = false;
		}
		return super.getText();
	}

	/**
	 * Returns the text content as a {@link CharSequence}, avoiding the allocation
	 * of a full {@link String} copy when the document is backed by a
	 * {@link StringBuilder}.
	 * <p>
	 * <b>Thread-safety contract:</b> The returned {@link CharSequence} references
	 * the live internal buffer and is valid until the next call to
	 * {@link #update(List)}. Callers must not retain or read the returned sequence
	 * across an update. In practice, the LSP protocol guarantees that
	 * {@code textDocument/didChange} (which calls {@link #update(List)}) cancels
	 * any in-progress parse via {@code CancelChecker} before modifying the buffer,
	 * so concurrent reads cannot occur.
	 * </p>
	 *
	 * @return the text content as a {@link CharSequence}.
	 */
	public CharSequence getTextSequence() {
		return textSequence;
	}

	/**
	 * Returns a {@link String} from the text content between the given
	 * <code>start</code> and <code>end</code> offsets.
	 *
	 * @param start the start offset.
	 * @param end   the end offset.
	 * @return a {@link String} from the text content between the given
	 *         <code>start</code> and <code>end</code> offsets.
	 */
	public String getText(int start, int end) {
		return StringUtils.getString(textSequence, start, end);
	}

	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
		lineTracker = null;
		getLineTracker();
	}

	public boolean isIncremental() {
		return incremental;
	}

	public Position positionAt(int position) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		return lineTracker.getPositionAt(position);
	}

	public int offsetAt(Position position) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		return lineTracker.getOffsetAt(position);
	}

	public String lineText(int lineNumber) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		Line line = lineTracker.getLineInformation(lineNumber);
		return StringUtils.getString(textSequence, line.offset, line.offset + line.length);
	}

	public int lineOffsetAt(int position) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		Line line = lineTracker.getLineInformationOfOffset(position);
		return line.offset;
	}

	/**
	 * Returns the line number the character at the given offset belongs to.
	 *
	 * @param position the offset whose line number to be determined
	 * @return the number of the line the offset is on
	 * @exception BadLocationException if the offset is invalid in this tracker
	 */
	public int lineAt(int position) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		return lineTracker.getLineNumberOfOffset(position);
	}

	public String lineDelimiter(int lineNumber) throws BadLocationException {
		ILineTracker lineTracker = getLineTracker();
		String lineDelimiter = lineTracker.getLineDelimiter(lineNumber);
		if (lineDelimiter == null) {
			if (lineTracker.getNumberOfLines() > 0) {
				lineDelimiter = lineTracker.getLineInformation(0).delimiter;
			}
		}
		if (lineDelimiter == null) {
			lineDelimiter = DEFAULT_DELIMTER;
		}
		return lineDelimiter;
	}

	public Range getWordRangeAt(int textOffset, Pattern wordDefinition) {
		try {
			Position pos = positionAt(textOffset);
			ILineTracker lineTracker = getLineTracker();
			Line line = lineTracker.getLineInformation(pos.getLine());
			String lineText = StringUtils.getString(textSequence, line.offset, textOffset);
			int position = lineText.length();
			Matcher m = wordDefinition.matcher(lineText);
			int currentPosition = 0;
			while (currentPosition != position) {
				if (m.find()) {
					currentPosition = m.end();
					if (currentPosition == position) {
						return new Range(new Position(pos.getLine(), m.start()), pos);
					}
				} else {
					currentPosition++;
				}
				m.region(currentPosition, position);
			}
			return new Range(pos, pos);
		} catch (BadLocationException e) {
			return null;
		}
	}

	private ILineTracker getLineTracker() {
		if (lineTracker == null) {
			lineTracker = createLineTracker();
		}
		return lineTracker;
	}

	private synchronized ILineTracker createLineTracker() {
		if (lineTracker != null) {
			return lineTracker;
		}
		ILineTracker lineTracker = isIncremental() ? new TreeLineTracker(new ListLineTracker()) : new ListLineTracker();
		lineTracker.set(textSequence);
		return lineTracker;
	}

	/**
	 * Dispose this document, releasing all retained memory (text content and
	 * line tracker) for garbage collection.
	 */
	public void dispose() {
		this.disposed = true;
		this.textSequence = null;
		this.lineTracker = null;
	}

	/**
	 * Returns true if this document has been disposed and false otherwise.
	 *
	 * @return true if this document has been disposed and false otherwise.
	 */
	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * Update text of the document by using the changes and according the
	 * incremental support.
	 * 
	 * @param changes the text document changes.
	 */
	public void update(List<TextDocumentContentChangeEvent> changes) {
		if (changes.size() < 1) {
			// no changes, ignore it.
			return;
		}
		if (isIncremental()) {
			try {
				long start = System.currentTimeMillis();
				synchronized (lock) {
					// Lazy conversion to StringBuilder for in-place editing via
					// buffer.replace() — avoids creating a new String on every keystroke.
					// Done here (first keystroke) instead of in setIncremental() so that
					// the humongous allocation triggers G1GC, which collects the old DOM
					// tree (nulled by cancelModel()) before re-parse — avoiding a
					// transient memory peak that could cause OOM.
					if (!(textSequence instanceof StringBuilder)) {
						textSequence = new StringBuilder(textSequence);
					}
					StringBuilder buffer = (StringBuilder) textSequence;
					for (int i = 0; i < changes.size(); i++) {

						TextDocumentContentChangeEvent changeEvent = changes.get(i);
						Range range = changeEvent.getRange();
						int length = 0;

						if (range != null) {
							Integer rangeLength = changeEvent.getRangeLength();
							length = rangeLength != null ? rangeLength.intValue()
									: offsetAt(range.getEnd()) - offsetAt(range.getStart());
						} else {
							// range is optional and if not given, the whole file content is replaced
							length = buffer.length();
							range = new Range(positionAt(0), positionAt(length));
						}
						String text = changeEvent.getText();
						int startOffset = offsetAt(range.getStart());
						buffer.replace(startOffset, startOffset + length, text);
						lineTracker.replace(startOffset, length, text);
					}
					textDirty = true;
				}
				LOGGER.fine("Text document content updated in " + (System.currentTimeMillis() - start) + "ms");
			} catch (BadLocationException e) {
				// Should never occur.
			}
		} else {
			// like vscode does, get the last changes
			// see
			// https://github.com/Microsoft/vscode-languageserver-node/blob/master/server/src/main.ts
			TextDocumentContentChangeEvent last = changes.size() > 0 ? changes.get(changes.size() - 1) : null;
			if (last != null) {
				setText(last.getText());
				lineTracker.set(last.getText());
			}
		}
	}
}
