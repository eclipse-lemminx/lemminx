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
package org.eclipse.lemminx.extensions.xsd.contentmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.SubstitutionGroupHandler;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.models.CMBuilder;
import org.apache.xerces.impl.xs.models.CMNodeFactory;
import org.apache.xerces.impl.xs.models.XSCMValidator;
import org.apache.xerces.impl.xs.util.XSObjectListImpl;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.settings.SchemaDocumentationType;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.MarkupKind;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XSD element declaration implementation.
 *
 */
public class CMXSDElementDeclaration implements CMElementDeclaration {

	private static final short PC_UNKWOWN = -1;

	private final CMXSDDocument document;

	private final XSElementDeclaration elementDeclaration;

	private final XSTypeDefinition typeDefinition;

	private Collection<CMAttributeDeclaration> attributes;

	private Collection<CMElementDeclaration> elements;

	private String documentation;

	private Map<String, String> textsDocumentation;

	private SchemaDocumentationType docStrategy;

	private Map<String, Boolean> elementOptionality;

	private CMXSDElementDeclaration(CMXSDDocument document, XSElementDeclaration elementDeclaration,
								   XSTypeDefinition typeDefinition) {
		this.document = document;
		this.elementDeclaration = elementDeclaration;
		this.typeDefinition = typeDefinition;
	}

	public CMXSDElementDeclaration(CMXSDDocument document, XSElementDeclaration elementDeclaration) {
		this(document, elementDeclaration, elementDeclaration.getTypeDefinition());
	}

	public CMXSDElementDeclaration refineType(XSTypeDefinition refinedType) {
		return new CMXSDElementDeclaration(document, elementDeclaration, refinedType);
	}

	@Override
	public String getLocalName() {
		return elementDeclaration.getName();
	}

	@Override
	public String getNamespace() {
		return elementDeclaration.getNamespace();
	}

	@Override
	public String getPrefix(String namespaceURI) {
		// TODO : implement this method
		return null;
	}

	@Override
	public Collection<CMAttributeDeclaration> getAttributes() {
		if (attributes == null) {
			attributes = new ArrayList<>();
			collectAttributesDeclaration(elementDeclaration, attributes);
		}
		return attributes;
	}

