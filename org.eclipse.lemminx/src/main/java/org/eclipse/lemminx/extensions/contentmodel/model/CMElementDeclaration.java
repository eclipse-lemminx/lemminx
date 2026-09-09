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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;

/**
 * Content model element which abstracts element declaration from a given
 * grammar (XML Schema, DTD).
 */
public interface CMElementDeclaration {

	public static final Collection<CMElementDeclaration> ANY_ELEMENT_DECLARATIONS = Collections
			.unmodifiableCollection(Arrays.asList());

	/**
	 * Returns the declared element local name.
	 * 
	 * @return the declared element local name.
	 */
	String getLocalName();

	/**
	 * Returns the target namespace and null otherwise.
	 * 
	 * @return the target namespace and null otherwise.
	 */
	String getNamespace();

	/**
	 * Returns the xmlns prefix from the given namespace URI and null otherwise.
	 *
	 * @param namespaceURI the namespace
	 * @return the xmlns prefix from the given namespace URI and null otherwise.
	 */
	String getPrefix(String namespaceURI);

	/**
	 * Returns the declared element name with the given prefix.
	 * 
	 * @return the declared element name with the given prefix.
	 */
	default String getName(String prefix) {
		String name = getLocalName();
		if (prefix == null || prefix.isEmpty()) {
			return name;
		}
		return prefix + ":" + name;
	}

	/**
	 * Returns the attributes of this declared element.
	 *
	 * @return the attributes element of this declared element.
	 */
	Collection<CMAttributeDeclaration> getAttributes();

	/**
	 * Returns the possible attributes for the given parent element, taking into
	 * account already-specified attributes to narrow choices (e.g. in RelaxNG
	 * choice groups).
	 *
	 * @param parentElement the DOM element being edited.
	 * @return the possible attribute declarations.
	 */
	default Collection<CMAttributeDeclaration> getPossibleAttributes(DOMElement parentElement) {
		return getAttributes();
	}

	/**
	 * Returns the children declared element of this declared element.
	 * 
	 * @return the children declared element of this declared element.
	 */
	Collection<CMElementDeclaration> getElements();

	/**
	 * Returns the possible declared elements at the given offset of the given
	 * parent element.
	 * 
	 * @param parentElement the parent element
	 * @param offset        the offset
	 * @return the possible declared elements at the given offset of the given
	 *         parent element.
	 */
	Collection<CMElementDeclaration> getPossibleElements(DOMElement parentElement, int offset);

	/**
	 * Returns the declared element which matches the given XML tag name / namespace
	 * and null otherwise.
	 * 
	 * @param tag
	 * @param namespace
	 * @return the declared element which matches the given XML tag name / namespace
	 *         and null otherwise.
	 */
	CMElementDeclaration findCMElement(String tag, String namespace);

	/**
	 * Returns the declared attribute which match the given name and null otherwise.
	 * 
	 * @param attribute the DOM attribute
	 * @return the declared attribute which match the given name and null otherwise.
	 */
	default CMAttributeDeclaration findCMAttribute(DOMAttr attr) {
		return findCMAttribute(attr.getLocalName(), attr.getNamespaceURI());
	}

	/**
	 * Returns the declared attribute which match the given name and null otherwise.
	 * 
	 * @param attributeName the attribute name.
	 * @param namespace     the attribute namespace and null otherwise.
	 * @return the declared attribute which match the given name and null otherwise.
	 */
	CMAttributeDeclaration findCMAttribute(String attributeName, String namespace);

	/**
	 * Returns formatted documentation of the declared element, according to
	 * settings defined in <code>request</code>.
	 * 
	 * @param request the request containing settings
	 * @return formatted documentation of the declared element, according to
	 *         settings defined in <code>request</code>.
	 */
	String getDocumentation(ISharedSettingsRequest request);

	/**
	 * Returns true if the element cannot contains element children or text content
	 * and false otherwise.
	 * 
	 * @return true if the element cannot contains element children or text content
	 *         and false otherwise.
	 */
	boolean isEmpty();

	/**
	 * Returns true if the element can have an explicit null value assigned to it.
	 * 
	 * @return true if the element can have an explicit null value assigned to it.
	 */
	boolean isNillable();

	/**
	 * Return the enumeration values.
	 * 
	 * @return the enumeration values.
	 */
	Collection<String> getEnumerationValues();

	/**
	 * Returns the documentation for the given enumeration value and null otherwise.
	 * 
	 * @param value   the enumeration value.
	 * @param request the shared settings.
	 * 
	 * @return the documentation for the given enumeration value and null otherwise.
	 */
	String getTextDocumentation(String value, ISharedSettingsRequest request);

	/**
	 * Returns the owner document URI where the element is declared.
	 * 
	 * @return the owner document URI where the element is declared.
	 */
	String getDocumentURI();

	/**
	 * Returns true if the element is a string type (ex : xs:string) and false
	 * otherwise.
	 * 
	 * @return true if the element is a string type (ex : xs:string) and false
	 *         otherwise.
	 */
	boolean isStringType();

	/**
	 * Returns true if the element can contains text and element both and false
	 * otherwise.
	 * 
	 * @return true if the element can contains text and element both and false
	 *         otherwise.
	 */
	boolean isMixedContent();

	/**
	 * Returns true if the child element name is optional and false otherwise.
	 * 
	 * @return true if the child element name is optional and false otherwise.
	 */
	boolean isOptional(String childElementName);

	/**
	 * Returns a list of required/non-optional child elements of the current
	 * element.
	 *
	 * @return a list of required/non-optional child elements of the current
	 *         element.
	 */
	Collection<CMElementDeclaration> getRequiredElements();

	/**
	 * Returns the local names of types derived from this element's declared type.
	 * Used to generate xsi:type snippet choices when the element's type is abstract
	 * and cannot be instantiated directly.
	 *
	 * @return the collection of derived type local names, or empty if the type is
	 *         not abstract or has no derived types.
	 */
	default Collection<String> getDerivedTypeNames() {
		return Collections.emptyList();
	}
}
