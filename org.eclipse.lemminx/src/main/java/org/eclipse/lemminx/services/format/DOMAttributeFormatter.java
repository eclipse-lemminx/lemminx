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

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats DOM element attributes: spacing, indentation, quoting, and line
 * splitting.
 *
 * <p>
 * Attribute formatting has three main phases for each attribute:
 * </p>
 * <ol>
 * <li><b>Left spacing / indentation</b> — controls whitespace before the
 * attribute name. Depends on the {@link SplitAttributes} setting:
 * <ul>
 * <li>{@code preserve} — keeps existing layout; wraps to new line only when
 * {@code maxLineWidth} is exceeded.
 * Use case: {@code <foo attr1="" attr2="">} stays on one line if it fits.</li>
 * <li>{@code splitNewLine} — each attribute on its own line (when &gt;1 attribute).
 * Use case: {@code <foo attr1="" attr2="">} →
 * <pre>
 * &lt;foo
 *     attr1=""
 *     attr2=""&gt;
 * </pre></li>
 * <li>{@code alignWithFirstAttr} — attributes aligned with the first attribute.
 * Use case:
 * <pre>
 * &lt;foo attr1=""
 *      attr2=""&gt;
 * </pre></li>
 * </ul></li>
 * <li><b>Delimiter formatting</b> — removes extra spaces around {@code =}.
 * Use case: {@code <foo attr = "val">} → {@code <foo attr="val">}.</li>
 * <li><b>Quote enforcement</b> — replaces quotes with the preferred style.
 * Use case (preferred = single): {@code <a name="value">} → {@code <a name='value'>}.</li>
 * </ol>
 *
 * @author Angelo ZERR
 */
public class DOMAttributeFormatter {

	/** The parent formatter document that provides settings, text access, and edit helpers. */
	private final XMLFormatterDocument formatterDocument;

	/**
	 * Creates a new attribute formatter.
	 *
	 * @param formatterDocument the parent formatter document.
	 */
	public DOMAttributeFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	/**
	 * Formats a single attribute: left spacing, delimiter ({@code =}), value,
	 * and quote style.
	 *
	 * <p>Use case (normalize spacing):</p>
	 * <pre>{@code <foo  attr1 = "val"  attr2="val">} → {@code <foo attr1="val" attr2="val">}</pre>
	 *
	 * <p>Use case (splitNewLine with 2+ attributes):</p>
	 * <pre>
	 * {@code <foo attr1="val" attr2="val">}
	 * →
	 * {@code <foo}
	 * {@code     attr1="val"}
	 * {@code     attr2="val">}
	 * </pre>
	 *
	 * @param attr              the attribute to format.
	 * @param prevOffset        the end offset of the previous token (tag name or
	 *                          previous attribute) — used as left boundary for
	 *                          whitespace replacement.
	 * @param singleAttribute   {@code true} if this is the only attribute on the element
	 *                          (splitNewLine does not split single attributes).
	 * @param useSettings       {@code true} to apply split/preserve settings;
	 *                          {@code false} to only normalize spacing.
	 * @param isFirstAttr       {@code true} if this is the first attribute
	 *                          (alignWithFirstAttr skips the first).
	 * @param parentConstraints the parent's formatting constraints (modified:
	 *                          availableLineWidth is updated).
	 * @param edits             the list of text edits to populate.
	 */
	public void formatAttribute(DOMAttr attr, int prevOffset, boolean singleAttribute, boolean useSettings,
			boolean isFirstAttr,
			XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		int indentLevel = parentConstraints.getIndentLevel();
		// Phase 1: format whitespace before the attribute name
		// Use case: <foo[space][space]attr="" → <foo[space]attr=""
		boolean alreadyIndented = false;
		if (useSettings) {
			// Apply split/line-break settings to position the attribute.
			// Use case (preserveAttributeLineBreaks): keep existing line breaks
			//   <foo\n  attr=""> → stays as-is with proper indent
			if (isPreserveAttributeLineBreaks() && hasLineBreak(prevOffset, attr.getStart())) {
				replaceLeftSpacesWithIndentation(indentLevel + 1, prevOffset, attr.getStart(), true, edits);
				alreadyIndented = true;
				parentConstraints.setStartTagCrossedLine(true);
			} else if (getSplitAttributes() == SplitAttributes.splitNewLine && !singleAttribute) {
				replaceLeftSpacesWithIndentation(indentLevel + getSplitAttributesIndentSize(), prevOffset,
						attr.getStart(), true, edits);
				alreadyIndented = true;
				parentConstraints.setStartTagCrossedLine(true);
			} else if (getSplitAttributes() == SplitAttributes.alignWithFirstAttr && !isFirstAttr) {
				replaceLeftSpacesWithIndentationWithOffsetSpaces(getFirstAttrOffset(attr.getOwnerElement(), indentLevel), prevOffset,
						attr.getStart(), edits);
				alreadyIndented = true;
				parentConstraints.setStartTagCrossedLine(true);
			}
		}

		// Phase 2: format the delimiter '=' — remove extra whitespace
		// Use case: <foo attr = "val"> → <foo attr="val">
		int attributeNamelength = 0;
		if (attr.hasDelimiter()) {
			int delimiterOffset = attr.getDelimiterOffset(); // <foo attr =| ""

			// 2.1 Remove extra spaces between end of attribute name and delimiter
			int attrNameEnd = attr.getNodeAttrName().getEnd(); // <foo attr| = ""
			removeLeftSpaces(attrNameEnd, delimiterOffset, edits);

			if (attr.getNodeAttrValue() != null) {
				// 2.2 Remove extra spaces between delimiter and start of attribute value
				int attrValueStart = attr.getNodeAttrValue().getStart(); // <foo attr = |""
				removeLeftSpaces(delimiterOffset, attrValueStart, edits);
			}

			// Compute max line width for attribute name and indents if maxLineWidth is
			// enabled
			if (isMaxLineWidthSupported()) {
				int availableLineWidth = parentConstraints.getAvailableLineWidth();
				if (isPreserveAttributeLineBreaks() && hasLineBreak(prevOffset, attr.getStart())) {
					availableLineWidth = getMaxLineWidth() - getTabSize() * (indentLevel + 1);
				} else if (getSplitAttributes() == SplitAttributes.splitNewLine && !singleAttribute) {
					availableLineWidth = getMaxLineWidth()
							- getTabSize() * (indentLevel + getSplitAttributesIndentSize());
				} else {
					// counts the space between the start tag name and attribute value
					availableLineWidth--;
				}
				// Add width for length of attribute name and 3 for '=""'
				// between start tag name and tag
				attributeNamelength = attrNameEnd - attr.getNodeAttrName().getStart() + 3;
				parentConstraints.setAvailableLineWidth(availableLineWidth - attributeNamelength);
			}
			formatAttributeValue(attr, parentConstraints, edits);
		}

		if (!alreadyIndented) {
			int from = prevOffset;
			int to = attr.getStart();
			if (isMaxLineWidthSupported() && parentConstraints.getAvailableLineWidth() < 0
					&& getSplitAttributes() == SplitAttributes.preserve) {
				replaceLeftSpacesWithIndentation(indentLevel + 1, from, to, true, edits);
				parentConstraints.setStartTagCrossedLine(true);
				int attrValuelength = attr.getValue() != null ? attr.getValue().length() : 0;
				parentConstraints.setAvailableLineWidth(
						getMaxLineWidth() - getTabSize() * (indentLevel + 1) - attributeNamelength
								- attrValuelength);
			} else {
				// remove extra whitespaces between previous attribute
				// attr0='name'[space][space][space]attr1='name' -->
				// attr0='name'[space]attr1='name'

				// Adjust the startAttr to avoid ignoring invalid content
				// ex : <asdf |""`=asdf />
				// must be adjusted with <asdf ""|`=asdf /> to keep the invalid content ""
				replaceLeftSpacesWithOneSpace(from, to, edits);
			}
		}

		// Phase 3: enforce preferred quote style
		// Use case (preferred = single quote):
		//   <a name="value"> → <a name='value'>
		String originalValue = attr.getOriginalValue();
		if (getEnforceQuoteStyle() == EnforceQuoteStyle.preferred && originalValue != null) {
			if (originalValue.charAt(0) != getQuotationAsChar() && StringUtils.isQuote(originalValue.charAt(0))) {
				replaceQuoteWithPreferred(attr.getNodeAttrValue().getStart(), attr.getNodeAttrValue().getStart() + 1,
						edits);
			}
			if (originalValue.charAt(originalValue.length() - 1) != getQuotationAsChar()
					&& StringUtils.isQuote(originalValue.charAt(originalValue.length() - 1))) {
				replaceQuoteWithPreferred(attr.getNodeAttrValue().getEnd() - 1, attr.getNodeAttrValue().getEnd(),
						edits);
			}
		}
	}

