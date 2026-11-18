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
package org.eclipse.lemminx.extensions.index.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

/**
 * Lightweight streaming XML scanner for workspace Bloom-index collection.
 *
 * <p><b>Design goals</b>
 * <ul>
 *   <li><b>Reader-based</b> — operates on a {@link java.io.Reader}; never loads
 *       the whole file into memory. Peak heap per file = the {@link BufferedReader}
 *       internal buffer (~8 KB) plus the small strings captured per token.</li>
 *   <li><b>Forward-only</b> — no backtracking beyond a single character; a
 *       16-char {@link PushbackReader} buffer is enough for all look-ahead.</li>
 *   <li><b>No DOM</b> — produces only the {@link IndexTokenType}s needed to build
 *       a {@link org.eclipse.lemminx.extensions.index.BloomFilter} and locate
 *       {@link org.eclipse.lemminx.extensions.index.GrammarReference}s.
 *       All other XML constructs are skipped without allocation.</li>
 *   <li><b>Inline position tracking</b> — {@code line} and {@code col} are
 *       updated on every {@link #read()} call; no {@code positionAt(offset)}
 *       look-up against a full-document line index is needed.</li>
 * </ul>
 *
 * <p><b>Usage</b>
 * <pre>
 * try (IndexScanner scanner = new IndexScanner(Files.newBufferedReader(path))) {
 *     IndexTokenType token;
 *     while ((token = scanner.next()) != IndexTokenType.EOS) {
 *         switch (token) {
 *             case START_TAG      -> System.out.println("element: " + scanner.getValue());
 *             case ATTRIBUTE_NAME -> System.out.println("attr: "    + scanner.getValue());
 *             case ATTRIBUTE_VALUE-> System.out.println("value: "   + scanner.getValue());
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 *
 * <p><b>Position</b>: {@link #getLine()} and {@link #getStartChar()} /
 * {@link #getEndChar()} are 0-indexed, matching LSP conventions.
 * They are captured at the start of each token's value content (quotes stripped
 * for attribute values and DOCTYPE identifiers).
 *
 * <p><b>Thread safety</b>: not thread-safe; use one instance per thread.
 */
public final class IndexScanner implements AutoCloseable {

	// ---- State constants ----
	private static final int S_DOCUMENT  = 0; // scanning document, looking for '<'
	private static final int S_PI_AFTER  = 1; // just yielded PI_NAME, next = PI_CONTENT
	private static final int S_IN_TAG    = 2; // inside <tag …>, yielding attrs
	private static final int S_ATTR_VAL  = 3; // yielded ATTR_NAME, attr value pending
	private static final int S_DOCTYPE   = 4; // inside <!DOCTYPE …>
	private static final int S_DONE      = 5; // EOF reached

	// 16-char pushback is enough for the longest literal we match: "OCTYPE" (6) or "CDATA[" (6)
	private static final int PUSHBACK_SIZE = 16;

	private final PushbackReader reader;
	private final int maxChars;
	private boolean reachedLimit = false;
	private int state = S_DOCUMENT;

	// Current reader position (updated by read / unread)
	private int offset = 0;
	private int line   = 0;
	private int col    = 0;

	// Current token position
	private int tokenOffset;
	private int tokenLine;
	private int tokenStartChar;
	private int tokenEndChar;
	private String value = "";

	// Pending attribute value queued after ATTRIBUTE_NAME is returned
	private String pendingValue;
	private int    pendingOffset;
	private int    pendingLine;
	private int    pendingStartChar;
	private int    pendingEndChar;

	private final StringBuilder sb = new StringBuilder(128);

	/**
	 * Creates a scanner that reads from {@code reader} with no character limit.
	 * The reader is wrapped in a {@link BufferedReader} (8 KB) unless it already is one.
	 *
	 * @param reader source of XML content; the scanner takes ownership and closes it.
	 */
	public IndexScanner(Reader reader) {
		this(reader, Integer.MAX_VALUE);
	}

