/**
 *  Copyright (c) 2018 Angelo ZERR
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
package org.eclipse.lemminx.settings;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.xsi.settings.XSISchemaLocationSplit;
import org.eclipse.lemminx.services.format.FormatElementCategory;
import org.eclipse.lsp4j.FormattingOptions;

/**
 * This class is the root of all formatting settings. It is necessary to update
 * this class for any new additions.
 *
 * All defaults should be set here to eventually be overridden if needed.
 */
public class XMLFormattingOptions extends org.eclipse.lemminx.settings.LSPFormattingOptions {

	public static final String DEFAULT_QUOTATION = "\"";

	public static final int DEFAULT_PRESERVER_NEW_LINES = 2;

	public static final int DEFAULT_TAB_SIZE = 2;

	public static final EnforceQuoteStyle DEFAULT_ENFORCE_QUOTE_STYLE = EnforceQuoteStyle.ignore;

	public static final boolean DEFAULT_PRESERVE_ATTR_LINE_BREAKS = true;

	public static final boolean DEFAULT_TRIM_TRAILING_SPACES = false;

	public static final int DEFAULT_SPLIT_ATTRIBUTES_INDENT_SIZE = 2;

	public static final String DEFAULT_XSI_SCHEMA_LOCATION_SPLIT = XSISchemaLocationSplit.onPair.name();

	public static final boolean DEFAULT_CLOSING_BRACKET_NEW_LINE = false;

	public static final List<String> DEFAULT_PRESERVE_SPACE = Arrays.asList("xsl:text", //
			"xsl:comment", //
			"xsl:processing-instruction", //
			"literallayout", //
			"programlisting", //
			"screen", //
			"synopsis", //
			"pre", //
			"xd:pre", //
			"style", //
			"script");

	private boolean legacy;
	private int maxLineWidth;

	public static enum SplitAttributes {
		preserve, splitNewLine, alignWithFirstAttr;
	}

	private String splitAttributes;
	private boolean joinCDATALines;
	private boolean formatComments;
	private boolean joinCommentLines;
	private boolean enabled;
	private boolean spaceBeforeEmptyCloseTag;
	private boolean joinContentLines;
	private int preservedNewlines;
	private String enforceQuoteStyle;

	private boolean preserveAttributeLineBreaks;
	private boolean preserveEmptyContent;
	private int splitAttributesIndentSize;
	private boolean closingBracketNewLine;

	/**
	 * Options for formatting empty elements.
	 *
	 * <ul>
	 * <li>{@link #expand} : expand empty elements. With this option the following
	 * XML:
	 *
	 * <pre>
	 * {@code
	 * <example />
	 * }
	 * </pre>
	 *
	 * will be formatted to :
	 *
	 * <pre>
	 * {@code
	 * <example><example>
	 * }
	 * </pre>
	 *
	 * </li>
	 * <li>{@link #collapse} : collapse empty elements. With this option the
	 * following XML:
	 *
	 * <pre>
	 * {@code
	 * <example></example>
	 * }
	 * </pre>
	 *
	 * will be formatted to :
	 *
	 * <pre>
	 * {@code
	 * <example />
	 * }
	 * </pre>
	 *
	 * </li>
	 * <li>{@link #ignore} : keeps the original XML content for empty elements.</li>
	 * </ul>
	 *
	 */
	public static enum EmptyElements {
		expand, collapse, ignore;
	}

	private String emptyElements;
	private List<String> preserveSpace;

	private boolean grammarAwareFormatting;

	private String xsiSchemaLocationSplit;

	public XMLFormattingOptions() {
		this(false);
	}

	/**
	 * Create an XMLFormattingOptions instance with the option to initialize default
	 * values for all supported settings.
	 */
	public XMLFormattingOptions(boolean initializeDefaults) {
		if (initializeDefaults) {
			initializeDefaultSettings();
		}
	}

