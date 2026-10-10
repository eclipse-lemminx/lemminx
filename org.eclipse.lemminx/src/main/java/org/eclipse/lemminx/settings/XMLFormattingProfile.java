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
package org.eclipse.lemminx.settings;

import java.util.List;

import org.eclipse.lemminx.settings.XMLFormattingOptions.EmptyElements;
import org.eclipse.lemminx.settings.XMLFormattingOptions.MixedContent;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;

/**
 * A formatting profile that overrides global {@code xml.format.*} settings for
 * documents matching specific criteria. Extends {@link DocumentMatcher} so that
 * matching criteria (pattern, namespaceURI, publicId, systemId, grammarURI) and
 * format override fields all live at the same JSON level (flat structure).
 *
 * <p>
 * Profiles are declared in {@code xml.format.profiles} and evaluated in order;
 * the first matching profile wins. All format fields use boxed types
 * ({@link Boolean}, {@link Integer}, {@link String}) so that {@code null} means
 * "not specified — use the global default".
 * </p>
 *
 * <h3>Matching criteria (all optional, combined with AND)</h3>
 * <ul>
 * <li>{@code pattern} — glob on the file URI
 * (inherited from {@link PathPatternMatcher}).</li>
 * <li>{@code namespaceURI} — root element namespace URI
 * (inherited from {@link DocumentMatcher}).</li>
 * <li>{@code rootElement} — root element local name
 * (inherited from {@link DocumentMatcher}).</li>
 * <li>{@code publicId} — DOCTYPE public identifier
 * (inherited from {@link DocumentMatcher}).</li>
 * <li>{@code systemId} — DOCTYPE system identifier
 * (inherited from {@link DocumentMatcher}).</li>
 * <li>{@code grammarURI} — resolved grammar URI from any source: DOCTYPE
 * systemId, {@code <?xml-model?>} href, file association, or XML catalog
 * (inherited from {@link DocumentMatcher}).</li>
 * </ul>
 *
 * <h3>Use case: DocBook formatting</h3>
 *
 * <pre>
 * "xml.format.profiles": [{
 *   "namespaceURI": ["http://docbook.org/ns/docbook*"],
 *   "mixedContent": "preserve"
 * }]
 * </pre>
 *
 * <h3>Use case: MyBatis via DOCTYPE</h3>
 *
 * <pre>
 * "xml.format.profiles": [{
 *   "publicId": ["-//mybatis.org//DTD Mapper 3.0//EN"],
 *   "splitAttributes": "preserve"
 * }]
 * </pre>
 *
 * <h3>Use case: POM files by path</h3>
 *
 * <pre>
 * "xml.format.profiles": [{
 *   "pattern": "**&#47;pom.xml",
 *   "splitAttributes": "force-expand-multiline"
 * }]
 * </pre>
 *
 * <h3>Use case: AND combination — DocBook files in a specific folder</h3>
 *
 * <pre>
 * "xml.format.profiles": [{
 *   "pattern": "**&#47;docs/**&#47;*.xml",
 *   "namespaceURI": ["http://docbook.org/ns/docbook*"],
 *   "mixedContent": "preserve"
 * }]
 * </pre>
 *
 * @see DocumentMatcher
 * @see XMLFormattingOptions
 */
public class XMLFormattingProfile extends DocumentMatcher {

	// --- Format override fields (boxed types, null = not specified) ---

	private String splitAttributes;
	private Boolean joinCDATALines;
	private Boolean formatComments;
	private Boolean joinCommentLines;
	private Boolean joinContentLines;
	private Boolean enabled;
	private Boolean spaceBeforeEmptyCloseTag;
	private Integer preservedNewlines;
	private String enforceQuoteStyle;
	private Boolean preserveAttributeLineBreaks;
	private Boolean preserveEmptyContent;
	private Integer splitAttributesIndentSize;
	private Boolean closingBracketNewLine;
	private String emptyElements;
	private List<String> preserveSpace;
	private List<String> blockElements;
	private String mixedContent;
	private Boolean grammarAwareFormatting;
	private String xsiSchemaLocationSplit;
	private Boolean legacy;
	private Integer maxLineWidth;

