/**
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import static org.eclipse.lemminx.utils.StringUtils.isSimilar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for MSG_CONTENT_INVALID.
 *
 * <p>
 * The error indicates that the children of an element don't match the expected
 * content model. This code action proposes:
 * </p>
 * <ul>
 * <li>Removing child elements not allowed by the content model</li>
 * <li>Replacing child elements with similarly-named expected elements
 * (typos like {@code <headign>} → {@code <heading>})</li>
 * <li>Reordering child elements when all are valid but in wrong order</li>
 * <li>Inserting missing required child elements</li>
 * </ul>
 *
 * <pre>
 * &lt;!ELEMENT note (to,from,heading,body)&gt;
 *
 * &lt;!-- Remove undeclared element --&gt;
 * &lt;note&gt;
 *   &lt;to&gt;Tove&lt;/to&gt;
 *   &lt;XXX&gt;undeclared&lt;/XXX&gt;  ← "Remove element 'XXX'"
 *   &lt;from&gt;Jani&lt;/from&gt;
 * &lt;/note&gt;
 *
 * &lt;!-- Reorder elements --&gt;
 * &lt;note&gt;
 *   &lt;from&gt;Jani&lt;/from&gt;     ← wrong order
 *   &lt;to&gt;Tove&lt;/to&gt;         ← "Reorder child elements"
 *   &lt;heading&gt;...&lt;/heading&gt;
 *   &lt;body&gt;...&lt;/body&gt;
 * &lt;/note&gt;
 *
 * &lt;!-- Insert missing element --&gt;
 * &lt;note&gt;
 *   &lt;to&gt;Tove&lt;/to&gt;
 *   &lt;heading&gt;...&lt;/heading&gt;  ← &lt;from&gt; missing → "Insert missing element 'from'"
 *   &lt;body&gt;...&lt;/body&gt;
 * &lt;/note&gt;
 * </pre>
 */
