/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.lsp4j.Diagnostic;

import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;

/**
 * Error codes for RNC (RELAX NG Compact Syntax) diagnostics.
 *
 * <p>Each error code maps to a specific type of problem detected during
 * parsing or validation of an RNC document. The codes are divided into:</p>
 * <ul>
 *   <li><strong>Parser errors</strong> — detected by {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser}
 *       during Phase 1 validation (e.g., missing braces, unclosed blocks).</li>
 *   <li><strong>Jing validation errors</strong> — detected by Jing during Phase 2
 *       validation (e.g., undefined patterns, duplicate definitions).</li>
 * </ul>
 *
 * <p>Use case: error codes are used to match diagnostics with their
 * corresponding {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction.IRNCCodeActionParticipant}
 * for quick fixes.</p>
 *
 * @see RNCValidator
 * @see LSPErrorReporterForRNC
 */
public enum RNCErrorCode implements IXMLErrorCode {

	// ---------- Parser errors ----------

	/** An {@code element}, {@code attribute}, or {@code namespace} keyword is missing its name. */
	MissingName("missing-name"),

	/** An {@code element} or {@code attribute} definition is missing the opening {@code &#123;}. */
	MissingOpenBrace("missing-open-brace"),

	/** A define is missing its assignment operator ({@code =}, {@code |=}, or {@code &=}). */
	MissingAssign("missing-assign"),

	/** An {@code include} or {@code external} directive is missing the quoted URI. */
	MissingUri("missing-uri"),

	/** A {@code &#123;} brace was opened but never closed. */
	UnclosedBlock("unclosed-block"),

	/** An included or external file does not exist on disk. */
	FileNotFound("file-not-found"),

	// ---------- Jing validation errors ----------

	/** A pattern references a define name that is not declared. */
	UndefinedPattern("undefined-pattern"),

	/** Two or more defines share the same name without compatible combine modes. */
	DuplicateDefinition("duplicate-definition"),

	/** An {@code element} or {@code attribute} block is empty (no content pattern). */
	MissingPattern("missing-pattern"),

	/** The grammar has defines but no {@code start} declaration. */
	MissingStartElement("missing-start-element"),

	/** A generic syntax error reported by Jing. */
	SyntaxError("syntax-error"),

	/** Jing expected a specific token (e.g. {@code "}"}) but found something else. */
	ExpectedToken("expected-token"),

	/** Both operands of an interleave ({@code &}) contain {@code text}. */
	InterleaveContainsText("interleave-contains-text"),

	/** An include body overrides a define that does not exist in the included file. */
	IncludeNotOverriding("include-not-overriding"),

	// ---------- Hints ----------

	/** A define is declared but never referenced (shown with {@code DiagnosticTag.Unnecessary}). */
	UnusedDefine("unused-define");

	private static final String UNDEFINED_PATTERN_PREFIX = "reference to undefined pattern \"";
	private static final String DUPLICATE_DEFINITION_PREFIX = "duplicate definition of \"";
	private static final String MULTIPLE_DEFINITIONS_PREFIX = "multiple definitions of \"";
	private static final String INCLUDE_NOT_OVERRIDING_PREFIX = "definition of \"";
	private static final String INCLUDE_NOT_OVERRIDING_SUFFIX = "does not override anything";
	private static final String MISSING_START_MESSAGE = "missing \"start\" element";
	private static final String INTERLEAVE_TEXT_MESSAGE = "both operands of \"interleave\" contain \"text\"";

	private final String code;

	private static final Map<String, RNCErrorCode> codes;

	static {
		codes = new HashMap<>();
		for (RNCErrorCode errorCode : values()) {
			codes.put(errorCode.code, errorCode);
		}
	}

	RNCErrorCode(String code) {
		this.code = code;
	}

	/**
	 * Returns the kebab-case diagnostic code string (e.g. {@code "undefined-pattern"}).
	 *
	 * @return the code used as the {@code Diagnostic.code} in LSP responses.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Looks up an error code by its kebab-case string.
	 *
	 * <p>Use case: when resolving code actions, the diagnostic code is matched
	 * to its enum constant to find the corresponding code action participant.</p>
	 *
	 * @param code the kebab-case code string.
	 * @return the matching enum constant, or {@code null} if not found.
	 */
	public static RNCErrorCode fromCode(String code) {
		return codes.get(code);
	}

	/**
	 * Determines the error code from a Jing validation error message by
	 * matching known message prefixes.
	 *
	 * <p>Use case: Jing reports errors as free-text messages. This method
	 * maps messages like {@code "reference to undefined pattern \"foo\""}
	 * to {@link #UndefinedPattern} so that diagnostics get structured codes
	 * and matching code actions.</p>
	 *
	 * @param message the Jing error message.
	 * @return the matching error code, or {@code null} if the message is
	 *         not recognized.
	 */
	public static RNCErrorCode fromMessage(String message) {
		if (message == null) {
			return null;
		}
		if (message.startsWith(UNDEFINED_PATTERN_PREFIX)) {
			return UndefinedPattern;
		}
		if (message.startsWith(DUPLICATE_DEFINITION_PREFIX) || message.startsWith(MULTIPLE_DEFINITIONS_PREFIX)) {
			return DuplicateDefinition;
		}
		if (MISSING_START_MESSAGE.equals(message)) {
			return MissingStartElement;
		}
		if ("syntax error".equals(message)) {
			return SyntaxError;
		}
		if (message.startsWith("expected ")) {
			return ExpectedToken;
		}
		if (INTERLEAVE_TEXT_MESSAGE.equals(message)) {
			return InterleaveContainsText;
		}
		if (message.startsWith(INCLUDE_NOT_OVERRIDING_PREFIX) && message.endsWith(INCLUDE_NOT_OVERRIDING_SUFFIX)) {
			return IncludeNotOverriding;
		}
		return null;
	}

	/**
	 * Extracts the quoted name from a diagnostic message (e.g. the pattern name
	 * from {@code "reference to undefined pattern \"foo\""}).
	 *
	 * <p>Use case: code actions like "Did you mean" and "Create define" need
	 * the offending name to generate fix suggestions.</p>
	 *
	 * @param diagnostic the LSP diagnostic whose message to parse.
	 * @return the extracted name, or {@code null} if the message format is
	 *         not recognized.
	 */
	public static String extractName(Diagnostic diagnostic) {
		String message = diagnostic.getMessage() != null && diagnostic.getMessage().isLeft()
				? diagnostic.getMessage().getLeft()
				: "";
		return extractName(message);
	}

	/**
	 * Extracts the quoted name from a raw error message string.
	 *
	 * @param message the error message text.
	 * @return the extracted name, or {@code null} if no known prefix matches.
	 */
	public static String extractName(String message) {
		String prefix = null;
		if (message.startsWith(UNDEFINED_PATTERN_PREFIX)) {
			prefix = UNDEFINED_PATTERN_PREFIX;
		} else if (message.startsWith(DUPLICATE_DEFINITION_PREFIX)) {
			prefix = DUPLICATE_DEFINITION_PREFIX;
		} else if (message.startsWith(MULTIPLE_DEFINITIONS_PREFIX)) {
			prefix = MULTIPLE_DEFINITIONS_PREFIX;
		} else if (message.startsWith(INCLUDE_NOT_OVERRIDING_PREFIX)) {
			prefix = INCLUDE_NOT_OVERRIDING_PREFIX;
		}
		if (prefix == null) {
			return null;
		}
		int start = prefix.length();
		int end = message.indexOf('"', start);
		if (end < 0) {
			return null;
		}
		return message.substring(start, end);
	}
}
