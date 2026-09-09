/**
 *  Copyright (c) 2018 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSElementDeclHelper;
import org.apache.xerces.impl.xs.XSLoaderImpl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.impl.xs.opti.ElementImpl;
import org.apache.xerces.impl.xs.traversers.XSDHandler;
import org.apache.xerces.impl.xs.util.SimpleLocator;
import org.apache.xerces.impl.xs.util.XSObjectListImpl;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSModelGroupDefinition;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.FilesChangedTracker;
import org.eclipse.lemminx.extensions.xerces.ReflectionUtils;
import org.eclipse.lemminx.extensions.xsd.utils.XSDUtils;
import org.eclipse.lemminx.extensions.xsi.XSISchemaModel;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.LocationLink;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XSD document implementation.
 *
 */
public class CMXSDDocument implements CMDocument, XSElementDeclHelper {

	private static final Logger LOGGER = Logger.getLogger(CMXSDDocument.class.getName());

	private final XSModel model;

	private final Map<XSElementDeclaration, CMXSDElementDeclaration> elementMappings;
	private final Map<CMXSDElementDeclaration, Map<XSTypeDefinition, CMXSDElementDeclaration>> refinedElementMappings;

	private Collection<CMElementDeclaration> elements;

	private final FilesChangedTracker tracker;

	private final XSLoaderImpl xsLoader;

	public CMXSDDocument(XSModel model, XSLoaderImpl xsLoaderImpl) {
		this.model = model;
		this.xsLoader = xsLoaderImpl;
		this.elementMappings = new HashMap<>();
		this.refinedElementMappings = new HashMap<>();
		this.tracker = createFilesChangedTracker(model);
	}

	/**
	 * Create files tracker to track all XML Schema (root and imported) from the XS
	 * model.
	 * 
	 * @return a files tracker to track all XML Schema (root and imported) from the
	 *         XS model.
	 */
	private static FilesChangedTracker createFilesChangedTracker(XSModel model) {
		Set<SchemaGrammar> grammars = new HashSet<>();
		XSNamespaceItemList namespaces = model.getNamespaceItems();
		for (int i = 0; i < namespaces.getLength(); i++) {
			SchemaGrammar grammar = getSchemaGrammar(namespaces.item(i));
			if (grammar != null) {
				grammars.add(grammar);
			}
		}
		return XSDUtils.createFilesChangedTracker(grammars);
	}

	@Override
	public boolean hasNamespace(String namespaceURI) {
		if (namespaceURI == null || model.getNamespaces() == null) {
			return false;
		}
		return model.getNamespaces().contains(namespaceURI);
	}

