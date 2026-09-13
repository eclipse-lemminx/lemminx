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
package org.eclipse.lemminx.extensions.contentmodel.model;

import java.util.Collection;
import java.util.Map;

import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.utils.StringUtils;

/**
 * Content model element which abstracts attribute declaration from a given
 * grammar (XML Schema, DTD, RelaxNG).
 */
public interface CMAttributeDeclaration {

	String getPrefix();
	
	/**
	 * Returns the declared attribute local name.
	 * 
	 * @return the declared attribute local name.
	 */
	String getLocalName();

	/**
	 * Returns the target namespace and null otherwise.
	 * 
	 * @return the target namespace and null otherwise.
	 */
	String getNamespace();

	/**
	 * Returns the owner element declaration.
	 * 
	 * @return the owner element declaration.
	 */
	CMElementDeclaration getOwnerElementDeclaration();

	/**
	 * Returns the declared attribute name with the given prefix.
	 * 
	 * @return the declared attribute name with the given prefix.
	 */
	default String getName(String prefix) {
		String name = getLocalName();
		if (prefix == null || prefix.isEmpty()) {
			return name;
		}
		return prefix + ":" + name;
	}

	/**
	 * Returns the declared attribute name with the proper prefix mapped with the
	 * attribute namespace and the local name otherwise.
	 * 
	 * @param prefixes map which contains namespace as key and prefix as value.
	 * 
	 * @return the declared attribute name with the proper prefix mapped with the
	 *         attribute namespace and the local name otherwise.
	 */
	default String getName(Map<String /* namespaceURI */, String /* prefix */> prefixes) {
		String namespace = getNamespace();
		String prefix = StringUtils.isEmpty(namespace) ? null : (prefixes != null ? prefixes.get(namespace) : null);
		return getName(prefix);
	}

	/**
	 * Returns the explicit default value of the declared attribute (from schema
	 * {@code default} or {@code fixed} constraints) and null otherwise. Does NOT
	 * include type-inferred defaults.
	 *
	 * @return the explicit default value of the declared attribute and null
	 *         otherwise.
	 */
	String getDefaultValue();

	/**
	 * Returns a type-aware default value suitable for document generation.
	 * For XSD attributes, this returns type-inferred defaults (e.g., "0" for
	 * xs:decimal) when no explicit default exists. For other grammars, defaults
	 * to {@link #getDefaultValue()}.
	 *
	 * @return the type-aware default value, or null.
	 */
	default String getTypeAwareDefaultValue() {
		return getDefaultValue();
	}

	/**
	 * Returns enumeration values of the declared attribute and empty collection
	 * otherwise.
	 * 
	 * @return enumeration values of the declared attribute and empty collection
	 *         otherwise.
	 */
	Collection<String> getEnumerationValues();

	/**
	 * Returns formatted documentation of the declared attribute according to
	 * settings defined in <code>request</code>
	 * 
	 * @param request the request that contains settings
	 * @return formatted documentation of the declared attribute according to
	 *         settings defined in <code>request</code>
	 */
	String getAttributeNameDocumentation(ISharedSettingsRequest request);

	/**
	 * Returns formatted documentation about <code>value</code>, according to
	 * settings defined in <code>request</code>
	 * 
	 * @param value   the attribute value to find documentation for
	 * @param request the request containing settings
	 * @return formatted documentation about <code>value</code>, according to
	 *         settings defined in <code>request</code>
	 */
	String getAttributeValueDocumentation(String value, ISharedSettingsRequest request);

	/**
	 * Returns true if the attribute is required and false otherwise.
	 * 
	 * @return true if the attribute is required and false otherwise.
	 */
	boolean isRequired();
}
