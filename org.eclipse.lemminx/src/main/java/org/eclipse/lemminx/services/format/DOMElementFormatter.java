/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.EmptyElements;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats DOM elements according to their {@link FormatElementCategory}.
 *
 * <p>
 * Each element is classified into one of four categories that determine how
 * its start tag, children, and end tag are formatted:
 * </p>
 * <ul>
 * <li><b>IgnoreSpace</b> — element contains only other elements (no text).
 * Children are indented on new lines.
 * Use case: {@code <root><child/></root>} → each child on its own line.</li>
 *
 * <li><b>NormalizeSpace</b> — element contains only text (no child elements).
 * Whitespace within text is normalized.
 * Use case: {@code <p>  hello   world  </p>} → {@code <p>hello world</p>}.</li>
 *
 * <li><b>MixedContent</b> — element contains both text and child elements.
 * Inline flow is preserved; elements only wrap to new lines when
 * maxLineWidth is exceeded or indentation needs normalizing.
 * Use case: {@code <p>text <b>bold</b> more</p>}.</li>
 *
 * <li><b>PreserveSpace</b> — element has {@code xml:space="preserve"}.
 * All whitespace is kept as-is.
 * Use case: {@code <pre>  keep  spaces  </pre>}.</li>
 * </ul>
 *
 * <p>
 * The formatter tracks {@code availableLineWidth} to enforce
 * {@code maxLineWidth}. Each formatted token decrements the remaining width;
 * when it goes negative, subsequent elements are moved to new lines.
 * </p>
 *
 * @author Angelo ZERR
 */
public class DOMElementFormatter {

	/** The parent formatter document that provides settings, text access, and edit helpers. */
	private final XMLFormatterDocument formatterDocument;

	/** The attribute formatter used to format element attributes. */
	private final DOMAttributeFormatter attributeFormatter;

	/**
	 * Creates a new element formatter.
	 *
	 * @param formatterDocument the parent formatter document (provides settings and edit helpers).
	 * @param attributeFormatter the formatter for element attributes.
	 */
	public DOMElementFormatter(XMLFormatterDocument formatterDocument, DOMAttributeFormatter attributeFormatter) {
		this.formatterDocument = formatterDocument;
		this.attributeFormatter = attributeFormatter;
	}

	/**
	 * Formats a DOM element: start tag, children, and end tag.
	 *
	 * <p>
	 * Formatting proceeds in three phases:
	 * </p>
	 * <ol>
	 * <li><b>Start tag</b> — indentation, attributes, and closing bracket
	 * ({@code >} or {@code />}). Updates {@code parentConstraints.availableLineWidth}.</li>
	 * <li><b>Children</b> — formatted recursively with a copy of the parent
	 * constraints (indent level incremented by 1).</li>
	 * <li><b>End tag</b> — indentation of {@code </tagName>}. Updates
	 * {@code parentConstraints.availableLineWidth} from the child constraints.</li>
	 * </ol>
	 *
	 * <p>Use case (IgnoreSpace):</p>
	 * <pre>
	 * &lt;root&gt;&lt;child/&gt;&lt;/root&gt;
	 * →
	 * &lt;root&gt;
	 *   &lt;child /&gt;
	 * &lt;/root&gt;
	 * </pre>
	 *
	 * @param element           the DOM element to format.
	 * @param parentConstraints the formatting constraints from the parent element.
	 *                          Modified in place: availableLineWidth is updated.
	 * @param start             the start offset of the formatting range (-1 for no limit).
	 * @param end               the end offset of the formatting range (-1 for no limit).
	 * @param edits             the list of text edits to populate.
	 */
	public void formatElement(DOMElement element, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		FormatElementCategory formatElementCategory = getFormatElementCategory(element, parentConstraints);
		EmptyElements emptyElements = getEmptyElements(element, formatElementCategory);

		int indentLevel = parentConstraints.getIndentLevel();
		parentConstraints.setStartTagCrossedLine(false);
		int width = formatStartTagElement(element, parentConstraints, emptyElements, start, end, edits);
		parentConstraints.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - width);

		// Track indent level for text nodes in mixed content so they
		// can be indented consistently with their parent element.
		if (parentConstraints.getMixedContentIndentLevel() == 0
				&& (parentConstraints.getFormatElementCategory() == FormatElementCategory.MixedContent
						|| (parentConstraints.getFormatElementCategory() == FormatElementCategory.IgnoreSpace
								&& formatterDocument.isMixedContentReflow()))) {
			parentConstraints.setMixedContentIndentLevel(indentLevel);
		}

