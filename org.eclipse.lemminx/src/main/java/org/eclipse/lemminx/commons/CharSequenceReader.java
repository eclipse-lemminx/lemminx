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
 * A {@link Reader} that reads from a {@link CharSequence} without
 * materializing it to a {@link String}. This avoids a costly copy
 * when the backing sequence is a {@link StringBuilder}.
 */
public final class CharSequenceReader extends Reader {

	private final CharSequence cs;
	private int pos;
	private final int length;

	public CharSequenceReader(CharSequence cs) {
		this.cs = cs;
		this.length = cs.length();
	}

	@Override
	public int read(char[] cbuf, int off, int len) {
		int remaining = length - pos;
		if (remaining <= 0) {
			return -1;
		}
		int n = Math.min(len, remaining);
		for (int i = 0; i < n; i++) {
			cbuf[off + i] = cs.charAt(pos + i);
		}
		pos += n;
		return n;
	}

	@Override
	public void close() {
	}
}
