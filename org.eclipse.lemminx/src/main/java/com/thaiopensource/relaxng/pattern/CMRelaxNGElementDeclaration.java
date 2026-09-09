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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
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

	private Name overrideName;

	private Collection<CMElementDeclaration> elements;

	private Collection<CMAttributeDeclaration> attributes;

	private Set<String> requiredElementNames;

	private Set<String> possibleRequiredElementNames;

	private DOMElement domElement;

	CMRelaxNGElementDeclaration(CMRelaxNGDocument document, ElementPattern pattern) {
		this.cmDocument = document;
		this.pattern = pattern;
	}

	void setOverrideName(Name overrideName) {
		this.overrideName = overrideName;
	}

	public ElementPattern getPattern() {
		return pattern;
	}

	@Override
	public String getLocalName() {
		return getJingName().getLocalName();
	}

	@Override
	public String getNamespace() {
		return getJingName().getNamespaceUri();
	}

	@Override
	public String getPrefix(String namespaceURI) {
		DOMElement element = getDOMElement();
		if (element != null) {
			return element.getPrefix(namespaceURI);
		}
		return null;
	}

	private Name getJingName() {
		if (overrideName != null) {
			return overrideName;
		}
		NameClass nameClass = pattern.getNameClass();
		if (nameClass instanceof SimpleNameClass) {
			return ((SimpleNameClass) nameClass).getName();
		}
		return null;
	}

	public DOMElement getDOMElement() {
		if (domElement != null) {
			return domElement;
		}
		DOMNode node = cmDocument.findNodeAt(pattern.getLocator());
		if (node != null && node.isElement()) {
			domElement = (DOMElement) node;
		}
		return domElement;
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
			for (DOMAttr attr : parentElement.attributes()) {
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
			if (possible != null) {
				possibleElements.add(possible);
			}
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
		return !getRequiredElementNames().contains(childElementName);
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

	@Override
	public Set<CMElementDeclaration> getRequiredElements() {
		Set<CMElementDeclaration> requiredElements = new LinkedHashSet<>();
		for (String elementName : getRequiredElementNames()) {
			requiredElements.add(findCMElement(elementName, null));
		}
		return requiredElements;
	}

	private Set<String> getRequiredElementNames() {
		if (requiredElementNames == null) {
			requiredElementNames = new LinkedHashSet<>();
			MyRequiredElementsFunction elementsFunction = new MyRequiredElementsFunction(requiredElementNames);
			getPattern().getContent().apply(elementsFunction);
		}
		return requiredElementNames;
	}

	public Collection<CMElementDeclaration> getPossibleRequiredElements() {
		Set<CMElementDeclaration> possibelRequiredElements = new LinkedHashSet<>();
		if (possibleRequiredElementNames == null) {
			possibleRequiredElementNames = new LinkedHashSet<>();
			PossibleRequiredElementsFunction elementsFunction = new PossibleRequiredElementsFunction(
					possibleRequiredElementNames);
			getPattern().getContent().apply(elementsFunction);
		}
		for (String elementName : possibleRequiredElementNames) {
			possibelRequiredElements.add(findCMElement(elementName, null));
		}

		List<CMElementDeclaration> sortedElements = possibelRequiredElements.stream().collect(Collectors.toList());
		Collections.sort(sortedElements, (e1, e2) -> {
			return ((CMElementDeclaration) e1).getLocalName().compareTo(((CMElementDeclaration) e2).getLocalName());
		});
		return sortedElements;
	}
}