	/**
	 * Applies all non-null format override fields from this profile onto the
	 * given target formatting options. Fields that are {@code null} are left
	 * unchanged in the target (the global default is kept).
	 *
	 * <p>
	 * Use case: given global settings with {@code splitAttributes=preserve} and a
	 * profile with {@code splitAttributes=force-expand-multiline}, calling
	 * {@code applyTo(globalCopy)} changes only {@code splitAttributes} while
	 * keeping all other fields from the global settings.
	 * </p>
	 *
	 * @param target the formatting options to modify (typically a copy of the
	 *               global settings).
	 */
	public void applyTo(XMLFormattingOptions target) {
		if (splitAttributes != null) {
			target.setSplitAttributes(SplitAttributes.fromString(splitAttributes));
		}
		if (joinCDATALines != null) {
			target.setJoinCDATALines(joinCDATALines);
		}
		if (formatComments != null) {
			target.setFormatComments(formatComments);
		}
		if (joinCommentLines != null) {
			target.setJoinCommentLines(joinCommentLines);
		}
		if (joinContentLines != null) {
			target.setJoinContentLines(joinContentLines);
		}
		if (enabled != null) {
			target.setEnabled(enabled);
		}
		if (spaceBeforeEmptyCloseTag != null) {
			target.setSpaceBeforeEmptyCloseTag(spaceBeforeEmptyCloseTag);
		}
		if (preservedNewlines != null) {
			target.setPreservedNewlines(preservedNewlines);
		}
		if (enforceQuoteStyle != null) {
			try {
				target.setEnforceQuoteStyle(EnforceQuoteStyle.valueOf(enforceQuoteStyle));
			} catch (IllegalArgumentException e) {
				// ignore invalid value
			}
		}
		if (preserveAttributeLineBreaks != null) {
			target.setPreserveAttributeLineBreaks(preserveAttributeLineBreaks);
		}
		if (preserveEmptyContent != null) {
			target.setPreserveEmptyContent(preserveEmptyContent);
		}
		if (splitAttributesIndentSize != null) {
			target.setSplitAttributesIndentSize(splitAttributesIndentSize);
		}
		if (closingBracketNewLine != null) {
			target.setClosingBracketNewLine(closingBracketNewLine);
		}
		if (emptyElements != null) {
			try {
				target.setEmptyElement(EmptyElements.valueOf(emptyElements));
			} catch (IllegalArgumentException e) {
				// ignore invalid value
			}
		}
		if (preserveSpace != null) {
			target.setPreserveSpace(preserveSpace);
		}
		if (blockElements != null) {
			target.setBlockElements(blockElements);
		}
		if (mixedContent != null) {
			try {
				target.setMixedContent(MixedContent.valueOf(mixedContent));
			} catch (IllegalArgumentException e) {
				// ignore invalid value
			}
		}
		if (grammarAwareFormatting != null) {
			target.setGrammarAwareFormatting(grammarAwareFormatting);
		}
		if (xsiSchemaLocationSplit != null) {
			target.setXsiSchemaLocationSplit(xsiSchemaLocationSplit);
		}
		if (legacy != null) {
			target.setLegacy(legacy);
		}
		if (maxLineWidth != null) {
			target.setMaxLineWidth(maxLineWidth);
		}
	}

	// --- Getters / Setters ---

	public String getSplitAttributes() {
		return splitAttributes;
	}

	public void setSplitAttributes(String splitAttributes) {
		this.splitAttributes = splitAttributes;
	}

	public Boolean getJoinCDATALines() {
		return joinCDATALines;
	}

	public void setJoinCDATALines(Boolean joinCDATALines) {
		this.joinCDATALines = joinCDATALines;
	}

