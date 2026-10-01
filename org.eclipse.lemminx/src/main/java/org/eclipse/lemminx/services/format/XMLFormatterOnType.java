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

import java.util.Arrays;
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
 * <li>{@code /} (slash) - when preceded by {@code <} (forming {@code </}),
 * aligns the closing tag with the matching start tag and generates the end
 * tag name.</li>
 * </ul>
 */
public class XMLFormatterOnType {

	private static final Logger LOGGER = Logger.getLogger(XMLFormatterOnType.class.getName());

	private static final char ENTER_CHAR = '\n';
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
	 *                       {@code /}).
	 * @param sharedSettings the shared settings containing formatting preferences.
	 * @return list of text edits to apply, or an empty list if no formatting is
	 *         needed.
	 */
	public List<? extends TextEdit> formatOnType(DOMDocument xmlDocument, Position position, String ch,
			SharedSettings sharedSettings) {
		// Only handle Enter and '/' triggers
		if (!isChar(ch, ENTER_CHAR) && !isChar(ch, SLASH_CHAR)) {
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

			// Inside start tag: indent attributes or closing bracket
			// Ex: <foo attr1=""|
			// |attr2="" --> indented according to splitAttributes setting
			// Ex: <foo attr1=""
			// |> --> '>' aligned with '<foo'
			if (element.isInStartTag(offset)) {
				return computeStartTagIndentation(element, position, formattingOptions, indent,
						textDocument);
			}

			// Between start and end tags: indent content
			// Ex: <foo>|
			// |text --> indented as child content
			// Ex: <foo>|
			// |</foo> --> content indent + newline + end tag indent
			if (element.isInInsideStartEndTag(offset)
					|| (!element.hasEndTag() && element.isStartTagClosed() && offset > element.getStartTagCloseOffset())) {
				return computeContentIndentation(element, position, indent, textDocument);
			}

			return Collections.emptyList();
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "OnType formatting failed", e);
			return Collections.emptyList();
		}
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

		String indentStr;
		if (indent.isInsertSpaces()) {
			indentStr = indent.createSpaceIndent(elementColumn);
		} else {
			indentStr = indent.createIndent(elementColumn / indent.getTabSize());
		}

		// Replace whitespace from line start to '<' of '</'
		int lineStart = openBracketOffset;
		while (lineStart > 0 && text.charAt(lineStart - 1) != '\n' && text.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}

		// Only format if there's nothing but whitespace before '<' on this line
		for (int i = lineStart; i < openBracketOffset; i++) {
			char c = text.charAt(i);
			if (c != ' ' && c != '\t') {
				return Collections.emptyList();
			}
		}

		Position startPos = textDocument.positionAt(lineStart);
		Position endPos = textDocument.positionAt(openBracketOffset);
		TextEdit indentEdit = new TextEdit(new Range(startPos, endPos), indentStr);

		// Generate end tag name if element has a tag name and nothing follows '/' on
		// the line
		String tagName = element.getTagName();
		if (tagName != null && isLineEmptyAfter(text, offset)) {
			TextEdit tagEdit = new TextEdit(new Range(position, position), tagName + ">");
			return Arrays.asList(indentEdit, tagEdit);
		}

		return Collections.singletonList(indentEdit);
	}

	/**
	 * Returns true if only whitespace (or nothing) remains after the given offset
	 * on the same line.
	 */
	private static boolean isLineEmptyAfter(CharSequence text, int offset) {
		for (int i = offset; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				break;
			}
			if (c != ' ' && c != '\t') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Computes indentation for a new line inside a start tag.
	 *
	 * <p>
	 * Indentation depends on the splitAttributes setting:
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
	 * @param options      the formatting options (splitAttributes mode, etc.).
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<TextEdit> computeStartTagIndentation(DOMElement element, Position position,
			XMLFormattingOptions options, XMLFormatterIndent indent, TextDocument textDocument)
			throws BadLocationException {
		CharSequence text = textDocument.getTextSequence();

		// Compute actual column of '<' in the document
		int elementStartOffset = element.getStartTagOpenOffset();
		int elementLineStart = elementStartOffset;
		while (elementLineStart > 0 && text.charAt(elementLineStart - 1) != '\n'
				&& text.charAt(elementLineStart - 1) != '\r') {
			elementLineStart--;
		}
		int elementColumn = elementStartOffset - elementLineStart;

		SplitAttributes splitAttributes = options.getSplitAttributes();

		String indentStr;
		if (indent.isInsertSpaces()) {
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
		} else {
			int indentLevel = elementColumn / indent.getTabSize();
			switch (splitAttributes) {
			case alignWithFirstAttr:
				indentStr = indent.createSpaceIndent(elementColumn + element.getTagName().length() + 2);
				break;
			case splitNewLine:
				indentStr = indent.createIndent(indentLevel + options.getSplitAttributesIndentSize());
				break;
			default:
				indentStr = indent.createIndent(indentLevel + 1);
				break;
			}
		}

		return replaceLineIndent(position, indentStr, textDocument);
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
	 * @param indent       the indent helper for computing indentation strings.
	 * @param textDocument the underlying text document.
	 * @return list of text edits to apply.
	 * @throws BadLocationException if position conversion fails.
	 */
	private static List<TextEdit> computeContentIndentation(DOMElement element, Position position,
			XMLFormatterIndent indent, TextDocument textDocument) throws BadLocationException {
		CharSequence text = textDocument.getTextSequence();

		// Compute actual column of '<' in the document
		int elementStartOffset = element.getStartTagOpenOffset();
		int elementLineStart = elementStartOffset;
		while (elementLineStart > 0 && text.charAt(elementLineStart - 1) != '\n'
				&& text.charAt(elementLineStart - 1) != '\r') {
			elementLineStart--;
		}
		int elementColumn = elementStartOffset - elementLineStart;

		String contentIndent;
		String endTagBaseIndent;
		if (indent.isInsertSpaces()) {
			contentIndent = indent.createSpaceIndent(elementColumn + indent.getTabSize());
			endTagBaseIndent = indent.createSpaceIndent(elementColumn);
		} else {
			int indentLevel = elementColumn / indent.getTabSize();
			contentIndent = indent.createIndent(indentLevel + 1);
			endTagBaseIndent = indent.createIndent(indentLevel);
		}

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
			String newText = contentIndent + indent.getLineDelimiter() + endTagBaseIndent;
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
