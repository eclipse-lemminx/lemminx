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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogCatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.catalog.entries.bindable" to list all bindable catalog
 * entries across all registered catalogs.
 *
 * <p>
 * Traverses each registered XML catalog (including {@code nextCatalog}
 * references), collecting {@code <public>}, {@code <system>}, and
 * {@code <uri>} entries. Each entry is returned as a
 * {@link BindableCatalogEntry} with its resolved target URI and detected
 * grammar type (xsd, dtd, rng, rnc).
 * </p>
 *
 * <p>
 * Use case: the vscode-xml extension calls this command to populate the
 * catalog binding quick pick, letting the user choose which catalog entry
 * to associate with the current XML file.
 * </p>
 */
public class ListBindableCatalogEntriesCommand implements IDelegateCommandHandler {

	private static final Logger LOGGER = Logger.getLogger(ListBindableCatalogEntriesCommand.class.getName());

	public static final String COMMAND_ID = "xml.catalog.entries.bindable";

	private static final String PUBLIC_ENTRY = "public";
	private static final String SYSTEM_ENTRY = "system";
	private static final String URI_ENTRY = "uri";
	private static final String NEXT_CATALOG_ENTRY = "nextCatalog";

	private final ContentModelManager contentModelManager;

	public ListBindableCatalogEntriesCommand(ContentModelManager contentModelManager) {
		this.contentModelManager = contentModelManager;
	}

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String[] catalogs = contentModelManager.getCatalogs();
		if (catalogs == null || catalogs.length == 0) {
			return new ArrayList<>();
		}
		List<BindableCatalogEntry> result = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		for (String catalogURI : catalogs) {
			cancelChecker.checkCanceled();
			collectEntries(catalogURI, result, visited);
		}
		return result;
	}

	private void collectEntries(String catalogURI, List<BindableCatalogEntry> result, Set<String> visited) {
		if (catalogURI == null || !visited.add(catalogURI)) {
			return;
		}
		DOMDocument document;
		try {
			document = DOMUtils.loadDocument(catalogURI, null);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to load catalog: " + catalogURI, e);
			return;
		}
		if (document == null || !DOMUtils.isCatalog(document)) {
			return;
		}
		collectEntries(catalogURI, document, result, visited);
	}

	void collectEntries(String catalogURI, DOMDocument document, List<BindableCatalogEntry> result,
			Set<String> visited) {
		List<CatalogEntry> entries = CatalogUtils.getCatalogEntries(document);
		for (CatalogEntry entry : entries) {
			DOMElement element = entry.getEntryElement();
			if (element == null) {
				continue;
			}
			String localName = element.getLocalName();
			if (localName == null) {
				continue;
			}
			if (PUBLIC_ENTRY.equals(localName) || SYSTEM_ENTRY.equals(localName) || URI_ENTRY.equals(localName)) {
				// <public publicId="..." uri="note.dtd" /> -> BindableCatalogEntry(type=public, id=publicId, ...)
				// <system systemId="..." uri="note.xsd" /> -> BindableCatalogEntry(type=system, id=systemId, ...)
				// <uri name="..." uri="tag.xsd" />         -> BindableCatalogEntry(type=uri, id=name, ...)
				BindableCatalogEntry bindable = createBindableEntry(document, catalogURI, localName, element, entry);
				if (bindable != null) {
					result.add(bindable);
				}
			} else if (NEXT_CATALOG_ENTRY.equals(localName) && entry instanceof CatalogCatalogEntry) {
				// <nextCatalog catalog="other-catalog.xml" /> -> recurse into referenced catalog
				String resolvedCatalogURI = CatalogUtils.getResolvedLocation(document, entry);
				if (resolvedCatalogURI != null) {
					collectEntries(resolvedCatalogURI, result, visited);
				}
			}
		}
	}

	/**
	 * Creates a {@link BindableCatalogEntry} from a catalog entry element.
	 *
	 * <p>
	 * Returns {@code null} for entries pointing to non-grammar files (e.g.
	 * {@code .xml}, {@code .html}) since they cannot be used for schema binding.
	 * </p>
	 *
	 * <p>
	 * For the target URI resolution:
	 * </p>
	 * <ul>
	 * <li>If the {@code uri} attribute is an absolute URI (contains {@code ://}),
	 * it is used directly as the target URI. This avoids corruption of HTTP/HTTPS
	 * URIs by {@code Paths.get} + {@code XMLEntityManager.expandSystemId} which
	 * would strip the double slash (e.g. {@code http://host/book.xsd} becomes
	 * {@code http:/host/book.xsd}).</li>
	 * <li>Otherwise, the URI is resolved relative to the catalog document via
	 * {@link CatalogUtils#getResolvedLocation}.</li>
	 * </ul>
	 */
	private static BindableCatalogEntry createBindableEntry(DOMDocument document, String catalogURI, String entryType,
			DOMElement element, CatalogEntry entry) {
		String identifier = getIdentifier(entryType, element);
		if (identifier == null || identifier.isEmpty()) {
			return null;
		}
		String uriAttrValue = element.getAttribute("uri");
		String targetURI;
		if (uriAttrValue != null && uriAttrValue.contains("://")) {
			targetURI = uriAttrValue;
		} else {
			targetURI = CatalogUtils.getResolvedLocation(document, entry);
		}
		String grammarType = detectGrammarType(uriAttrValue != null ? uriAttrValue : targetURI);
		// Skip non-grammar files (e.g. .xml, .html) that can't be bound as schemas
		if (grammarType == null) {
			return null;
		}
		return new BindableCatalogEntry(catalogURI, entryType, identifier, targetURI, uriAttrValue, grammarType);
	}

	/**
	 * Returns the identifier attribute value for the given catalog entry type:
	 * {@code publicId} for "public", {@code systemId} for "system", {@code name}
	 * for "uri".
	 */
	private static String getIdentifier(String entryType, DOMElement element) {
		switch (entryType) {
			case PUBLIC_ENTRY:
				return element.getAttribute("publicId");
			case SYSTEM_ENTRY:
				return element.getAttribute("systemId");
			case URI_ENTRY:
				return element.getAttribute("name");
			default:
				return null;
		}
	}

	/**
	 * Detects the grammar type from a URI based on its file extension.
	 * Recognizes {@code .xsd}, {@code .dtd}/{@code .ent}/{@code .mod},
	 * {@code .rng}, and {@code .rnc}. Returns {@code null} for unknown
	 * extensions.
	 */
	private static String detectGrammarType(String uri) {
		if (uri == null) {
			return null;
		}
		String lower = uri.toLowerCase();
		if (lower.endsWith(".xsd")) {
			return "xsd";
		} else if (lower.endsWith(".dtd") || lower.endsWith(".ent") || lower.endsWith(".mod")) {
			return "dtd";
		} else if (lower.endsWith(".rng")) {
			return "rng";
		} else if (lower.endsWith(".rnc")) {
			return "rnc";
		}
		return null;
	}
}
