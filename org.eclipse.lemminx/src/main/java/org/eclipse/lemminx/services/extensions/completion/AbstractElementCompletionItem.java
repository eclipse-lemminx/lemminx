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
package org.eclipse.lemminx.services.extensions.completion;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonObject;

/**
 * Abstract class of {@link CompletionItem} to report an XML element completion
 * item. This class returns a {@link TextEdit} which contains the full element
 * content (ex : <foo></foo>) or only the start tag element (ex : <foo) if it
 * exists an orphan end tag (ex : </foo>).
 * 
 * When it exists an orphan end tag, an aditional text edit is reported to
 * update this end tag with the start tag of the completion item.
 * 
 * // Samples with completion from Text node
 * 
 * // fo| ==> <foo attr=""></foo>
 * // fo| </foo> ==> <foo attr="">
 * 
 * // Samples with completion from element
 * 
 * // <fo| ==> foo attr=""></foo>
 * // <fo| </foo> ==> foo attr="">
 * // <fo|> ==> foo
 * // <fo|/> ==> foo
 * // <fo| attr="" ==> foo
 *
 * @param <S> the source element Java type.
 * @param <G> the generator Java type.
 */
public abstract class AbstractElementCompletionItem<S, G> extends CompletionItem {

	public static final String UPDATE_END_TAG_NAME_FIELD = "updateEndTagName";

	private final transient String tagName;

	private final transient S sourceElement;

	private final transient G generator;
	private final transient ICompletionRequest request;

	public AbstractElementCompletionItem(String tagName, S sourceElement, G generator,
			ICompletionRequest request) {
		this.tagName = tagName;
		this.sourceElement = sourceElement;
		this.generator = generator;
		this.request = request;
		super.setLabel(tagName);
		super.setSortText(tagName);
		super.setFilterText(request.getFilterForStartTagName(tagName));
		super.setKind(CompletionItemKind.Property);
		// Update TextEdit
		boolean generateFullElement = updateTextEdit();
		boolean updateEndTagName = !generateFullElement && !request.getSharedSettings().isLinkedEditingEnabled();
		// Update AdditionalTextEdits to update end tag name
		boolean resolveAdditionalTextEditsSupported = updateAdditionalTextEdits(updateEndTagName);
		// Update documentation
		boolean resolveDocumentationSupported = updateDocumentation();
		if (resolveDocumentationSupported || resolveAdditionalTextEditsSupported) {
			// resolve documentation, additionalTextEdits if participant exists
			String resolverParticipantId = getResolverParticipantId();
			if (resolverParticipantId != null) {
				JsonObject data = addResolveData(request, resolverParticipantId);
				data.addProperty(UPDATE_END_TAG_NAME_FIELD, updateEndTagName);
			}
		}
	}

	/**
	 * Returns the tag name of the XML element completion item (ex : 'foo',
	 * 'x:foo').
	 * 
	 * @return the tag name of the XML element completion item (ex : 'foo',
	 *         'x:foo').
	 */
	protected String getTagName() {
		return tagName;
	}

	/**
	 * Returns the source element (ex : a DOM element, a grammar element
	 * declaration, etc) used to generate the completion item.
	 * 
	 * @return the source element (ex : a DOM element, a grammar element
	 *         declaration, etc) used to generate the completion item.
	 */
	protected S getSourceElement() {
		return sourceElement;
	}

	/**
	 * Returns the generator to generate the full XML element content and null
	 * otherwise.
	 * 
	 * @return the generator to generate the full XML element content and null
	 *         otherwise.
	 */
	protected G getGenerator() {
		return generator;
	}

	/**
	 * Returns the completion request.
	 * 
	 * @return the completion request.
	 */
	protected ICompletionRequest getRequest() {
		return request;
	}

	/**
	 * Update text edit with the XML element content.
	 */
	private boolean updateTextEdit() {
		DOMNode node = request.getNode();
		int offset = request.getOffset();
		boolean hasOrphanEndTag = node.getOrphanEndElement(offset, tagName, true) != null;
		boolean isTextNode = isTextNode(node, offset);
		boolean shouldGenerateFullElement = isTextNode || isElementClosed((DOMElement) node);
		if (!hasOrphanEndTag && shouldGenerateFullElement) {
			// generate full element (ex : <foo attr="" ></foo>)
			String xml = generateFullElementContent(!hasOrphanEndTag && request.isAutoCloseTags());
			super.setTextEdit(Either.forLeft(new TextEdit(request.getReplaceRange(), xml)));
			super.setInsertTextFormat(InsertTextFormat.Snippet);
			return true;
		} else {
			// generate only start tag element (ex : <foo>)
			DOMElement element = node.isElement() ? (DOMElement) node : null;
			StringBuilder insertText = new StringBuilder(isTextNode ? "<" : "");
			insertText.append(tagName);
			if (request.isCompletionSnippetsSupported()) {
				SnippetsBuilder.tabstops(0, insertText);
				super.setInsertTextFormat(InsertTextFormat.Snippet);
			}
			// <fo|o></bar> ==> foo
			boolean shouldStartTagClosed = isTextNode || element == null
					|| !hasContentAfterTagName(element);
			if (shouldStartTagClosed) {
				insertText.append('>');
			}
			super.setTextEdit(Either
					.forLeft(new TextEdit(
							isTextNode ? request.getReplaceRange() : request.getReplaceRangeForTagName(),
							insertText.toString())));
			return false;
		}
	}

