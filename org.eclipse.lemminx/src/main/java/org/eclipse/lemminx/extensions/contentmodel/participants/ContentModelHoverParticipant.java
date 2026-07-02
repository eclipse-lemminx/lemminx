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
package org.eclipse.lemminx.extensions.contentmodel.participants;

import static org.eclipse.lemminx.utils.MarkupContentFactory.createHover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.utils.XMLGenerator;
import org.eclipse.lemminx.extensions.xsi.XSISchemaModel;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.services.extensions.hover.HoverParticipantAdapter;
import org.eclipse.lemminx.services.extensions.hover.IHoverRequest;
import org.eclipse.lemminx.uriresolver.CacheResourceDownloadingException;
import org.eclipse.lemminx.utils.MarkupContentFactory;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Extension to support XML hover based on content model (XML Schema
 * documentation, etc)
 */
public class ContentModelHoverParticipant extends HoverParticipantAdapter {

	@Override
	public Hover onTag(IHoverRequest hoverRequest, CancelChecker cancelChecker) throws Exception {
		try {
			ContentModelManager contentModelManager = hoverRequest.getComponent(ContentModelManager.class);
			DOMElement element = (DOMElement) hoverRequest.getNode();
			Collection<CMDocument> cmDocuments = collectCMDocuments(element, contentModelManager);
			if (cmDocuments.isEmpty()) {
				// no bound grammar -> no documentation
				return null;
			}
			// Compute element declaration documentation from bound grammars
			List<MarkupContent> contentValues = new ArrayList<>();
			for (CMDocument cmDocument : cmDocuments) {
				CMElementDeclaration cmElement = cmDocument.findCMElement(element);
				if (cmElement != null) {
					MarkupContent content = XMLGenerator.createMarkupContent(cmElement, hoverRequest);
					fillHoverContent(content, contentValues);
				}
			}
			return createHover(contentValues);
		} catch (CacheResourceDownloadingException e) {
			return getCacheWarningHover(e, hoverRequest);
		}
	}

	@Override
	public Hover onAttributeName(IHoverRequest hoverRequest, CancelChecker cancelChecker) throws Exception {
		DOMAttr attribute = (DOMAttr) hoverRequest.getNode();
		DOMElement element = attribute.getOwnerElement();
		try {
			ContentModelManager contentModelManager = hoverRequest.getComponent(ContentModelManager.class);
			Collection<CMDocument> cmDocuments = collectCMDocuments(element, contentModelManager);
			if (cmDocuments.isEmpty()) {
				// no bound grammar -> no documentation
				return null;
			}
			// Compute attribute name declaration documentation from bound grammars
			List<MarkupContent> contentValues = new ArrayList<>();
			for (CMDocument cmDocument : cmDocuments) {
				CMElementDeclaration cmElement = cmDocument.findCMElement(element);
				if (cmElement != null) {
					CMAttributeDeclaration cmAttribute = cmElement.findCMAttribute(attribute);
					if (cmAttribute != null) {
						MarkupContent content = XMLGenerator.createMarkupContent(cmAttribute, cmElement, hoverRequest);
						fillHoverContent(content, contentValues);
					}
				}
			}
			return createHover(contentValues);
		} catch (CacheResourceDownloadingException e) {
			return getCacheWarningHover(e, hoverRequest);
		}
	}

	@Override
	public Hover onAttributeValue(IHoverRequest hoverRequest, CancelChecker cancelChecker) throws Exception {
		DOMAttr attribute = (DOMAttr) hoverRequest.getNode();

		// Attempts to compute specifically for XSI related attributes since
		// the XSD itself does not have enough information. Should create a mock XSD
		// eventually.
		Hover temp = XSISchemaModel.computeHoverResponse(attribute, hoverRequest);
		if (temp != null) {
			return temp;
		}

		DOMElement element = attribute.getOwnerElement();
		try {
			ContentModelManager contentModelManager = hoverRequest.getComponent(ContentModelManager.class);
			Collection<CMDocument> cmDocuments = collectCMDocuments(element, contentModelManager);
			if (cmDocuments.isEmpty()) {
				// no bound grammar -> no documentation
				return null;
			}
			String attributeValue = attribute.getValue();
			// Compute attribute value declaration documentation from bound grammars
			List<MarkupContent> contentValues = new ArrayList<>();
			for (CMDocument cmDocument : cmDocuments) {
				CMElementDeclaration cmElement = cmDocument.findCMElement(element);
				if (cmElement != null) {
					CMAttributeDeclaration cmAttribute = cmElement.findCMAttribute(attribute);
					if (cmAttribute != null) {
						MarkupContent content = XMLGenerator.createMarkupContent(cmAttribute, attributeValue, cmElement,
								hoverRequest);
						fillHoverContent(content, contentValues);
					}
				}
			}
			return createHover(contentValues);
		} catch (CacheResourceDownloadingException e) {
			return getCacheWarningHover(e, hoverRequest);
		}
	}