	private void collectAttributesDeclaration(XSElementDeclaration elementDecl,
			Collection<CMAttributeDeclaration> attributes) {
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			// TODO...
			break;
		case XSTypeDefinition.COMPLEX_TYPE:
			collectAttributesDeclaration((XSComplexTypeDefinition) typeDefinition, attributes);
			break;
		}
	}

	private void collectAttributesDeclaration(XSComplexTypeDefinition typeDefinition,
			Collection<CMAttributeDeclaration> attributes) {
		XSObjectList list = typeDefinition.getAttributeUses();
		if (list != null) {
			for (int i = 0; i < list.getLength(); i++) {
				XSObject object = list.item(i);
				if (object.getType() == XSConstants.ATTRIBUTE_USE) {
					XSAttributeUse attributeUse = (XSAttributeUse) object;
					attributes.add(new CMXSDAttributeDeclaration(this, attributeUse));
				}
			}
		}
	}

	@Override
	public Collection<CMElementDeclaration> getElements() {
		if (elements == null) {
			elements = new ArrayList<>();
			collectElementsDeclaration(elementDeclaration, elements);
		}
		return elements;
	}

	@Override
	public Collection<CMElementDeclaration> getPossibleElements(DOMElement parentElement, int offset) {
		if (typeDefinition != null && typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			// The type definition is complex (ex: xs:all; xs:sequence), returns list of
			// element declaration according those XML Schema constraints

			// Initialize Xerces validator
			CMBuilder cmBuilder = new CMBuilder(new CMNodeFactory());
			XSCMValidator validator = ((XSComplexTypeDecl) typeDefinition).getContentModel(cmBuilder);
			if (validator == null) {
				return Collections.emptyList();
			}

			SubstitutionGroupHandler handler = new SubstitutionGroupHandler(document);
			// For xs:all, element order is irrelevant and each element can appear at most
			// once, so all existing siblings must be considered regardless of cursor
			// position. For xs:sequence/xs:choice, only elements before the cursor matter.
			boolean isAllGroup = false;
			XSParticle particle = ((XSComplexTypeDecl) typeDefinition).getParticle();
			if (particle != null) {
				XSTerm term = particle.getTerm();
				if (term instanceof XSModelGroup
						&& ((XSModelGroup) term).getCompositor() == XSModelGroup.COMPOSITOR_ALL) {
					isAllGroup = true;
				}
			}
			// For xs:all, element order is irrelevant and each element can appear at most
			// once, so all existing siblings must be considered regardless of cursor
			// position. For xs:sequence/xs:choice, only elements before the cursor matter.
			int effectiveOffset = isAllGroup ? Integer.MAX_VALUE : offset;
			// Compute list of child element (QName)
			List<QName> qNames = toQNames(parentElement, effectiveOffset);
			// Loop for each element (QName) and check if it is valid according the XML
			// Schema constraint
			int[] states = validator.startContentModel();
			for (QName elementName : qNames) {
				Object decl = validator.oneTransition(elementName, states, handler);
				if (decl == null) {
					if (isAllGroup) {
						// For xs:all, skip invalid/unknown elements (e.g. typos) so that
						// valid siblings are still counted against their maxOccurs.
						continue;
					}
					return Collections.emptyList();
				}
			}

			// At this step, all child elements are valid, the call of
			// XSCMValidator#oneTransition has updated the states flag.
			// Collect the next valid elements according the XML Schema constraints
			Vector<?> result = validator.whatCanGoHere(states);
			if (result.isEmpty()) {
				return Collections.emptyList();
			}

			// Compute list of possible elements
			Collection<CMElementDeclaration> possibleElements = new HashSet<>();
			for (Object object : result) {
				if (object instanceof XSElementDeclaration) {
					XSElementDeclaration elementDecl = (XSElementDeclaration) object;
					document.collectElement(elementDecl, possibleElements);
					// Collect substitution group
					XSObjectList group = document.getSubstitutionGroup(elementDecl);
					if (group != null) {
						for (int i = 0; i < group.getLength(); i++) {
							XSElementDeclaration o = (XSElementDeclaration) group.item(i);
							document.collectElement(o, possibleElements);
						}
					}
				} else {
					// case with xs:any. Ex:
					// <xs:sequence>
					// <xs:any maxOccurs="2" processContents="lax" />
					// </xs:sequence>
					Collection<CMElementDeclaration> anyElements = getXSAnyElements(object);
					if (anyElements != null) {
						return anyElements;
					}
				}
			}
			return possibleElements;
		}
		return getElements();
	}

	/**
	 * Returns the possible elements declaration if the given declaration is an
	 * xs:any and null otherwise.
	 *
	 * @param declaration the element, wildcard declaration.
	 * @return the possible elements declaration if the given declaration is an
	 *         xs:any and null otherwise.
	 *
	 */
	private Collection<CMElementDeclaration> getXSAnyElements(Object declaration) {
		short processContents = getXSAnyProcessContents(declaration);
		if (processContents != PC_UNKWOWN) {
			// xs:any
			switch (processContents) {
			case XSWildcard.PC_STRICT:
			case XSWildcard.PC_SKIP:
				// <xs:any processContents="strict" /> or <xs:any processContents="skip" />
				// only global element declaration from the XML Schema are allowed
				return document.getElements();
			default:
				// <xs:any processContents="lax" />
				// all tags are allowed.
				return ANY_ELEMENT_DECLARATIONS;
			}
		}
		return null;
	}

	/**
	 * Returns the value of the xs:any/@processContents if the given element is a
	 * xs:any and {@link #PC_UNKWOWN} otherwise.
	 *
	 * @param declaration the element, wildcard declaration.
	 * @return the value of the xs:any/@processContents if the given element is a
	 *         xs:any and {@link #PC_UNKWOWN} otherwise.
	 */
	private static short getXSAnyProcessContents(Object declaration) {
		if (declaration instanceof XSWildcard) {
			XSWildcard wildcard = (XSWildcard) declaration;
			if ((wildcard.getConstraintType() == XSWildcard.NSCONSTRAINT_ANY)) {
				return wildcard.getProcessContents();
			}
		}
		return PC_UNKWOWN;
	}

	/**
	 * Returns list of element (QName) of child elements of the given parent element
	 * upon the given offset
	 *
	 * @param parentElement the parent element
	 * @param offset        the offset where child element must be belong to
	 * @return list of element (QName) of child elements of the given parent element
	 *         upon the given offset
	 */
	private static List<QName> toQNames(DOMElement parentElement, int offset) {
		if (parentElement == null || !parentElement.hasChildNodes()) {
			return Collections.emptyList();
		}
		List<QName> qNames = new ArrayList<>();
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
				qNames.add(createQName(element));
			}
		}
		return qNames;
	}

	private static QName createQName(Element tag) {
		// intern must be called since Xerces uses == to compare String ?
		// -> see
		// https://github.com/apache/xerces2-j/blob/trunk/src/org/apache/xerces/impl/xs/SubstitutionGroupHandler.java#L55
		String namespace = tag.getNamespaceURI();
		return new QName(tag.getPrefix(), tag.getLocalName().intern(), tag.getTagName().intern(),
				StringUtils.isEmpty(namespace) ? null : namespace.intern());
	}

	private void collectElementsDeclaration(XSElementDeclaration elementDecl,
			Collection<CMElementDeclaration> elements) {
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			// TODO...
			break;
		case XSTypeDefinition.COMPLEX_TYPE:
			collectElementsDeclaration((XSComplexTypeDefinition) typeDefinition, elements);
			break;
		}
	}

	private void collectElementsDeclaration(XSComplexTypeDefinition typeDefinition,
			Collection<CMElementDeclaration> elements) {
		XSParticle particle = typeDefinition.getParticle();
		if (particle != null) {
			collectElementsDeclaration(particle.getTerm(), elements);
		}
	}

	public boolean isOptional(String childElementName) {
		if (elementOptionality == null) {
			this.elementOptionality = new HashMap<String, Boolean>();
			switch (typeDefinition.getTypeCategory()) {
			case XSTypeDefinition.SIMPLE_TYPE:
				break;
			case XSTypeDefinition.COMPLEX_TYPE:
				XSParticle particle = ((XSComplexTypeDefinition) typeDefinition).getParticle();
				if (particle != null) {
					XSObjectList objectList = ((XSModelGroup) particle.getTerm()).getParticles();
					for (int i = 0; i < objectList.getLength(); i++) {
						XSParticle xp = (XSParticle) objectList.item(i);
						XSTerm t = xp.getTerm();
						if (t instanceof XSElementDeclaration) {
							if (xp != null) {
								elementOptionality.put(t.getName(), xp.getMinOccurs() == 0);
							}
						}
					}
				}
			}
		}
		Boolean isOptional = elementOptionality.get(childElementName);
		return (isOptional != null) ? isOptional : false;
	}

	@SuppressWarnings("unchecked")
	private void collectElementsDeclaration(XSTerm term, Collection<CMElementDeclaration> elements) {
		if (term == null) {
			return;
		}
		switch (term.getType()) {
		case XSConstants.WILDCARD:
			// XSWildcard wildcard = (XSWildcard) term;
			// ex : xsd:any
			document.getElements().forEach(e -> {
				if (!elements.contains(e)) {
					elements.add(e);
				}
			});
			break;
		case XSConstants.MODEL_GROUP:
			XSObjectList particles = ((XSModelGroup) term).getParticles();
			particles.forEach(p -> collectElementsDeclaration(((XSParticle) p).getTerm(), elements));
			break;
		case XSConstants.ELEMENT_DECLARATION:
			XSElementDeclaration elementDeclaration = (XSElementDeclaration) term;
			document.collectElement(elementDeclaration, elements);
			break;
		}
	}

	@Override
	public String getDocumentation(ISharedSettingsRequest request) {
		SchemaDocumentationType currStrategy = request.getSharedSettings().getPreferences()
				.getShowSchemaDocumentationType();
		if (this.docStrategy != currStrategy) {
			clearDocumentation();
		} else if (this.documentation != null) {
			return this.documentation;
		}
		this.docStrategy = currStrategy;
		XSObjectList annotations = getElementAnnotations();
		boolean markdownSupported = request.canSupportMarkupKind(MarkupKind.MARKDOWN);
		this.documentation = new XSDDocumentation(annotations, docStrategy, !markdownSupported)
				.getFormattedDocumentation(markdownSupported);
		return this.documentation;
	}

	/**
	 * Returns list of xs:annotation from the element declaration or type
	 * declaration.
	 *
	 * @return list of xs:annotation from the element declaration or type
	 *         declaration.
	 */
	private XSObjectList getElementAnnotations() {
		// Try get xs:annotation from the element declaration
		XSObjectList annotation = elementDeclaration.getAnnotations();
		if (annotation != null && annotation.getLength() > 0) {
			return annotation;
		}
		// Try get xs:annotation from the type of element declaration
		if (typeDefinition == null) {
			return null;
		}
		return getElementAnnotations(typeDefinition);
	}

	private XSObjectList getElementAnnotations(XSTypeDefinition typeDefinition) {
		if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			XSObjectList annotation = ((XSComplexTypeDecl) typeDefinition).getAnnotations();
			// Get annotations for types derived with extension:base
			if (((XSComplexTypeDecl) typeDefinition).getDerivationMethod() == XSConstants.DERIVATION_EXTENSION) {
				XSObjectListImpl allAnnotations = new XSObjectListImpl();
				XSTypeDefinition baseType = ((XSComplexTypeDecl) typeDefinition).getBaseType();
				// Get annotations for current type
				for (Object xsObject : annotation.toArray()) {
					if (((XSObject) xsObject) != null) {
						allAnnotations.addXSObject((XSObject) xsObject);
					}
				}
				// Get annotations for base type
				for (Object xsObject : getElementAnnotations(baseType).toArray()) {
					if (((XSObject) xsObject) != null) {
						allAnnotations.addXSObject((XSObject) xsObject);
					}
				}
				return allAnnotations;
			} else {
				return annotation;
			}
		} else if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
			return ((XSSimpleTypeDecl) typeDefinition).getAnnotations();
		}
		return null;
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
	public String toString() {
		return getLocalName();
	}

	@Override
	public boolean isEmpty() {
		if (typeDefinition != null && typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition) typeDefinition;
			return complexTypeDefinition.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_EMPTY;
		}
		return false;
	}

	@Override
	public boolean isNillable() {
		return elementDeclaration == null ? false : elementDeclaration.getNillable();
	}

	@Override
	public Collection<String> getEnumerationValues() {
		if (typeDefinition != null) {
			XSSimpleTypeDefinition simpleDefinition = null;
			if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
				simpleDefinition = (XSSimpleTypeDefinition) typeDefinition;
			} else if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
				simpleDefinition = ((XSComplexTypeDefinition) typeDefinition).getSimpleType();
			}
			return CMXSDDocument.getEnumerationValues(simpleDefinition);
		}
		return Collections.emptyList();
	}

	@Override
	public String getTextDocumentation(String textContent, ISharedSettingsRequest request) {
		SchemaDocumentationType currStrategy = request.getSharedSettings().getPreferences()
				.getShowSchemaDocumentationType();
		if (this.docStrategy != currStrategy) {
			clearDocumentation();
		}
		this.docStrategy = currStrategy;
		if (textsDocumentation == null) {
			textsDocumentation = createTextsDocumentation(request);
		}
		return textsDocumentation.get(textContent);
	}

	private Map<String, String> createTextsDocumentation(ISharedSettingsRequest request) {
		boolean markdownSupported = request.canSupportMarkupKind(MarkupKind.MARKDOWN);
		Map<String, String> textsDocumentation = new HashMap<>();
		// loop for each enumeration values and update the values documentation map with
		// documentation
		getEnumerationValues().forEach(value -> {
			String documentation = null;
			XSObjectList annotations = getTextAnnotations(value);
			if (annotations != null) {
				documentation = new XSDDocumentation(annotations, value, docStrategy, !markdownSupported)
						.getFormattedDocumentation(markdownSupported);
			}
			if (StringUtils.isBlank(documentation)) {
				documentation = getDocumentation(request);
			}
			textsDocumentation.put(value, documentation);
		});
		return textsDocumentation;
	}

	private XSObjectList getTextAnnotations(String textContent) {
		if (typeDefinition != null) {
			XSSimpleTypeDefinition simpleDefinition = null;
			if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
				simpleDefinition = (XSSimpleTypeDefinition) typeDefinition;
			} else if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
				simpleDefinition = ((XSComplexTypeDefinition) typeDefinition).getSimpleType();
			}
			return CMXSDDocument.getEnumerationAnnotations(simpleDefinition, textContent);
		}
		return null;
	}

	private void clearDocumentation() {
		this.textsDocumentation = null;
		this.documentation = null;
	}

	XSElementDeclaration getElementDeclaration() {
		return elementDeclaration;
	}

	@Override
	public String getDocumentURI() {
		SchemaGrammar schemaGrammar = document.getOwnerSchemaGrammar(elementDeclaration);
		String uri = CMXSDDocument.getSchemaURI(schemaGrammar);
		if (uri == null) {
			// For local elements inside xs:group, getOwnerSchemaGrammar may resolve to
			// BuiltinSchemaGrammar (which has no document URI) because the element's type
			// (e.g. xsd:dateTime) belongs to the XSD namespace. Fall back to finding the
			// grammar by the element's own namespace.
			schemaGrammar = document.findSchemaGrammarByNamespace(elementDeclaration.getNamespace());
			uri = CMXSDDocument.getSchemaURI(schemaGrammar);
		}
		return uri;
	}

	@Override
	public boolean isStringType() {
		if (typeDefinition != null) {
			XSSimpleTypeDefinition simpleDefinition = null;
			if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
				simpleDefinition = (XSSimpleTypeDefinition) typeDefinition;
			} else if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
				simpleDefinition = ((XSComplexTypeDefinition) typeDefinition).getSimpleType();
			}
			if (simpleDefinition != null) {
				return "string".equals(simpleDefinition.getName());
			}
		}
		return false;
	}

	@Override
	public boolean isMixedContent() {
		if (typeDefinition != null && typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition) typeDefinition;
			return complexTypeDefinition.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED;
		}
		return false;
	}

	@Override
	public Collection<String> getDerivedTypeNames() {
		// Only abstract complex types require xsi:type to specify a concrete derived type
		if (!(typeDefinition instanceof XSComplexTypeDefinition)
				|| !((XSComplexTypeDefinition) typeDefinition).getAbstract()) {
			return Collections.emptyList();
		}
		return document.findDerivedTypeLocalNames(typeDefinition);
	}

	@Override
	public Set<CMElementDeclaration> getRequiredElements() {
		Set<CMElementDeclaration> requiredElements = new LinkedHashSet<>();
		for (CMElementDeclaration element : elements) {
			if (!isOptional(element.getLocalName())) {
				requiredElements.add(element);
			}
		}
		return requiredElements;
	}
}