		if (emptyElements == EmptyElements.ignore) {
			// Format children with a copy of the constraints (indent +1)
			XMLFormattingConstraints constraints = new XMLFormattingConstraints();
			constraints.copyConstraints(parentConstraints);
			if ((element.isClosed())) {
				constraints.setIndentLevel(indentLevel + 1);
			}
			constraints.setFormatElementCategory(formatElementCategory);
			// Per-element flags, not inherited from parent.
			constraints.setWrapAllChildren(false);
			constraints.setSoftWrapped(false);
			constraints.setStartTagCrossedLine(parentConstraints.isStartTagCrossedLine());
			parentConstraints.setStartTagCrossedLine(false);

			// expand mode: force all MixedContent children to block layout.
			if (formatElementCategory == FormatElementCategory.MixedContent
					&& formatterDocument.isMixedContentExpand()) {
				constraints.setWrapAllChildren(true);
			}

			formatChildren(element, constraints, start, end, edits);

			if (element.hasEndTag() && (element.getEndTagOpenOffset() > start
					&& (end == -1 || element.getEndTagCloseOffset() < end))) {
				width = formatEndTagElement(element, parentConstraints, constraints, edits);
				parentConstraints.setAvailableLineWidth(constraints.getAvailableLineWidth() - width);
			}
		}
	}

	/**
	 * Formats the start tag of an element: handles indentation based on
	 * {@link FormatElementCategory}, formats attributes, and closes the tag.
	 *
	 * <p>
	 * Returns the width consumed by the closing portion of the start tag
	 * ({@code >}, {@code />}, or expanded {@code ></tag>}). The tag name
	 * width is applied directly to {@code parentConstraints.availableLineWidth}
	 * inside this method (line 196).
	 * </p>
	 *
	 * @param element           the element whose start tag to format.
	 * @param parentConstraints the parent's formatting constraints (modified in place).
	 * @param emptyElements     how to handle empty elements (expand, collapse, ignore).
	 * @param start             start offset of formatting range (-1 for no limit).
	 * @param end               end offset of formatting range (-1 for no limit).
	 * @param edits             the list of text edits to populate.
	 * @return the width of the closing portion of the start tag.
	 */
	private int formatStartTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			EmptyElements emptyElements, int start, int end, List<TextEdit> edits) {
		if (!element.hasStartTag()) {
			// Malformed element with only an end tag (e.g., "</" fragment)
			return element.getEnd() - element.getStart();
		}
		int indentLevel = parentConstraints.getIndentLevel();
		// width starts as '<' + tagName length (e.g., "<div" = 4)
		int width = element.getTagName() != null ? element.getTagName().length() + 1 : 0;
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		int startTagOpenOffset = element.getStartTagOpenOffset();
		int startTagCloseOffset = element.getStartTagCloseOffset();

		if (isElementOutsideRange(element, startTagOpenOffset, startTagCloseOffset, start, end)) {
			return 0;
		}

		switch (formatElementCategory) {
		case PreserveSpace:
			// xml:space="preserve" — keep all whitespace as-is
			break;

		case MixedContent: {
			// Block/inline distinction only in reflow or expand mode.
			// In normalize mode (default), all elements stay inline — backward compatible.
			//
			// Use case (reflow + blockElements=["div"]):
			//   <p>text <div>details</div> <b>bold</b></p>
			//   → <div> is block (in blockElements) → own line
			//   → <b> is not block → stays on same line as text
			//
			// Use case (expand): all children get own line regardless of blockElements.
			//
			// Use case (normalize + blockElements=["div"]):
			//   <p>text <div>details</div></p>
			//   → blockElements ignored in normalize mode, all elements stay inline
			if (formatterDocument.isMixedContentReflow()
					&& (isBlockElement(element) || parentConstraints.isWrapAllChildren())) {
				int parentContentStart = getParentContentStartOffset(element);
				int replaced = replaceLeftSpacesWithIndentation(indentLevel, parentContentStart,
						startTagOpenOffset, true, true, edits);
				if (replaced == 0) {
					insertIndentation(indentLevel, startTagOpenOffset, edits);
				}
				resetLineWidth(parentConstraints, indentLevel);
				parentConstraints.setSoftWrapped(true);
				break;
			}
			// Inline element in mixed content — stay inline with surrounding text.
			// Soft-wrap: move to new line when the full element doesn't fit on
			// the current line. Unlike expand mode, only overflowing elements move.
			// Use case: <p>text <b>here</b> more <b>bold</b></p> with maxLineWidth=40
			//   → <b>bold</b> wraps to next line only if it overflows.
			boolean moved = false;
			if (isMaxLineWidthSupported()) {
				int parentContentStart = getParentContentStartOffset(element);
				if (formatterDocument.isMixedContentReflow()) {
					int fullElementWidth = element.getEnd() - element.getStart();
					if (parentConstraints.getAvailableLineWidth() - fullElementWidth < 0) {
						int replaced = replaceLeftSpacesWithIndentation(indentLevel, parentContentStart,
								startTagOpenOffset, true, true, edits);
						if (replaced == 0) {
							insertIndentation(indentLevel, startTagOpenOffset, edits);
						}
						resetLineWidth(parentConstraints, indentLevel);
						parentConstraints.setSoftWrapped(true);
						moved = true;
					}
				} else if (shouldMoveOverflowingMixedContentElement(element, parentConstraints,
						width, parentContentStart, startTagOpenOffset)) {
					int replaced = replaceLeftSpacesWithIndentation(indentLevel, parentContentStart,
							startTagOpenOffset, true, true, edits);
					if (replaced == 0) {
						insertIndentation(indentLevel, startTagOpenOffset, edits);
					}
					resetLineWidth(parentConstraints, indentLevel);
					moved = true;
				}
			}
			// Phase 2: indentation normalization
			// Fix inconsistent indentation without moving elements between lines.
			if (!moved && shouldNormalizeMixedContentIndentation(element, startTagOpenOffset)) {
				DOMNode prevSibling = element.getPreviousSibling();
				int leftLimit = prevSibling != null
						? prevSibling.getEnd()
						: getParentContentStartOffset(element);
				replaceLeftSpacesWithIndentationPreservedNewLines(leftLimit, startTagOpenOffset,
						indentLevel, edits);
				resetLineWidth(parentConstraints, indentLevel);
			}
			break;
		}

		case IgnoreSpace:
			if (isFirstChildOfDocument(element)) {
				// Root element at start of file — remove leading whitespace/newlines
				// Use case: "\n  <root>" → "<root>"
				replaceLeftSpacesWithIndentation(indentLevel, 0, startTagOpenOffset, false, edits);
				break;
			}
			// Non-root elements — indent on new line
			// Use case: "<root><child/></root>" → "<root>\n  <child />\n</root>"
			replaceLeftSpacesWithIndentationPreservedNewLines(0, startTagOpenOffset,
					indentLevel, edits);
			resetLineWidth(parentConstraints, indentLevel);
			break;

		case NormalizeSpace:
			// Text-only elements — no indentation change needed for the start tag
			break;
		}

		parentConstraints.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - width);

		if (formatElementCategory != FormatElementCategory.PreserveSpace) {
			formatAttributes(element, parentConstraints, edits);
			boolean formatted = false;
			width = 0;
			switch (emptyElements) {
			case expand: {
				if (element.isSelfClosed()) {
					// Expand: <example /> → <example></example>
					StringBuilder tag = new StringBuilder();
					tag.append(">");
					tag.append("</");
					tag.append(element.getTagName());
					tag.append('>');
					int from = getOffsetAfterStartTagOrLastAttribute(element);
					int to = element.getEnd();
					createTextEditIfNeeded(from, to, tag.toString(), edits);
					formatted = true;
					width += element.getTagName() != null ? element.getTagName().length() + 4 : 0;
				}
				break;
			}
			case collapse: {
				// Collapse: <example></example> → <example />
				if (!element.isSelfClosed() && (end == -1 || element.getEndTagOpenOffset() + 1 < end)
						&& (shouldCollapseEmptyElement(element, formatterDocument.getSharedSettings()))) {
					StringBuilder tag = new StringBuilder();
					if (isSpaceBeforeEmptyCloseTag()) {
						tag.append(" ");
					}
					tag.append("/>");
					int from = getOffsetAfterStartTagOrLastAttribute(element);
					int to = element.getEnd();
					createTextEditIfNeeded(from, to, tag.toString(), edits);
					formatted = true;
					width++;
				}
				break;
			}
			default:
				width++;
			}

			if (!formatted) {
				if (element.isStartTagClosed() || element.isSelfClosed()) {
					width = formatElementStartTagOrSelfClosed(element, parentConstraints, edits);
				}
			}
		}
		return width;
	}

	/**
	 * Returns {@code true} if a mixed content element should be moved to a new
	 * line because it would overflow {@code maxLineWidth}.
	 *
	 * <p>Four scenarios trigger a move:</p>
	 * <ol>
	 * <li><b>Already overflowed</b> — {@code availableLineWidth < 0} from prior content.
	 *   <pre>&lt;p&gt;text &lt;a&gt;aaa&lt;/a&gt;\n  &lt;b&gt;bbb&lt;/b&gt;&lt;/p&gt;</pre></li>
	 * <li><b>Idempotent</b> — element was wrapped on a prior pass and stays wrapped.</li>
	 * <li><b>Adjacent start-tag overflow</b> — {@code </a><b>} with no whitespace,
	 *   start tag alone overflows remaining width.</li>
	 * <li><b>Adjacent full-element overflow</b> — {@code </g><g>kkk</g>}, start tag
	 *   fits but the full element overflows remaining width.</li>
	 * </ol>
	 *
	 * @param element           the element to check.
	 * @param parentConstraints the parent's formatting constraints.
	 * @param startTagWidth     width of {@code <tagName} (tagName.length + 1).
	 * @param parentContentStart offset after the parent's {@code >}.
	 * @param startTagOpenOffset offset of this element's {@code <}.
	 * @return {@code true} if the element should be moved to a new line.
	 */
	private boolean shouldMoveOverflowingMixedContentElement(DOMElement element,
			XMLFormattingConstraints parentConstraints, int startTagWidth,
			int parentContentStart, int startTagOpenOffset) {
		int availableWidth = parentConstraints.getAvailableLineWidth();
		if (availableWidth < 0) {
			return true;
		}
		int wsFrom = formatterDocument.adjustOffsetWithLeftWhitespaces(parentContentStart, startTagOpenOffset);
		boolean hasWhitespaceBefore = wsFrom >= 0 && wsFrom < startTagOpenOffset;
		if (hasWhitespaceBefore) {
			// Whitespace exists before the element (e.g., newline+indent from prior pass).
			// Only move if the start tag overflows AND there's a line break in the
			// whitespace (to preserve inline spacing like "</b> <i>").
			return availableWidth - startTagWidth - 1 < 0
					&& formatterDocument.hasLineBreak(wsFrom, startTagOpenOffset);
		}
		// No whitespace before the element — adjacent to previous sibling
		// (e.g., "</a><b>"). Check if the FULL element overflows.
		DOMNode prevSibling = element.getPreviousSibling();
		if (prevSibling != null && prevSibling.isElement()) {
			int fullElementWidth = element.getEnd() - element.getStart();
			return availableWidth - fullElementWidth < 0;
		}
		return false;
	}

	/**
	 * Returns {@code true} if a mixed content element's indentation should be
	 * normalized (without moving it between lines).
	 *
	 * <p>Two scenarios trigger normalization:</p>
	 * <ol>
	 * <li><b>First child after parent start tag</b> — whitespace between parent's
	 *   {@code >} and the first child is always normalized (no line break required).
	 *   <pre>&lt;a&gt;   &lt;b&gt;content&lt;/b&gt;&lt;/a&gt; → &lt;a&gt; &lt;b&gt;content&lt;/b&gt;&lt;/a&gt;</pre></li>
	 * <li><b>Siblings with line break</b> — inconsistent indentation is fixed only
	 *   when the whitespace contains a line break, to preserve inline spacing.
	 *   <pre>
	 *   &lt;set&gt;
	 *       &lt;if&gt;...&lt;/if&gt;
	 *                           &lt;if&gt;...&lt;/if&gt;  ← normalized to proper indent
	 *   </pre></li>
	 * </ol>
	 *
	 * <p>Inline spacing without line breaks (e.g., {@code </b> <i>}) is preserved.</p>
	 *
	 * @param element            the element to check.
	 * @param startTagOpenOffset offset of this element's {@code <}.
	 * @return {@code true} if the element's indentation should be normalized.
	 */
	private boolean shouldNormalizeMixedContentIndentation(DOMElement element, int startTagOpenOffset) {
		DOMNode prevSibling = element.getPreviousSibling();
		int leftLimit;
		if (prevSibling != null) {
			leftLimit = prevSibling.getEnd();
		} else {
			leftLimit = getParentContentStartOffset(element);
		}
		return leftLimit != startTagOpenOffset
				&& StringUtils.isWhitespace(formatterDocument.getTextSequence(), leftLimit, startTagOpenOffset)
				&& (prevSibling == null || formatterDocument.hasLineBreak(leftLimit, startTagOpenOffset));
	}

	/**
	 * Returns the offset right after the parent element's start tag {@code >}.
	 * This is the beginning of the parent's content area.
	 *
	 * @param element the child element.
	 * @return offset after parent's {@code >}, or 0 if no parent.
	 */
	private static int getParentContentStartOffset(DOMElement element) {
		return element.getParentElement() != null
				? element.getParentElement().getStartTagCloseOffset() + 1 : 0;
	}

	/**
	 * Returns {@code true} if the element is outside the formatting range
	 * and should be skipped.
	 */
	private static boolean isElementOutsideRange(DOMElement element,
			int startTagOpenOffset, int startTagCloseOffset, int start, int end) {
		return (end != -1 && startTagOpenOffset > end)
				|| (start != -1 && startTagCloseOffset != -1 && startTagCloseOffset < start)
				|| (start != -1 && element.isSelfClosed() && element.getEnd() < start);
	}

	/**
	 * Returns {@code true} if the element is the first child of the document root.
	 * Use case: the root element at the very start of the file.
	 */
	private static boolean isFirstChildOfDocument(DOMElement element) {
		return element.getParentNode().isOwnerDocument()
				&& element.getParentNode().getFirstChild() == element;
	}

	/**
	 * Returns the offset after the start tag name or the last attribute,
	 * whichever comes later. This is the position just before {@code >} or {@code />}.
	 */
	private static int getOffsetAfterStartTagOrLastAttribute(DOMElement element) {
		DOMAttr attr = getLastAttribute(element);
		if (attr != null) {
			return attr.getEnd();
		}
		return element.getOffsetAfterStartTag();
	}

	/**
	 * Formats element attributes: spacing, alignment, and line breaks.
	 *
	 * <p>Delegates each attribute to {@link DOMAttributeFormatter} which handles
	 * splitting, alignment, and whitespace between attributes.</p>
	 *
	 * <p>Use case: {@code <foo  attr1=""   attr2="">} → {@code <foo attr1="" attr2="">}</p>
	 *
	 * @param element           the element whose attributes to format.
	 * @param parentConstraints the parent's formatting constraints.
	 * @param edits             the list of text edits to populate.
	 * @return always 0 (width tracking handled by attribute formatter).
	 */
	private int formatAttributes(DOMElement element, XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		if (element.hasAttributes()) {
			// Walk attributes left-to-right: <foo| attr1="" attr2="">
			int prevOffset = element.getOffsetAfterStartTag();
			boolean singleAttribute = element.hasSingleAttribute();
			boolean isFirstAttr = true;
			for (DOMAttr attr : element.attributes()) {
				attributeFormatter.formatAttribute(attr, prevOffset, singleAttribute, true, isFirstAttr,
						parentConstraints, edits);
				isFirstAttr = false;
				// Advance: <foo attr1=""| attr2="">
				prevOffset = attr.getEnd();
			}
		}
		return 0;
	}

	/**
	 * Formats the start tag's closing bracket (>) according to
	 * {@code XMLFormattingOptions#isPreserveAttrLineBreaks()}
	 *
	 * {@code XMLFormattingOptions#isPreserveAttrLineBreaks()}: If true, must add a
	 * newline + indent before the closing bracket if the last attribute of the
	 * element and the closing bracket are in different lines.
	 *
	 * @param element
	 * @throws BadLocationException
	 */
	private int formatElementStartTagOrSelfClosed(DOMElement element, XMLFormattingConstraints parentConstraints,
			List<TextEdit> edits) {
		// <foo| >
		// <foo| />
		int startTagClose = element.getOffsetBeforeCloseOfStartTag();
		// <foo |>
		// <foo |/>
		int startTagOpen = element.getOffsetAfterStartTag();
		String replace = "";
		boolean spaceBeforeEmptyCloseTag = isSpaceBeforeEmptyCloseTag();
		int width = 0;
		if (isPreserveAttributeLineBreaks() && element.hasAttributes()
				&& hasLineBreak(getLastAttribute(element).getEnd(), startTagClose)) {
			spaceBeforeEmptyCloseTag = false;
			int indentLevel = parentConstraints.getIndentLevel();
			if (indentLevel == 0) {
				// <foo\n
				// attr1="" >

				// Add newline when there is no indent
				replace = formatterDocument.getLineDelimiter();
			} else {
				// <foo>\n
				// <bar\n
				// attr1="" >
				// Add newline with indent according to indent level
				replaceLeftSpacesWithIndentation(indentLevel, startTagOpen, startTagClose, true, edits);
				return width;
			}
		} else if (shouldFormatClosingBracketNewLine(element)) {
			int indentLevel = parentConstraints.getIndentLevel();
			if (getSplitAttributes().isAlignWithFirstAttr()) {
				int indentOffset = indentLevel * getTabSize() + element.getTagName().length() + 2;
				replaceLeftSpacesWithIndentationWithOffsetSpaces(indentOffset, startTagOpen, startTagClose, edits);
				return indentOffset;
			} else {
				replaceLeftSpacesWithIndentation(indentLevel + getSplitAttributesIndentSize(), startTagOpen,
						startTagClose, true, edits);
				return (indentLevel + getSplitAttributesIndentSize()) * getTabSize();
			}
		}
		if (element.isSelfClosed()) {
			if (spaceBeforeEmptyCloseTag) {
				// <foo attr1=""/> --> <foo attr1=""[space] />
				replace = replace + " ";
				width++; // add width for [space]
			}
			width++; // add width for '/'
		}
		// remove spaces from the offset of start tag and start tag close
		// <foo|[space][space]|> --> <foo>
		// <foo attr1="" attr2="" |[space][space]|> --> <foo>
		replaceLeftSpacesWith(startTagOpen, startTagClose, replace, edits);
		width++; // add width for '>'
		return width;
	}

	/**
	 * Formats the end tag ({@code </tagName>}) of an element.
	 *
	 * <p>
	 * Two operations are performed:
	 * </p>
	 * <ol>
	 * <li><b>Left whitespace</b> — adjusts indentation before {@code </tagName}
	 * based on the element's category.</li>
	 * <li><b>Closing bracket</b> — removes extra spaces before {@code >}
	 * (e.g., {@code </a   >} → {@code </a>}).</li>
	 * </ol>
	 *
	 * @param element           the element whose end tag to format.
	 * @param parentConstraints the parent's formatting constraints (for indent level).
	 * @param constraints       the element's own constraints (for category).
	 * @param edits             the list of text edits to populate.
	 * @return the width consumed by the end tag ({@code </tagName>}).
	 */
	private int formatEndTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			XMLFormattingConstraints constraints, List<TextEdit> edits) {
		if (formatterDocument.isFormatterOff()) {
			return element.getTagName() != null ? element.getTagName().length() + 2 : 0;
		}
		int indentLevel = parentConstraints.getIndentLevel();
		FormatElementCategory formatElementCategory = constraints.getFormatElementCategory();
		int endTagOpenOffset = element.getEndTagOpenOffset();
		int startTagCloseOffset = element.getStartTagCloseOffset();
		// width = '</' + tagName length (e.g., "</div" = 5)
		int width = element.getTagName() != null ? element.getTagName().length() + 2 : 0;

		switch (formatElementCategory) {
		case PreserveSpace:
			// Use case (#1301): <doc xml:space="preserve">\nContent\n</doc>
			// Content is preserved, but the end tag must be indented when it
			// directly follows a bare newline (no existing indentation).
			if (isEndTagDirectlyAfterNewline(endTagOpenOffset, startTagCloseOffset)) {
				replaceLeftSpacesWithIndentation(indentLevel, endTagOpenOffset, endTagOpenOffset, false, edits);
				width += indentLevel * getTabSize();
			}
			break;
		case MixedContent: {
			DOMNode lastContent = getLastNonWhitespaceChild(element);
			boolean lastIsBlock = formatterDocument.isMixedContentReflow()
					&& lastContent != null && lastContent.isElement()
					&& isBlockElement((DOMElement) lastContent);
			if (lastIsBlock || constraints.isWrapAllChildren()
					|| constraints.isSoftWrapped()
					|| constraints.isStartTagCrossedLine()) {
				// End tag on own line when:
				// - last child is block: <p>text <div>x</div></p> → </p> on own line
				// - expand mode: <p>\n  text\n  <b>bold</b>\n</p>
				// - soft-wrap occurred: content wrapped due to maxLineWidth overflow
				boolean endTagAfterNewline = isEndTagDirectlyAfterNewline(endTagOpenOffset, startTagCloseOffset);
				boolean joinWillRemoveNewline = endTagAfterNewline
						&& formatterDocument.getSharedSettings().getFormattingSettings().isJoinContentLines();
				if (!endTagAfterNewline || joinWillRemoveNewline) {
					int replaced = replaceLeftSpacesWithIndentation(indentLevel,
							startTagCloseOffset, endTagOpenOffset, true, true, edits);
					if (replaced == 0) {
						insertIndentation(indentLevel, endTagOpenOffset, edits);
					}
				}
				width += indentLevel * getTabSize();
			} else if (hasTrailingWhitespaceAfterElementOrComment(element, endTagOpenOffset)) {
				replaceLeftSpacesWithIndentationPreservedNewLines(startTagCloseOffset, endTagOpenOffset,
						indentLevel, edits);
				width += indentLevel * getTabSize();
			}
			break;
		}
		case IgnoreSpace:
			// Use case: <root>  <child/>  </root> → <root>\n  <child />\n</root>
			// End tag always gets indented on its own line.
			replaceLeftSpacesWithIndentationPreservedNewLines(startTagCloseOffset, endTagOpenOffset,
					indentLevel, edits);
			width += indentLevel * getTabSize();
			break;
		case NormalizeSpace:
			if (constraints.isSoftWrapped()
					|| (constraints.isStartTagCrossedLine() && !element.isEmpty())) {
				int replaced = replaceLeftSpacesWithIndentation(indentLevel,
						startTagCloseOffset, endTagOpenOffset, true, true, edits);
				if (replaced == 0) {
					insertIndentation(indentLevel, endTagOpenOffset, edits);
				}
				width += indentLevel * getTabSize();
			}
			break;
		}
		// Remove extra spaces before the closing '>'
		// Use case: </a   > → </a>
		if (element.isEndTagClosed()) {
			int endTagCloseOffset = element.getEndTagCloseOffset();
			removeLeftSpaces(element.getEndTagOpenOffset(), endTagCloseOffset, edits);
			width++;
		}
		return width;
	}

	/**
	 * Returns {@code true} if the end tag directly follows a newline character,
	 * meaning it needs indentation in PreserveSpace mode.
	 *
	 * <p>Use case: {@code <pre>\ncontent\n</pre>} — the {@code </pre>} follows
	 * a bare newline and needs indentation added.</p>
	 */
	private boolean isEndTagDirectlyAfterNewline(int endTagOpenOffset, int startTagCloseOffset) {
		if (endTagOpenOffset <= startTagCloseOffset + 1) {
			return false;
		}
		char c = formatterDocument.getTextSequence().charAt(endTagOpenOffset - 1);
		return c == '\n' || c == '\r';
	}

	/**
	 * Returns the last child that is not a whitespace-only text node,
	 * or {@code null} if no such child exists.
	 *
	 * <p>Use case: {@code <p><div>block</div>   </p>} — skips trailing whitespace
	 * text, returns {@code <div>}.</p>
	 */
	private DOMNode getLastNonWhitespaceChild(DOMElement element) {
		DOMNode child = element.getLastChild();
		while (child != null && child.isText()
				&& StringUtils.isWhitespace(formatterDocument.getTextSequence(),
						child.getStart(), child.getEnd())) {
			child = child.getPreviousSibling();
		}
		return child;
	}

	/**
	 * Returns {@code true} if the element's last child is an element or comment
	 * AND there is trailing whitespace before the end tag.
	 *
	 * <p>Use case (true): {@code <p><b>bold</b>  </p>} — last child is {@code <b>},
	 * whitespace before {@code </p>}.</p>
	 * <p>Use case (false): {@code <p>text</p>} — last child is text, no normalization.</p>
	 */
	private boolean hasTrailingWhitespaceAfterElementOrComment(DOMElement element, int endTagOpenOffset) {
		DOMNode lastChild = element.getLastChild();
		return lastChild != null
				&& (lastChild.isElement() || lastChild.isComment())
				&& Character.isWhitespace(formatterDocument.getTextSequence().charAt(endTagOpenOffset - 1));
	}

	/**
	 * Return the option to use to generate empty elements.
	 *
	 * @param element the DOM element
	 * @return the option to use to generate empty elements.
	 */
	private EmptyElements getEmptyElements(DOMElement element, FormatElementCategory formatElementCategory) {
		EmptyElements emptyElements = getEmptyElements();
		if (emptyElements != EmptyElements.ignore) {
			if (element.isClosed() && element.isEmpty()) {
				// Element is empty and closed
				switch (emptyElements) {
				case expand:
				case collapse: {
					if (formatElementCategory == FormatElementCategory.PreserveSpace) {
						// preserve content
						if (element.hasChildNodes()) {
							// The element is empty and contains somes spaces which must be preserved
							return EmptyElements.ignore;
						}
					}
					return emptyElements;
				}
				default:
					return emptyElements;
				}
			}
		}
		return EmptyElements.ignore;
	}

	/**
	 * Return true if conditions are met to format according to the
	 * closingBracketNewLine setting.
	 *
	 * 1. splitAttribute must be set to true 2. there must be at least 2 attributes
	 * in the element
	 *
	 * @param element the DOM element
	 * @return true if should format according to closingBracketNewLine setting.
	 */
	private boolean shouldFormatClosingBracketNewLine(DOMElement element) {
		return (formatterDocument.getSharedSettings().getFormattingSettings().getClosingBracketNewLine()
				&& !getSplitAttributes().isPreserve()
				&& element.hasAttributes() && !element.hasSingleAttribute());
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWith}. */
	private void replaceLeftSpacesWith(int from, int to, String replace, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(from, to, replace, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation} with conflict removal. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			boolean removeConflictingEdits, List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, removeConflictingEdits, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationPreservedNewLines}. */
	private void replaceLeftSpacesWithIndentationPreservedNewLines(int spaceStart, int spaceEnd,
			int indentLevel, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd, indentLevel,
				edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationWithOffsetSpaces}. */
	private void replaceLeftSpacesWithIndentationWithOffsetSpaces(int spaceCount, int from, int to,
			List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationWithOffsetSpaces(spaceCount, from, to, true, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#removeLeftSpaces}. */
	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#createTextEditIfNeeded}. */
	private void createTextEditIfNeeded(int from, int to, String expectedContent, List<TextEdit> edits) {
		formatterDocument.createTextEditIfNeeded(from, to, expectedContent, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#insertIndentation}. */
	private void insertIndentation(int indentLevel, int offset, List<TextEdit> edits) {
		formatterDocument.insertIndentation(indentLevel, offset, edits);
	}

	/**
	 * Returns true if the DOM document have some line break in the given range
	 * [from, to] and false otherwise.
	 *
	 * @param from the from offset range.
	 * @param to   the to offset range.
	 *
	 * @return true if the DOM document have some line break in the given range
	 *         [from, to] and false otherwise.
	 */
	private boolean hasLineBreak(int from, int to) {
		return formatterDocument.hasLineBreak(from, to);
	}

	/**
	 * Returns the last attribute of the given DOMelement and null otherwise.
	 *
	 * @param element the DOM element.
	 *
	 * @return the last attribute of the given DOMelement and null otherwise.
	 */
	private static DOMAttr getLastAttribute(DOMElement element) {
		if (!element.hasAttributes()) {
			return null;
		}
		return element.getLastAttr();
	}

	/** Returns true if attribute line breaks should be preserved. */
	private boolean isPreserveAttributeLineBreaks() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveAttributeLineBreaks();
	}

	/** Returns the configured split attributes mode. */
	private SplitAttributes getSplitAttributes() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributes();
	}

	/** Returns the number of extra indent levels for split attributes. */
	private int getSplitAttributesIndentSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributesIndentSize();
	}

	/** Returns true if a space should be added before {@code />} in empty elements. */
	private boolean isSpaceBeforeEmptyCloseTag() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isSpaceBeforeEmptyCloseTag();
	}

	/** Returns the configured empty elements handling mode. */
	private EmptyElements getEmptyElements() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getEmptyElements();
	}

	/** Delegates to {@link XMLFormatterDocument#formatChildren}. */
	private void formatChildren(DOMElement element, XMLFormattingConstraints constraints, int start, int end,
			List<TextEdit> edits) {
		formatterDocument.formatChildren(element, constraints, start, end, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#getFormatElementCategory}. */
	private FormatElementCategory getFormatElementCategory(DOMElement element,
			XMLFormattingConstraints parentConstraints) {
		return formatterDocument.getFormatElementCategory(element, parentConstraints);
	}

	/** Delegates to {@link XMLFormatterDocument#shouldCollapseEmptyElement}. */
	private boolean shouldCollapseEmptyElement(DOMElement element, SharedSettings settings) {
		return formatterDocument.shouldCollapseEmptyElement(element, settings);
	}

	/** Delegates to {@link XMLFormatterDocument#isBlockElement}. */
	private boolean isBlockElement(DOMElement element) {
		return formatterDocument.isBlockElement(element);
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	/** Delegates to {@link XMLFormatterDocument#resetLineWidth}. */
	private void resetLineWidth(XMLFormattingConstraints constraints, int indentLevel) {
		formatterDocument.resetLineWidth(constraints, indentLevel);
	}
}
