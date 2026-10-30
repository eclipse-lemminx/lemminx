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
 * Returns a list of {@link BindableCatalogEntry} objects.
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
				BindableCatalogEntry bindable = createBindableEntry(catalogURI, localName, element, entry);
				if (bindable != null) {
					result.add(bindable);
				}
			} else if (NEXT_CATALOG_ENTRY.equals(localName) && entry instanceof CatalogCatalogEntry) {
				String resolvedCatalogURI = CatalogUtils.getResolvedLocation(document, entry);
				if (resolvedCatalogURI != null) {
					collectEntries(resolvedCatalogURI, result, visited);
				}
			}
		}
	}

	private static BindableCatalogEntry createBindableEntry(String catalogURI, String entryType, DOMElement element,
			CatalogEntry entry) {
		String identifier = getIdentifier(entryType, element);
		if (identifier == null || identifier.isEmpty()) {
			return null;
		}
		String targetURI = entry.getResolvedURI();
		String uriAttrValue = element.getAttribute("uri");
		String grammarType = detectGrammarType(uriAttrValue != null ? uriAttrValue : targetURI);
		return new BindableCatalogEntry(catalogURI, entryType, identifier, targetURI, uriAttrValue, grammarType);
	}

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

	private static String detectGrammarType(String uri) {
		if (uri == null) {
			return null;
		}
		String lower = uri.toLowerCase();
		if (lower.endsWith(".xsd")) {
			return "xsd";
		} else if (lower.endsWith(".dtd")) {
			return "dtd";
		} else if (lower.endsWith(".rng")) {
			return "rng";
		} else if (lower.endsWith(".rnc")) {
			return "rnc";
		}
		return null;
	}
}