	@Override
	public Collection<CMElementDeclaration> getElements() {
		if (elements == null) {
			elements = new ArrayList<>();
			XSNamedMap map = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int j = 0; j < map.getLength(); j++) {
				XSElementDeclaration elementDeclaration = (XSElementDeclaration) map.item(j);
				collectElement(elementDeclaration, elements);
			}
		}
		return elements;
	}

	/**
	 * Fill the given elements list from the given Xerces elementDeclaration
	 * 
	 * @param elementDeclaration
	 * @param elements
	 */
	void collectElement(XSElementDeclaration elementDeclaration, Collection<CMElementDeclaration> elements) {
		XSObjectList list = getSubstitutionGroup(elementDeclaration);
		if (list != null) {
			// ex xsl : <xs:element name="template" substitutionGroup="xsl:declaration">
			for (int i = 0; i < list.getLength(); i++) {
				XSObject object = list.item(i);
				if (object.getType() == XSConstants.ELEMENT_DECLARATION) {
					XSElementDeclaration subElementDeclaration = (XSElementDeclaration) object;
					collectElement(subElementDeclaration, elements);
				}
			}
		}
		if (!elementDeclaration.getAbstract()) {
			// element declaration is NOT marked as abstract
			// ex with xsl: <xs:element name="declaration" type="xsl:generic-element-type"
			// abstract="true"/>
			CMElementDeclaration cmElement = getXSDElement(elementDeclaration);
			// check element declaration is not already added (ex: xs:annotation)
			if (!elements.contains(cmElement)) {
				elements.add(cmElement);
			}
		}
	}

	XSObjectList getSubstitutionGroup(XSElementDeclaration elementDeclaration) {
		return model.getSubstitutionGroup(elementDeclaration);
	}

	@Override
	public CMElementDeclaration findCMElement(DOMElement element, String namespace) {
		List<DOMElement> paths = new ArrayList<>();
		while (element != null && (namespace == null || namespace.equals(element.getNamespaceURI()))) {
			paths.add(0, element);
			element = element.getParentNode() instanceof DOMElement ? (DOMElement) element.getParentNode() : null;
		}
		CMXSDElementDeclaration declaration = null;
		for (int i = 0; i < paths.size(); i++) {
			DOMElement elt = paths.get(i);
			if (i == 0) {
				declaration = (CMXSDElementDeclaration) findElementDeclaration(elt.getLocalName(), namespace);
			} else {
				declaration = (CMXSDElementDeclaration) declaration.findCMElement(elt.getLocalName(), namespace);
			}
			if (declaration == null) {
				break;
			}
			// Refine CMElementDeclaration with specific type.
			XSTypeDefinition exactType = findXsiType(elt);
			if (exactType != null) {
				CMXSDElementDeclaration baseDeclaration = declaration;
				Map<XSTypeDefinition, CMXSDElementDeclaration> refinedElementMappingsForDeclaration =
						refinedElementMappings.computeIfAbsent(baseDeclaration,
								_key -> new HashMap<>());

				declaration = refinedElementMappingsForDeclaration.get(exactType);
				if (declaration == null) {
					declaration = baseDeclaration.refineType(exactType);
					refinedElementMappingsForDeclaration.put(exactType, declaration);
				}
			}
		}
		return declaration;
	}

	private XSTypeDefinition findXsiType(DOMElement element) {
		org.w3c.dom.NamedNodeMap attrs = element.getAttributes();
		if (attrs == null) {
			return null;
		}
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			if (attr.getLocalName().equals("type") && XSISchemaModel.XSI_WEBSITE.equals(attr.getNamespaceURI())) {
				String[] possiblyQualifiedType = attr.getNodeValue().split(":", 2);
				javax.xml.namespace.QName qualifiedType;
				if (possiblyQualifiedType.length == 1) {
					String typeName = possiblyQualifiedType[0];
					qualifiedType = new javax.xml.namespace.QName(null, typeName);
					XSTypeDefinition result = (XSTypeDefinition) model
							.getComponents(XSConstants.TYPE_DEFINITION).get(qualifiedType);
					if (result != null) {
						return result;
					}
					String defaultNs = element.getNamespaceURI(null);
					if (defaultNs != null) {
						qualifiedType = new javax.xml.namespace.QName(defaultNs, typeName);
					}
				} else {
					qualifiedType = new javax.xml.namespace.QName(
							element.getNamespaceURI(possiblyQualifiedType[0]),
							possiblyQualifiedType[1]);
				}
				return (XSTypeDefinition) model.getComponents(XSConstants.TYPE_DEFINITION).get(qualifiedType);
			}
		}
		return null;
	}

	@Override
	public Collection<String> findDerivedTypeNames(DOMElement element) {
		// Find the content model element declaration to get the element's declared type
		CMXSDElementDeclaration cmElement = (CMXSDElementDeclaration) findCMElement(element,
				element.getNamespaceURI());
		if (cmElement == null) {
			return Collections.emptyList();
		}
		// Get the base type declared for this element (e.g. BaseType in
		// <xs:element name="item" type="tns:BaseType"/>)
		XSTypeDefinition baseType = cmElement.getElementDeclaration().getTypeDefinition();
		if (baseType == null) {
			return Collections.emptyList();
		}
		// Iterate all type definitions in the schema and collect those that derive
		// from the base type via extension or restriction (for use as xsi:type values)
		XSNamedMap types = model.getComponents(XSConstants.TYPE_DEFINITION);
		Collection<String> result = new ArrayList<>();
		for (int i = 0; i < types.getLength(); i++) {
			XSTypeDefinition type = (XSTypeDefinition) types.item(i);
			if (type == baseType) {
				// Skip the base type itself (it's already the element's declared type)
				continue;
			}
			if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespace())) {
				// Skip built-in XML Schema types (xs:anyType, xs:string, etc.)
				continue;
			}
			if (type.getName() == null) {
				// Skip anonymous types
				continue;
			}
			if (type.derivedFromType(baseType,
					(short) (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION))) {
				String qualifiedName = getQualifiedTypeName(type, element);
				if (qualifiedName != null) {
					result.add(qualifiedName);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the qualified type name for the given type definition, using the
	 * appropriate prefix from the XML element context. If the type's namespace has
	 * an explicit prefix (e.g. xmlns:tns), returns "tns:TypeName". If only the
	 * default namespace matches, returns just "TypeName".
	 */
	private static String getQualifiedTypeName(XSTypeDefinition type, DOMElement element) {
		String typeNamespace = type.getNamespace();
		String typeName = type.getName();
		if (typeNamespace != null) {
			// Try to find an explicit prefix for the namespace (e.g. xmlns:tns="...")
			String prefix = element.getPrefix(typeNamespace);
			if (prefix != null) {
				return prefix + ":" + typeName;
			}
			// Fallback: if the default namespace matches, use unprefixed name
			String defaultNs = element.getNamespaceURI(null);
			if (typeNamespace.equals(defaultNs)) {
				return typeName;
			}
			// No prefix available for this namespace
			return null;
		}
		return typeName;
	}

	/**
	 * Returns the local names of all types that derive from the given base type
	 * via extension or restriction. Used by {@link CMXSDElementDeclaration} to
	 * provide xsi:type snippet choices for elements with abstract types.
	 */
	Collection<String> findDerivedTypeLocalNames(XSTypeDefinition baseType) {
		XSNamedMap types = model.getComponents(XSConstants.TYPE_DEFINITION);
		Collection<String> result = new ArrayList<>();
		for (int i = 0; i < types.getLength(); i++) {
			XSTypeDefinition type = (XSTypeDefinition) types.item(i);
			if (type == baseType || type.getName() == null) {
				continue;
			}
			if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespace())) {
				continue;
			}
			if (type.derivedFromType(baseType,
					(short) (XSConstants.DERIVATION_EXTENSION | XSConstants.DERIVATION_RESTRICTION))) {
				result.add(type.getName());
			}
		}
		return result;
	}

	private CMElementDeclaration findElementDeclaration(String tag, String namespace) {
		for (CMElementDeclaration cmElement : getElements()) {
			if (cmElement.getLocalName().equals(tag)) {
				return cmElement;
			}
		}
		return findElementInModelGroups(tag, namespace);
	}

	/**
	 * Fallback search for elements declared locally inside xs:group definitions.
	 * This is needed when the element is not a global element declaration (e.g.
	 * elements defined inside &lt;xs:group name="..."&gt;) and the parent element
	 * in the XML document belongs to a different namespace, preventing the normal
	 * DOM hierarchy walk from resolving the element.
	 *
	 * @param tag       the local name of the element to find.
	 * @param namespace the namespace URI of the element to find.
	 * @return the matching element declaration, or null if not found.
	 *
	 * @see <a href="https://github.com/redhat-developer/vscode-xml/issues/1129">vscode-xml#1129</a>
	 */
	private CMElementDeclaration findElementInModelGroups(String tag, String namespace) {
		XSNamedMap groupMap = model.getComponents(XSConstants.MODEL_GROUP_DEFINITION);
		for (int i = 0; i < groupMap.getLength(); i++) {
			XSModelGroupDefinition groupDef = (XSModelGroupDefinition) groupMap.item(i);
			if (namespace != null && !namespace.equals(groupDef.getNamespace())) {
				continue;
			}
			XSModelGroup group = groupDef.getModelGroup();
			CMElementDeclaration found = findElementInModelGroup(group, tag, namespace);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private CMElementDeclaration findElementInModelGroup(XSModelGroup group, String tag, String namespace) {
		XSObjectList particles = group.getParticles();
		for (int i = 0; i < particles.getLength(); i++) {
			XSTerm term = ((XSParticle) particles.item(i)).getTerm();
			if (term.getType() == XSConstants.ELEMENT_DECLARATION) {
				XSElementDeclaration elemDecl = (XSElementDeclaration) term;
				if (tag.equals(elemDecl.getName())
						&& (namespace == null || namespace.equals(elemDecl.getNamespace()))) {
					return getXSDElement(elemDecl);
				}
			} else if (term.getType() == XSConstants.MODEL_GROUP) {
				CMElementDeclaration found = findElementInModelGroup((XSModelGroup) term, tag, namespace);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	CMElementDeclaration getXSDElement(XSElementDeclaration elementDeclaration) {
		CMXSDElementDeclaration element = elementMappings.get(elementDeclaration);
		if (element == null) {
			element = new CMXSDElementDeclaration(this, elementDeclaration);
			elementMappings.put(elementDeclaration, element);
		}
		return element;
	}

	static Collection<String> getEnumerationValues(XSSimpleTypeDefinition typeDefinition) {
		if (typeDefinition != null) {
			if (isBooleanType(typeDefinition)) {
				return StringUtils.TRUE_FALSE_ARRAY;
			}
			StringList enumerations = typeDefinition.getLexicalEnumeration();
			if (enumerations != null && !enumerations.isEmpty()) {
				return enumerations;
			}
			XSObjectList memberTypes = typeDefinition.getMemberTypes();
			if (memberTypes != null && !memberTypes.isEmpty()) {
				// xs:union
				Collection<String> enumerationValues = new ArrayList<>();
				for (Object memberType : memberTypes) {
					if (memberType instanceof XSSimpleTypeDefinition) {
						enumerationValues.addAll(getEnumerationValues((XSSimpleTypeDefinition) memberType));
					}
				}
				return enumerationValues;
			}
		}
		return Collections.emptyList();
	}

	static boolean isBooleanType(XSSimpleTypeDefinition typeDefinition) {
		if (typeDefinition instanceof XSSimpleType) {
			return ((XSSimpleType) typeDefinition).getPrimitiveKind() == XSSimpleType.PRIMITIVE_BOOLEAN;
		}
		return false;
	}

	static XSObjectList getEnumerationAnnotations(XSSimpleTypeDefinition simpleTypeDefinition, String value) {
		if (simpleTypeDefinition instanceof XSSimpleTypeDecl) {
			XSSimpleTypeDecl simpleTypeDecl = (XSSimpleTypeDecl) simpleTypeDefinition;
			XSObjectList multiFacets = simpleTypeDecl.getMultiValueFacets();
			if (!multiFacets.isEmpty()) {
				XSMultiValueFacet facet = (XSMultiValueFacet) multiFacets.get(0);
				multiFacets = facet.getAnnotations();
				Object[] annotationArray = multiFacets.toArray();
				if (!onlyContainsNull(annotationArray)) { // if multiValueFacets has annotations
					return simpleTypeDecl.getMultiValueFacets();
				}
			} else {
				XSObjectList memberTypes = simpleTypeDecl.getMemberTypes();
				if (memberTypes != null) {
					for (Object memberType : memberTypes) {
						if (memberType instanceof XSSimpleTypeDecl) {
							XSSimpleTypeDecl type = (XSSimpleTypeDecl) memberType;
							XSObjectList enumerationAnnotations = type.enumerationAnnotations;
							if (enumerationAnnotations != null) {
								StringList values = type.getLexicalEnumeration();
								if (values != null) {
									int enumIndex = -1;
									for (int i = 0; i < values.getLength(); i++) {
										String enumValue = values.item(i);
										if (value.equals(enumValue)) {
											enumIndex = i;
											break;
										}
									}
									if (enumIndex != -1 && enumerationAnnotations.size() > enumIndex) {
										XSAnnotation annotation = (XSAnnotation) enumerationAnnotations.get(enumIndex);
										if (annotation == null) {
											return null;
										}
										return new XSObjectListImpl(new XSAnnotation[] { annotation }, 1);
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean onlyContainsNull(Object[] arr) {
		if (arr == null || arr.length == 0) {
			return true;
		}
		for (Object o : arr) {
			if (o != null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public XSElementDecl getGlobalElementDecl(QName element) {
		return (XSElementDecl) model.getElementDeclaration(element.localpart, element.uri);
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
		// Try to retrieve XSD element declaration from the given element.
		CMXSDElementDeclaration elementDeclaration = (CMXSDElementDeclaration) findCMElement(originElement,
				originElement.getNamespaceURI());
		if (elementDeclaration == null) {
			return null;
		}

		// Try to find the Xerces xs:element (which stores the offset) bound with the
		// XSElementDeclaration
		// case when xs:element is declared inside xs:choice, xs:all, xs:sequence, etc
		ElementImpl xercesElement = findLocalMappedXercesElement(elementDeclaration.getElementDeclaration(), xsLoader);
		// case when xs:element is declared as global or inside xs:complexType
		SchemaGrammar schemaGrammar = getOwnerSchemaGrammar(elementDeclaration.getElementDeclaration());
		if (schemaGrammar == null && xercesElement == null) {
			return null;
		}

		String documentURI = xercesElement != null ? xercesElement.getOwnerDocument().getDocumentURI()
				: getSchemaURI(schemaGrammar);
		if (URIUtils.isFileResource(documentURI)) {
			// Only XML Schema file is supported. In the case of XML file is bound with an
			// HTTP url and cache is enable, documentURI is a file uri from the cache
			// folder.

			// Xerces doesn't give the capability to know the location of xs:element,
			// xs:attribute.
			// To retrieve the proper location of xs:element, xs:attribute, we load the XML
			// Schema in the DOM Document which stores location.
			DOMDocument targetSchema = DOMUtils.loadDocument(documentURI,
					originNode.getOwnerDocument().getResolverExtensionManager());
			if (targetSchema == null) {
				return null;
			}
			if (originAttribute != null) {
				// find location of xs:attribute declaration
				CMXSDAttributeDeclaration attributeDeclaration = (CMXSDAttributeDeclaration) elementDeclaration
						.findCMAttribute(originAttribute);
				if (attributeDeclaration != null) {
					XSAttributeDeclaration attributeDecl = attributeDeclaration.getAttrDeclaration();
					if (attributeDecl.getScope() == XSConstants.SCOPE_LOCAL) {
						return findLocalXSAttribute(originAttribute, targetSchema,
								attributeDecl.getEnclosingCTDefinition(), schemaGrammar);
					}
				}
			} else {
				// find location of xs:element declaration
				boolean globalElement = elementDeclaration.getElementDeclaration()
						.getScope() == XSElementDecl.SCOPE_GLOBAL;
				if (globalElement) {
					// global xs:element
					return findGlobalXSElement(originElement, targetSchema);
				} else {
					// local xs:element
					// 1) use the Xerces xs:element strategy
					if (xercesElement != null) {
						return findLocalXSElement(originElement, targetSchema, xercesElement.getCharacterOffset());
					}
					// 2) use the Xerces xs:complexType strategy
					XSComplexTypeDefinition complexTypeDefinition = elementDeclaration.getElementDeclaration()
							.getEnclosingCTDefinition();
					if (complexTypeDefinition != null) {
						return findLocalXSElement(originElement, targetSchema, complexTypeDefinition, schemaGrammar);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the schema URI of the given schema grammar.
	 * 
	 * @param schemaGrammar the Xerces schema grammar.
	 * @return the schema URI of the given schema grammar.
	 */
	static String getSchemaURI(SchemaGrammar schemaGrammar) {
		if (schemaGrammar == null) {
			return null;
		}
		return schemaGrammar.getDocumentLocations().item(0);
	}

	/**
	 * Returns the owner schema grammar of the given XSD element declaration and
	 * null otherwise.
	 * 
	 * @param elementDeclaration the XSD element declaration
	 * @return the owner schema grammar of the given XSD element declaration and
	 *         null otherwise.
	 */
	SchemaGrammar getOwnerSchemaGrammar(XSElementDeclaration elementDeclaration) {
		XSTypeDefinition enclosingType = elementDeclaration.getEnclosingCTDefinition();
		if (enclosingType == null && elementDeclaration.getScope() == XSConstants.SCOPE_ABSENT) {
			enclosingType = elementDeclaration.getTypeDefinition();
		}
		// 1) when XSD element declaration has namespace, it is bound with the grammar.
		XSNamespaceItem namespaceItem = enclosingType != null ? enclosingType.getNamespaceItem()
				: elementDeclaration.getNamespaceItem();
		SchemaGrammar grammar = getSchemaGrammar(namespaceItem);
		if (grammar != null) {
			return grammar;
		}
		// 2) XSD element declaration has none namespace. Here we loop for each grammar
		// to discover if the element type belong to this grammar.
		XSNamespaceItemList namespaces = model.getNamespaceItems();

		// 2.1) XSD global element, uses the SchemaGrammar#getElementDeclaration method
		if (elementDeclaration.getScope() == XSConstants.SCOPE_GLOBAL) {
			for (int i = 0; i < namespaces.getLength(); i++) {
				XSNamespaceItem namespace = namespaces.item(i);
				if (namespace instanceof SchemaGrammar && (elementDeclaration
						.equals(((SchemaGrammar) namespace).getElementDeclaration(elementDeclaration.getName())))) {
					return (SchemaGrammar) namespace;
				}
			}
		}

		// 2.2) XSD local element inside a group, find grammar by element namespace
		if (enclosingType == null) {
			String ns = elementDeclaration.getNamespace();
			if (ns != null) {
				for (int i = 0; i < namespaces.getLength(); i++) {
					XSNamespaceItem namespace = namespaces.item(i);
					if (namespace instanceof SchemaGrammar && ns.equals(namespace.getSchemaNamespace())) {
						return (SchemaGrammar) namespace;
					}
				}
			}
			return null;
		}
		// 2.3) XSD local element, get the parent xs:complexType of the XSD element and
		// loop for each xs:complexType of the SchemaGrammar
		for (int i = 0; i < namespaces.getLength(); i++) {
			XSNamespaceItem namespace = namespaces.item(i);
			if (namespace instanceof SchemaGrammar) {
				XSComplexTypeDecl[] complexTypes = getXSComplexTypeDecls((SchemaGrammar) namespace);
				if (complexTypes != null) {
					for (int j = 0; j < complexTypes.length; j++) {
						if (enclosingType.equals(complexTypes[j])) {
							return (SchemaGrammar) namespace;
						}
					}
				}
			}
		}
		return null;

	}

	/**
	 * Returns the schema grammar from the given namespace and null otherwise.
	 * 
	 * @param namespaceItem the namespace
	 * @return the schema grammar from the given namespace and null otherwise.
	 */
	private static SchemaGrammar getSchemaGrammar(XSNamespaceItem namespaceItem) {
		return (namespaceItem != null && namespaceItem instanceof SchemaGrammar) ? (SchemaGrammar) namespaceItem : null;
	}

	/**
	 * Returns the schema grammar matching the given namespace URI. Used as a
	 * fallback when the standard grammar resolution (via enclosing type or
	 * namespace item) fails for local elements inside xs:group definitions,
	 * where Xerces may resolve to the BuiltinSchemaGrammar instead of the
	 * user's schema grammar.
	 *
	 * @param namespace the namespace URI to search for.
	 * @return the matching schema grammar, or null if not found.
	 */
	SchemaGrammar findSchemaGrammarByNamespace(String namespace) {
		if (namespace == null) {
			return null;
		}
		XSNamespaceItemList namespaces = model.getNamespaceItems();
		for (int i = 0; i < namespaces.getLength(); i++) {
			XSNamespaceItem namespaceItem = namespaces.item(i);
			if (namespaceItem instanceof SchemaGrammar && namespace.equals(namespaceItem.getSchemaNamespace())) {
				return (SchemaGrammar) namespaceItem;
			}
		}
		return null;
	}

	/**
	 * Returns the location of the global xs:element declared in the given XML
	 * Schema <code>targetSchema</code> which matches the given XML element
	 * <code>originElement</code> and null otherwise.
	 * 
	 * @param originElement the XML element
	 * @param targetSchema  the XML Schema
	 * @return the location of the global xs:element declared in the given XML
	 *         Schema <code>targetSchema</code> which matches the given XML element
	 *         <code>originElement</code> and null otherwise.
	 */
	private static LocationLink findGlobalXSElement(DOMElement originElement, DOMDocument targetSchema) {
		// In global xs:element case, the xs:element are declared after the document
		// element xs:schema.
		// Here we just loop of children of xs:schema and return the location of
		// xs:element/@name which matches the tag name of the origin XML element
		NodeList children = targetSchema.getDocumentElement().getChildNodes();
		return findXSElement(originElement, children, false);
	}

	/**
	 * Returns the Xerces DOM xs:element which have created the given instance
	 * <code>elementDeclaration</code> and null otherwise.
	 * 
	 * @param elementDeclaration
	 * @param xsLoader
	 * @return the Xerces DOM xs:element which have created the given instance
	 *         <code>elementDeclaration</code> and null otherwise
	 */
	private static ElementImpl findLocalMappedXercesElement(XSElementDeclaration elementDeclaration,
			XSLoaderImpl xsLoader) {
		try {
			// When XML Schema is loaded by XSLoaderImpl, it uses XMLSchemaLoader which uses
			// XSDHandler.
			// Xerces stores the location in the XSDHandler instance in 2 arrays:
			// - fParticle array of XSParticleDecl where fValue is an instance of
			// XSElementDeclaration
			// - fLocalElementDecl array of Xerces Element which stores the element offset.

			// Get the XMLSchemaLoader instance from the XSLoader instance
			XMLSchemaLoader schemaLoader = ReflectionUtils.getFieldValue(xsLoader, "fSchemaLoader");

			// Get the XSDHandler instance from the XMLSchemaLoader instance
			XSDHandler handler = ReflectionUtils.getFieldValue(schemaLoader, "fSchemaHandler");

			// Get the XSParticleDecl array from the XSDHandler instance
			XSParticleDecl[] fParticle = ReflectionUtils.getFieldValue(handler, "fParticle");

			// Get the index where elementDeclaration is associated with a XSParticleDecl
			int i = getXSElementDeclIndex(elementDeclaration, fParticle);
			if (i >= 0) {
				// Get the Xerces Element array from the XSDHandler instance
				Element[] fLocalElementDecl = ReflectionUtils.getFieldValue(handler, "fLocalElementDecl");
				return (ElementImpl) fLocalElementDecl[i];
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error while retrieving mapped Xerces xs:element of '" + elementDeclaration.getName() + "'.", e);
		}
		return null;

	}

	/**
	 * Returns the location link for the given origin element and target element at
	 * the given offset.
	 * 
	 * @param originElement the origin element
	 * @param targetSchema  the target DOM document
	 * @param offset        the offset of the element range to return.
	 * @return the location link for the given origin element and target element at
	 *         the given offset.
	 */
	private static LocationLink findLocalXSElement(DOMElement originElement, DOMDocument targetSchema, int offset) {
		DOMNode node = targetSchema.findNodeAt(offset);
		if (node != null && node.isElement()) {
			return findXSElement(originElement, (Element) node);
		}
		return null;
	}

	/**
	 * Returns the index where the given element declaration is store in the
	 * particles array and null otherwise.
	 * 
	 * @param elementDeclaration the XS element declaration.
	 * @param particles          the XS particles declaration.
	 * @return the index where the given element declaration is store in the
	 *         particles array and null otherwise.
	 */
	private static int getXSElementDeclIndex(XSElementDeclaration elementDeclaration, XSParticleDecl[] particles) {
		for (int i = 0; i < particles.length; i++) {
			XSParticleDecl particle = particles[i];
			if (particle != null && elementDeclaration.equals(particle.fValue)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the location of the local xs:element declared in the given XML Schema
	 * <code>targetSchema</code> which matches the given XML element
	 * <code>originElement</code> and null otherwise.
	 * 
	 * @param originElement the XML element
	 * @param targetSchema  the XML Schema
	 * @param enclosingType the enclosing type of the XS element declaration which
	 *                      matches the XML element
	 * @param grammar       the Xerces grammar
	 * @return the location of the global xs:element declared in the given XML
	 *         Schema <code>targetSchema</code> which matches the given XML element
	 *         <code>originElement</code> and null otherwise.
	 */
	private static LocationLink findLocalXSElement(DOMElement originElement, DOMDocument targetSchema,
			XSComplexTypeDefinition enclosingType, SchemaGrammar schemaGrammar) {
		// In local xs:element case, xs:element is declared inside a complex type
		// (enclosing type).
		// Xerces stores in the SchemaGrammar the locator (offset) for each complex type
		// (XSComplexTypeDecl)
		// Here we get the offset of the local enclosing complex type xs:complexType.
		// After that
		// we just loop of children of local xs:complexType and return the
		// location of
		// xs:element/@name which matches the tag name of the origin XML element

		// Get the location of the local xs:complexType
		int complexTypeOffset = getComplexTypeOffset(enclosingType, schemaGrammar);
		if (complexTypeOffset != -1) {
			// location of xs:complexType is found, find the xs:element declared inside the
			// xs:complexType
			DOMNode node = targetSchema.findNodeAt(complexTypeOffset);
			if (node != null && node.isElement() && node.hasChildNodes()) {
				return findXSElement((DOMElement) originElement, node.getChildNodes(), true);
			}
		}
		return null;
	}

	/**
	 * Returns the offset where the local xs:complexType is declared and -1
	 * otherwise.
	 * 
	 * @param complexType the local complex type
	 * @param grammar     the grammar where local complex type is declared.
	 * @return the offset where the local xs:complexType is declared and -1
	 *         otherwise.
	 */
	private static int getComplexTypeOffset(XSComplexTypeDefinition complexType, SchemaGrammar grammar) {
		try {
			// Xerces stores in SchemaGrammar instance, the location in 2 arrays:
			// - fCTLocators array of locator
			// - fComplexTypeDecls array of XSComplexTypeDecl

			// As it's not an API, we must use Java Reflection to get those 2 arrays
			SimpleLocator[] fCTLocators = ReflectionUtils.getFieldValue(grammar, "fCTLocators");

			// Find the location offset of the given complexType
			XSComplexTypeDecl[] fComplexTypeDecls = getXSComplexTypeDecls(grammar);
			for (int i = 0; i < fComplexTypeDecls.length; i++) {
				if (complexType.equals(fComplexTypeDecls[i])) {
					XMLLocator locator = fCTLocators[i];
					return locator != null ? locator.getCharacterOffset() : -1;
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error while retrieving offset of local xs:complexType'" + complexType.getName() + "'.", e);
		}
		// Offset where xs:complexType is declared cannot be found
		return -1;
	}

	/**
	 * Returns all xs:complexTypes (global and local) from the given schema grammar
	 * 
	 * @param grammar the grammar
	 * @return all xs:complexTypes (global and local) from the given schema grammar
	 */
	private static XSComplexTypeDecl[] getXSComplexTypeDecls(SchemaGrammar grammar) {
		try {
			return ReflectionUtils.getFieldValue(grammar, "fComplexTypeDecls");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while retrieving list of xs:complexType of the grammar '"
					+ grammar.getSchemaNamespace() + "'.", e);
			return null;
		}
	}

	/**
	 * Returns the location of the xs:element declared in the given XML Schema
	 * <code>targetSchema</code> which matches the given XML element
	 * <code>originElement</code> and null otherwise.
	 * 
	 * @param originElement the XML element
	 * @param children      the children where xs:element must be searched
	 * @param inAnyLevel    true if search must be done in any level and false
	 *                      otherwise.
	 * @return
	 */
	private static LocationLink findXSElement(DOMElement originElement, NodeList children, boolean inAnyLevel) {
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) n;
				LocationLink location = findXSElement(originElement, elt);
				if (location != null) {
					return location;
				}
				if (inAnyLevel && elt.hasChildNodes()) {
					location = findXSElement(originElement, elt.getChildNodes(), inAnyLevel);
					if (location != null) {
						return location;
					}
				}
			}
		}
		return null;
	}

	private static LocationLink findXSElement(DOMElement originElement, Element elt) {
		if (XSDUtils.isXSElement(elt)) {
			if (originElement.getLocalName().equals(elt.getAttribute("name"))) {
				DOMAttr targetAttr = (DOMAttr) elt.getAttributeNode("name");
				LocationLink location = XMLPositionUtility.createLocationLink(originElement,
						targetAttr.getNodeAttrValue());
				return location;
			}
		}
		return null;
	}

	private static LocationLink findLocalXSAttribute(DOMAttr originAttribute, DOMDocument targetSchema,
			XSComplexTypeDefinition enclosingType, SchemaGrammar schemaGrammar) {
		int complexTypeOffset = getComplexTypeOffset(enclosingType, schemaGrammar);
		if (complexTypeOffset != -1) {
			// location of xs:complexType is found, find the xs:attribute declared inside
			// the
			// xs:complexType
			DOMNode node = targetSchema.findNodeAt(complexTypeOffset);
			if (node != null && node.isElement() && node.hasChildNodes()) {
				return findXSAttribute(originAttribute, node.getChildNodes(), true);
			}
		}
		return null;
	}

	private static LocationLink findXSAttribute(DOMAttr originAttribute, NodeList children, boolean inAnyLevel) {
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				DOMElement elt = (DOMElement) n;
				if (XSDUtils.isXSAttribute(elt)) {
					if (originAttribute.getName().equals(elt.getAttribute("name"))) {
						DOMAttr targetAttr = (DOMAttr) elt.getAttributeNode("name");
						LocationLink location = XMLPositionUtility.createLocationLink(originAttribute.getNodeAttrName(),
								targetAttr.getNodeAttrValue());
						return location;
					}
				}
				if (inAnyLevel && elt.hasChildNodes()) {
					LocationLink location = findXSAttribute(originAttribute, elt.getChildNodes(), inAnyLevel);
					if (location != null) {
						return location;
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isDirty() {
		return tracker.isDirty();
	}
}
