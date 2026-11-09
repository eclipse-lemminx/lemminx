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
package org.eclipse.lemminx.extensions.relaxng.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.FilesChangedTracker;
import org.eclipse.lemminx.extensions.relaxng.jing.RelaxNGGrammar;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Objects;

/**
 * RelaxNG utilities.
 * 
 * @author Angelo ZERR
 *
 */
public class RelaxNGUtils {

	public static final String GRAMMAR_TAG = "grammar";
	public static final String DATA_TAG = "data";
	public static final String VALUE_TAG = "value";
	private static final String REF_TAG = "ref";
	private static final String INCLUDE_TAG = "include";
	private static final String EXTERNAL_REF_TAG = "externalRef";

	private static final String DEFINE_TAG = "define";
	public static final String NAME_ATTR = "name";
	public static final String HREF_ATTR = "href";
	public static final String TYPE_ATTR = "type";
	public static final String DATATYPE_LIBRARY_ATTR = "datatypeLibrary";

	public static final String[] PATTERN_ELEMENTS = { "element", "attribute", "group", "interleave", "choice",
			"optional", "zeroOrMore", "oneOrMore", "list", "mixed", "ref", "parentRef", "empty", "text",
			"value", "data", "notAllowed", "externalRef", "grammar" };

	public static final String[] GRAMMAR_ELEMENTS = { "define", "start", "include", "div" };

	public static final String[] ANYNAME_EXCEPT_ELEMENTS = { "nsName", "name" };

	public static final String[] NSNAME_EXCEPT_ELEMENTS = { "name" };

	/**
	 * Returns the effective {@code datatypeLibrary} attribute value for the
	 * given element by walking up the DOM ancestors. Returns {@code null} if no
	 * ancestor defines a {@code datatypeLibrary} attribute.
	 *
	 * @param element the element to start from.
	 * @return the effective datatypeLibrary URI, or {@code null}.
	 */
	public static String getEffectiveDatatypeLibrary(DOMElement element) {
		DOMNode current = element;
		while (current != null && current.isElement()) {
			String value = ((DOMElement) current).getAttribute(DATATYPE_LIBRARY_ATTR);
			if (value != null) {
				return value;
			}
			current = current.getParentNode();
		}
		return null;
	}

	public static FilesChangedTracker createFilesChangedTracker(RelaxNGGrammar grammar) {
		FilesChangedTracker tracker = new FilesChangedTracker();
		// Track the grammar
		String relaxNGURI = getRelaxNGURI(grammar);
		if (relaxNGURI != null && URIUtils.isFileResource(relaxNGURI)) {
			// The RelaxNG is a file, track when file changed
			tracker.addFileURI(relaxNGURI);
		}
		return tracker;
	}

	private static String getRelaxNGURI(RelaxNGGrammar grammar) {
		return grammar.getGrammarDescription().getExpandedSystemId();
	}

	/**
	 * Binding type of rng grammar
	 *
	 */
	public enum BindingType {
		DEFINE // defines a reference from an attribute (ex : ref/@name) to a define/@name attribute.
		, NONE;
	}

	/**
	 * Returns the binding type of the origin attribute (ex : ref/@name) which
	 * bounds an another
	 * target attribute (ex : define/@name).
	 *
	 * @param originAttr the origin attribute (ex : ref/@name)
	 * 
	 * @return the binding type of the origin attribute (ex : ref/@name) which
	 *         bounds an another
	 *         target attribute (ex : define/@name).
	 */
	public static BindingType getBindingType(DOMAttr originAttr) {
		if (originAttr != null) {
			String name = originAttr.getName();
			if (NAME_ATTR.equals(name)) {
				if (isRef(originAttr.getOwnerElement())) {
					// - <ref name=" --> <define name="
					return BindingType.DEFINE;
				}
			}
		}
		return BindingType.NONE;
	}

	/**
	 * Collect RNG target attributes declared in the RNG grammar according the given
	 * attribute and binding type.
	 *
	 * @param originAttr             the origin attribute.
	 * @param matchAttr              true if the attribute value must match the
	 *                               value of target attribute value and false
	 *                               otherwise.
	 * @param searchInExternalSchema true if search must be done in included XML
	 *                               Schema (include) and false otherwise.
	 * @param collector              collector to collect RNG target attributes.
	 */
	public static void searchRNGTargetAttributes(DOMAttr originAttr, BindingType bindingType, boolean matchAttr,
			boolean searchInExternalSchema, BiConsumer<String, DOMAttr> collector) {
		if (bindingType == BindingType.NONE) {
			return;
		}

		DOMDocument document = originAttr.getOwnerDocument();
		DOMElement documentElement = document != null ? document.getDocumentElement() : null;
		if (documentElement == null) {
			return;
		}
		String originAttrValue = originAttr.getValue();
		if (matchAttr && StringUtils.isEmpty(originAttrValue)) {
			return;
		}
		String targetNamespacePrefix = null;
		int index = originAttrValue.indexOf(':');
		if (index != -1) {
			// ex : jakartaee:applicationType
			targetNamespacePrefix = originAttrValue.substring(0, index);
		}

		String originName = null;
		if (matchAttr) {
			originName = getOriginName(originAttrValue, targetNamespacePrefix);
		}

		// Loop for element define.
		searchRNGTargetAttributes(originAttr, bindingType, matchAttr, collector, documentElement, targetNamespacePrefix,
				originName, new HashSet<>(), searchInExternalSchema);
	}

