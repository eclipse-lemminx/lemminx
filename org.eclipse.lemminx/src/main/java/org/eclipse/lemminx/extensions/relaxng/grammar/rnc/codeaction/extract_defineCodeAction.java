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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction;

import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAttribute;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCElement;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.StringValue;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonObject;

/**
 * Refactoring code action that extracts an {@code element} or {@code attribute}
 * pattern into a named define.
 *
 * <p>Before:</p>
 * <pre>
 * start = element addressBook {
 *   element card { text }
 * }
 * </pre>
 *
 * <p>After extracting {@code element card { text }}:</p>
 * <pre>
 * start = element addressBook {
 *   card
 * }
 *
 * card = element card { text }
 * </pre>
 */
public class extract_defineCodeAction {

	public static final String PARTICIPANT_ID = extract_defineCodeAction.class.getName();

	private static final String DATA_OFFSET_FIELD = "offset";

	/**
	 * Generates an "Extract define" code action if the cursor is on an
	 * extractable element or attribute node.
	 *
	 * @param rncDocument      the parsed RNC document.
	 * @param range            the selected range in the document.
	 * @param snippetSupported {@code true} if the client supports snippet syntax.
	 * @param resolveSupported {@code true} if the client supports lazy resolve.
	 * @param codeActions      the list to add the created code action to.
	 */
	public void doCodeAction(RNCDocument rncDocument, Range range, boolean snippetSupported,
			boolean resolveSupported, List<CodeAction> codeActions) {
		try {
			int offset = rncDocument.getTextDocument().offsetAt(range.getStart());
			RNCNode target = findExtractableNode(rncDocument.findNodeAt(offset));
			if (target == null) {
				return;
			}

			String name = getNodeName(target);
			if (name == null || name.isEmpty()) {
				return;
			}

			// Generate a unique name if the define already exists
			name = uniqueDefineName(rncDocument, name);

			String title = "Extract define '" + name + "'";

			if (resolveSupported) {
				CodeAction codeAction = new CodeAction(title);
				codeAction.setKind(CodeActionKind.RefactorExtract);
				JsonObject data = DataEntryField.createData(
						rncDocument.getTextDocument().getUri(), PARTICIPANT_ID);
				data.addProperty(DATA_OFFSET_FIELD, offset);
				codeAction.setData(data);
				codeActions.add(codeAction);
			} else {
				WorkspaceEdit edit = computeEdit(rncDocument, target, name, snippetSupported);
				if (edit != null) {
					CodeAction codeAction = new CodeAction(title);
					codeAction.setKind(CodeActionKind.RefactorExtract);
					codeAction.setEdit(edit);
					codeActions.add(codeAction);
				}
			}
		} catch (BadLocationException e) {
			// ignore
		}
	}

	/**
	 * Resolves a deferred "Extract define" code action by computing the
	 * workspace edit (replace inline pattern with name, append define at end).
	 *
	 * @param rncDocument      the parsed RNC document.
	 * @param unresolved       the code action to resolve.
	 * @param snippetSupported {@code true} if the client supports snippet syntax.
	 * @return the resolved code action with its edit populated.
	 */
	public CodeAction resolveCodeAction(RNCDocument rncDocument, CodeAction unresolved,
			boolean snippetSupported) {
		JsonObject data = (JsonObject) unresolved.getData();
		int offset = data.get(DATA_OFFSET_FIELD).getAsInt();
		RNCNode target = findExtractableNode(rncDocument.findNodeAt(offset));
		if (target != null) {
			String name = getNodeName(target);
			if (name != null && !name.isEmpty()) {
				WorkspaceEdit edit = computeEdit(rncDocument, target, name, snippetSupported);
				unresolved.setEdit(edit);
			}
		}
		return unresolved;
	}