	/**
	 * Necessary: Initialize default values in case client does not provide one
	 */
	private void initializeDefaultSettings() {
		super.setTabSize(DEFAULT_TAB_SIZE);
		super.setInsertSpaces(true);
		super.setTrimFinalNewlines(true);
		this.setSplitAttributes(SplitAttributes.preserve);
		this.setJoinCDATALines(false);
		this.setFormatComments(true);
		this.setJoinCommentLines(false);
		this.setJoinContentLines(false);
		this.setEnabled(true);
		this.setLegacy(false);
		this.setMaxLineWidth(100);
		this.setSpaceBeforeEmptyCloseTag(true);
		this.setPreserveEmptyContent(false);
		this.setPreservedNewlines(DEFAULT_PRESERVER_NEW_LINES);
		this.setEmptyElement(EmptyElements.ignore);
		this.setSplitAttributesIndentSize(DEFAULT_SPLIT_ATTRIBUTES_INDENT_SIZE);
		this.setXsiSchemaLocationSplit(DEFAULT_XSI_SCHEMA_LOCATION_SPLIT);
		this.setClosingBracketNewLine(DEFAULT_CLOSING_BRACKET_NEW_LINE);
		this.setPreserveAttributeLineBreaks(DEFAULT_PRESERVE_ATTR_LINE_BREAKS);
		this.setPreserveSpace(DEFAULT_PRESERVE_SPACE);
		this.setGrammarAwareFormatting(true);
	}

	public XMLFormattingOptions(int tabSize, boolean insertSpaces, boolean initializeDefaultSettings) {
		if (initializeDefaultSettings) {
			initializeDefaultSettings();
		}
		super.setTabSize(tabSize);
		super.setInsertSpaces(insertSpaces);
	}

	public XMLFormattingOptions(int tabSize, boolean insertSpaces) {
		this(tabSize, insertSpaces, true);
	}

	public XMLFormattingOptions(FormattingOptions options, boolean initializeDefaultSettings) {
		if (initializeDefaultSettings) {
			initializeDefaultSettings();
		}
		merge(options);
	}

	public XMLFormattingOptions(FormattingOptions options) {
		this(options, true);
	}

	public SplitAttributes getSplitAttributes() {
		String value = splitAttributes;
		if ((value != null)) {
			try {
				return SplitAttributes.valueOf(value);
			} catch (Exception e) {
			}
		}
		return SplitAttributes.preserve;
	}

	public void setSplitAttributes(SplitAttributes splitAttributes) {
		this.splitAttributes = splitAttributes.name();
	}

	public boolean isJoinCDATALines() {
		return joinCDATALines;
	}

	public void setJoinCDATALines(final boolean joinCDATALines) {
		this.joinCDATALines = joinCDATALines;
	}

	public boolean isFormatComments() {
		return formatComments;
	}

	public void setFormatComments(final boolean formatComments) {
		this.formatComments = formatComments;
	}

	public boolean isJoinCommentLines() {
		return joinCommentLines;
	}

	public void setJoinCommentLines(final boolean joinCommentLines) {
		this.joinCommentLines = joinCommentLines;
	}

	public boolean isJoinContentLines() {
		return joinContentLines;
	}

	public void setJoinContentLines(final boolean joinContentLines) {
		this.joinContentLines = joinContentLines;
	}

	/**
	 * Returns true if the legacy formatter must be used and false otherwise.
	 * 
	 * @return true if the legacy formatter must be used and false otherwise.
	 */
	public boolean isLegacy() {
		return legacy;
	}

	/**
	 * Set true if the legacy formatter must be used and false otherwise.
	 * 
	 * @param legacy true if the legacy formatter must be used and false
	 *                     otherwise.
	 */
	public void setLegacy(final boolean legacy) {
		this.legacy = legacy;
	}

	/**
	 * Sets the value of max line width.
	 *
	 * @param maxLineWidth the new value for max line width.
	 */
	public void setMaxLineWidth(int maxLineWidth) {
		this.maxLineWidth = maxLineWidth;
	}

