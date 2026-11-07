/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.dom.LineIndentInfo;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix close start tag element.
 *
 */
public class CloseTagCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int startOffset = document.offsetAt(diagnosticRange.getStart()) + 1;
			DOMNode node = document.findNodeAt(startOffset);
			if (node == null || !node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();
			if (element.hasEndTag() && !element.isEndTagClosed() && element.hasTagName()) {
				// Code actions for fixing end tag
				doCodeActionsForEndTagUnclosed(element, document, diagnostic, codeActions, snippetSupport);
			} else {
				// Code actions for fixing start tag
				boolean startTagClosed = element.isStartTagClosed();
				if (!startTagClosed) {
					doCodeActionsForStartTagUnclosed(element, document, diagnosticRange, diagnostic, codeActions,
							snippetSupport);
				} else {
					doCodeActionsForStartTagClosed(element, document, diagnostic, codeActions,
							snippetSupport, request);
				}
			}
		} catch (BadLocationException e) {
			// do nothing
		}
	}

	/**
	 * Add code actions to fix unclosed end-tag.
	 *
	 * @param element     the end tag element to fix.
	 * @param document    the owner DOM document.
	 * @param diagnostic  the diagnostic.
	 * @param codeActions the code actions list to fill.
	 *
	 * @throws BadLocationException
	 */
	private void doCodeActionsForEndTagUnclosed(DOMElement element, DOMDocument document, Diagnostic diagnostic,
			List<CodeAction> codeActions, boolean snippetSupport) throws BadLocationException {
		// ex: <foo> </foo
		Position endTagPosition = document
				.positionAt(element.getEndTagOpenOffset() + 2 + element.getTagName().length());
		// Close with '>'
		CodeAction autoCloseAction = CodeActionFactory.insert("Close end-tag with '>'", endTagPosition,
				snippetSupport ? ">$0" : ">", document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(autoCloseAction);
	}

	/**
	 * Add code actions to fix unclosed start-tag.
	 *
	 * @param element         the start tag element to fix.
	 * @param document        the owner DOM document.
	 * @param diagnosticRange the diagnostic range.
	 * @param diagnostic      the diagnostic.
	 * @param codeActions     the code actions list to fill.
	 *
	 * @throws BadLocationException
	 */
	private void doCodeActionsForStartTagUnclosed(DOMElement element, DOMDocument document, Range diagnosticRange,
			Diagnostic diagnostic, List<CodeAction> codeActions, boolean snippetSupport) throws BadLocationException {
		// Here start tag element is not closed with '>'.
		CharSequence text = document.getTextSequence();
		int closeAngleBracketOffset = element.getUnclosedStartTagCloseOffset();
		final Position closeAngleBracketPosition = document.positionAt(closeAngleBracketOffset);
		if (!element.hasEndTag()) {
			// The element has no an end tag
			// ex : <foo
			// ex : <foo attr="
			// ex : <foo /
			boolean hasSlash = isCharAt(text, closeAngleBracketOffset - 1, '/');
			if (hasSlash) {
				// ex : <foo /
				CodeAction autoCloseAction = insertGreaterThanCharacterCodeAction(document, diagnostic,
						closeAngleBracketPosition, snippetSupport);
				codeActions.add(autoCloseAction);
			} else {
				// ex : <foo
				// ex : <foo attr="
				// if element has tag name
				String tagName = element.getTagName();
				if (tagName != null) {
					// Close with '/>'
					CodeAction autoCloseAction = CodeActionFactory.insert("Close with '/>'",
							closeAngleBracketPosition, snippetSupport ? "/>$0" : "/>",
							document.getTextDocument(), diagnostic, snippetSupport);
					codeActions.add(autoCloseAction);
					// Close with '></foo>'
					String insertText = "></" + tagName + ">";
					CodeAction closeEndTagAction = CodeActionFactory.insert(
							"Close with '" + insertText + "'", closeAngleBracketPosition,
							snippetSupport ? insertText + "$0" : insertText,
							document.getTextDocument(), diagnostic, snippetSupport);
					codeActions.add(closeEndTagAction);
				}
			}
		} else {
			// ex : <foo </foo>
			CodeAction autoCloseAction = insertGreaterThanCharacterCodeAction(document, diagnostic,
					closeAngleBracketPosition, snippetSupport);
			codeActions.add(autoCloseAction);
		}
	}

	/**
	 * Add code actions to fix closed start-tag.
	 *
	 * @param element     the start tag element to fix.
	 * @param document    the owner DOM document.
	 * @param diagnostic  the diagnostic.
	 * @param codeActions the code actions list to fill.
	 *
	 * @throws BadLocationException
	 */
	private void doCodeActionsForStartTagClosed(DOMElement element, DOMDocument document,
			Diagnostic diagnostic, List<CodeAction> codeActions, boolean snippetSupport, ICodeActionRequest request)
			throws BadLocationException {
		// Here start tag element is closed with '>'.
		CharSequence text = document.getTextSequence();
		if (!element.hasEndTag()) {
			// The element has no an end tag
			// ex : <foo attr="" >
			String tagName = element.getTagName();

			// Check if the grammar declares this element as EMPTY
			boolean isEmptyByGrammar = isEmptyElement(element, request);

			if (!isEmptyByGrammar) {
				// No grammar info or element can have content:
				// offer closing end tag (Strategy 1)
				if (!element.hasChildNodes()) {
					// ex : <foo><bar></foo>
					// Close with '</foo>'
					String insertText = "</" + tagName + ">";
					int endOffset = element.getStartTagCloseOffset() + 1;
					Position endPosition = document.positionAt(endOffset);
					CodeAction closeEndTagAction = CodeActionFactory.insert("Close with '" + insertText + "'",
							endPosition, snippetSupport ? insertText + "$0" : insertText,
							document.getTextDocument(), diagnostic, snippetSupport);
					codeActions.add(closeEndTagAction);
				} else {
					// the element have some children(Text node, Element node, etc)
					boolean hasChildWithNoTagName = false;
					// Search orphan elements in the children to replace
					for (DOMNode child : element.children()) {
						if (child.isElement()) {
							DOMElement childElement = (DOMElement) child;
							if (!childElement.hasTagName() || childElement.isOrphanEndTag()) {
								// // ex : <a> <b> </c> --> the </c> breaks the <b> element, it should be
								// replaced with </b>
								// ex : <a> <b> </ --> the </ breaks the <b> element, it should be replaced
								// with
								// </b>
								String replaceTagName = element.getTagName();
								CodeAction replaceTagAction = replaceEndTagNameCodeAction(childElement,
										replaceTagName, diagnostic, snippetSupport);
								codeActions.add(replaceTagAction);
								hasChildWithNoTagName = !childElement.hasTagName();
							}
						}
					}
					if (!hasChildWithNoTagName) {
						// Here, there are no child end tag element with '</'
						int endOffset = element.getLastChild().getEnd() - 1;
						if (endOffset < text.length()) {
							// remove whitespaces
							char ch = text.charAt(endOffset);
							while (Character.isWhitespace(ch)) {
								endOffset--;
								ch = text.charAt(endOffset);
							}
						}
						endOffset++;
						String label = "</" + tagName + ">";
						String insertText = label;
						boolean multiLine = hasElements(element);
						if (multiLine) {
							// The element have element node as children
							// the </foo> must be inserted with a new line and indent
							int elementLine = document.positionAt(element.getStart()).getLine();
							LineIndentInfo indentInfo = document.getLineIndentInfo(elementLine);
							insertText = indentInfo.getLineDelimiter() + indentInfo.getWhitespacesIndent()
									+ insertText;
						}
						// ex : <foo>bar --> the </foo> must be inserted after bar text
						// ex : <a> <b> </c> --> the </> must be inserted after the </c>
						Position endPosition = document.positionAt(endOffset);
						// Don't use SnippetTextEdit for multi-line inserts because
						// VSCode adjustIndentation adds the current line's indent
						// on top of the indent we already computed.
						boolean useSnippet = snippetSupport && !multiLine;
						CodeAction closeEndTagAction = CodeActionFactory.insert("Close with '" + label + "'",
								endPosition, useSnippet ? insertText + "$0" : insertText,
								document.getTextDocument(), diagnostic, useSnippet);
						codeActions.add(closeEndTagAction);
					}
				}
			} else if (element.hasChildNodes()) {
				// EMPTY element with children: still offer orphan end-tag replacement
				for (DOMNode child : element.children()) {
					if (child.isElement()) {
						DOMElement childElement = (DOMElement) child;
						if (!childElement.hasTagName() || childElement.isOrphanEndTag()) {
							String replaceTagName = element.getTagName();
							CodeAction replaceTagAction = replaceEndTagNameCodeAction(childElement,
									replaceTagName, diagnostic, snippetSupport);
							codeActions.add(replaceTagAction);
						}
					}
				}
			}

			// Offer self-close option (Strategy 2).
			// When grammar says EMPTY, this is the only option offered.
			// Suppress for document root elements with content children
			// (elements, CDATA, non-whitespace text) that cannot legally
			// follow the document element. Allow when children are only
			// comments, PIs, or whitespace (valid XML misc content).
			boolean isDocumentRoot = element.getParentNode() instanceof DOMDocument;
			if (isEmptyByGrammar
					|| !(isDocumentRoot && hasContentChildren(element))) {
				addSelfCloseCodeAction(element, document, diagnostic, codeActions, snippetSupport);
			}
		} else {
			// The element has an end tag
			// Search orphan end tag elements in the children which breaks the XML.
			for (DOMNode child : element.children()) {
				if (child.isElement()) {
					DOMElement childElement = (DOMElement) child;
					if (!childElement.hasTagName() || childElement.isOrphanEndTag()) {
						// ex: <foo> </ </foo> --> here the code action removes '</'
						// ex: <foo> </bar> </foo> --> here the code action removes '</bar>'
						CodeAction removeAction = removeTagCodeAction(childElement, document, diagnostic,
								snippetSupport);
						codeActions.add(removeAction);
					}
				}
			}
		}
	}

	/**
	 * Create a code action which insert '>' at the end of the diagnostic error.
	 *
	 * @param diagnostic the diagnostic.
	 * @param document   the DOM document.
	 * @param position   the position where the '>' should be inserted
	 * @return a code action which insert '>' at the end of the diagnostic error.
	 */
	private static CodeAction insertGreaterThanCharacterCodeAction(DOMDocument document, Diagnostic diagnostic,
			Position position, boolean snippetSupport) {
		CodeAction autoCloseAction = CodeActionFactory.insert("Close with '>'", position,
				snippetSupport ? ">$0" : ">", document.getTextDocument(), diagnostic, snippetSupport);
		return autoCloseAction;
	}

	/**
	 * Create a code action which remove the content of the given DOM element.
	 *
	 * @param element    the DOM element to remove.
	 * @param document   the DOM document.
	 * @param diagnostic the diagnostic.
	 * @return
	 * @throws BadLocationException
	 */
	private static CodeAction removeTagCodeAction(DOMElement element, DOMDocument document, Diagnostic diagnostic,
			boolean snippetSupport) throws BadLocationException {
		CharSequence text = document.getTextSequence();
		Position startPosition = document.positionAt(element.getStart());
		Position endPosition = document.positionAt(element.getEnd());
		String contentToRemove = StringUtils.getString(text, element.getStart(), element.getEnd());
		CodeAction removeAction = CodeActionFactory.replace("Remove '" + contentToRemove + "'",
				new Range(startPosition, endPosition), snippetSupport ? "$0" : "",
				document.getTextDocument(), diagnostic, snippetSupport);
		return removeAction;
	}

	/**
	 * Create a code action which replaces the end tag name of the given element
	 * with the given replace tag name.
	 *
	 * @param element        the DOM element to replace
	 * @param replaceTagName the replace tag name
	 * @param diagnostic     the diagnostic.
	 * @return a code action which replaces the end tag name of the given element
	 *         with the given replace tag name.
	 */
	private static CodeAction replaceEndTagNameCodeAction(DOMElement element, String replaceTagName,
			Diagnostic diagnostic, boolean snippetSupport) {
		// <a><b></c>
		// Replace with 'b' closing tag
		DOMDocument document = element.getOwnerDocument();
		Range replaceRange = XMLPositionUtility.selectEndTagName(element);
		String tagName = element.getTagName();
		if (tagName == null) {
			tagName = "</";
		}
		String replaceText = replaceTagName;
		if (!element.isEndTagClosed()) {
			replaceText = replaceText + ">";
		}
		return CodeActionFactory.replace("Replace '" + tagName + "' with '" + replaceTagName + "' closing tag",
				replaceRange,
				snippetSupport ? CodeActionFactory.escapeSnippetText(replaceText) + "$0" : replaceText,
				document.getTextDocument(), diagnostic, snippetSupport);
	}

	/**
	 * Returns true if the given element has elements as children and false
	 * otherwise.
	 *
	 * @param element the DOM element.
	 *
	 * @return true if the given element has elements as children and false
	 *         otherwise.
	 */
	private static boolean hasElements(DOMElement element) {
		for (DOMNode node : element.children()) {
			if (node.isElement()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the given element has content children (elements, CDATA,
	 * or non-whitespace text) that cannot appear after the document element.
	 */
	private static boolean hasContentChildren(DOMElement element) {
		for (DOMNode node : element.children()) {
			if (node.isElement() || node.isCDATA()) {
				return true;
			}
			if (node.isText() && !((DOMText) node).isElementContentWhitespace()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCharAt(CharSequence text, int offset, char ch) {
		if (text.length() <= offset) {
			return false;
		}
		return text.charAt(offset) == ch;
	}

	/**
	 * Add a code action to self-close the element by replacing '>' with '/>'.
	 */
	private static void addSelfCloseCodeAction(DOMElement element, DOMDocument document, Diagnostic diagnostic,
			List<CodeAction> codeActions, boolean snippetSupport) throws BadLocationException {
		int startTagCloseOffset = element.getStartTagCloseOffset();
		Position startPosition = document.positionAt(startTagCloseOffset);
		Position endPosition = document.positionAt(startTagCloseOffset + 1);
		CodeAction selfCloseAction = CodeActionFactory.replace("Close with '/>'",
				new Range(startPosition, endPosition),
				snippetSupport ? "/>$0" : "/>",
				document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(selfCloseAction);
	}

	/**
	 * Returns true if the grammar declares the given element as EMPTY (cannot
	 * contain children or text content) and false otherwise.
	 */
	private static boolean isEmptyElement(DOMElement element, ICodeActionRequest request) {
		try {
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
			if (contentModelManager != null) {
				for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
					CMElementDeclaration elementDeclaration = cmDocument.findCMElement(element);
					if (elementDeclaration != null) {
						return elementDeclaration.isEmpty();
					}
				}
			}
		} catch (Exception e) {
			// ignore - fall back to non-grammar behavior
		}
		return false;
	}

}