	/**
	 * Creates a scanner that reads from {@code reader} and stops after {@code maxChars}
	 * characters. Use {@link #isPartial()} after scanning to check whether the limit was hit.
	 *
	 * @param reader   source of XML content; the scanner takes ownership and closes it.
	 * @param maxChars maximum characters to consume; {@link Integer#MAX_VALUE} = no limit.
	 */
	public IndexScanner(Reader reader, int maxChars) {
		Reader br = reader instanceof BufferedReader ? reader : new BufferedReader(reader, 8192);
		this.reader = new PushbackReader(br, PUSHBACK_SIZE);
		this.maxChars = maxChars;
	}

	/**
	 * Advances to the next significant token and returns its type.
	 *
	 * @return the next token, or {@link IndexTokenType#EOS} at end of stream.
	 * @throws IOException if the underlying reader fails.
	 */
	public IndexTokenType next() throws IOException {
		while (true) {
			switch (state) {
				case S_DOCUMENT: { IndexTokenType t = nextDocument();  if (t != null) return t; break; }
				case S_PI_AFTER: { return nextPIContent(); }
				case S_IN_TAG:   { IndexTokenType t = nextTagAttr();   if (t != null) return t; break; }
				case S_ATTR_VAL: { return nextAttrValue(); }
				case S_DOCTYPE:  { IndexTokenType t = nextDoctype();   if (t != null) return t; break; }
				default:           return IndexTokenType.EOS;
			}
		}
	}

	/** Returns the string value of the last token (quotes stripped for values). */
	public String getValue()      { return value; }

	/** Absolute character offset of the token start in the source stream. */
	public int    getOffset()     { return tokenOffset; }

	/** 0-indexed line number of the token start. */
	public int    getLine()       { return tokenLine; }

	/** 0-indexed start column of the token value (LSP {@code character}). */
	public int    getStartChar()  { return tokenStartChar; }

	/** 0-indexed exclusive end column of the token value. */
	public int    getEndChar()    { return tokenEndChar; }

