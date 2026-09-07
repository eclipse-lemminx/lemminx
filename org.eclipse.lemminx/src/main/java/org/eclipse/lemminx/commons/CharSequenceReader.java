/**
 *  Copyright (c) 2026 Angelo ZERR.
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

import java.io.Reader;

/**
 * A {@link Reader} that reads from a {@link CharSequence} without requiring
 * conversion to {@link String}. This avoids the memory cost of materializing a
 * full String copy when the backing store is a {@link StringBuilder}.
 * <p>
 * Supports {@link #mark(int)}/{@link #reset()} for compatibility with parsers
 * (e.g. Xerces) that may use mark/reset during encoding detection.
 * </p>
 */
public class CharSequenceReader extends Reader {

	private final CharSequence source;
	private final int length;
	private int pos;
	private int mark;

	public CharSequenceReader(CharSequence source) {
		this.source = source;
		this.length = source.length();
	}

	@Override
	public int read() {
		if (pos >= length) {
			return -1;
		}
		return source.charAt(pos++);
	}

	@Override
	public int read(char[] cbuf, int off, int len) {
		if (pos >= length) {
			return -1;
		}
		int count = Math.min(len, length - pos);
		if (source instanceof String) {
			((String) source).getChars(pos, pos + count, cbuf, off);
		} else if (source instanceof StringBuilder) {
			((StringBuilder) source).getChars(pos, pos + count, cbuf, off);
		} else {
			for (int i = 0; i < count; i++) {
				cbuf[off + i] = source.charAt(pos + i);
			}
		}
		pos += count;
		return count;
	}

	@Override
	public long skip(long n) {
		long skipped = Math.min(n, length - pos);
		pos += (int) skipped;
		return skipped;
	}

	@Override
	public boolean ready() {
		return pos < length;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void mark(int readAheadLimit) {
		mark = pos;
	}

	@Override
	public void reset() {
		pos = mark;
	}

	@Override
	public void close() {
	}
}
