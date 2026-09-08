/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
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

/**
 * Base class for DOM nodes that have a position range (start/end offsets) and
 * flags in the parsed document. DOMAttr does not extend this class because it
 * computes its own start/end from name/value offsets.
 */
public abstract class DOMTreeNode extends DOMNode {

	static final byte FLAG_CLOSED = 0x01;
	static final byte FLAG_SELF_CLOSED = 0x02;           // DOMElement
	static final byte FLAG_START_TAG_CLOSE = 0x04;       // DOMProcessingInstruction
	static final byte FLAG_PROLOG = 0x08;                // DOMProcessingInstruction
	static final byte FLAG_PROCESSING_INSTRUCTION = 0x10;// DOMProcessingInstruction
	static final byte FLAG_WHITESPACE = 0x20;            // DOMCharacterData
	static final byte FLAG_COMMENT_SAME_LINE = 0x40;     // DOMComment
	static final byte FLAG_HAS_START_TAG = 0x04;         // DOMElement (overlaps FLAG_START_TAG_CLOSE, different subclass)
	static final byte FLAG_END_TAG_CLOSED = 0x08;        // DOMElement (overlaps FLAG_PROLOG, different subclass)

	private byte flags = 0;

	final int start;
	int end;

	public DOMTreeNode(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getEnd() {
		return end;
	}

	@Override
	void setEnd(int end) {
		this.end = end;
	}

	boolean getFlag(byte flag) {
		return (flags & flag) != 0;
	}

	void setFlag(byte flag, boolean value) {
		if (value) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}
	}

	@Override
	public boolean isClosed() {
		return getFlag(FLAG_CLOSED);
	}

	@Override
	void setClosed(boolean closed) {
		setFlag(FLAG_CLOSED, closed);
	}
}
