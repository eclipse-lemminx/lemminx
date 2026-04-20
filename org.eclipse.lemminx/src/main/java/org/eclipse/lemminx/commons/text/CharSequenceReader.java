package org.eclipse.lemminx.commons.text;

import java.io.IOException;
import java.io.Reader;

final class CharSequenceReader extends Reader {

	private final CharSequence charSequence;
	private int position = 0;

	public CharSequenceReader(CharSequence charSequence) {
		this.charSequence = charSequence;
	}

	@Override
	public int read(char[] cbuf, int off, int len) {
		if (position >= charSequence.length()) {
			return -1;
		}

		int charsToRead = Math.min(len, charSequence.length() - position);
		for (int i = 0; i < charsToRead; i++) {
			cbuf[off + i] = charSequence.charAt(position++);
		}
		return charsToRead;
	}

	@Override
	public void close() throws IOException {
		// rien à fermer
	}
}