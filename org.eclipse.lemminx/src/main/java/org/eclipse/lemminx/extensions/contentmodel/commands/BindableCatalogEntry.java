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

/**
 * Represents a catalog entry that can be used to bind an XML document to a
 * grammar. Serialized as JSON for the LSP response.
 */
public class BindableCatalogEntry {

	private final String catalogURI;
	private final String entryType;
	private final String identifier;
	private final String targetURI;
	private final String uriValue;
	private final String grammarType;

	/**
	 * @param catalogURI  which catalog file this entry comes from
	 * @param entryType   "public", "system", or "uri"
	 * @param identifier  the publicId, systemId, or name attribute value
	 * @param targetURI   the resolved URI to the grammar file
	 * @param uriValue    the raw uri attribute value from the catalog entry
	 * @param grammarType "xsd", "dtd", "rng", "rnc", or null if unknown
	 */
	public BindableCatalogEntry(String catalogURI, String entryType, String identifier, String targetURI,
			String uriValue, String grammarType) {
		this.catalogURI = catalogURI;
		this.entryType = entryType;
		this.identifier = identifier;
		this.targetURI = targetURI;
		this.uriValue = uriValue;
		this.grammarType = grammarType;
	}

	public String getCatalogURI() {
		return catalogURI;
	}

	public String getEntryType() {
		return entryType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getTargetURI() {
		return targetURI;
	}

	public String getUriValue() {
		return uriValue;
	}

	public String getGrammarType() {
		return grammarType;
	}
}