	/**
	 * Computes the column offset of the first attribute for
	 * {@code alignWithFirstAttr} mode.
	 *
	 * <p>Use case: for {@code <foo attr1="">} at indent level 1 with tabSize 2,
	 * offset = 2 (indent) + 3 (foo) + 2 ({'<'} + space) = 7.</p>
	 *
	 * @param ownerElement the element that owns the attributes.
	 * @param indentLevel  the current indent level.
	 * @return the column offset where the first attribute starts.
	 */
	private int getFirstAttrOffset(DOMElement ownerElement, int indentLevel) {
		// +1 for '<', +1 for space between element name and first attr name
		return getTabSize() * indentLevel + ownerElement.getTagName().length() + 2;
	}

	/** Delegates to {@link XMLFormatterDocument#formatAttributeValue}. */
	private void formatAttributeValue(DOMAttr attr, XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		formatterDocument.formatAttributeValue(attr, parentConstraints, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceQuoteWithPreferred}. */
	private void replaceQuoteWithPreferred(int from, int to, List<TextEdit> edits) {
		formatterDocument.replaceQuoteWithPreferred(from, to, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationWithOffsetSpaces}. */
	private void replaceLeftSpacesWithIndentationWithOffsetSpaces(int spaceCount, int from, int to,
			List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationWithOffsetSpaces(spaceCount, from, to, true, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithOneSpace}. */
	private void replaceLeftSpacesWithOneSpace(int from, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithOneSpace(from, to, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private void replaceLeftSpacesWithIndentation(int indentLevel, int leftLimit, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, leftLimit, to, addLineSeparator, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#removeLeftSpaces}. */
	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	/** Returns the configured split attributes mode. */
	private SplitAttributes getSplitAttributes() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributes();
	}

	/** Returns the number of extra indent levels for split attributes. */
	private int getSplitAttributesIndentSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributesIndentSize();
	}

	/** Returns true if attribute line breaks should be preserved. */
	boolean isPreserveAttributeLineBreaks() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveAttributeLineBreaks();
	}

	/** Returns true if there is a line break between the two offsets. */
	private boolean hasLineBreak(int prevOffset, int start) {
		return formatterDocument.hasLineBreak(prevOffset, start);
	}

	/** Returns the preferred quotation character (single or double quote). */
	private char getQuotationAsChar() {
		return formatterDocument.getSharedSettings().getPreferences().getQuotationAsChar();
	}

	/** Returns the configured quote enforcement style. */
	private EnforceQuoteStyle getEnforceQuoteStyle() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getEnforceQuoteStyle();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}
}