	@Override
	public Hover onText(IHoverRequest hoverRequest, CancelChecker cancelChecker) throws Exception {
		DOMText text = (DOMText) hoverRequest.getNode();
		DOMElement element = text.getParentElement();
		if (element == null) {
			return null;
		}
		try {
			ContentModelManager contentModelManager = hoverRequest.getComponent(ContentModelManager.class);
			Collection<CMDocument> cmDocuments = collectCMDocuments(element, contentModelManager);
			if (cmDocuments.isEmpty()) {
				// no bound grammar -> no documentation
				return null;
			}
			String textContent = text.getTextContent();
			if (textContent != null) {
				textContent = textContent.trim();
			}
			// Compute attribute name declaration documentation from bound grammars
			List<MarkupContent> contentValues = new ArrayList<>();
			for (CMDocument cmDocument : cmDocuments) {
				CMElementDeclaration cmElement = cmDocument.findCMElement(element);
				if (cmElement != null) {
					MarkupContent content = XMLGenerator.createMarkupContent(cmElement, textContent, hoverRequest);
					fillHoverContent(content, contentValues);
				}
			}
			return createHover(contentValues);
		} catch (CacheResourceDownloadingException e) {
			return getCacheWarningHover(e, hoverRequest);
		}
	}

	private static Hover getCacheWarningHover(CacheResourceDownloadingException e, ISharedSettingsRequest support) {
		// Here cache is enabled and some XML Schema, DTD, etc are loading
		MarkupContent content = MarkupContentFactory.createMarkupContent(
				"Cannot process " + (e.isDTD() ? "DTD" : "XML Schema") + " hover: " + e.getMessage(),
				MarkupKind.MARKDOWN, support);
		return new Hover(content);
	}

	private static void fillHoverContent(MarkupContent content, List<MarkupContent> contents) {
		if (content != null && !StringUtils.isEmpty(content.getValue())) {
			contents.add(content);
		}
	}

	/**
	 * Collect the content model documents which may declare the given element. When
	 * the element is inside an ancestor with an xsi:type attribute, the documents
	 * bound to the ancestor's namespace are also collected so that derived types
	 * declared in other schemas can be resolved.
	 *
	 * @param element             the hovered XML element.
	 * @param contentModelManager the content model manager.
	 * @return the content model documents to search.
	 */
	private static Collection<CMDocument> collectCMDocuments(DOMElement element,
			ContentModelManager contentModelManager) {
		Set<CMDocument> documents = new LinkedHashSet<>(contentModelManager.findCMDocument(element));
		DOMElement current = element;
		while (current != null) {
			DOMElement parent = current.getParentNode() instanceof DOMElement ? (DOMElement) current.getParentNode()
					: null;
			if (parent != null && hasXSIType(parent)) {
				documents.addAll(contentModelManager.findCMDocument(parent));
			}
			current = parent;
		}
		return documents;
	}

	private static boolean hasXSIType(DOMElement element) {
		org.w3c.dom.NamedNodeMap attrs = element.getAttributes();
		if (attrs == null) {
			return false;
		}
		for (int i = 0; i < attrs.getLength(); i++) {
			org.w3c.dom.Node attr = attrs.item(i);
			if ("type".equals(attr.getLocalName()) && XSISchemaModel.XSI_WEBSITE.equals(attr.getNamespaceURI())) {
				return true;
			}
		}
		return false;
	}

}
