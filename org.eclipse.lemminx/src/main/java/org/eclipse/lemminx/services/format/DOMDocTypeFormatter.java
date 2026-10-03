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

import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DTDAttlistDecl;
import org.eclipse.lemminx.dom.DTDDeclNode;
import org.eclipse.lemminx.dom.DTDDeclParameter;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.TextEdit;
import org.w3c.dom.Node;

/**
 * Formats DOCTYPE declarations and DTD content.
 *
 * <p>Handles two contexts:</p>
 * <ul>
 * <li><b>Inline DOCTYPE</b> — a {@code <!DOCTYPE ...>} declaration inside an
 * XML document. The declaration is indented relative to its parent, parameters
 * are separated by single spaces, and quotes are enforced.
 * <pre>
 * &lt;!DOCTYPE note SYSTEM "note.dtd"&gt;
 * </pre></li>
 *
 * <li><b>DTD file / internal subset</b> — DTD declarations ({@code <!ELEMENT>},
 * {@code <!ENTITY>}, {@code <!ATTLIST>}, {@code <!NOTATION>}) are formatted
 * with proper indentation, single-space parameter separation, and quote
 * enforcement.
 * <pre>
 * &lt;!DOCTYPE person [
 *   &lt;!ELEMENT person (name, age)&gt;
 *   &lt;!ENTITY AUTHOR "John Doe"&gt;
 * ]&gt;
 * </pre></li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class DOMDocTypeFormatter {

	private final XMLFormatterDocument formatterDocument;

	/**
	 * Creates a new DOCTYPE formatter.
	 *
	 * @param formatterDocument the parent formatter document (provides settings and edit helpers).
	 */
	public DOMDocTypeFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	/**
	 * Formats a DOCTYPE declaration: indentation, parameter spacing, internal
	 * subset, and closing bracket.
	 *
	 * <p>For XML documents, the DOCTYPE is indented relative to its parent.
	 * For standalone DTD files, only the child declarations are formatted.</p>
	 *
	 * <p>Use case (XML with DOCTYPE):</p>
	 * <pre>
	 *   &lt;!DOCTYPE  note  SYSTEM  "note.dtd" &gt;
	 * →
	 * &lt;!DOCTYPE note SYSTEM "note.dtd"&gt;
	 * </pre>
	 *
	 * <p>Use case (internal subset closing):</p>
	 * <pre>
	 * &lt;!DOCTYPE person [
	 *   &lt;!ENTITY AUTHOR "John Doe"&gt;  ]  &gt;
	 * →
	 * &lt;!DOCTYPE person [
	 *   &lt;!ENTITY AUTHOR "John Doe"&gt;
	 * ]&gt;
	 * </pre>
	 *
	 * @param docType           the DOCTYPE node to format.
	 * @param parentConstraints the parent's formatting constraints.
	 * @param start             the start offset of the formatting range.
	 * @param end               the end offset of the formatting range.
	 * @param edits             the list of text edits to populate.
	 */
	public void formatDocType(DOMDocumentType docType, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		boolean isDTD = docType.getOwnerDocument().isDTD();
		if (isDTD) {
			formatDTD(docType, parentConstraints, start, end, edits);
		} else {
			replaceLeftSpacesWithIndentation(parentConstraints.getIndentLevel(), docType.getParentNode().getStart(),
					docType.getStart(), true, edits);
			List<DTDDeclParameter> parameters = docType.getParameters();
			if (!parameters.isEmpty()) {
				for (DTDDeclParameter parameter : parameters) {
					replaceLeftSpacesWithOneSpace(docType.getStart(), parameter.getStart(), edits);
					if (docType.isInternalSubset(parameter)) {
						// level + 1 since the 'level' value is the doctype tag's level
						XMLFormattingConstraints constraints = new XMLFormattingConstraints();
						constraints.copyConstraints(parentConstraints);
						constraints.setIndentLevel(constraints.getIndentLevel() + 1);
						formatDTD(docType, constraints, start, end, edits);
					}
				}
				if (getEnforceQuoteStyle() == EnforceQuoteStyle.preferred) {
					int quoteStart = getDocTypeIdStart(docType);
					int quoteEnd = getDocTypeIdEnd(docType);

					if (quoteStart != -1 && quoteEnd != -1) {
						// replace current quote with preferred quote in the case:
						// <!DOCTYPE note SYSTEM "note.dtd">
						formatterDocument.replaceQuoteWithPreferred(quoteStart, quoteStart + 1, edits);
						formatterDocument.replaceQuoteWithPreferred(quoteEnd - 1, quoteEnd, edits);
					}
				}
			}
		}
		DTDDeclParameter internalSubset = docType.getInternalSubsetNode();
		if (internalSubset == null) {
			if (docType.isClosed()) {
				// Remove space between content and end bracket in case of no internal subset
				// Example: <!DOCTYPE note SYSTEM "note.dtd"|>
				int startDocType = docType.getStart();
				int endDocType = docType.getEnd() - 1;
				removeLeftSpaces(startDocType, endDocType, edits);
			}
		} else {
			// Add new line at end of internal subset
			// <!DOCTYPE person [...
			// <!ENTITY AUTHOR \"John Doe\">|]>
			int startDocType = internalSubset.getStart();
			int endDocType = internalSubset.getEnd() - 1;
			String lineDelimiter = formatterDocument.getLineDelimiter();
			replaceLeftSpacesWith(startDocType, endDocType, lineDelimiter, edits);
			// Remove space between end brackets
			// Exmaple Before: <!DOCTYPE person [...
			// <!ENTITY AUTHOR \"John Doe\">]|>
			removeLeftSpaces(internalSubset.getEnd(), docType.getEnd() - 1, edits);
		}
	}

	/**
	 * Formats DTD child declarations (ELEMENT, ENTITY, ATTLIST, NOTATION).
	 *
	 * <p>Iterates over the DOCTYPE's children and formats each DTD declaration
	 * node with proper indentation and parameter spacing.</p>
	 *
	 * <p>Use case:</p>
	 * <pre>
	 *     &lt;!ELEMENT   person  (name, age)&gt;
	 *     &lt;!ENTITY   AUTHOR   "John Doe"&gt;
	 * →
	 *   &lt;!ELEMENT person (name, age)&gt;
	 *   &lt;!ENTITY AUTHOR "John Doe"&gt;
	 * </pre>
	 *
	 * @param docType           the DOCTYPE node whose children to format.
	 * @param parentConstraints the formatting constraints for indentation.
	 * @param start             the start offset of the formatting range.
	 * @param end               the end offset of the formatting range.
	 * @param edits             the list of text edits to populate.
	 */
	private void formatDTD(DOMDocumentType docType, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		boolean addLineSeparator = !docType.getOwnerDocument().isDTD();
		for (DOMNode child : docType.children()) {
			switch (child.getNodeType()) {

			case DOMNode.DTD_ELEMENT_DECL_NODE:
			case DOMNode.DTD_ATT_LIST_NODE:
			case Node.ENTITY_NODE:
			case DOMNode.DTD_NOTATION_DECL:
				// Format DTD node declaration, for example:
				// <!ENTITY AUTHOR "John Doe">
				DTDDeclNode nodeDecl = (DTDDeclNode) child;
				formatDTDNodeDecl(nodeDecl, parentConstraints, addLineSeparator, edits);
				addLineSeparator = true;
				break;

			default:
				// unknown, so just leave alone for now but make sure to update
				// available line width
				formatterDocument.format(child, parentConstraints, start, end, edits);
				if (isMaxLineWidthSupported()){
					int width = updateLineWidthWithLastLine(child, parentConstraints.getAvailableLineWidth());
					parentConstraints.setAvailableLineWidth(width);
				}
			}
		}
	}

	/** Delegates to {@link XMLFormatterDocument#updateLineWidthWithLastLine}. */
	private int updateLineWidthWithLastLine(DOMNode child, int availableLineWidth) {
		return formatterDocument.updateLineWidthWithLastLine(child, availableLineWidth);
	}

	/**
	 * Formats a single DTD declaration node (ELEMENT, ENTITY, ATTLIST, or NOTATION).
	 *
	 * <p>Two formatting operations:</p>
	 * <ol>
	 * <li><b>Indentation</b> — the declaration is indented to the correct level,
	 * respecting {@code preservedNewlines} to keep blank lines between declarations.</li>
	 * <li><b>Parameter spacing</b> — extra spaces between parameters are collapsed
	 * to a single space. For ATTLIST with multiple attributes, internal declarations
	 * are indented on separate lines.</li>
	 * </ol>
	 *
	 * <p>Use case (ENTITY):</p>
	 * <pre>
	 * &lt;!ENTITY   AUTHOR   "John Doe"&gt;
	 * →
	 * &lt;!ENTITY AUTHOR "John Doe"&gt;
	 * </pre>
	 *
	 * <p>Use case (ATTLIST with internal declarations):</p>
	 * <pre>
	 * &lt;!ATTLIST payment type (check|cash) "cash"
	 *   amount CDATA #REQUIRED&gt;
	 * </pre>
	 *
	 * @param nodeDecl          the DTD declaration node to format.
	 * @param parentConstraints the formatting constraints for indentation.
	 * @param addLineSeparator  whether to add a line separator before the declaration.
	 * @param edits             the list of text edits to populate.
	 */
	private void formatDTDNodeDecl(DTDDeclNode nodeDecl, XMLFormattingConstraints parentConstraints,
			boolean addLineSeparator, List<TextEdit> edits) {
		// 1) indent the DTD element, entity, notation declaration
		// before formatting : [space][space]<!ELEMENT>
		// after formatting : <!ELEMENT>
		int parentNodeStart = nodeDecl.getParentNode().getStart();
		int nodeDeclStart = nodeDecl.getStart();
		int indentLevel = parentConstraints.getIndentLevel();
		int preservedNewLines = getPreservedNewlines();
		int currentNewLineCount = XMLFormatterDocument.getExistingNewLineCount(formatterDocument.getTextSequence(),
				nodeDeclStart, formatterDocument.getLineDelimiter());
		if (currentNewLineCount > preservedNewLines) {
			// Reduce to number of new lines to the new line number specified by
			// preservedNewLines setting
			// Example: preservedNewLines = 1
			// <a> \r\n
			// \r\n
			// \r\n --> remove this line, leaving one remaining new line
			// </a>
			replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, parentNodeStart,
					nodeDeclStart, preservedNewLines + 1, edits);
		} else {
			// Maintain the current number of new lines or add line separator where there is
			// no new line present
			int newLineCount = currentNewLineCount == 0 ? 1 : currentNewLineCount;
			replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, parentNodeStart, nodeDeclStart,
					newLineCount, edits);
		}

		// 2 separate each parameters with one space
		// before formatting : <!ELEMENT[space][space]note>
		// after formatting : <!ELEMENT[space]note>
		DTDAttlistDecl attlist = nodeDecl.isDTDAttListDecl() ? (DTDAttlistDecl) nodeDecl : null;
		if (attlist != null) {
			indentLevel = nodeDecl.getOwnerDocument().isDTD() ? 1 : 2;
			List<DTDAttlistDecl> internalDecls = attlist.getInternalChildren();
			if (internalDecls == null) {
				int previousOffset = attlist.getStart();
				for (DTDDeclParameter parameter : attlist.getParameters()) {
					// Normalize space at the start of parameter to a single space for ATTLIST, for
					// example:
					// <!ATTLIST |E |WIDTH |CDATA |"0">
					replaceLeftSpacesWithOneSpace(previousOffset, parameter.getStart(), edits);
					// replace current quote with preferred quote in the case:
					// <!ATTLIST E WIDTH CDATA "0">
					replaceQuoteWithPreferred(nodeDecl, parameter, edits);
					previousOffset = parameter.getEnd();
				}
			} else {
				boolean multipleInternalAttlistDecls = false;
				List<DTDDeclParameter> params = attlist.getParameters();
				DTDDeclParameter parameter;
				int previousOffset = attlist.getStart();
				for (int i = 0; i < params.size(); i++) {
					parameter = params.get(i);
					if (attlist.getNameParameter().equals(parameter)) {
						replaceLeftSpacesWithOneSpace(previousOffset, parameter.getStart(), edits);
						if (attlist.getParameters().size() > 1) { // has parameters after elementName
							multipleInternalAttlistDecls = true;
						}
					} else {
						if (multipleInternalAttlistDecls && i == 1) {
							replaceLeftSpacesWithIndentation(indentLevel, previousOffset, parameter.getStart(), true,
									edits);
						} else {
							replaceLeftSpacesWithOneSpace(previousOffset, parameter.getStart(), edits);
						}
					}
					previousOffset = parameter.getEnd();
				}

				for (DTDAttlistDecl attlistDecl : internalDecls) {
					params = attlistDecl.getParameters();
					previousOffset = attlistDecl.getStart();
					for (int i = 0; i < params.size(); i++) {
						parameter = params.get(i);
						if (i == 0) {
							replaceLeftSpacesWithIndentation(indentLevel, previousOffset, parameter.getStart(), true,
									edits);
						} else {
							replaceLeftSpacesWithOneSpace(previousOffset, parameter.getStart(), edits);
						}
						previousOffset = parameter.getEnd();
					}
				}
			}
		} else {
			List<DTDDeclParameter> parameters = nodeDecl.getParameters();
			if (!parameters.isEmpty()) {
				int previousOffset = nodeDeclStart;
				for (DTDDeclParameter parameter : parameters) {
					// Normalize space at the start of parameter to a single space for non-ATTLIST,
					// for example:
					// <!ENTITY |AUTHOR |"John Doe">
					replaceLeftSpacesWithOneSpace(previousOffset, parameter.getStart(), edits);
					// replace current quote with preferred quote in the case:
					// <!ENTITY AUTHOR "John Doe">
					replaceQuoteWithPreferred(nodeDecl, parameter, edits);
					previousOffset = parameter.getEnd();
				}
			}
		}
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWith}. */
	private void replaceLeftSpacesWith(int from, int to, String replacement, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(from, to, replacement, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithOneSpace}. */
	private void replaceLeftSpacesWithOneSpace(int from, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithOneSpace(from, to, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationWithMultiNewLines}. */
	private int replaceLeftSpacesWithIndentationWithMultiNewLines(int indentLevel, int from, int to, int newLineCount,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, from, to, newLineCount,
				edits);
	}

	/** Delegates to {@link XMLFormatterDocument#removeLeftSpaces}. */
	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	/** Returns the configured quote enforcement style. */
	private EnforceQuoteStyle getEnforceQuoteStyle() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getEnforceQuoteStyle();
	}

	/** Returns the maximum number of blank lines to preserve between declarations. */
	private int getPreservedNewlines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getPreservedNewlines();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

	/**
	 * Returns the start offset of the DOCTYPE's public or system ID value,
	 * for quote enforcement.
	 * Use case: {@code <!DOCTYPE note SYSTEM "note.dtd">} → offset of first {@code "}.
	 *
	 * @param docType the DOCTYPE node.
	 * @return the start offset, or -1 if no ID is present.
	 */
	private static int getDocTypeIdStart(DOMDocumentType docType) {
		if (docType.getPublicIdNode() != null) {
			return docType.getPublicIdNode().getStart();
		} else if (docType.getSystemIdNode() != null) {
			return docType.getSystemIdNode().getStart();
		} else
			return -1;
	}

	/**
	 * Returns the end offset of the DOCTYPE's public or system ID value,
	 * for quote enforcement.
	 *
	 * @param docType the DOCTYPE node.
	 * @return the end offset, or -1 if no ID is present.
	 */
	private static int getDocTypeIdEnd(DOMDocumentType docType) {
		if (docType.getPublicIdNode() != null) {
			return docType.getPublicIdNode().getEnd();
		} else if (docType.getSystemIdNode() != null) {
			return docType.getSystemIdNode().getEnd();
		} else
			return -1;
	}

	/**
	 * Replaces quotes around a DTD parameter value with the user's preferred
	 * quote style, if the {@code enforceQuoteStyle} setting is set to
	 * {@code preferred}.
	 *
	 * <p>Use case: with preferred quote {@code '}, {@code "John Doe"} → {@code 'John Doe'}</p>
	 *
	 * @param nodeDecl  the DTD declaration containing the parameter.
	 * @param parameter the parameter whose quotes to replace.
	 * @param edits     the list of text edits to populate.
	 */
	private void replaceQuoteWithPreferred(DTDDeclNode nodeDecl, DTDDeclParameter parameter, List<TextEdit> edits) {
		int paramStart = parameter.getStart();
		int paramEnd = parameter.getEnd();
		if (StringUtils.isQuote(nodeDecl.getOwnerDocument().getTextSequence().charAt(paramStart))
				&& StringUtils.isQuote(nodeDecl.getOwnerDocument().getTextSequence().charAt(paramEnd - 1))) {
			if (getEnforceQuoteStyle() == EnforceQuoteStyle.preferred) {
				formatterDocument.replaceQuoteWithPreferred(paramStart, paramStart + 1, edits);
				formatterDocument.replaceQuoteWithPreferred(paramEnd - 1, paramEnd, edits);

			}
		}
	}
}
