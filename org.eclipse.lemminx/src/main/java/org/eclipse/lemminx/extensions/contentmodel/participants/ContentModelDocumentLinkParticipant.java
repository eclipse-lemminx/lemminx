/**
 *  Copyright (c) 2018-2020 Angelo ZERR
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

import static org.eclipse.lemminx.utils.XMLPositionUtility.createDocumentLink;

import java.util.Collection;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.DTDEntityDecl;
import org.eclipse.lemminx.dom.NoNamespaceSchemaLocation;
import org.eclipse.lemminx.dom.SchemaLocation;
import org.eclipse.lemminx.dom.SchemaLocationHint;
import org.eclipse.lemminx.dom.XMLModel;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.DocumentLink;
import org.w3c.dom.NamedNodeMap;

/**
 * Document link for :
 *
 * <ul>
 * <li>XML Schema xsi:noNamespaceSchemaLocation</li>
 * <li>DTD SYSTEM (ex : <!DOCTYPE root-element SYSTEM "./extended.dtd" )</li>
 * <li>Entity SYSTEM (ex : <!ENTITY % xinclude SYSTEM
 * "http://www.docbook.org/xml/4.4/xinclude.mod"> )</li>
 * <li>XML Schema xsi:schemaLocation</li>
 * </ul>
 *
 * @author Angelo ZERR
 *
 */
public class ContentModelDocumentLinkParticipant implements IDocumentLinkParticipant {

	private final URIResolverExtensionManager resolverManager;

	public ContentModelDocumentLinkParticipant(URIResolverExtensionManager resolverManager) {
		this.resolverManager = resolverManager;
	}

	@Override
	public void findDocumentLinks(DOMDocument document, List<DocumentLink> links) {
		// Document link for DTD
		DOMDocumentType docType = document.getDoctype();
		if (docType != null) {
			// <!DOCTYPE root-element SYSTEM "./extended.dtd"
			String location = resolverManager.resolve(document.getDocumentURI(), docType.getPublicIdWithoutQuotes(),
					docType.getSystemIdWithoutQuotes());
			if (location != null) {
				try {
					DOMRange systemIdRange = docType.getSystemIdNode();
					if (DOMUtils.isNonEmptyRange(systemIdRange, true)) {
						links.add(createDocumentLink(systemIdRange, location, true));
					}
				} catch (BadLocationException e) {
					// Do nothing
				}
			}

			// <!ENTITY % xinclude SYSTEM "http://www.docbook.org/xml/4.4/xinclude.mod">
			NamedNodeMap entities = docType.getEntities();
			for (int i = 0; i < entities.getLength(); i++) {
				DTDEntityDecl entity = (DTDEntityDecl) entities.item(i);
				location = resolverManager.resolve(document.getDocumentURI(), entity.getPublicId(),
						entity.getSystemId());
				if (location != null) {
					try {
						DOMRange systemIdRange = entity.getSystemIdNode();
						if (DOMUtils.isNonEmptyRange(systemIdRange, true)) {
							links.add(createDocumentLink(systemIdRange, location, true));
						}
					} catch (BadLocationException e) {
						// Do nothing
					}
				}
			}
		}

		// Document link for xml-model/href
		List<XMLModel> xmlModels = document.getXMLModels();
		for (XMLModel xmlModel : xmlModels) {
			String location = resolverManager.resolve(document.getDocumentURI(), null, xmlModel.getHref());
			if (location != null) {
				try {
					DOMRange hrefRange = xmlModel.getHrefNode();
					if (DOMUtils.isNonEmptyRange(hrefRange, true)) {
						links.add(createDocumentLink(hrefRange, location, true));
					}
				} catch (BadLocationException e) {
					// Do nothing
				}
			}
		}

		// Document link for xsi:noNamespaceSchemaLocation
		NoNamespaceSchemaLocation noNamespaceSchemaLocation = document.getNoNamespaceSchemaLocation();
		if (noNamespaceSchemaLocation != null) {
			try {
				String location = resolverManager.resolve(document.getDocumentURI(), null,
						noNamespaceSchemaLocation.getLocation());
				if (location != null) {
					DOMRange attrValue = noNamespaceSchemaLocation.getAttr().getNodeAttrValue();
					if (DOMUtils.isNonEmptyRange(attrValue, true)) {
						links.add(createDocumentLink(attrValue, location, true));
					}
				}
			} catch (BadLocationException e) {
				// Do nothing
			}
		}

		// Doc link for xsi:schemaLocation
		SchemaLocation schemaLocation = document.getSchemaLocation();
		if (schemaLocation != null) {
			try {
				Collection<SchemaLocationHint> schemaLocationHints = schemaLocation.getSchemaLocationHints();
				String location;
				for (SchemaLocationHint schemaLocationHint : schemaLocationHints) {
					location = resolverManager.resolve(document.getDocumentURI(), null, schemaLocationHint.getHint());
					if (DOMUtils.isNonEmptyRange(schemaLocationHint, false)) {
						links.add(createDocumentLink(schemaLocationHint, location, false));
					}
				}
			} catch (BadLocationException e) {
				// Do nothing
			}
		}
	}

}
