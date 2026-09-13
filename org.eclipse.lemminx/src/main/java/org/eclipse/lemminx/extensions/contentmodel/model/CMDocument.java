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
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.LocationLink;
import org.w3c.dom.Entity;

/**
 * Content model document which abstracts element declaration from a given
 * grammar (XML Schema, DTD).
 */
public interface CMDocument {

	/**
	 * Returns the target namespace of this grammar document, or {@code null} if the
	 * grammar has no target namespace (e.g., a no-namespace XSD or a DTD).
	 *
	 * @return the target namespace URI, or {@code null}.
	 */
	default String getNamespace() {
		return null;
	}

	/**
	 * Returns true if the model document defines the given namespace and false
	 * otherwise.
	 *
	 * @param namespaceURI
	 * @return true if the model document defines the given namespace and false
	 *         otherwise.
	 */
	boolean hasNamespace(String namespaceURI);

	/**
	 * Returns the elements declaration of the model document root.
	 * 
	 * @return the elements declaration of the model document root.
	 */
	Collection<CMElementDeclaration> getElements();

	/**
	 * Returns the declared element which matches the given XML element and null
	 * otherwise.
	 * 
	 * @param element the XML element
	 * @return the declared element which matches the given XML element and null
	 *         otherwise.
	 */
	default CMElementDeclaration findCMElement(DOMElement element) {
		return findCMElement(element, element.getNamespaceURI());
	}

	/**
	 * Returns the declared element which matches the given XML element and null
	 * otherwise.
	 * 
	 * @param element   the XML element
	 * @param namespace the given namespace
	 * @return the declared element which matches the given XML element and null
	 *         otherwise.
	 */
	CMElementDeclaration findCMElement(DOMElement element, String namespace);

	default CMAttributeDeclaration findCMAttribute(DOMAttr attr) {
		CMElementDeclaration elementDeclaration = findCMElement(attr.getOwnerElement());
		return elementDeclaration != null ? elementDeclaration.findCMAttribute(attr) : null;
	}

	/**
	 * Returns the location of the type definition of the given node.
	 * 
	 * @param node the node
	 * @return the location of the type definition of the given node.
	 */
	LocationLink findTypeLocation(DOMNode node);

	/**
	 * Returns true if the content model document is dirty and false otherwise.
	 * 
	 * @return true if the content model document is dirty and false otherwise.
	 */
	boolean isDirty();

	/**
	 * Returns the names of types that can be used as xsi:type values for the given
	 * element (i.e. derived types of the element's declared type).
	 *
	 * @param element the DOM element
	 * @return the collection of qualified type names.
	 */
	default Collection<String> findDerivedTypeNames(DOMElement element) {
		return Collections.emptyList();
	}

	/**
	 * Returns the child element declarations of the xsi:type-derived type
	 * for the given element. Used when the derived type is defined in this
	 * schema but the element itself belongs to a different namespace.
	 *
	 * @param element the DOM element with xsi:type attribute.
	 * @return the child element declarations, or empty if not applicable.
	 */
	default Collection<CMElementDeclaration> findXsiTypeDerivedElements(DOMElement element) {
		return Collections.emptyList();
	}

	/**
	 * Returns list of declared entities.
	 *
	 * @return list of declared entities.
	 */
	default List<Entity> getEntities() {
		return Collections.emptyList();
	}
}