public class MSG_CONTENT_INVALIDCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		Diagnostic diagnostic = request.getDiagnostic();
		try {
			Range diagnosticRange = diagnostic.getRange();
			int startOffset = document.offsetAt(diagnosticRange.getStart()) + 1;
			DOMNode node = document.findNodeAt(startOffset);

			if (node == null || !node.isElement()) {
				return;
			}

			DOMElement parentElement = (DOMElement) node;

			List<String> expectedOrder = collectExpectedChildOrder(parentElement, request);
			if (expectedOrder.isEmpty()) {
				return;
			}
			Set<String> expectedNames = new HashSet<>(expectedOrder);

			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();

			List<DOMElement> childElements = new ArrayList<>();
			boolean hasInvalidChild = false;
			for (DOMNode child : parentElement.getChildren()) {
				if (!child.isElement()) {
					continue;
				}
				DOMElement childElement = (DOMElement) child;
				String childName = childElement.getLocalName();
				if (childName == null) {
					continue;
				}
				childElements.add(childElement);

				if (!expectedNames.contains(childName)) {
					hasInvalidChild = true;
					addRemoveCodeAction(childElement, childName, document, diagnostic, snippetSupport,
							codeActions);
					addReplaceWithSimilarCodeActions(childElement, childName, expectedNames, document,
							diagnostic, snippetSupport, codeActions);
				}
			}

			if (!hasInvalidChild && childElements.size() > 1) {
				addReorderCodeAction(childElements, expectedOrder, document, diagnostic, codeActions);
			}

			addInsertMissingCodeActions(childElements, expectedOrder, expectedNames, parentElement,
					document, diagnostic, snippetSupport, codeActions);

		} catch (BadLocationException e) {
			// do nothing
		}
	}

	private List<String> collectExpectedChildOrder(DOMElement parentElement, ICodeActionRequest request) {
		ContentModelManager cmManager = request.getComponent(ContentModelManager.class);
		List<String> expectedOrder = new ArrayList<>();
		for (CMDocument cmDoc : cmManager.findCMDocument(parentElement)) {
			CMElementDeclaration cmElement = cmDoc.findCMElement(parentElement);
			if (cmElement != null) {
				for (CMElementDeclaration child : cmElement.getElements()) {
					expectedOrder.add(child.getLocalName());
				}
				if (!expectedOrder.isEmpty()) {
					break;
				}
			}
		}
		return expectedOrder;
	}

	/**
	 * Adds a "Remove element" code action that removes the entire child element
	 * line.
	 */
	private void addRemoveCodeAction(DOMElement childElement, String childName, DOMDocument document,
			Diagnostic diagnostic, boolean snippetSupport, List<CodeAction> codeActions)
			throws BadLocationException {
		int startLine = document.positionAt(childElement.getStart()).getLine();
		int endLine = document.positionAt(childElement.getEnd()).getLine();
		Range removeRange = new Range(new Position(startLine, 0), new Position(endLine + 1, 0));
		String replaceText = snippetSupport ? "$0" : "";
		codeActions.add(CodeActionFactory.replace(
				"Remove element '" + childName + "'",
				removeRange, replaceText, document.getTextDocument(), diagnostic, snippetSupport));
	}

	/**
	 * Adds "Replace with" code actions for expected elements whose names are
	 * similar to the invalid child (handles typos like {@code <headign>} →
	 * {@code <heading>}).
	 */
	private void addReplaceWithSimilarCodeActions(DOMElement childElement, String childName,
			Set<String> expectedNames, DOMDocument document, Diagnostic diagnostic,
			boolean snippetSupport, List<CodeAction> codeActions) {
		Collection<String> similarNames = new TreeSet<>(Collator.getInstance());
		for (String expected : expectedNames) {
			if (isSimilar(expected, childName)) {
				similarNames.add(expected);
			}
		}

		if (similarNames.isEmpty()) {
			return;
		}

		List<Range> ranges = new ArrayList<>();
		Range startRange = XMLPositionUtility.selectStartTagName(childElement);
		ranges.add(startRange);
		Range endRange = XMLPositionUtility.selectEndTagName(childElement);
		if (endRange != null) {
			ranges.add(endRange);
		}

		for (String name : similarNames) {
			String replaceText = snippetSupport
					? CodeActionFactory.escapeSnippetText(name) + "$0"
					: name;
			codeActions.add(CodeActionFactory.replaceAt(
					"Did you mean '" + name + "'?", replaceText,
					document.getTextDocument(), diagnostic, ranges, snippetSupport));
		}
	}

	/**
	 * Adds a "Reorder child elements" code action when all children are valid
	 * but in wrong order relative to the content model sequence.
	 */
	private void addReorderCodeAction(List<DOMElement> childElements, List<String> expectedOrder,
			DOMDocument document, Diagnostic diagnostic, List<CodeAction> codeActions)
			throws BadLocationException {
		Map<String, Integer> orderIndex = new LinkedHashMap<>();
		for (int i = 0; i < expectedOrder.size(); i++) {
			orderIndex.put(expectedOrder.get(i), i);
		}

		List<DOMElement> sorted = new ArrayList<>(childElements);
		sorted.sort((a, b) -> {
			int ia = orderIndex.getOrDefault(a.getLocalName(), Integer.MAX_VALUE);
			int ib = orderIndex.getOrDefault(b.getLocalName(), Integer.MAX_VALUE);
			return Integer.compare(ia, ib);
		});

		boolean needsReorder = false;
		for (int i = 0; i < childElements.size(); i++) {
			if (childElements.get(i) != sorted.get(i)) {
				needsReorder = true;
				break;
			}
		}
		if (!needsReorder) {
			return;
		}

		DOMElement firstChild = childElements.get(0);
		DOMElement lastChild = childElements.get(childElements.size() - 1);
		int firstLine = document.positionAt(firstChild.getStart()).getLine();
		Position lastChildEnd = document.positionAt(lastChild.getEnd());
		String lineDelimiter = document.lineDelimiter(firstLine);
		String indent = extractLeadingWhitespace(document.lineText(firstLine));

		String text = document.getText();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sorted.size(); i++) {
			DOMElement child = sorted.get(i);
			sb.append(indent);
			sb.append(text, child.getStart(), child.getEnd());
			if (i < sorted.size() - 1) {
				sb.append(lineDelimiter);
			}
		}

		Range replaceRange = new Range(new Position(firstLine, 0), lastChildEnd);
		codeActions.add(CodeActionFactory.replace(
				"Reorder child elements",
				replaceRange, sb.toString(), document.getTextDocument(), diagnostic));
	}

	/**
	 * Adds "Insert missing element" code actions for required elements that are
	 * absent from the parent's children. Skips elements that have a similar typo
	 * already present (covered by the replace code action instead).
	 */
	private void addInsertMissingCodeActions(List<DOMElement> childElements, List<String> expectedOrder,
			Set<String> expectedNames, DOMElement parentElement, DOMDocument document,
			Diagnostic diagnostic, boolean snippetSupport, List<CodeAction> codeActions)
			throws BadLocationException {
		Set<String> presentNames = new HashSet<>();
		for (DOMElement child : childElements) {
			String name = child.getLocalName();
			if (name != null) {
				presentNames.add(name);
			}
		}

		for (String expected : expectedOrder) {
			if (presentNames.contains(expected)) {
				continue;
			}
			if (hasSimilarTypoPresent(expected, presentNames, expectedNames)) {
				continue;
			}

			Position insertPos;
			String indent;
			String lineDelimiter;
			if (childElements.isEmpty()) {
				int startTagClose = parentElement.getStartTagCloseOffset();
				int lineNum = document.positionAt(startTagClose).getLine();
				lineDelimiter = document.lineDelimiter(lineNum);
				String parentLineText = document.lineText(lineNum);
				indent = extractLeadingWhitespace(parentLineText) + "\t";
				insertPos = new Position(lineNum + 1, 0);
			} else {
				int expectedIdx = expectedOrder.indexOf(expected);
				DOMElement insertAfter = findPredecessor(childElements, expectedOrder, expectedIdx);
				if (insertAfter != null) {
					int lineNum = document.positionAt(insertAfter.getEnd()).getLine();
					lineDelimiter = document.lineDelimiter(lineNum);
					indent = extractLeadingWhitespace(document.lineText(lineNum));
					insertPos = new Position(lineNum + 1, 0);
				} else {
					DOMElement firstChild = childElements.get(0);
					int lineNum = document.positionAt(firstChild.getStart()).getLine();
					lineDelimiter = document.lineDelimiter(lineNum);
					indent = extractLeadingWhitespace(document.lineText(lineNum));
					insertPos = new Position(lineNum, 0);
				}
			}

			String insertText;
			if (snippetSupport) {
				insertText = indent + "<" + expected + ">$0</" + expected + ">" + lineDelimiter;
			} else {
				insertText = indent + "<" + expected + "></" + expected + ">" + lineDelimiter;
			}

			codeActions.add(CodeActionFactory.insert(
					"Insert missing element '" + expected + "'",
					insertPos, insertText, document.getTextDocument(), diagnostic, snippetSupport));
		}
	}

	/**
	 * Returns true if a present invalid child has a name similar to the expected
	 * element (i.e., a typo replacement is already available).
	 */
	private boolean hasSimilarTypoPresent(String expected, Set<String> presentNames,
			Set<String> expectedNames) {
		for (String present : presentNames) {
			if (!expectedNames.contains(present) && isSimilar(expected, present)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the last child element whose expected position comes before
	 * {@code expectedIdx} in the content model, so we can insert after it.
	 */
	private DOMElement findPredecessor(List<DOMElement> childElements, List<String> expectedOrder,
			int expectedIdx) {
		DOMElement result = null;
		for (DOMElement child : childElements) {
			String childName = child.getLocalName();
			int childIdx = expectedOrder.indexOf(childName);
			if (childIdx >= 0 && childIdx < expectedIdx) {
				result = child;
			}
		}
		return result;
	}

	private static String extractLeadingWhitespace(String line) {
		int i = 0;
		while (i < line.length() && Character.isWhitespace(line.charAt(i))
				&& line.charAt(i) != '\n' && line.charAt(i) != '\r') {
			i++;
		}
		return line.substring(0, i);
	}
}
