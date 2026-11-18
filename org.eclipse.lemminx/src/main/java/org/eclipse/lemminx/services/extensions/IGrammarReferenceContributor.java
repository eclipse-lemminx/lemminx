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
package org.eclipse.lemminx.services.extensions;

/**
 * Extension point that allows XML plugins to contribute grammar reference
 * attribute patterns to the workspace index.
 *
 * <p>Implement this interface to tell the index scanner which XML attributes
 * in your domain point to grammar files. For example, the OASIS XML Catalog
 * extension contributes a pattern for {@code uri="..."} on catalog entries.
 *
 * <p>Register your implementation in
 * {@link IXMLExtension#start} via
 * {@link XMLExtensionsRegistry#registerGrammarReferenceContributor}.
 *
 * <p>All registered contributors are consulted by the index scanner
 * ({@code XMLReferenceCollector} and {@code GrammarReferenceLocator}) after
 * the built-in patterns (schemaLocation, noNamespaceSchemaLocation, xml-model
 * href, xs:include/import schemaLocation, DOCTYPE SYSTEM) are checked.
 *
 * @see XMLExtensionsRegistry#registerGrammarReferenceContributor
 */
public interface IGrammarReferenceContributor {

	/**
	 * How to extract file paths from the attribute value.
	 */
	enum ExtractionKind {
		/** The whole attribute value is a single file path. */
		PATH,
		/** The value is whitespace-separated namespace–path pairs; odd-index tokens are paths. */
		SCHEMA_LOCATION_PAIRS
	}

	/**
	 * Returns how the attribute value should be interpreted, or {@code null} if
	 * this contributor does not handle the given attribute.
	 *
	 * <p>All string arguments are already the <em>local</em> part of the name
	 * (prefix stripped). {@code hasPrefix} is {@code true} when the original
	 * attribute name contained a colon.
	 *
	 * @param elementLocalName local name of the containing element.
	 * @param attrLocalName    local name of the attribute being examined.
	 * @param hasPrefix        {@code true} if the attribute has a namespace prefix.
	 * @return extraction kind, or {@code null} to skip this attribute.
	 */
	ExtractionKind classify(String elementLocalName, String attrLocalName, boolean hasPrefix);
}