	/**
	 * Returns {@code true} if scanning stopped because the {@code maxChars} limit was reached
	 * before the end of the stream. When {@code true}, callers should treat the indexed file
	 * as partially scanned and conservatively include it as a candidate in all lookups.
	 */
	public boolean isPartial() {
		return reachedLimit;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	// ---- State handlers ----

	private IndexTokenType nextDocument() throws IOException {
		int c;
		while ((c = read()) != -1) {
			if (c != '<') continue;
			int d = read();
			if (d == '?') {
				return scanPIName();
			} else if (d == '!') {
				handleBang();
				if (state == S_DOCTYPE) return null; // re-enter main loop for DOCTYPE
			} else if (d == '/') {
				skipTo('>'); // end tag — skip entirely
			} else if (d != -1 && isNameStart(d)) {
				unread(d);
				return scanStartTag();
			} else {
				if (d != -1) unread(d);
			}
		}
		state = S_DONE;
		return null;
	}

	private IndexTokenType scanPIName() throws IOException {
		sb.setLength(0);
		skipWhitespace();
		captureStart();
		int c;
		while ((c = read()) != -1 && !Character.isWhitespace(c) && c != '?' && c != '>') {
			sb.append((char) c);
		}
		if (c != -1) unread(c);
		captureValue();
		state = S_PI_AFTER;
		return IndexTokenType.PI_NAME;
	}

	private IndexTokenType nextPIContent() throws IOException {
		sb.setLength(0);
		skipWhitespace();
		captureStart();
		int c;
		while ((c = read()) != -1) {
			if (c == '?') {
				int d = read();
				if (d == '>') break;
				sb.append((char) c);
				if (d != -1) sb.append((char) d);
			} else {
				sb.append((char) c);
			}
		}
		value = sb.toString();
		tokenEndChar = col;
		state = S_DOCUMENT;
		return IndexTokenType.PI_CONTENT;
	}

	private IndexTokenType scanStartTag() throws IOException {
		sb.setLength(0);
		captureStart();
		int c;
		while ((c = read()) != -1 && isNameChar(c)) {
			sb.append((char) c);
		}
		if (c != -1) unread(c);
		// Strip namespace prefix: "xs:element" → "element"
		String full = sb.toString();
		int colon = full.lastIndexOf(':');
		value = colon >= 0 && colon < full.length() - 1 ? full.substring(colon + 1) : full;
		tokenEndChar = tokenStartChar + full.length();
		state = S_IN_TAG;
		return IndexTokenType.START_TAG;
	}

	private IndexTokenType nextTagAttr() throws IOException {
		while (true) {
			skipWhitespace();
			int c = read();
			if (c == -1 || c == '>') { state = S_DOCUMENT; return null; }
			if (c == '/') {
				int d = read();
				if (d != '>') if (d != -1) unread(d);
				state = S_DOCUMENT;
				return null;
			}
			if (!isNameStart(c)) continue; // skip unexpected chars
			unread(c);
			return scanAttrNameAndValue();
		}
	}

	private IndexTokenType scanAttrNameAndValue() throws IOException {
		// --- Attribute name ---
		sb.setLength(0);
		captureStart();
		int c;
		while ((c = read()) != -1 && isAttrNameChar(c)) {
			sb.append((char) c);
		}
		if (c != -1) unread(c);
		captureValue();

		// --- Skip '=' and read quoted value ---
		skipWhitespace();
		int eq = read();
		if (eq == '=') {
			skipWhitespace();
			int q = read();
			if (q == '"' || q == '\'') {
				// Capture position right after opening quote (content start)
				pendingOffset    = offset;
				pendingLine      = line;
				pendingStartChar = col;
				sb.setLength(0);
				while ((c = read()) != -1 && c != q) {
					sb.append((char) c);
				}
				pendingValue    = sb.toString();
				pendingEndChar  = pendingStartChar + pendingValue.length();
				state = S_ATTR_VAL;
			}
			// No quoted value: attribute without value or unquoted — skip
		} else {
			if (eq != -1) unread(eq);
		}
		return IndexTokenType.ATTRIBUTE_NAME;
	}

	private IndexTokenType nextAttrValue() {
		value          = pendingValue;
		tokenOffset    = pendingOffset;
		tokenLine      = pendingLine;
		tokenStartChar = pendingStartChar;
		tokenEndChar   = pendingEndChar;
		pendingValue   = null;
		state          = S_IN_TAG;
		return IndexTokenType.ATTRIBUTE_VALUE;
	}

	/**
	 * Handles {@code <!}: identifies comment ({@code <!--}), DOCTYPE
	 * ({@code <!DOCTYPE}), or CDATA ({@code <![CDATA[}) and delegates accordingly.
	 * Sets {@code state = S_DOCTYPE} when a DOCTYPE declaration is found.
	 */
	private void handleBang() throws IOException {
		int c = read();
		if (c == '-') {
			int d = read();
			if (d == '-') { skipComment(); return; }
			if (d != -1) unread(d);
			unread(c);
			skipTo('>');
		} else if (c == 'D') {
			if (matchLiteralTracked("OCTYPE")) { state = S_DOCTYPE; }
			else { unread(c); skipTo('>'); }
		} else if (c == '[') {
			if (matchLiteralTracked("CDATA[")) { skipCDATA(); }
			else { unread(c); skipTo('>'); }
		} else {
			if (c != -1) unread(c);
			skipTo('>');
		}
	}

	private IndexTokenType nextDoctype() throws IOException {
		int c;
		while ((c = read()) != -1) {
			if (c == '>') { state = S_DOCUMENT; return null; }
			if (c == '[') { skipInternalSubset(); }
			else if (c == '"' || c == '\'') {
				// Bare quoted string following PUBLIC id — treat as SYSTEM id
				unread(c);
				return readQuotedId(IndexTokenType.DOCTYPE_SYSTEM_ID);
			} else if (c == 'S') {
				if (matchLiteralTracked("YSTEM")) {
					skipWhitespace();
					return readQuotedId(IndexTokenType.DOCTYPE_SYSTEM_ID);
				}
			} else if (c == 'P') {
				if (matchLiteralTracked("UBLIC")) {
					skipWhitespace();
					return readQuotedId(IndexTokenType.DOCTYPE_PUBLIC_ID);
					// Stays in S_DOCTYPE so the SYSTEM id is picked up on next call
				}
			}
		}
		state = S_DONE;
		return null;
	}

	private IndexTokenType readQuotedId(IndexTokenType type) throws IOException {
		int q = read();
		if (q != '"' && q != '\'') { if (q != -1) unread(q); return null; }
		sb.setLength(0);
		captureStart();
		int c;
		while ((c = read()) != -1 && c != q) {
			sb.append((char) c);
		}
		captureValue();
		return type;
	}

	// ---- Low-level position helpers ----

	private void captureStart() {
		tokenOffset    = offset;
		tokenLine      = line;
		tokenStartChar = col;
	}

	private void captureValue() {
		value        = sb.toString();
		tokenEndChar = tokenStartChar + value.length();
	}

	private int read() throws IOException {
		if (offset >= maxChars) {
			reachedLimit = true;
			return -1;
		}
		int c = reader.read();
		if (c == -1) return -1;
		offset++;
		if (c == '\n') { line++; col = 0; }
		else col++;
		return c;
	}

	private void unread(int c) throws IOException {
		if (c == -1) return;
		reader.unread(c);
		offset--;
		if (c == '\n') { line = Math.max(0, line - 1); col = 0; }
		else col = Math.max(0, col - 1);
	}

	// ---- Skip helpers ----

	private void skipWhitespace() throws IOException {
		int c;
		while ((c = read()) != -1 && Character.isWhitespace(c)) { }
		if (c != -1) unread(c);
	}

	private void skipTo(int end) throws IOException {
		int c;
		while ((c = read()) != -1 && c != end) { }
	}

	private void skipComment() throws IOException {
		// Already consumed '<!--'; skip until '-->'
		int c;
		while ((c = read()) != -1) {
			if (c == '-') {
				int d = read();
				if (d == '-') {
					int e = read();
					if (e == '>') return;
					if (e != -1) unread(e);
				} else {
					if (d != -1) unread(d);
				}
			}
		}
	}

	private void skipCDATA() throws IOException {
		// Already consumed '<![CDATA['; skip until ']]>'
		int c;
		while ((c = read()) != -1) {
			if (c == ']') {
				int d = read();
				if (d == ']') {
					int e = read();
					if (e == '>') return;
					if (e != -1) unread(e);
				} else {
					if (d != -1) unread(d);
				}
			}
		}
	}

	private void skipInternalSubset() throws IOException {
		// Already consumed '['; skip until matching ']' (tracking depth)
		int depth = 1;
		int c;
		while ((c = read()) != -1) {
			if (c == '[') depth++;
			else if (c == ']') { if (--depth == 0) return; }
		}
	}

	/**
	 * Reads and matches {@code s} character-by-character with full position tracking.
	 * On mismatch, all consumed characters are pushed back and positions are restored.
	 *
	 * <p>Assumes {@code s} contains no newlines (true for all patterns used here:
	 * {@code "OCTYPE"}, {@code "CDATA["}, {@code "YSTEM"}, {@code "UBLIC"}).
	 */
	private boolean matchLiteralTracked(String s) throws IOException {
		char[] buf = new char[s.length()];
		int count = 0;
		boolean match = true;
		for (int i = 0; i < s.length(); i++) {
			int c = read();
			if (c == -1) { match = false; break; }
			buf[count++] = (char) c;
			if (c != s.charAt(i)) { match = false; break; }
		}
		if (!match) {
			// Push back consumed chars in reverse; manually undo position tracking
			for (int i = count - 1; i >= 0; i--) {
				reader.unread(buf[i]);
				offset--;
				if (buf[i] == '\n') { line = Math.max(0, line - 1); col = 0; }
				else col = Math.max(0, col - 1);
			}
		}
		return match && count == s.length();
	}

	// ---- Character classification ----

	private static boolean isNameStart(int c) {
		return c == '_' || c == ':' || Character.isLetter(c);
	}

	private static boolean isNameChar(int c) {
		return c == '_' || c == ':' || c == '-' || c == '.' || Character.isLetterOrDigit(c);
	}

	private static boolean isAttrNameChar(int c) {
		return c != -1
				&& !Character.isWhitespace(c)
				&& c != '=' && c != '>' && c != '/' && c != '<'
				&& c != '"' && c != '\'';
	}
}
