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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Bridges Jing's SAX {@link ErrorHandler} to LSP {@link Diagnostic} objects.
 *
 * <p>When Jing validates an RNC schema via {@code CompactParseable}, it reports
 * errors through SAX's {@link ErrorHandler} interface (line/column based).
 * This class converts those SAX errors into LSP diagnostics with proper
 * ranges, error codes, and severity levels.</p>
 *
 * <p>Use case: Jing reports "reference to undefined pattern" at line 5,
 * column 12. This reporter maps the error message to
 * {@link RNCErrorCode#UndefinedPattern}, uses {@link RNCPositionUtils} to
 * select the word at that position, and produces a diagnostic that highlights
 * the undefined identifier.</p>
 *
 * @see RNCValidator
 * @see RNCErrorCode
 */
public class LSPErrorReporterForRNC implements ErrorHandler {

	private static final String SOURCE = "rnc";

	private final TextDocument document;
	private final List<Diagnostic> diagnostics;
	private final CancelChecker cancelChecker;

	public LSPErrorReporterForRNC(TextDocument document, List<Diagnostic> diagnostics, CancelChecker cancelChecker) {
		this.document = document;
		this.diagnostics = diagnostics;
		this.cancelChecker = cancelChecker;
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		cancelChecker.checkCanceled();
		addDiagnostic(e, DiagnosticSeverity.Warning);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		cancelChecker.checkCanceled();
		addDiagnostic(e, DiagnosticSeverity.Error);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		cancelChecker.checkCanceled();
		addDiagnostic(e, DiagnosticSeverity.Error);
	}

	private void addDiagnostic(SAXParseException e, DiagnosticSeverity severity) {
		String errorSystemId = e.getSystemId();
		String documentUri = document.getUri();
		if (errorSystemId != null && documentUri != null && !errorSystemId.equals(documentUri)) {
			return;
		}
		int line = Math.max(0, e.getLineNumber() - 1);
		int column = Math.max(0, e.getColumnNumber() - 1);
		String message = e.getMessage();
		RNCErrorCode errorCode = RNCErrorCode.fromMessage(message);
		String text = document.getText();
		int offset;
		try {
			offset = document.offsetAt(new Position(line, column));
		} catch (BadLocationException ex) {
			offset = -1;
		}
		message = enhanceMessage(message, text, offset, errorCode);

		// Refine: "syntax error" at '}' inside an empty body → MissingPattern
		if (errorCode == RNCErrorCode.SyntaxError && offset >= 0 && isMissingPattern(text, offset)) {
			errorCode = RNCErrorCode.MissingPattern;
		}

		Range range = computeErrorRange(text, offset, line, column, message, errorCode);
		Diagnostic d = new Diagnostic(range, message, severity, SOURCE);
		if (errorCode != null) {
			d.setCode(errorCode.getCode());
		}
		diagnostics.add(d);
	}

	private static boolean isMissingPattern(String text, int offset) {
		int len = text.length();
		if (len == 0) {
			return false;
		}
		int pos = offset;
		if (pos >= len) {
			pos = len - 1;
		}
		while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}
		if (pos < len && text.charAt(pos) == '}') {
			int p = pos - 1;
			while (p >= 0 && Character.isWhitespace(text.charAt(p))) {
				p--;
			}
			return p >= 0 && text.charAt(p) == '{';
		}
		return false;
	}

	private static String enhanceMessage(String message, String text, int offset, RNCErrorCode errorCode) {
		if (errorCode == null || offset < 0) {
			return message;
		}
		switch (errorCode) {
			case SyntaxError: {
				String token = getTokenAt(text, offset);
				if (token != null) {
					if ("}".equals(token)) {
						return "Expected a pattern (e.g. 'empty', 'text'), found '}'";
					}
					return "Syntax error at '" + token + "'";
				}
				return message;
			}
			case ExpectedToken: {
				String token = getTokenAt(text, offset);
				if (token != null) {
					return message + ", found '" + token + "'";
				}
				return message;
			}
			default:
				return message;
		}
	}

	private static String getTokenAt(String text, int offset) {
		int len = text.length();
		if (offset >= len) {
			offset = len > 0 ? len - 1 : 0;
		}
		int pos = offset;
		while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}
		if (pos >= len) {
			return null;
		}
		char ch = text.charAt(pos);
		if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.') {
			int start = pos;
			int end = pos + 1;
			while (start > 0 && isIdentifierChar(text.charAt(start - 1))) {
				start--;
			}
			while (end < len && isIdentifierChar(text.charAt(end))) {
				end++;
			}
			return text.substring(start, end);
		}
		if (ch == '"') {
			int start = pos - 1;
			while (start > 0 && text.charAt(start) != '"') {
				start--;
			}
			if (start >= 0 && text.charAt(start) == '"') {
				return text.substring(start, pos + 1);
			}
		}
		return String.valueOf(ch);
	}

	private Range computeErrorRange(String text, int offset, int line, int column, String message,
			RNCErrorCode errorCode) {
		if (offset < 0) {
			return new Range(new Position(line, column), new Position(line, column));
		}
		try {
			if (errorCode != null) {
				switch (errorCode) {
					case UndefinedPattern:
					case DuplicateDefinition: {
						String name = RNCErrorCode.extractName(message);
						if (name != null) {
							return RNCPositionUtils.selectWord(document, text, offset, name);
						}
						break;
					}
					case MissingPattern: {
						Range nameRange = RNCPositionUtils.selectNameBeforeBraces(document, text, offset);
						if (nameRange != null) {
							return nameRange;
						}
						break;
					}
					case MissingStartElement: {
						return RNCPositionUtils.selectToken(document, text, offset);
					}
					case InterleaveContainsText: {
						Range mixedRange = selectMixedKeyword(text, offset);
						if (mixedRange != null) {
							return mixedRange;
						}
						break;
					}
					case IncludeNotOverriding: {
						String name = RNCErrorCode.extractName(message);
						if (name != null) {
							Range nameRange = selectWordInBlock(text, offset, name);
							if (nameRange != null) {
								return nameRange;
							}
						}
						break;
					}
					default:
						break;
				}
			}

			return RNCPositionUtils.selectToken(document, text, offset);
		} catch (BadLocationException e) {
			return new Range(new Position(line, column), new Position(line, column));
		}
	}

	private Range selectWordInBlock(String text, int offset, String word) throws BadLocationException {
		int len = text.length();
		int pos = offset;
		int wordLen = word.length();
		int depth = 0;
		while (pos < len) {
			char ch = text.charAt(pos);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				if (depth == 0) {
					break;
				}
				depth--;
			} else if (ch == word.charAt(0) && pos + wordLen <= len) {
				boolean match = true;
				for (int i = 1; i < wordLen; i++) {
					if (text.charAt(pos + i) != word.charAt(i)) {
						match = false;
						break;
					}
				}
				if (match) {
					int end = pos + wordLen;
					boolean startOk = pos == 0 || !isIdentifierChar(text.charAt(pos - 1));
					boolean endOk = end >= len || !isIdentifierChar(text.charAt(end));
					if (startOk && endOk) {
						return new Range(document.positionAt(pos), document.positionAt(end));
					}
				}
			}
			pos++;
		}
		return null;
	}

	private static final String MIXED_KEYWORD = "mixed";

	private Range selectMixedKeyword(String text, int offset) throws BadLocationException {
		int len = text.length();
		int pos = offset;
		int depth = 0;
		while (pos < len) {
			char ch = text.charAt(pos);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				if (depth == 0) {
					break;
				}
				depth--;
			} else if (ch == 'm' && pos + MIXED_KEYWORD.length() <= len) {
				boolean match = true;
				for (int i = 1; i < MIXED_KEYWORD.length(); i++) {
					if (text.charAt(pos + i) != MIXED_KEYWORD.charAt(i)) {
						match = false;
						break;
					}
				}
				if (match) {
					int end = pos + MIXED_KEYWORD.length();
					boolean startOk = pos == 0 || !isIdentifierChar(text.charAt(pos - 1));
					boolean endOk = end >= len || !isIdentifierChar(text.charAt(end));
					if (startOk && endOk) {
						return new Range(document.positionAt(pos), document.positionAt(end));
					}
				}
			}
			pos++;
		}
		return null;
	}

	private static boolean isIdentifierChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.';
	}
}