	private WorkspaceEdit computeEdit(RNCDocument rncDocument, RNCNode target, String name,
			boolean snippetSupported) {
		CharSequence source = rncDocument.getSource();
		String patternText = source.subSequence(target.getStart(), target.getEnd()).toString();

		// Dedent the pattern text to column 0
		patternText = dedent(patternText);

		Range replaceRange = RNCPositionUtils.createRange(rncDocument.getTextDocument(),
				target.getStart(), target.getEnd());
		if (replaceRange == null) {
			return null;
		}

		String lineSeparator = RNCPositionUtils.getLineSeparator(rncDocument.getTextDocument());
		Position endPos = RNCPositionUtils.getEndOfDocumentPosition(source);
		String prefix = RNCPositionUtils.endsWithNewline(source) ? "" : lineSeparator;

		// When extracting a top-level pattern (direct child of document), replace
		// with "start = name" instead of bare "name"
		String replacement = name;
		if (target.getParent() instanceof RNCDocument && rncDocument.getStartDecl() == null) {
			replacement = "start = " + name;
		}

		TextDocumentItem document = new TextDocumentItem(rncDocument.getTextDocument().getUri(), "rnc", 0, "");
		VersionedTextDocumentIdentifier versionedId = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());

		List<Either<TextEdit, SnippetTextEdit>> edits;
		if (snippetSupported) {
			String snippetName = "${0:" + name + "}";
			String defineText = prefix + lineSeparator + snippetName + " = " + patternText + lineSeparator;
			edits = List.of(
					Either.forLeft(new TextEdit(replaceRange, replacement)),
					Either.forRight(new SnippetTextEdit(new Range(endPos, endPos),
							new StringValue("snippet", defineText))));
		} else {
			String defineText = prefix + lineSeparator + name + " = " + patternText + lineSeparator;
			edits = List.of(
					Either.forLeft(new TextEdit(replaceRange, replacement)),
					Either.forLeft(new TextEdit(new Range(endPos, endPos), defineText)));
		}

		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedId, edits);
		return new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
	}

	/**
	 * Removes common leading whitespace from all lines after the first.
	 */
	private static String dedent(String text) {
		// Detect line separator
		String sep = "\n";
		int crlfIdx = text.indexOf("\r\n");
		if (crlfIdx >= 0) {
			sep = "\r\n";
		}
		String[] lines = text.split("\\r?\\n", -1);
		if (lines.length <= 1) {
			return text;
		}
		// Find common indent of non-empty continuation lines
		int minIndent = Integer.MAX_VALUE;
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			if (line.trim().isEmpty()) {
				continue;
			}
			int indent = 0;
			for (int j = 0; j < line.length(); j++) {
				if (line.charAt(j) == ' ' || line.charAt(j) == '\t') {
					indent++;
				} else {
					break;
				}
			}
			minIndent = Math.min(minIndent, indent);
		}
		if (minIndent == Integer.MAX_VALUE || minIndent == 0) {
			return text;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(lines[0]);
		for (int i = 1; i < lines.length; i++) {
			sb.append(sep);
			String line = lines[i];
			if (line.length() > minIndent) {
				sb.append(line.substring(minIndent));
			} else {
				sb.append(line.trim());
			}
		}
		return sb.toString();
	}

	private static String uniqueDefineName(RNCDocument rncDocument, String baseName) {
		if (rncDocument.findDefine(baseName) == null) {
			return baseName;
		}
		for (int i = 1; ; i++) {
			String candidate = baseName + i;
			if (rncDocument.findDefine(candidate) == null) {
				return candidate;
			}
		}
	}

	private RNCNode findExtractableNode(RNCNode node) {
		while (node != null) {
			if (node instanceof RNCElement || node instanceof RNCAttribute) {
				RNCNode parent = node.getParent();
				if (parent != null && !(parent instanceof RNCDefine)) {
					return node;
				}
			}
			node = node.getParent();
		}
		return null;
	}

	private String getNodeName(RNCNode node) {
		if (node instanceof RNCElement) {
			return ((RNCElement) node).getName();
		}
		if (node instanceof RNCAttribute) {
			return ((RNCAttribute) node).getName();
		}
		return null;
	}
}
