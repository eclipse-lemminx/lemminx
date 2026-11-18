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
package org.eclipse.lemminx.extensions.catalog.participants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.AbstractTypeDefinitionParticipant;
import org.eclipse.lemminx.services.extensions.ITypeDefinitionRequest;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides "Go to Type Definition" locations for the catalog entries that bind
 * the XML document's namespace to its grammar.
 *
 * <p>When the user triggers type-definition on an XML element whose namespace
 * URI is mapped by a catalog entry ({@code <namespace>} or {@code <uri>}),
 * this participant returns a {@link LocationLink} pointing to that entry in the
 * catalog file, in addition to the XSD type location returned by the content-
 * model participant.
 *
 * <p>Example:
 * <pre>
 * order.xml:    &lt;order xmlns="http://example.org/order"&gt;  ← type definition here
 * catalog.xml:  &lt;namespace name="http://example.org/order" uri="order.xsd"/&gt;  ← navigates here too
 * </pre>
 */
public class XMLCatalogTypeDefinitionParticipant extends AbstractTypeDefinitionParticipant {

	private final ContentModelManager contentModelManager;

	public XMLCatalogTypeDefinitionParticipant(ContentModelManager contentModelManager) {
		this.contentModelManager = contentModelManager;
	}

	@Override
	protected boolean match(DOMDocument document) {
		return true;
	}

	@Override
	protected void doFindTypeDefinition(ITypeDefinitionRequest request, List<LocationLink> locations,
			CancelChecker cancelChecker) {
		DOMNode node = request.getNode();
		if (node == null) {
			return;
		}
		DOMElement element = null;
		if (node.isElement()) {
			element = (DOMElement) node;
		} else if (node.isAttribute()) {
			element = ((DOMAttr) node).getOwnerElement();
		}
		if (element == null) {
			return;
		}
		String namespaceURI = element.getNamespaceURI();
		if (namespaceURI == null || namespaceURI.isEmpty()) {
			return;
		}
		String[] catalogs = contentModelManager.getCatalogs();
		if (catalogs == null || catalogs.length == 0) {
			return;
		}
		for (String catalogUri : catalogs) {
			cancelChecker.checkCanceled();
			findCatalogEntryLocations(catalogUri, namespaceURI, node, locations);
		}
	}

	private void findCatalogEntryLocations(String catalogUri, String namespaceURI, DOMNode originNode,
			List<LocationLink> locations) {
		try {
			Path path = FilesUtils.getPathOrNull(catalogUri);
			if (path == null || !Files.exists(path)) {
				return;
			}
			String content = Files.readString(path);
			DOMDocument catalogDoc = DOMParser.getInstance().parse(content, catalogUri, null);
			for (CatalogEntry entry : CatalogUtils.getCatalogEntries(catalogDoc)) {
				String id = CatalogUtils.getCatalogIdentifier(entry);
				if (namespaceURI.equals(id)) {
					DOMElement entryElement = entry.getEntryElement();
					if (entryElement != null) {
						DOMRange originRange = (originNode instanceof DOMAttr)
								? ((DOMAttr) originNode).getNodeAttrName()
								: originNode;
						Range originSelectionRange = XMLPositionUtility.createSelectionRange(originRange);
						Range targetRange = XMLPositionUtility.createRange(entryElement);
						DOMAttr identifierAttr = CatalogUtils.getCatalogIdentifierAttr(entryElement);
						Range targetSelectionRange = identifierAttr != null
								? XMLPositionUtility.selectAttributeValue(identifierAttr, true)
								: targetRange;
						locations.add(new LocationLink(catalogUri, targetRange, targetSelectionRange, originSelectionRange));
					}
				}
			}
		} catch (IOException e) {
			// skip unreadable catalog files
		}
	}
}
