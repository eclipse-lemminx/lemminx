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

/**
 * Mutable state passed through the formatting tree to track the current
 * formatting context: how deep the indentation is, how much line width
 * remains, and which {@link FormatElementCategory} applies.
 *
 * <p>
 * Each element creates a <b>copy</b> of its parent's constraints before
 * formatting its children (so child formatting does not corrupt the
 * parent's tracking), then propagates the final
 * {@code availableLineWidth} back to the parent after the end tag.
 * </p>
 *
 * <p>Flow example for {@code <root><a><b/></a></root>}:</p>
 * <ol>
 * <li>{@code <root>} creates constraints with indentLevel=0, copies to child constraints with indentLevel=1.</li>
 * <li>{@code <a>} receives indentLevel=1, copies to child constraints with indentLevel=2.</li>
 * <li>{@code <b/>} receives indentLevel=2, formatted inline.</li>
 * <li>{@code </a>} propagates remaining availableLineWidth back to {@code <root>}'s constraints.</li>
 * </ol>
 *
 * @author Angelo ZERR
 */
public class XMLFormattingConstraints {

	/**
	 * How this element's whitespace should be handled.
	 * Set by the parent element based on content analysis.
	 */
	private FormatElementCategory formatElementCategory;

	/**
	 * Characters remaining on the current line before {@code maxLineWidth}
	 * is exceeded. Decremented as tokens are formatted; when negative,
	 * the next element in mixed content wraps to a new line.
	 * Reset to {@code maxLineWidth} when a new line is started.
	 *
	 * <p>Use case: with maxLineWidth=80 and 60 characters already on the line,
	 * availableLineWidth=20. Formatting a 25-character token makes it −5,
	 * triggering a wrap for the next sibling.</p>
	 */
	private int availableLineWidth = 0;

	/**
	 * Current nesting depth for indentation. Each level adds
	 * {@code tabSize} spaces (or one tab character) to the indent.
	 * Incremented by 1 for each non-root closed element.
	 *
	 * <p>Use case: indentLevel=2 with tabSize=2 produces 4 spaces of indent.</p>
	 */
	private int indentLevel = 0;

	/**
	 * Indent level captured when the first child element inside a
	 * {@link FormatElementCategory#MixedContent} parent is encountered.
	 * Used by {@link DOMTextFormatter} to indent wrapped text at the
	 * same level as the surrounding mixed content, rather than at the
	 * deeper child level.
	 *
	 * <p>Use case: in {@code <p>text that wraps <b>bold</b></p>},
	 * wrapped text lines align with the {@code <p>} indent, not the
	 * {@code <b>} indent.</p>
	 *
	 * <p>Zero means "not yet set" — the actual indent level is used instead.</p>
	 */
	private int mixedContentIndentLevel = 0;

	/**
	 * When {@code true}, all child elements in a MixedContent parent are
	 * formatted as block elements (each on its own line), regardless of the
	 * {@code blockElements} setting. Only used in {@code expand} mode.
	 */
	private boolean wrapAllChildren = false;

	/**
	 * Set to {@code true} when text or elements are soft-wrapped to a new
	 * line due to {@code maxLineWidth} overflow in {@code normalize} mode.
	 * Used by the end tag handler to put the closing tag on its own line.
	 *
	 * <p>Use case: {@code <p>Click <b>here</b> to see the <b>details</b></p>}
	 * with {@code maxLineWidth=40} → {@code <b>details</b>} wraps, so
	 * {@code </p>} goes on its own line.</p>
	 */
	private boolean softWrapped = false;

	/**
	 * Set to {@code true} when the formatter emits a newline before an
	 * attribute during the current formatting pass. Tracked per-element
	 * to detect multi-line start tags in the formatted output (not the
	 * original document).
	 */
	private boolean startTagCrossedLine = false;