	protected static boolean hasContentAfterTagName(DOMElement element) {
		return (element.hasAttributes() && element.getAttributeAtIndex(0).hasDelimiter())
				|| element.isStartTagClosed();
	}

	/**
	 * Update documentation if needed.
	 * 
	 * @return true if client can support completion resolve of 'documentation' and
	 *         false otherwise.
	 */
	private boolean updateDocumentation() {
		// TODO : implement resolve of element documentation
		// if (request.isResolveDocumentationSupported()) {
		// return true;
		// }
		MarkupContent documentation = generateDocumentation();
		super.setDocumentation(documentation);
		return false;
	}

	/**
	 * Update additional text edits if an orphan end tag should be updated.
	 * 
	 * @param updateEndTagName
	 * 
	 * @return true if client can support completion resolve of
	 *         'additionalTextEdits' and
	 *         false otherwise.
	 */
	private boolean updateAdditionalTextEdits(boolean updateEndTagName) {
		if (request.isResolveAdditionalTextEditsSupported()) {
			return true;
		}
		if (updateEndTagName) {
			updateEndTagName(request.getNode(), request.getOffset(), request, tagName,
					this);
		}
		return false;
	}

	public static boolean isElementClosed(DOMElement element) {
		if (element.hasEndTag() || element.isSelfClosed()) {
			// <fo|o></foo>
			// <fo|o />
			return false;
		}
		// <fo| </foo>
		return true;
	}

	private static boolean isTextNode(DOMNode node, int offset) {
		if (!node.isElement()) {
			// fo| </foo>
			return true;
		}
		DOMElement element = (DOMElement) node;
		if (element.getEnd() <= offset) {
			// When the cursor is at the exact end of an incomplete start tag
			// (e.g., <f| or <|), the cursor is still inside the start tag context,
			// not in a text node context. The '<' is already present in the document.
			if (element.getEnd() == offset && element.hasStartTag() && !element.isStartTagClosed() && !element.isSelfClosed()) {
				return false;
			}
			// <foo></foo> |
			return true;
		}
		if ((element.isStartTagClosed() && offset > element.getStartTagCloseOffset())
				|| element.isInInsideStartEndTag(offset)) {
			// <foo>
			// |
			// </foo>
			return true;
		}
		return false;
	}

	/**
	 * Update completion item data for resolve support.
	 * 
	 * @param request       the completion request.
	 * @param participantId the resolve participant id.
	 * @return
	 */
	protected JsonObject addResolveData(ICompletionRequest request, String participantId) {
		JsonObject data = DataEntryField.createCompletionData(request, participantId);
		super.setData(data);
		return data;
	}

	/**
	 * Returns the full element content (ex : <foo attr=""></foo>).
	 * 
	 * @param generateEndTag true if the element end tag must be generated and
	 *                       false otherwise.
	 * 
	 * @return the full element content (ex : <foo attr=""></foo>).
	 */
	protected abstract String generateFullElementContent(boolean generateEndTag);

	/**
	 * Returns the completion item documentation and null otherwise.
	 * 
	 * @return the completion item documentation and null otherwise.
	 */
	protected abstract MarkupContent generateDocumentation();

	public static void updateEndTagName(DOMNode node, int offset, ISharedSettingsRequest request, String tagName,
			CompletionItem completionItem) {
		List<TextEdit> additionalTextEdits = getAdditionalTextEdits(node, offset,
				tagName, request);
		if (additionalTextEdits != null) {
			completionItem.setAdditionalTextEdits(additionalTextEdits);
		}
	}

	private static List<TextEdit> getAdditionalTextEdits(DOMNode node, int offset, String tagName,
			ISharedSettingsRequest request) {
		DOMElement endElement = null;
		if (isTextNode(node, offset)) {
			endElement = node.getOrphanEndElement(offset, tagName, true);
		} else {
			DOMElement element = (DOMElement) node;
			int endElementOffset = node.getEnd() - 1;
			endElement = element.hasEndTag() ? element : element.getOrphanEndElement(endElementOffset, tagName, true);
			if (endElement == null && !element.hasEndTag()) {
				// The orphan end tag might be a sibling rather than a child
				// (e.g., <f|</buz> where </buz> is a sibling of <f>).
				// Search using the cursor offset which reaches beyond the element.
				endElement = element.getOrphanEndElement(offset, tagName, true);
			}
		}
		if (endElement != null && !tagName.equals(endElement.getTagName())) {
			Range range = XMLPositionUtility.selectEndTagName(endElement);
			return Arrays.asList(new TextEdit(range, tagName));
		}
		return null;
	}

	/**
	 * Returns the resolver participant id.
	 * 
	 * @return the resolver participant id.
	 */
	protected String getResolverParticipantId() {
		return ElementEndTagCompletionResolver.PARTICIPANT_ID;
	}

}
