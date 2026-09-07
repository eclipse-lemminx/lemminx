/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.commons;

import org.eclipse.lsp4j.Position;

/**
 * Memory-efficient line tracker using flat arrays instead of an AVL tree.
 *
 * <p>Uses ~5 bytes per line (4-byte int offset + 1-byte delimiter type) vs
 * ~40 bytes per line for the tree-based tracker. For a 621K-line file this
 * saves ~22 MB of heap.</p>
 *
 * <p>Query operations (offset-to-line, line-to-offset) are O(log n) via
 * binary search. Update operations are O(n) for the array shift but use
 * {@link System#arraycopy} which is extremely fast in practice.</p>
 */
public class ArrayLineTracker implements ILineTracker {

	private static final String[] DELIMITERS = { "\r", "\n", "\r\n" };
	private static final String NO_DELIM = "";

	private static final byte DELIM_NONE = 0;
	private static final byte DELIM_LF = 1;
	private static final byte DELIM_CR = 2;
	private static final byte DELIM_CRLF = 3;

	private int[] lineStarts;
	private byte[] delimTypes;
	private int lineCount;
	private int textLength;

	public ArrayLineTracker() {
		lineStarts = new int[16];
		delimTypes = new byte[16];
		lineStarts[0] = 0;
		delimTypes[0] = DELIM_NONE;
		lineCount = 1;
		textLength = 0;
	}