	/**
	 * Copies all constraint values from another instance.
	 * Used to create child constraints from the parent before formatting
	 * an element's children.
	 *
	 * @param constraints the source constraints to copy from (must not be null).
	 */
	public void copyConstraints(XMLFormattingConstraints constraints) {
		setFormatElementCategory(constraints.getFormatElementCategory());
		setAvailableLineWidth(constraints.getAvailableLineWidth());
		setIndentLevel(constraints.getIndentLevel());
		setMixedContentIndentLevel(constraints.getMixedContentIndentLevel());
		setWrapAllChildren(constraints.isWrapAllChildren());
	}

	/**
	 * Returns the format element category that determines whitespace handling.
	 *
	 * @return the current format element category.
	 */
	public FormatElementCategory getFormatElementCategory() {
		return formatElementCategory;
	}

	/**
	 * Sets the format element category.
	 *
	 * @param formatElementCategory the category to set.
	 */
	public void setFormatElementCategory(FormatElementCategory formatElementCategory) {
		this.formatElementCategory = formatElementCategory;
	}

	/**
	 * Returns the number of characters remaining on the current line
	 * before {@code maxLineWidth} is exceeded.
	 *
	 * @return remaining line width (negative means the line is overflowed).
	 */
	public int getAvailableLineWidth() {
		return availableLineWidth;
	}

	/**
	 * Sets the available line width.
	 *
	 * @param availableLineWidth the remaining width to set.
	 */
	public void setAvailableLineWidth(int availableLineWidth) {
		this.availableLineWidth = availableLineWidth;
	}

	/**
	 * Returns the current indentation nesting depth.
	 *
	 * @return the indent level (0 = root level).
	 */
	public int getIndentLevel() {
		return indentLevel;
	}

	/**
	 * Sets the indentation nesting depth.
	 *
	 * @param indentLevel the indent level to set.
	 */
	public void setIndentLevel(int indentLevel) {
		this.indentLevel = indentLevel;
	}

	/**
	 * Returns the indent level for mixed content text wrapping.
	 * Zero means "not yet captured" — the regular indent level is used.
	 *
	 * @return the mixed content indent level, or 0 if not set.
	 */
	public int getMixedContentIndentLevel() {
		return mixedContentIndentLevel;
	}

	/**
	 * Sets the indent level used for mixed content text wrapping.
	 *
	 * @param mixedContentIndentLevel the indent level to set.
	 */
	public void setMixedContentIndentLevel(int mixedContentIndentLevel) {
		this.mixedContentIndentLevel = mixedContentIndentLevel;
	}

	/**
	 * Returns {@code true} if all children should be formatted as block
	 * elements (one child per line). Only used in {@code expand} mode.
	 *
	 * <p>Use case: {@code <p>text <b>bold</b></p>} with {@code mixedContent=expand}
	 * → {@code <p>\n  text\n  <b>bold</b>\n</p>}.</p>
	 *
	 * @return {@code true} if wrap-all-children mode is active.
	 */
	public boolean isWrapAllChildren() {
		return wrapAllChildren;
	}

	/**
	 * Sets the wrap-all-children flag.
	 *
	 * @param wrapAllChildren {@code true} to force all children to block layout.
	 */
	public void setWrapAllChildren(boolean wrapAllChildren) {
		this.wrapAllChildren = wrapAllChildren;
	}

	/**
	 * Returns {@code true} if content was soft-wrapped due to
	 * {@code maxLineWidth} overflow during child formatting.
	 *
	 * @return {@code true} if soft wrapping occurred.
	 */
	public boolean isSoftWrapped() {
		return softWrapped;
	}

	/**
	 * Sets the soft-wrapped flag.
	 *
	 * @param softWrapped {@code true} when text or element wrapping occurred.
	 */
	public void setSoftWrapped(boolean softWrapped) {
		this.softWrapped = softWrapped;
	}

	public boolean isStartTagCrossedLine() {
		return startTagCrossedLine;
	}

	public void setStartTagCrossedLine(boolean startTagCrossedLine) {
		this.startTagCrossedLine = startTagCrossedLine;
	}

}