	public Boolean getFormatComments() {
		return formatComments;
	}

	public void setFormatComments(Boolean formatComments) {
		this.formatComments = formatComments;
	}

	public Boolean getJoinCommentLines() {
		return joinCommentLines;
	}

	public void setJoinCommentLines(Boolean joinCommentLines) {
		this.joinCommentLines = joinCommentLines;
	}

	public Boolean getJoinContentLines() {
		return joinContentLines;
	}

	public void setJoinContentLines(Boolean joinContentLines) {
		this.joinContentLines = joinContentLines;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getSpaceBeforeEmptyCloseTag() {
		return spaceBeforeEmptyCloseTag;
	}

	public void setSpaceBeforeEmptyCloseTag(Boolean spaceBeforeEmptyCloseTag) {
		this.spaceBeforeEmptyCloseTag = spaceBeforeEmptyCloseTag;
	}

	public Integer getPreservedNewlines() {
		return preservedNewlines;
	}

	public void setPreservedNewlines(Integer preservedNewlines) {
		this.preservedNewlines = preservedNewlines;
	}

	public String getEnforceQuoteStyle() {
		return enforceQuoteStyle;
	}

	public void setEnforceQuoteStyle(String enforceQuoteStyle) {
		this.enforceQuoteStyle = enforceQuoteStyle;
	}

	public Boolean getPreserveAttributeLineBreaks() {
		return preserveAttributeLineBreaks;
	}

	public void setPreserveAttributeLineBreaks(Boolean preserveAttributeLineBreaks) {
		this.preserveAttributeLineBreaks = preserveAttributeLineBreaks;
	}

	public Boolean getPreserveEmptyContent() {
		return preserveEmptyContent;
	}

	public void setPreserveEmptyContent(Boolean preserveEmptyContent) {
		this.preserveEmptyContent = preserveEmptyContent;
	}

	public Integer getSplitAttributesIndentSize() {
		return splitAttributesIndentSize;
	}

	public void setSplitAttributesIndentSize(Integer splitAttributesIndentSize) {
		this.splitAttributesIndentSize = splitAttributesIndentSize;
	}

	public Boolean getClosingBracketNewLine() {
		return closingBracketNewLine;
	}

	public void setClosingBracketNewLine(Boolean closingBracketNewLine) {
		this.closingBracketNewLine = closingBracketNewLine;
	}

	public String getEmptyElements() {
		return emptyElements;
	}

	public void setEmptyElements(String emptyElements) {
		this.emptyElements = emptyElements;
	}

	public List<String> getPreserveSpace() {
		return preserveSpace;
	}

	public void setPreserveSpace(List<String> preserveSpace) {
		this.preserveSpace = preserveSpace;
	}

	public List<String> getBlockElements() {
		return blockElements;
	}

	public void setBlockElements(List<String> blockElements) {
		this.blockElements = blockElements;
	}

	public String getMixedContent() {
		return mixedContent;
	}

	public void setMixedContent(String mixedContent) {
		this.mixedContent = mixedContent;
	}

	public Boolean getGrammarAwareFormatting() {
		return grammarAwareFormatting;
	}

	public void setGrammarAwareFormatting(Boolean grammarAwareFormatting) {
		this.grammarAwareFormatting = grammarAwareFormatting;
	}

	public String getXsiSchemaLocationSplit() {
		return xsiSchemaLocationSplit;
	}

	public void setXsiSchemaLocationSplit(String xsiSchemaLocationSplit) {
		this.xsiSchemaLocationSplit = xsiSchemaLocationSplit;
	}

	public Boolean getLegacy() {
		return legacy;
	}

	public void setLegacy(Boolean legacy) {
		this.legacy = legacy;
	}

	public Integer getMaxLineWidth() {
		return maxLineWidth;
	}

	public void setMaxLineWidth(Integer maxLineWidth) {
		this.maxLineWidth = maxLineWidth;
	}

}
