/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.thaiopensource.relaxng.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thaiopensource.relaxng.sax.Context;
import com.thaiopensource.xml.util.Name;

/**
 * RelaxNG content element implementation.
 * 
 * <p>
 * NOTE : this class is hosted in 'com.thaiopensource.relaxng.pattern' because
 * {@link Pattern} implementation like {@link ElementPattern} are not public.
 * Once https://github.com/relaxng/jing-trang/issues/271 will be fixed we could
 * move this class in 'org.eclipse.lemminx.extensions.relaxng.contentmodel'
 * package.
 * </p>
 * 
 * @author Angelo ZERR
 *
 */
public class CMRelaxNGElementDeclaration implements CMElementDeclaration {

	private final CMRelaxNGDocument cmDocument;

	private final ElementPattern pattern;

	private Collection<CMElementDeclaration> elements;

	private Collection<CMAttributeDeclaration> attributes;

	private Set<Name> requiredElementNames;

	CMRelaxNGElementDeclaration(CMRelaxNGDocument document, ElementPattern pattern) {
		this.cmDocument = document;
		this.pattern = pattern;
	}

	public ElementPattern getPattern() {
		return pattern;
	}

	@Override
	public String getName() {
		return getJingName().getLocalName();
	}

	@Override
	public String getNamespace() {
		return getJingName().getNamespaceUri();
	}

	private Name getJingName() {
		NameClass nameClass = pattern.getNameClass();
		if (nameClass instanceof SimpleNameClass) {
			return ((SimpleNameClass) nameClass).getName();
		}
		return null;
	}

	@Override
	public Collection<CMAttributeDeclaration> getAttributes() {
		if (attributes == null) {
			attributes = new CMRelaxNGAttributeDeclarationCollector(this, pattern.getContent()).getAttributes();
		}
		return attributes;
	}

	@Override
	public Collection<CMElementDeclaration> getElements() {
		if (elements == null) {
			elements = new CMRelaxNGElementDeclarationCollector(cmDocument, pattern.getContent()).getElements();
		}
		return elements;
	}

	@Override
	public Collection<CMElementDeclaration> getPossibleElements(DOMElement parentElement, int offset) {
		PatternMatcher matcher = new PatternMatcher(pattern, new ValidatorPatternBuilder(new SchemaPatternBuilder()));
		matcher.matchStartDocument();
		Context context = new Context();
		Name n = createName(parentElement);
		matcher.matchStartTagOpen(n, n.getLocalName(), context);
		if (parentElement.hasAttributes()) {
			List<DOMAttr> attributes = parentElement.getAttributeNodes();
			for (DOMAttr attr : attributes) {
				Name a = createName(attr);
				matcher.matchAttributeName(a, a.getLocalName(), context);
				matcher.matchAttributeValue(attr.getValue(), a, a.getLocalName(), context);
			}
		}
		matcher.matchStartTagClose(n, n.getLocalName(), context);

		List<Name> names = toNames(parentElement, offset);
		for (Name name : names) {
			matcher.matchStartTagOpen(name, name.getLocalName(), context);
			matcher.matchEndTag(name, name.getLocalName(), context);
		}

		com.thaiopensource.relaxng.match.NameClass nc = matcher.possibleStartTagNames();
		Set<Name> allowed = nc.getIncludedNames();
		List<CMElementDeclaration> possibleElements = new ArrayList<>();
		for (Name name : allowed) {
			CMElementDeclaration possible = findCMElement(name.getLocalName(), name.getNamespaceUri());
			possibleElements.add(possible);
		}
		return possibleElements;
	}

	private static List<Name> toNames(DOMElement parentElement, int offset) {
		if (parentElement == null || !parentElement.hasChildNodes()) {
			return Collections.emptyList();
		}
		List<Name> qNames = new ArrayList<>();
		NodeList children = parentElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				// child node is an element
				DOMElement element = (DOMElement) child;
				if (element.getEnd() > offset) {
					// child element is after the given offset, stop the computing
					break;
				}
				if (!element.isClosed()) {
					// Element is not closed, ignore it
					continue;
				}
				qNames.add(createName(element));
			}
		}
		return qNames;
	}

	private static Name createName(Element tag) {
		String namespace = tag.getNamespaceURI();
		return new Name(namespace == null ? "" : namespace, tag.getLocalName());
	}

	private Name createName(DOMAttr attr) {
		return new Name("", attr.getLocalName());
	}

	@Override
	public CMElementDeclaration findCMElement(String tag, String namespace) {
		for (CMElementDeclaration cmElement : getElements()) {
			if (cmElement.getName().equals(tag)) {
				return cmElement;
			}
		}
		return null;
	}

	@Override
	public CMAttributeDeclaration findCMAttribute(String attributeName, String namespace) {
		for (CMAttributeDeclaration cmAttribute : getAttributes()) {
			if (cmAttribute.getName().equals(attributeName)) {
				return cmAttribute;
			}
		}
		return null;
	}

	@Override
	public String getDocumentation(ISharedSettingsRequest request) {
		return cmDocument.getDocumentation(pattern.getLocator());
	}

	@Override
	public boolean isEmpty() {
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

	@Override
	public String getDocumentURI() {
		return pattern.getLocator().getSystemId();
	}

	@Override
	public boolean isStringType() {
		return false;
	}

	@Override
	public boolean isMixedContent() {
		return false;
	}

	@Override
	public boolean isOptional(String childElementName) {
		if (requiredElementNames == null) {
			RequiredElementsFunction elementsFunction = new RequiredElementsFunction();
			requiredElementNames = pattern.apply(elementsFunction);
			for (Name name : requiredElementNames) {
				if (childElementName.equals(name.getLocalName())) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean isNillable() {
		return false;
	}

	void addElement(CMRelaxNGElementDeclaration element) {
		if (elements == null) {
			elements = new ArrayList<>();
		}
		elements.add(element);
	}

	public CMRelaxNGDocument getCMDocument() {
		return cmDocument;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("<element");
		result.append(" name=\"");
		result.append(getJingName().getLocalName());
		result.append("\"");
		result.append(" namespaceUri=\"");
		result.append(getJingName().getNamespaceUri());
		result.append("\"");
		result.append(" />");
		return result.toString();
	}
}
