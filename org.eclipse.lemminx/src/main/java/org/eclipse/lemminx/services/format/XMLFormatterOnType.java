/**
 *  Copyright (c) 2026 Angelo ZERR
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
package org.eclipse.lemminx.services.format;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
 * XML on-type formatter support.
 *
 * <p>
 * Handles {@code textDocument/onTypeFormatting} requests with trigger
 * characters:
 * </p>
 * <ul>
 * <li>{@code \n} (Enter) - indents attributes inside start tags (respecting
 * splitAttributes settings), content between tags, and closing brackets
 * ({@code >}, {@code />}) at element level.</li>
 * <li>{@code <} (open bracket) - indents as child (parent has end tag) or
 * sibling (parent unclosed).</li>
 * <li>{@code /} (slash) - when preceded by {@code <} (forming {@code </}),
 * aligns the closing tag with the matching start tag.</li>
 * </ul>
 */
public class XMLFormatterOnType {

	private static final Logger LOGGER = Logger.getLogger(XMLFormatterOnType.class.getName());

	private static final char ENTER_CHAR = '\n';
	private static final char OPEN_BRACKET_CHAR = '<';
	private static final char SLASH_CHAR = '/';

	/**
	 * Returns true if the given trigger string matches the expected character.
	 *
	 * @param ch       the trigger string from the LSP request.
	 * @param expected the expected trigger character.
	 * @return true if the trigger string is a single character matching expected.
	 */
	private static boolean isChar(String ch, char expected) {
		return ch.length() == 1 && ch.charAt(0) == expected;
	}

