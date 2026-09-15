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
package org.eclipse.lemminx.extensions.dtd.contentmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.impl.dtd.XMLElementDecl;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.dtd.contentmodel.CMDTDDocument.DTDElementInfo;
import org.eclipse.lemminx.extensions.dtd.contentmodel.CMDTDDocument.DTDNodeInfo;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;

/**
 * DTD element declaration.
 *
 */
public class CMDTDElementDeclaration extends XMLElementDecl implements CMElementDeclaration {

	private final int index;
	private final CMDTDDocument document;
	private List<CMElementDeclaration> elements;
	private List<CMAttributeDeclaration> attributes;
	private String documentation;

	public CMDTDElementDeclaration(CMDTDDocument document, int index) {
		this.document = document;
		this.index = index;
	}

	@Override
	public String getLocalName() {
		return super.name.localpart;
	}

	@Override
	public String getNamespace() {
		return null;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		return null;
	}

	@Override
	public Collection<CMAttributeDeclaration> getAttributes() {
		if (attributes == null) {
			attributes = new ArrayList<>();
			document.collectAttributesDeclaration(this, attributes);
		}
		return attributes;
	}

	@Override
	public Collection<CMElementDeclaration> getElements() {
		if (elements == null) {
			elements = new ArrayList<>();
			document.collectElementsDeclaration(getLocalName(), elements);
		}
		return elements;
	}

	@Override
	public Collection<CMElementDeclaration> getPossibleElements(DOMElement parentElement, int offset) {
		// TODO: support valid element declaration for DTD
		return getElements();
	}

	@Override
	public CMElementDeclaration findCMElement(String tag, String namespace) {
		for (CMElementDeclaration cmElement : getElements()) {
			if (cmElement.getLocalName().equals(tag)) {
				return cmElement;
			}
		}
		return null;
	}

	@Override
	public CMAttributeDeclaration findCMAttribute(String attributeName, String namespace) {
		for (CMAttributeDeclaration cmAttribute : getAttributes()) {
			if (cmAttribute.getLocalName().equals(attributeName)) {
				return cmAttribute;
			}
		}
		return null;
	}

	@Override
	public String getDocumentation(ISharedSettingsRequest settings) {
		if (documentation != null) {
			return documentation;
		}
		Map<String, DTDElementInfo> hierarchiesMap = document.getHierarchiesMap();
		if (hierarchiesMap != null) {
			DTDElementInfo dtdElementInfo = hierarchiesMap.get(getLocalName());
			documentation = dtdElementInfo.getComment();
		}
		return documentation;
	}

	public String getDocumentation(String attrName) {
		Map<String, DTDElementInfo> hierarchiesMap = document.getHierarchiesMap();
		if (hierarchiesMap != null) {
			DTDElementInfo dtdElementInfo = hierarchiesMap.get(getLocalName());
			Map<String, DTDNodeInfo> attributesMap = dtdElementInfo.getAttributes();
			DTDNodeInfo nodeInfo = attributesMap.get(attrName);
			if (nodeInfo != null) {
				documentation = nodeInfo.getComment();
			}
		}
		return documentation;
	}

	@Override
	public boolean isEmpty() {
		return super.type == XMLElementDecl.TYPE_EMPTY;
	}

	@Override
	public boolean isNillable() {
		// implement later, return false
		return false;
	}

	@Override
	public Collection<String> getEnumerationValues() {
		return Collections.emptyList();
	}

	@Override
	public String getTextDocumentation(String value, ISharedSettingsRequest request) {
		return null;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public String getDocumentURI() {
		return document.getURI();
	}

	@Override
	public boolean isStringType() {
		return false;
	}

	@Override
	public boolean isMixedContent() {
		return super.type == XMLElementDecl.TYPE_MIXED;
	}

	@Override
	public boolean isOptional(String childElementName) {
		// implement later, return false
		return false;
	}

	@Override
	public Set<CMElementDeclaration> getRequiredElements() {
		Set<CMElementDeclaration> requiredElements = new LinkedHashSet<>();
		for (CMElementDeclaration element : getElements()) {
			// cannot be used, isOptional is not implemented
			if (!isOptional(element.getLocalName())) {
				requiredElements.add(element);
			}
		}
		return requiredElements;
	}
}