	/**
	 * Returns the value of max line width or zero if it was set to a negative value
	 *
	 * @return the value of max line width or zero if it was set to a negative value
	 */
	public int getMaxLineWidth() {
		return maxLineWidth < 0 ? 0 : maxLineWidth;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public void setSpaceBeforeEmptyCloseTag(final boolean spaceBeforeEmptyCloseTag) {
		this.spaceBeforeEmptyCloseTag = spaceBeforeEmptyCloseTag;
	}

	public boolean isSpaceBeforeEmptyCloseTag() {
		return spaceBeforeEmptyCloseTag;
	}

	public void setPreserveEmptyContent(final boolean preserveEmptyContent) {
		this.preserveEmptyContent = preserveEmptyContent;
	}

	public boolean isPreserveEmptyContent() {
		return preserveEmptyContent;
	}

	public void setPreservedNewlines(final int preservedNewlines) {
		this.preservedNewlines = preservedNewlines;
	}

	public int getPreservedNewlines() {
		return preservedNewlines;
	}

	public void setEmptyElement(EmptyElements emptyElement) {
		this.emptyElements = emptyElement.name();
	}

	public EmptyElements getEmptyElements() {
		String value = emptyElements;
		if ((value != null)) {
			try {
				return EmptyElements.valueOf(value);
			} catch (Exception e) {
			}
		}
		return EmptyElements.ignore;
	}

	public void setEnforceQuoteStyle(EnforceQuoteStyle enforce) {
		this.enforceQuoteStyle = enforce.name();
	}

	public EnforceQuoteStyle getEnforceQuoteStyle() {
		String value = this.enforceQuoteStyle;
		EnforceQuoteStyle enforceStyle = null;

		try {
			enforceStyle = value == null ? null : EnforceQuoteStyle.valueOf(value);
		} catch (IllegalArgumentException e) {
			return DEFAULT_ENFORCE_QUOTE_STYLE;
		}

		return enforceStyle == null ? DEFAULT_ENFORCE_QUOTE_STYLE : enforceStyle;
	}

	/**
	 * Sets the value of preserveAttrLineBreaks
	 */
	public void setPreserveAttributeLineBreaks(final boolean preserveAttributeLineBreaks) {
		this.preserveAttributeLineBreaks = preserveAttributeLineBreaks;
	}

	/**
	 * Returns the value of preserveAttrLineBreaks
	 *
	 * @return the value of preserveAttrLineBreaks
	 */
	public boolean isPreserveAttributeLineBreaks() {
		if (this.getSplitAttributes() != SplitAttributes.preserve) {
			// splitAttributes overrides preserveAttrLineBreaks
			return false;
		}
		return preserveAttributeLineBreaks;
	}

	/**
	 * Sets the value of splitAttributesIndentSize
	 *
	 * @param splitAttributesIndentSize the new value for splitAttributesIndentSize
	 */
	public void setSplitAttributesIndentSize(int splitAttributesIndentSize) {
		this.splitAttributesIndentSize = splitAttributesIndentSize;
	}

	/**
	 * Returns the value of splitAttributesIndentSize or zero if it was set to a
	 * negative value
	 *
	 * @return the value of splitAttributesIndentSize or zero if it was set to a
	 *         negative value
	 */
	public int getSplitAttributesIndentSize() {
		int splitAttributesIndentSize = this.splitAttributesIndentSize;
		return splitAttributesIndentSize < 0 ? 0 : splitAttributesIndentSize;
	}

	/**
	 * Returns the value of closingBracketNewLine or false if it was set to null
	 * 
	 * A setting for enabling the XML formatter to move the closing bracket of a tag
	 * with at least 2 attributes to a new line.
	 *
	 * @return the value of closingBracketNewLine or false if it was set to null
	 */
	public boolean getClosingBracketNewLine() {
		return closingBracketNewLine;
	}

	/**
	 * Sets the value of closingBracketNewLine
	 *
	 * @param closingBracketNewLine the new value for closingBracketNewLine
	 */
	public void setClosingBracketNewLine(final boolean closingBracketNewLine) {
		this.closingBracketNewLine = closingBracketNewLine;
	}

	/**
	 * Sets the element name list which must preserve space.
	 *
	 * @param preserveSpace the element name list which must preserve space.
	 */
	public void setPreserveSpace(List<String> preserveSpace) {
		this.preserveSpace = preserveSpace;
	}

	/**
	 * Returns the element name list which must preserve space.
	 *
	 * @return the element name list which must preserve space.
	 */
	public List<String> getPreserveSpace() {
		return preserveSpace;
	}

	public boolean isGrammarAwareFormatting() {
		return grammarAwareFormatting;
	}

	public void setGrammarAwareFormatting(boolean grammarAwareFormatting) {
		this.grammarAwareFormatting = grammarAwareFormatting;
	}

	public String getXsiSchemaLocationSplit() {
		return xsiSchemaLocationSplit;
	}

	public void setXsiSchemaLocationSplit(String xsiSchemaLocationSplit) {
		this.xsiSchemaLocationSplit = xsiSchemaLocationSplit;
	}

	public XMLFormattingOptions merge(XMLFormattingOptions formattingOptions) {
		setTabSize(formattingOptions.getTabSize());
		setInsertFinalNewline(formattingOptions.isInsertFinalNewline());
		setInsertSpaces(formattingOptions.isInsertSpaces());
		setTrimFinalNewlines(formattingOptions.isTrimFinalNewlines());
		setTrimTrailingWhitespace(formattingOptions.isTrimTrailingWhitespace());
		setLegacy(formattingOptions.isLegacy());
		setMaxLineWidth(formattingOptions.getMaxLineWidth());
		setSplitAttributes(formattingOptions.getSplitAttributes());
		setJoinCDATALines(formattingOptions.isJoinCDATALines());
		setFormatComments(formattingOptions.isFormatComments());
		setJoinCommentLines(formattingOptions.isJoinCommentLines());
		setEnabled(formattingOptions.isEnabled());
		setSpaceBeforeEmptyCloseTag(formattingOptions.isSpaceBeforeEmptyCloseTag());
		setJoinContentLines(formattingOptions.isJoinContentLines());
		setPreservedNewlines(formattingOptions.getPreservedNewlines());
		setEnforceQuoteStyle(formattingOptions.getEnforceQuoteStyle());
		setPreserveAttributeLineBreaks(formattingOptions.isPreserveAttributeLineBreaks());
		setPreserveEmptyContent(formattingOptions.isPreserveEmptyContent());
		setSplitAttributesIndentSize(formattingOptions.getSplitAttributesIndentSize());
		setClosingBracketNewLine(formattingOptions.getClosingBracketNewLine());
		setEmptyElement(formattingOptions.getEmptyElements());
		setXsiSchemaLocationSplit(formattingOptions.getXsiSchemaLocationSplit());
		// New formatter settings
		setPreserveSpace(formattingOptions.getPreserveSpace());
		setGrammarAwareFormatting(formattingOptions.isGrammarAwareFormatting());
		setMaxLineWidth(formattingOptions.getMaxLineWidth());
		return this;
	}

	public XMLFormattingOptions merge(FormattingOptions formattingOptions) {
		setTabSize(formattingOptions.getTabSize());
		setInsertFinalNewline(formattingOptions.isInsertFinalNewline());
		setInsertSpaces(formattingOptions.isInsertSpaces());
		setTrimFinalNewlines(formattingOptions.isTrimFinalNewlines());
		setTrimTrailingWhitespace(formattingOptions.isTrimTrailingWhitespace());
		return this;
	}

	public FormatElementCategory getFormatElementCategory(DOMElement element) {
		if (preserveSpace != null) {
			for (String elementName : preserveSpace) {
				if (elementName.equals(element.getTagName())) {
					return FormatElementCategory.PreserveSpace;
				}
			}
		}
		return null;
	}
}