	@Override
	public void set(CharSequence text) {
		textLength = text != null ? text.length() : 0;
		int count = 1;
		if (text != null) {
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '\n') {
					count++;
				} else if (ch == '\r') {
					count++;
					if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
						i++;
					}
				}
			}
		}

		lineStarts = new int[count];
		delimTypes = new byte[count];
		lineCount = count;

		lineStarts[0] = 0;
		int lineIndex = 0;
		if (text != null) {
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '\n') {
					delimTypes[lineIndex] = DELIM_LF;
					lineIndex++;
					lineStarts[lineIndex] = i + 1;
				} else if (ch == '\r') {
					if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
						delimTypes[lineIndex] = DELIM_CRLF;
						lineIndex++;
						lineStarts[lineIndex] = i + 2;
						i++;
					} else {
						delimTypes[lineIndex] = DELIM_CR;
						lineIndex++;
						lineStarts[lineIndex] = i + 1;
					}
				}
			}
		}
		delimTypes[lineIndex] = DELIM_NONE;
	}

	@Override
	public void replace(int offset, int length, String text) throws BadLocationException {
		int textLen = text != null ? text.length() : 0;
		int delta = textLen - length;

		int startLine = findLineByOffset(offset);
		int endLine = (length == 0) ? startLine : findLineByOffset(offset + length);

		byte endLineDelim = delimTypes[endLine];

		int newDelimCount = 0;
		int[] newStarts = null;
		byte[] newDelims = null;

		if (textLen > 0) {
			int count = 0;
			for (int i = 0; i < textLen; i++) {
				char ch = text.charAt(i);
				if (ch == '\n') {
					count++;
				} else if (ch == '\r') {
					count++;
					if (i + 1 < textLen && text.charAt(i + 1) == '\n') {
						i++;
					}
				}
			}
			if (count > 0) {
				newDelimCount = count;
				newStarts = new int[count];
				newDelims = new byte[count];
				int idx = 0;
				for (int i = 0; i < textLen; i++) {
					char ch = text.charAt(i);
					if (ch == '\n') {
						newDelims[idx] = DELIM_LF;
						newStarts[idx] = offset + i + 1;
						idx++;
					} else if (ch == '\r') {
						if (i + 1 < textLen && text.charAt(i + 1) == '\n') {
							newDelims[idx] = DELIM_CRLF;
							newStarts[idx] = offset + i + 2;
							idx++;
							i++;
						} else {
							newDelims[idx] = DELIM_CR;
							newStarts[idx] = offset + i + 1;
							idx++;
						}
					}
				}
			}
		}

		int removedLines = endLine - startLine;
		int linesDelta = newDelimCount - removedLines;
		int newLineCount = lineCount + linesDelta;

		ensureCapacity(newLineCount);

		int tailSrc = endLine + 1;
		int tailDst = startLine + 1 + newDelimCount;
		int tailLen = lineCount - tailSrc;

		if (tailLen > 0) {
			System.arraycopy(lineStarts, tailSrc, lineStarts, tailDst, tailLen);
			System.arraycopy(delimTypes, tailSrc, delimTypes, tailDst, tailLen);
			if (delta != 0) {
				for (int i = tailDst; i < tailDst + tailLen; i++) {
					lineStarts[i] += delta;
				}
			}
		}

		if (newDelimCount > 0) {
			for (int i = 0; i < newDelimCount; i++) {
				lineStarts[startLine + 1 + i] = newStarts[i];
				delimTypes[startLine + i] = newDelims[i];
			}
		}
		delimTypes[startLine + newDelimCount] = endLineDelim;

		lineCount = newLineCount;
		textLength += delta;
	}

	@Override
	public String getLineDelimiter(int line) throws BadLocationException {
		if (line < 0 || line >= lineCount) {
			throw new BadLocationException();
		}
		String delim = delimiterTypeToString(delimTypes[line]);
		return NO_DELIM.equals(delim) ? null : delim;
	}

	@Override
	public int computeNumberOfLines(String text) {
		int count = 0;
		if (text != null) {
			for (int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if (ch == '\n') {
					count++;
				} else if (ch == '\r') {
					count++;
					if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
						i++;
					}
				}
			}
		}
		return count;
	}

	@Override
	public int getNumberOfLines() {
		return lineCount;
	}

	@Override
	public int getNumberOfLines(int offset, int length) throws BadLocationException {
		if (length == 0) {
			return 1;
		}
		int startLine = findLineByOffset(offset);
		int endLine = findLineByOffset(offset + length);
		return endLine - startLine + 1;
	}

	@Override
	public int getLineOffset(int line) throws BadLocationException {
		if (line < 0 || line >= lineCount) {
			throw new BadLocationException();
		}
		return lineStarts[line];
	}

	@Override
	public int getLineLength(int line) throws BadLocationException {
		if (line < 0 || line >= lineCount) {
			throw new BadLocationException();
		}
		if (line + 1 < lineCount) {
			return lineStarts[line + 1] - lineStarts[line];
		}
		return textLength - lineStarts[line];
	}

	@Override
	public int getLineNumberOfOffset(int offset) throws BadLocationException {
		return findLineByOffset(offset);
	}

	@Override
	public Position getPositionAt(int offset) throws BadLocationException {
		int line = findLineByOffset(offset);
		int character = offset - lineStarts[line];
		return new Position(line, character);
	}

	@Override
	public int getOffsetAt(Position position) throws BadLocationException {
		int line = position.getLine();
		if (line < 0 || line >= lineCount) {
			throw new BadLocationException("The line value, {" + line + "}, is out of bounds.");
		}
		int lineOffset = lineStarts[line];
		int lineLength = pureLength(line);
		int character = position.getCharacter();
		int offset = lineOffset + character;
		int endLineOffset = lineOffset + lineLength;
		if (offset > endLineOffset) {
			throw new BadLocationException(
					"The character value, {" + character + "} of the line" + line + "}, is out of bounds.");
		}
		return offset;
	}

	@Override
	public Line getLineInformationOfOffset(int offset) throws BadLocationException {
		int line = findLineByOffset(offset);
		return new Line(lineStarts[line], pureLength(line));
	}

	@Override
	public Line getLineInformation(int line) throws BadLocationException {
		try {
			if (line < 0 || line >= lineCount) {
				throw new BadLocationException();
			}
			return new Line(lineStarts[line], pureLength(line));
		} catch (BadLocationException x) {
			if (line > 0 && line == lineCount) {
				int lastLine = line - 1;
				int lastLineEnd = lineStarts[lastLine] + getLineLength(lastLine);
				if (getLineLength(lastLine) > 0) {
					return new Line(lastLineEnd, 0);
				}
			}
			throw x;
		}
	}

	private int pureLength(int line) {
		int totalLength;
		if (line + 1 < lineCount) {
			totalLength = lineStarts[line + 1] - lineStarts[line];
		} else {
			totalLength = textLength - lineStarts[line];
		}
		return totalLength - delimLength(delimTypes[line]);
	}

	private int findLineByOffset(int offset) throws BadLocationException {
		if (offset < 0 || offset > textLength) {
			throw new BadLocationException();
		}
		int lo = 0, hi = lineCount - 1;
		while (lo < hi) {
			int mid = lo + (hi - lo + 1) / 2;
			if (lineStarts[mid] <= offset) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return lo;
	}

	private void ensureCapacity(int minCapacity) {
		if (minCapacity > lineStarts.length) {
			int newCapacity = Math.max(minCapacity, lineStarts.length + (lineStarts.length >> 1));
			int[] newStarts = new int[newCapacity];
			byte[] newDelims = new byte[newCapacity];
			System.arraycopy(lineStarts, 0, newStarts, 0, lineCount);
			System.arraycopy(delimTypes, 0, newDelims, 0, lineCount);
			lineStarts = newStarts;
			delimTypes = newDelims;
		}
	}

	private static int delimLength(byte delimType) {
		switch (delimType) {
		case DELIM_LF:
		case DELIM_CR:
			return 1;
		case DELIM_CRLF:
			return 2;
		default:
			return 0;
		}
	}

	private static String delimiterTypeToString(byte delimiterType) {
		switch (delimiterType) {
		case DELIM_LF:
			return DELIMITERS[1];
		case DELIM_CR:
			return DELIMITERS[0];
		case DELIM_CRLF:
			return DELIMITERS[2];
		default:
			return NO_DELIM;
		}
	}
}