	private static void searchRNGTargetAttributes(DOMAttr originAttr, BindingType bindingType, boolean matchAttr,
			BiConsumer<String, DOMAttr> collector, DOMElement documentElement, String targetNamespacePrefix,
			String originName, Set<String> visitedURIs, boolean searchInExternalSchema) {
		if (visitedURIs != null) {
			DOMDocument document = documentElement.getOwnerDocument();
			String documentURI = document.getDocumentURI();
			if (visitedURIs.contains(documentURI)) {
				return;
			}
			visitedURIs.add(documentURI);
		}
		Set<String> externalURIS = null;
		Node parent = documentElement;
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				DOMElement targetElement = (DOMElement) node;
				if (isBounded(originAttr.getOwnerElement(), bindingType, targetElement)) {
					// node is a define element which
					// matches the binding type of the originAttr
					DOMAttr targetAttr = (DOMAttr) targetElement.getAttributeNode(NAME_ATTR);
					if (targetAttr != null && (!matchAttr || Objects.equal(originName, targetAttr.getValue()))) {
						collector.accept(targetNamespacePrefix, targetAttr);
					}
				} else if (isInclude(targetElement) || isExternalRef(targetElement)) {
					// collect include RNG Schema location
					String schemaLocation = targetElement.getAttribute(HREF_ATTR);
					if (schemaLocation != null) {
						if (externalURIS == null) {
							externalURIS = new HashSet<>();
						}
						externalURIS.add(schemaLocation);
					}
				} else {
					searchRNGTargetAttributes(originAttr, bindingType, matchAttr, collector, targetElement,
							targetNamespacePrefix, originName, null, false);
				}
			}
		}
		if (searchInExternalSchema && externalURIS != null) {
			// Search in include location
			DOMDocument document = documentElement.getOwnerDocument();
			String documentURI = document.getDocumentURI();
			URIResolverExtensionManager resolverExtensionManager = document.getResolverExtensionManager();
			for (String externalURI : externalURIS) {
				String resourceURI = resolverExtensionManager.resolve(documentURI, null, externalURI);
				if (URIUtils.isFileResource(resourceURI)) {
					DOMDocument externalDocument = DOMUtils.loadDocument(resourceURI,
							document.getResolverExtensionManager());
					if (externalDocument != null) {
						searchRNGTargetAttributes(originAttr, bindingType, matchAttr, collector,
								externalDocument.getDocumentElement(), targetNamespacePrefix, originName, visitedURIs,
								searchInExternalSchema);
					}
				}
			}
		}
	}

	private static String getOriginName(String originAttrValue, String targetNamespacePrefix) {
		int index = originAttrValue.indexOf(":");
		if (index != -1) {
			String prefix = originAttrValue.substring(0, index);
			if (!Objects.equal(prefix, targetNamespacePrefix)) {
				return null;
			}
			return originAttrValue.substring(index + 1, originAttrValue.length());
		}
		return originAttrValue;
	}

	private static boolean isBounded(Element originElement, BindingType originBinding, Element targetElement) {
		if (originBinding == BindingType.DEFINE) {
			// - define/@name attributes if originAttr is ref/@name
			return isDefine(targetElement);
		}
		return false;
	}

	/**
	 * Search origin attributes from the given target node..
	 *
	 * @param targetNode the referenced node
	 * @param collector  the collector to collect reference between an origin and
	 *                   target attribute.
	 */
	public static void searchRNGOriginAttributes(DOMNode targetNode, BiConsumer<DOMAttr, DOMAttr> collector,
			CancelChecker cancelChecker) {
		// get referenced attribute nodes from the given referenced node
		List<DOMAttr> targetAttrs = getTargetAttrs(targetNode);
		if (targetAttrs.isEmpty()) {
			// None referenced nodes, stop the search of references
			return;
		}

		// Here referencedNodes is filled with a list of attributes
		// define/@name

		DOMDocument document = targetNode.getOwnerDocument();
		DOMElement documentElement = document.getDocumentElement();

		String targetNamespacePrefix = null;

		// Collect references for each references nodes

		NodeList nodes = documentElement.getChildNodes();
		searchRNGOriginAttributes(nodes, targetAttrs, targetNamespacePrefix, collector, cancelChecker);
	}

	/**
	 * Returns the target attributes list (ex : list of define/@name) from the
	 * given referenced node (ex: DOM document).
	 *
	 * @param referencedNode the referenced node (ex: DOM document).
	 * 
	 * @return the target attributes list (ex : list of define/@name) from the
	 *         given referenced node (ex: DOM document).
	 */
	private static List<DOMAttr> getTargetAttrs(DOMNode referencedNode) {
		if (referencedNode == null) {
			return Collections.emptyList();
		}
		List<DOMAttr> targetAttrs = new ArrayList<>();
		Document document = referencedNode.getOwnerDocument();
		switch (referencedNode.getNodeType()) {
			case Node.ATTRIBUTE_NODE:
				// The referenced node is an attribute, add it to search references from it.
			case Node.ELEMENT_NODE:
				// The referenced node is an element, get the attribute name and add it to
				// search references from it.
				addTargetNode(referencedNode, targetAttrs);
				break;
			case Node.DOCUMENT_NODE:
				// The referenced node is the DOM document, collect all attributes
				// define/@name which can be referenced
				Element documentElement = document.getDocumentElement();
				if (documentElement == null) {
					break;
				}
				Node parent = documentElement;
				addTargetAttrs(parent, targetAttrs);
		}
		return targetAttrs;
	}

	/**
	 * Collect all target attributes (ex : define/@name) from the given parent DOM
	 * node.
	 * 
	 * @param parent      the parent DOM node.
	 * @param targetAttrs the target attributes to fill.
	 */
	private static void addTargetAttrs(Node parent, List<DOMAttr> targetAttrs) {
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				DOMElement element = (DOMElement) n;
				if (isRNGTargetElement(element)) {
					addTargetNode(element, targetAttrs);
				}
				addTargetAttrs(n, targetAttrs);
			}
		}
	}

	/**
	 * Add the given node as reference node if it is applicable.
	 *
	 * @param node        the node to add.
	 * @param targetAttrs the list of referenced nodes.
	 */
	private static void addTargetNode(DOMNode node, List<DOMAttr> targetAttrs) {
		DOMAttr attr = null;
		switch (node.getNodeType()) {
			case Node.ATTRIBUTE_NODE:
				attr = (DOMAttr) node;
				break;
			case Node.ELEMENT_NODE:
				attr = ((DOMElement) node).getAttributeNode(NAME_ATTR);
				break;
		}
		// Attribute must exists and her value must be not empty.
		if (attr != null && !StringUtils.isEmpty(attr.getValue())) {
			targetAttrs.add(attr);
		}
	}

	private static void searchRNGOriginAttributes(NodeList nodes, List<DOMAttr> targetAttrs,
			String targetNamespacePrefix, BiConsumer<DOMAttr, DOMAttr> collector, CancelChecker cancelChecker) {
		for (int i = 0; i < nodes.getLength(); i++) {
			if (cancelChecker != null) {
				cancelChecker.checkCanceled();
			}
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				DOMElement originElement = (DOMElement) node;
				NamedNodeMap originAttributes = originElement.getAttributes();
				if (originAttributes != null) {
					for (int j = 0; j < originAttributes.getLength(); j++) {
						DOMAttr originAttr = (DOMAttr) originAttributes.item(j);
						BindingType originBnding = RelaxNGUtils.getBindingType(originAttr);
						if (originBnding != BindingType.NONE) {
							String originName = getOriginName(originAttr.getValue(), targetNamespacePrefix);
							for (DOMAttr targetAttr : targetAttrs) {
								Element targetElement = targetAttr.getOwnerElement();
								if (isBounded(originAttr.getOwnerElement(), originBnding, targetElement)) {
									// node is a define element which matches the binding type of the originAttr
									if (targetAttr != null && (Objects.equal(originName, targetAttr.getValue()))) {
										collector.accept(originAttr, targetAttr);
									}
								}
							}
						}
					}
				}
			}
			if (node.hasChildNodes()) {
				searchRNGOriginAttributes(node.getChildNodes(), targetAttrs, targetNamespacePrefix, collector,
						cancelChecker);
			}
		}

	}

	public static boolean isInclude(Element element) {
		return element != null && INCLUDE_TAG.equals(element.getLocalName());
	}

	public static boolean isExternalRef(Element element) {
		return element != null && EXTERNAL_REF_TAG.equals(element.getLocalName());
	}

	public static boolean isDefine(Element element) {
		return element != null && DEFINE_TAG.equals(element.getLocalName());
	}

	public static boolean isRef(Element element) {
		return element != null && REF_TAG.equals(element.getLocalName());
	}
	
	/**
	 * Returns true if the given element is an RNG target element (define element)
	 * and false
	 * otherwise.
	 * 
	 * @param element the DOM element.
	 * 
	 * @return true if the given element is an RNG target element (define element)
	 *         and false
	 *         otherwise.
	 */
	public static boolean isRNGTargetElement(Element element) {
		return isDefine(element);
	}

	public static DOMAttr getHref(DOMElement element) {
		if (!(isInclude(element) || isExternalRef(element))) {
			return null;
		}
		return element.getAttributeNode(HREF_ATTR);
	}
}