	/**
	 * Formats the document after a trigger character has been typed.
	 *
	 * @param xmlDocument    the XML document.
	 * @param position       the cursor position after the character was typed.
	 * @param ch             the trigger character that was typed ({@code \n} or
	 *                       {@code <}).
	 * @param sharedSettings the shared settings containing formatting preferences.
	 * @return list of text edits to apply, or an empty list if no formatting is
	 *         needed.
	 */
	public List<? extends TextEdit> formatOnType(DOMDocument xmlDocument, Position position, String ch,
			SharedSettings sharedSettings) {
		// Only handle Enter, '<' and '/' triggers
		if (!isChar(ch, ENTER_CHAR) && !isChar(ch, OPEN_BRACKET_CHAR) && !isChar(ch, SLASH_CHAR)) {
			return Collections.emptyList();
		}

		if (!sharedSettings.getFormattingSettings().isEnabled()) {
			return Collections.emptyList();
		}

		try {
			TextDocument textDocument = xmlDocument.getTextDocument();
			int offset = xmlDocument.offsetAt(position);

			XMLFormattingOptions formattingOptions = sharedSettings.getFormattingSettings();
			XMLFormatterIndent indent = new XMLFormatterIndent(formattingOptions.getTabSize(),
					formattingOptions.isInsertSpaces(),
					textDocument.lineDelimiter(Math.max(0, position.getLine() - 1)));

			// Handle '<' trigger: indent open bracket as child or sibling
			// Ex: <foo>
			// |< --> indented as child of <foo>
			if (isChar(ch, OPEN_BRACKET_CHAR)) {
				return formatOnOpenBracket(xmlDocument, position, offset, indent, textDocument);
			}

			// Handle '/' trigger: indent '</' to align with matching start tag
			// Ex: <foo>
			//        </ --> <foo>
			//                </
			if (isChar(ch, SLASH_CHAR)) {
				return formatOnSlash(xmlDocument, position, offset, indent, textDocument);
			}

			// From here, handle Enter ('\n') trigger
			DOMNode node = xmlDocument.findNodeAt(offset);
			if (node == null) {
				return Collections.emptyList();
			}

			DOMElement element = node.isElement() ? (DOMElement) node : node.getParentElement();
			if (element == null || !element.hasStartTag()) {
				return Collections.emptyList();
			}

			int indentLevel = element.getIndentLevel();

			// Inside start tag: indent attributes or closing bracket
			// Ex: <foo attr1=""|
			// |attr2="" --> indented according to splitAttributes setting
			// Ex: <foo attr1=""
			// |> --> '>' aligned with '<foo'
			if (element.isInStartTag(offset)) {
				return computeStartTagIndentation(element, position, indentLevel, formattingOptions, indent,
						textDocument);
			}

			// Between start and end tags: indent content
			// Ex: <foo>|
			// |text --> indented as child content
			// Ex: <foo>|
			// |</foo> --> content indent + newline + end tag indent
			if (element.isInInsideStartEndTag(offset)
					|| (!element.hasEndTag() && element.isStartTagClosed() && offset > element.getStartTagCloseOffset())) {
				return computeContentIndentation(element, position, indentLevel, indent, textDocument);
			}

			return Collections.emptyList();
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "OnType formatting failed", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Formats on {@code <} trigger: replaces leading whitespace before the open
	 * bracket with the correct indentation.
	 *
	 * <p>
	 * Closed parent (has end tag) → child indent (level + 1):
	 * </p>
	 *
	 * <pre>
	 * &lt;foo&gt;
	 *           |&lt; --&gt; &lt;foo&gt;
	 *                     |&lt;
	 *                   &lt;/foo&gt;
	 * </pre>
	 *
	 * <p>
	 * Unclosed parent (no end tag) → sibling indent (same level):
	 * </p>
	 *
	 * <pre>
	 * &lt;foo&gt;
	 *           |&lt; --&gt; &lt;foo&gt;
	 *                   |&lt;
	 * </pre>
	 *
	 * @param xmlDocument  the XML document.
	 * @param position     the cursor position after {@code <} was typed.
	 * @param offset       the document offset corresponding to position.
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<? extends TextEdit> formatOnOpenBracket(DOMDocument xmlDocument, Position position, int offset,
			XMLFormatterIndent indent, TextDocument textDocument) throws BadLocationException {
		int openBracketOffset = offset - 1;
		if (openBracketOffset < 0) {
			return Collections.emptyList();
		}

		DOMNode node = xmlDocument.findNodeAt(openBracketOffset);
		if (node == null) {
			return Collections.emptyList();
		}

		DOMElement parentElement = node.isElement() ? (DOMElement) node : node.getParentElement();
		if (parentElement == null) {
			return Collections.emptyList();
		}

		// Closed parent → child indent (level+1); unclosed parent → sibling indent
		// (same level)
		int indentLevel;
		if (parentElement.hasEndTag()) {
			indentLevel = parentElement.getIndentLevel() + 1;
		} else {
			indentLevel = parentElement.getIndentLevel();
		}

		String indentStr = indent.createIndent(indentLevel);

		// Replace whitespace from line start to '<' with computed indent
		CharSequence text = textDocument.getTextSequence();
		int lineStart = openBracketOffset;
		while (lineStart > 0 && text.charAt(lineStart - 1) != '\n' && text.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}

		Position startPos = textDocument.positionAt(lineStart);
		Position endPos = textDocument.positionAt(openBracketOffset);
		return Collections.singletonList(new TextEdit(new Range(startPos, endPos), indentStr));
	}

	/**
	 * Formats on {@code /} trigger: if preceded by {@code <} (forming
	 * {@code </}), replaces leading whitespace to align with the matching
	 * start tag.
	 *
	 * <pre>
	 * &lt;foo&gt;
	 *        |&lt;/ --&gt; &lt;foo&gt;
	 *                |&lt;/
	 * </pre>
	 *
	 * @param xmlDocument  the XML document.
	 * @param position     the cursor position after {@code /} was typed.
	 * @param offset       the document offset corresponding to position.
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<? extends TextEdit> formatOnSlash(DOMDocument xmlDocument, Position position, int offset,
			XMLFormatterIndent indent, TextDocument textDocument) throws BadLocationException {
		// '/' must be preceded by '<' to form '</'
		int slashOffset = offset - 1;
		if (slashOffset < 1) {
			return Collections.emptyList();
		}
		CharSequence text = textDocument.getTextSequence();
		if (text.charAt(slashOffset - 1) != '<') {
			return Collections.emptyList();
		}

		int openBracketOffset = slashOffset - 1;

		// Find the parent element that this '</' will close
		DOMNode node = xmlDocument.findNodeAt(openBracketOffset);
		if (node == null) {
			return Collections.emptyList();
		}

		DOMElement element = node.isElement() ? (DOMElement) node : node.getParentElement();
		if (element == null || !element.hasStartTag()) {
			return Collections.emptyList();
		}

		// Compute actual column of the matching start tag's '<'
		int elementStartOffset = element.getStartTagOpenOffset();
		int elementLineStart = elementStartOffset;
		while (elementLineStart > 0 && text.charAt(elementLineStart - 1) != '\n'
				&& text.charAt(elementLineStart - 1) != '\r') {
			elementLineStart--;
		}
		int elementColumn = elementStartOffset - elementLineStart;

		String indentStr = indent.createSpaceIndent(elementColumn);

		// Replace whitespace from line start to '<' of '</'
		int lineStart = openBracketOffset;
		while (lineStart > 0 && text.charAt(lineStart - 1) != '\n' && text.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}

		Position startPos = textDocument.positionAt(lineStart);
		Position endPos = textDocument.positionAt(openBracketOffset);
		return Collections.singletonList(new TextEdit(new Range(startPos, endPos), indentStr));
	}

	/**
	 * Computes indentation for a new line inside a start tag.
	 *
	 * <p>
	 * If the line contains only {@code >} or {@code />}, indents at the element
	 * level (aligned with {@code <element}):
	 * </p>
	 *
	 * <pre>
	 * &lt;foo attr1=""|
	 *     |&gt;              --&gt; &lt;foo attr1=""
	 *                          |&gt;
	 * </pre>
	 *
	 * <p>
	 * Otherwise, indentation depends on the splitAttributes setting:
	 * </p>
	 *
	 * <pre>
	 * alignWithFirstAttr:   &lt;foo attr1=""
	 *                            |attr2=""
	 *
	 * splitNewLine:          &lt;foo attr1=""
	 *                            |attr2=""  (indentLevel + splitAttributesIndentSize)
	 *
	 * preserve (default):   &lt;foo attr1=""
	 *                          |attr2=""  (indentLevel + 1)
	 * </pre>
	 *
	 * @param element      the DOM element whose start tag contains the cursor.
	 * @param position     the cursor position on the new line.
	 * @param indentLevel  the element's indent level.
	 * @param options      the formatting options (splitAttributes mode, etc.).
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<TextEdit> computeStartTagIndentation(DOMElement element, Position position, int indentLevel,
			XMLFormattingOptions options, XMLFormatterIndent indent, TextDocument textDocument)
			throws BadLocationException {
		CharSequence text = textDocument.getTextSequence();
		int lineStartOffset = textDocument.offsetAt(new Position(position.getLine(), 0));

		// Compute actual column of '<' in the document
		int elementStartOffset = element.getStartTagOpenOffset();
		int elementLineStart = elementStartOffset;
		while (elementLineStart > 0 && text.charAt(elementLineStart - 1) != '\n'
				&& text.charAt(elementLineStart - 1) != '\r') {
			elementLineStart--;
		}
		int elementColumn = elementStartOffset - elementLineStart;

		// > or /> alone on the line: indent at element level (aligned with <element)
		if (isCloseBracketOnLine(text, lineStartOffset)) {
			return replaceLineIndent(position, indent.createSpaceIndent(elementColumn), textDocument);
		}

		SplitAttributes splitAttributes = options.getSplitAttributes();

		String indentStr;
		switch (splitAttributes) {
		case alignWithFirstAttr:
			indentStr = indent.createSpaceIndent(elementColumn + element.getTagName().length() + 2);
			break;
		case splitNewLine:
			indentStr = indent.createSpaceIndent(
					elementColumn + options.getSplitAttributesIndentSize() * indent.getTabSize());
			break;
		default:
			indentStr = indent.createSpaceIndent(elementColumn + indent.getTabSize());
			break;
		}

		return replaceLineIndent(position, indentStr, textDocument);
	}

	/**
	 * Returns true if the line starting at the given offset contains only {@code >}
	 * or {@code />} (ignoring leading whitespace).
	 *
	 * <pre>
	 *    |&gt;     --&gt; true
	 *    |/&gt;    --&gt; true
	 *    |attr   --&gt; false
	 * </pre>
	 *
	 * @param text   the document text.
	 * @param offset the offset at the start of the line.
	 * @return true if the line contains only a closing bracket.
	 */
	private static boolean isCloseBracketOnLine(CharSequence text, int offset) {
		int pos = offset;
		while (pos < text.length()) {
			char c = text.charAt(pos);
			if (c == '\r' || c == '\n') {
				break;
			}
			if (c != ' ' && c != '\t') {
				if (c == '>') {
					return true;
				}
				if (c == '/' && pos + 1 < text.length() && text.charAt(pos + 1) == '>') {
					return true;
				}
				return false;
			}
			pos++;
		}
		return false;
	}

	/**
	 * Computes indentation for content between start and end tags.
	 *
	 * <p>
	 * Content followed by end tag — inserts content indent + newline + end tag
	 * indent:
	 * </p>
	 *
	 * <pre>
	 * &lt;foo&gt;|&lt;/foo&gt;  --&gt;  &lt;foo&gt;
	 *                       |
	 *                     &lt;/foo&gt;
	 * </pre>
	 *
	 * <p>
	 * Content followed by text — indents content at indentLevel + 1:
	 * </p>
	 *
	 * <pre>
	 * &lt;foo&gt;
	 * |text         --&gt;  &lt;foo&gt;
	 *                       |text
	 * </pre>
	 *
	 * @param element      the DOM element containing the content.
	 * @param position     the cursor position on the new line.
	 * @param indentLevel  the element's indent level.
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<TextEdit> computeContentIndentation(DOMElement element, Position position, int indentLevel,
			XMLFormatterIndent indent, TextDocument textDocument) throws BadLocationException {
		String contentIndent = indent.createIndent(indentLevel + 1);

		CharSequence text = textDocument.getTextSequence();
		Position lineStart = new Position(position.getLine(), 0);
		int lineStartOffset = textDocument.offsetAt(lineStart);

		int afterWhitespace = lineStartOffset;
		while (afterWhitespace < text.length()) {
			char c = text.charAt(afterWhitespace);
			if (c != ' ' && c != '\t') {
				break;
			}
			afterWhitespace++;
		}

		// Enter between <element>|</element> → insert content indent + newline + end
		// tag indent
		if (afterWhitespace + 1 < text.length() && text.charAt(afterWhitespace) == '<'
				&& text.charAt(afterWhitespace + 1) == '/') {
			String endTagIndent = indent.createIndent(indentLevel);
			String newText = contentIndent + indent.getLineDelimiter() + endTagIndent;
			return Collections.singletonList(
					new TextEdit(new Range(lineStart, textDocument.positionAt(afterWhitespace)), newText));
		}

		return Collections.singletonList(
				new TextEdit(new Range(lineStart, textDocument.positionAt(afterWhitespace)), contentIndent));
	}

	/**
	 * Replaces all leading whitespace on the given line with the specified indent
	 * string. Always starts from column 0 to override any editor auto-indent.
	 *
	 * <pre>
	 * "    |text"  --&gt;  "  |text"  (replaces 4 spaces with 2)
	 * "|text"      --&gt;  "  |text"  (inserts 2 spaces)
	 * </pre>
	 *
	 * @param position     the cursor position (used to determine the line).
	 * @param indentStr    the indentation string to set.
	 * @param textDocument the underlying text document.
	 * @return list containing a single text edit.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<TextEdit> replaceLineIndent(Position position, String indentStr, TextDocument textDocument)
			throws BadLocationException {
		Position lineStart = new Position(position.getLine(), 0);
		int offset = textDocument.offsetAt(lineStart);
		CharSequence text = textDocument.getTextSequence();

		int endWhitespace = offset;
		while (endWhitespace < text.length()) {
			char c = text.charAt(endWhitespace);
			if (c != ' ' && c != '\t') {
				break;
			}
			endWhitespace++;
		}

		Position end = textDocument.positionAt(endWhitespace);
		return Collections.singletonList(new TextEdit(new Range(lineStart, end), indentStr));
	}
}
