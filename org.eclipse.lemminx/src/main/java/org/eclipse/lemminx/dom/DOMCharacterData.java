/**
 *  Copyright (c) 2018 Angelo ZERR.
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
package org.eclipse.lemminx.dom;

import static java.lang.System.lineSeparator;

import org.eclipse.lemminx.commons.BadLocationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.CharacterData;

/**
 * A CharacterData node.
 *
 */
public abstract class DOMCharacterData extends DOMTreeNode implements CharacterData {

	public DOMCharacterData(int start, int end) {
		super(start, end);
	}

	public boolean hasMultiLine() {
		CharSequence text = getOwnerDocument().getTextSequence();
		for (int i = getStartContent(); i < getEndContent(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				return true;
			}
		}
		return false;
	}

	public String getDelimiter() {
		try {
			return getOwnerDocument().getTextDocument().lineDelimiter(0);
		} catch (BadLocationException e) {
			return lineSeparator();
		}
	}

	/**
	 * If data ends with a new line character.
	 *
	 * Returns false if a character is found before a new line. Non-newline
	 * whitespace will be ignored while searching.
	 *
	 * If no data exists, returns false.
	 *
	 * @return true if newline character occurs before non-whitespace character
	 */
	public boolean endsWithNewLine() {
		CharSequence text = getOwnerDocument().getTextSequence();
		int startContent = getStartContent();
		int endContent = getEndContent();
		for (int i = endContent - 1; i >= startContent; i--) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c)) {
				return false;
			}
			if (c == '\n') {
				return true;
			}
		}
		return false;
	}

	/**
	 * If data starts with a new line character.
	 *
	 * Returns false if a character is found before a new line. Non-newline
	 * whitespace will be ignored while searching.
	 *
	 * @return true if newline character occurs before non-whitespace character
	 */
	public boolean startsWithNewLine() {
		CharSequence text = getOwnerDocument().getTextSequence();
		int startContent = getStartContent();
		int endContent = getEndContent();
		for (int i = startContent; i < endContent; i++) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c)) {
				return false;
			}
			if (c == '\n' || c == '\r') {
				return true;
			}
		}
		return false;
	}

	public boolean hasData() {
		return getStartContent() < getEndContent();
	}

	public int getStartContent() {
		return start;
	}

	public int getEndContent() {
		return end;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.CharacterData#getData()
	 */
	@Override
	public String getData() {
		// No caching - extract directly from document to save memory
		// The document text is already in memory, so this is just a substring operation
		return getOwnerDocument().getText(getStartContent(), getEndContent());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getNodeValue()
	 */
	@Override
	public String getNodeValue() throws DOMException {
		return getData();
	}

	public boolean isWhitespace() {
		return getFlag(FLAG_WHITESPACE);
	}

	public void setWhitespace(boolean whitespace) {
		setFlag(FLAG_WHITESPACE, whitespace);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#appendData(java.lang.String)
	 */
	@Override
	public void appendData(String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#deleteData(int, int)
	 */
	@Override
	public void deleteData(int offset, int count) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#getLength()
	 */
	@Override
	public int getLength() {
		return getEndContent() - getStartContent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#insertData(int, java.lang.String)
	 */
	@Override
	public void insertData(int offset, String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#replaceData(int, int, java.lang.String)
	 */
	@Override
	public void replaceData(int offset, int count, String data) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#setData(java.lang.String)
	 */
	@Override
	public void setData(String value) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.CharacterData#substringData(int, int)
	 */
	@Override
	public String substringData(int offset, int count) throws DOMException {
		throw new UnsupportedOperationException();
	}
}