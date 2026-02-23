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

import static org.eclipse.lemminx.utils.DOMUtils.findFirstChildElementByTagName;
import static org.eclipse.lemminx.utils.DOMUtils.isDOMElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.FilesChangedTracker;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.xml.sax.Locator;

/**
 * RelaxNG content document implementation.
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
public class CMRelaxNGDocument implements CMDocument {

	private static final String NAME_ATTR = "name";

	private static final String DOCUMENTATION_ELT = "documentation";

	private static final String CHOICE_ELT = "choice";

	private static final String VALUE_ELT = "value";

	private Collection<CMElementDeclaration> elements;
	private final Pattern start;
	private final URIResolverExtensionManager resolverExtensionManager;
	private final FilesChangedTracker tracker;

	private final Map<String, DOMDocument> documents;

	private final Map<ElementPattern, CMRelaxNGElementDeclaration> elementMappings;

	public CMRelaxNGDocument(String relaxNGURI, Pattern start, URIResolverExtensionManager resolverExtensionManager) {
		this.start = start;
		this.resolverExtensionManager = resolverExtensionManager;
		if (URIUtils.isFileResource(relaxNGURI)) {
			tracker = new FilesChangedTracker();
			tracker.addFileURI(relaxNGURI);
		} else {
			tracker = null;
		}
		this.documents = new HashMap<>();
		this.elementMappings = new HashMap<>();
	}

	@Override
	public boolean hasNamespace(String namespaceURI) {
		return false;
	}

	@Override
	public Collection<CMElementDeclaration> getElements() {
		if (elements == null) {
			elements = new CMRelaxNGElementDeclarationCollector(this, start).getElements();
		}
		return elements;
	}

	CMRelaxNGElementDeclaration getPatternElement(ElementPattern elementDeclaration) {
		CMRelaxNGElementDeclaration element = elementMappings.get(elementDeclaration);
		if (element == null) {
			element = new CMRelaxNGElementDeclaration(this, elementDeclaration);
			elementMappings.put(elementDeclaration, element);
		}
		return element;
	}

	@Override
	public CMElementDeclaration findCMElement(DOMElement element, String namespace) {
		List<DOMElement> paths = new ArrayList<>();
		while (element != null && (namespace == null || namespace.equals(element.getNamespaceURI()))) {
			paths.add(0, element);
			element = element.getParentNode() instanceof DOMElement ? (DOMElement) element.getParentNode() : null;
		}
		CMElementDeclaration declaration = null;
		for (int i = 0; i < paths.size(); i++) {
			DOMElement elt = paths.get(i);
			if (i == 0) {
				declaration = findElementDeclaration(elt.getLocalName(), namespace);
			} else {
				declaration = declaration != null ? declaration.findCMElement(elt.getLocalName(), namespace) : null;
			}
			if (declaration == null) {
				break;
			}
		}
		return declaration;
	}

	private CMElementDeclaration findElementDeclaration(String tag, String namespace) {
		for (CMElementDeclaration cmElement : getElements()) {
			if (cmElement.getLocalName().equals(tag)) {
				return cmElement;
			}
		}
		return null;
	}

	@Override
	public LocationLink findTypeLocation(DOMNode originNode) {
		DOMElement originElement = null;
		DOMAttr originAttribute = null;
		if (originNode.isElement()) {
			originElement = (DOMElement) originNode;
		} else if (originNode.isAttribute()) {
			originAttribute = (DOMAttr) originNode;
			originElement = originAttribute.getOwnerElement();
		}
		if (originElement == null || originElement.getLocalName() == null) {
			return null;
		}
		// Try to retrieve RelaxNG element declaration from the given element.
		CMRelaxNGElementDeclaration elementDeclaration = (CMRelaxNGElementDeclaration) findCMElement(originElement,
				originElement.getNamespaceURI());
		if (elementDeclaration == null) {
			return null;
		}

		CMRelaxNGAttributeDeclaration attributeDeclaration = null;
		if (originAttribute != null) {
			attributeDeclaration = (CMRelaxNGAttributeDeclaration) elementDeclaration.findCMAttribute(originAttribute);
			if (attributeDeclaration == null) {
				return null;
			}
		}

		Locator locator = attributeDeclaration != null ? attributeDeclaration.getPattern().getLocator()
				: elementDeclaration.getPattern().getLocator();
		if (locator == null) {
			return null;
		}

		String systemId = locator.getSystemId();
		if (!URIUtils.isFileResource(systemId)) {
			return null;
		}

		DOMRange range = DOMUtils.isRelaxNGUriCompactSyntax(systemId) ? null
				: getDeclaredTypeRange(originNode, locator);
		if (range != null) {
			return XMLPositionUtility.createLocationLink(originNode,
					range);
		}
		Range originRange = XMLPositionUtility.createSelectionRange(originNode);
		Range targetRange = new Range(createValidPosition(locator.getLineNumber() - 1, locator.getColumnNumber() - 1),
				createValidPosition(locator.getLineNumber() - 1, locator.getColumnNumber()));
		return new LocationLink(systemId, targetRange, targetRange, originRange);
	}

	private static Position createValidPosition(int line, int character) {
		return new Position(Math.max(line, 0), Math.max(character, 0));
	}

	private DOMRange getDeclaredTypeRange(DOMNode originNode, Locator locator) {
		DOMNode node = findNodeAt(locator);
		if (node == null) {
			return null;
		}
		if (node.isElement()) {
			DOMElement element = (DOMElement) node;
			DOMAttr name = element.getAttributeNode(NAME_ATTR);
			if (name != null && originNode.getLocalName().equals(name.getValue())) {
				return name;
			}
		}
		return null;
	}

	public DOMDocument getDocument(String systemId) {
		DOMDocument document = documents.get(systemId);
		if (document != null) {
			return document;
		}
		return getSynchDocument(systemId);
	}

	private synchronized DOMDocument getSynchDocument(String systemId) {
		DOMDocument document = documents.get(systemId);
		if (document != null) {
			return document;
		}
		document = DOMUtils.loadDocument(systemId,
				resolverExtensionManager);
		documents.put(systemId, document);
		return document;
	}

	@Override
	public boolean isDirty() {
		return tracker != null ? tracker.isDirty() : false;
	}

	public String getDocumentation(Locator locator) {
		return getDocumentation(locator, null);
	}

	public String getDocumentation(Locator locator, String value) {
		DOMNode node = findNodeAt(locator);
		if (node == null) {
			return null;
		}
		if (node.isElement()) {
			// <element name="TEI"
			// <attribute name="cert"
			DOMElement documentation = getDocumentationElement((DOMElement) node, value);
			if (documentation == null) {
				return null;
			}
			return getTextContent(documentation);
		}
		return null;
	}

	private DOMElement getDocumentationElement(DOMElement element, String value) {
		if (value == null) {
			// <attribute name="level">
			// <a:documentation
			// xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0">indicates ...
			return findFirstChildElementByTagName(element, DOCUMENTATION_ELT);
		}
		// <attribute name="level">
		// <a:documentation
		// xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0">indicates ...
		// <choice>
		// <value>a</value>
		// <a:documentation
		// xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0">(analytic) ...
		// <value>m</value>
		DOMElement choice = findFirstChildElementByTagName(element, CHOICE_ELT);
		if (choice == null) {
			return null;
		}
		int length = choice.getChildren().size();
		for (int i = 0; i < length; i++) {
			DOMNode child = choice.getChild(i);
			if (isDOMElement(child, VALUE_ELT)) {
				// <value>a</value>
				DOMElement valueElement = (DOMElement) child;
				if (i < length - 1 && value.equals(getTextContent(valueElement))) {
					DOMNode next = choice.getChild(i + 1);
					if (isDOMElement(next, DOCUMENTATION_ELT)) {
						return (DOMElement) next;
					}
					return null;
				}
			}
		}
		return null;
	}

	DOMNode findNodeAt(Locator locator) {
		if (locator == null) {
			return null;
		}
		String systemId = locator.getSystemId();
		if (!URIUtils.isFileResource(systemId)) {
			return null;
		}
		DOMDocument targetSchema = getDocument(systemId);
		if (targetSchema == null) {
			return null;
		}

		// <define name="TEI">
		// <element name="TEI">...</element>|

		try {
			int offset = targetSchema
					.offsetAt(new Position(locator.getLineNumber() - 1, locator.getColumnNumber() - 1));
			return targetSchema.findNodeAt(offset);
		} catch (BadLocationException e) {
		}
		return null;
	}

	private static String getTextContent(DOMElement element) {
		int start = element.getStartTagCloseOffset() + 1;
		int end = element.getEndTagOpenOffset();
		return element.getOwnerDocument().getTextSequence().subSequence(start, end).toString();
	}
}
