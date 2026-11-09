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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to replace an invalid element with a valid RNG pattern or grammar
 * element using SnippetTextEdit for cursor placement.
 *
 * <p>
 * Use cases:
 * </p>
 * <ul>
 * <li>{@code expected_pattern}: replaces an unrecognized element (e.g.
 * {@code <foo/>}) inside {@code <start>} or {@code <define>} with one of the
 * 19 valid RELAX NG pattern elements.</li>
 * <li>{@code expected_define}: replaces an invalid element at the
 * {@code <grammar>} level with a valid grammar child ({@code define},
 * {@code start}, {@code include}, {@code div}).</li>
 * </ul>
 */
public class RNGReplaceElementCodeAction implements ICodeActionParticipant {

	private static final String[] PATTERN_ELEMENTS = { "element", "attribute", "group", "interleave", "choice",
			"optional", "zeroOrMore", "oneOrMore", "list", "mixed", "ref", "parentRef", "empty", "text",
			"value", "data", "notAllowed", "externalRef", "grammar" };

	private static final String[] GRAMMAR_ELEMENTS = { "define", "start", "include", "div" };

	private final String[] elements;

	public RNGReplaceElementCodeAction(String[] elements) {
		this.elements = elements;
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node == null || !node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;
			String tagName = element.getLocalName();
			if (tagName == null) {
				return;
			}
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();
			Range elementRange = new Range(
					document.positionAt(element.getStart()),
					document.positionAt(element.getEnd()));
			for (String replacementElement : elements) {
				if (!replacementElement.equals(tagName)) {
					String replacement = buildReplacement(replacementElement, snippetSupport);
					CodeAction replaceAction = CodeActionFactory.replace(
							"Replace with '" + replacementElement + "'",
							elementRange, replacement, document.getTextDocument(), diagnostic,
							snippetSupport);
					codeActions.add(replaceAction);
				}
			}
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

	private static String buildReplacement(String el, boolean snippetSupport) {
		if ("empty".equals(el) || "text".equals(el) || "notAllowed".equals(el)) {
			return "<" + el + " />";
		}
		if ("externalRef".equals(el)) {
			return snippetSupport
					? "<externalRef href=\"$1\" />"
					: "<externalRef href=\"\" />";
		}
		if ("ref".equals(el) || "parentRef".equals(el)) {
			return snippetSupport
					? "<" + el + " name=\"$1\" />"
					: "<" + el + " name=\"\" />";
		}
		if ("element".equals(el) || "attribute".equals(el)) {
			return snippetSupport
					? "<" + el + " name=\"$1\">\n$0\n</" + el + ">"
					: "<" + el + " name=\"\"></" + el + ">";
		}
		if ("value".equals(el)) {
			return snippetSupport
					? "<value type=\"$1\">$0</value>"
					: "<value type=\"\"></value>";
		}
		if ("data".equals(el)) {
			return snippetSupport
					? "<data type=\"$1\" />"
					: "<data type=\"\" />";
		}
		if ("define".equals(el)) {
			return snippetSupport
					? "<define name=\"$1\">\n$0\n</define>"
					: "<define name=\"\"></define>";
		}
		if ("start".equals(el)) {
			return snippetSupport
					? "<start>\n$0\n</start>"
					: "<start></start>";
		}
		if ("include".equals(el)) {
			return snippetSupport
					? "<include href=\"$1\" />"
					: "<include href=\"\" />";
		}
		return snippetSupport
				? "<" + el + ">$0</" + el + ">"
				: "<" + el + "></" + el + ">";
	}

	/**
	 * Returns the pattern elements array for {@code expected_pattern} errors.
	 */
	public static String[] getPatternElements() {
		return PATTERN_ELEMENTS;
	}

	/**
	 * Returns the grammar elements array for {@code expected_define} errors.
	 */
	public static String[] getGrammarElements() {
		return GRAMMAR_ELEMENTS;
	}
}
