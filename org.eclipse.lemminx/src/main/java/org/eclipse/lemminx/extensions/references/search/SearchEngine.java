/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.references.search;

import static org.eclipse.lemminx.extensions.references.search.SearchQueryFactory.getInversedDirection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.references.search.SearchNode.Direction;
import org.eclipse.lemminx.extensions.references.search.SearchQuery.QueryDirection;
import org.eclipse.lemminx.extensions.references.settings.XMLReferenceExpression;
import org.eclipse.lemminx.extensions.references.settings.XMLReferencesSettings;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * XML references search engine to collect attribute , text which matches XML
 * references expression {@link XMLReferenceExpression}.
 * 
 * @author Angelo ZERR
 *
 */
public class SearchEngine {

	private static final SearchEngine INSTANCE = new SearchEngine();

	public static SearchEngine getInstance() {
		return INSTANCE;
	}

	private static final String INCLUDE_TAG = "include";

	private static final String HREF_ATTR = "href";

	/**
	 * Perform the XML references search by using the given search query.
	 * 
	 * @param query         the search query.
	 * @param collector     the collector used to collect attribute, text nodes.
	 * 
	 * @param cancelChecker the cancel checker.
	 */
	public final void search(SearchQuery query, IXMLReferenceCollector collector, CancelChecker cancelChecker) {
		DOMDocument document = query.getNode().getOwnerDocument();
		Set<String> visitedURIs = query.isSearchInIncludedFiles() ? new HashSet<>() : null;
		searchInDocument(document, query, collector, visitedURIs, cancelChecker);
	}

	public Collection<ReferenceLink> searchLinks(DOMDocument document, XMLReferencesSettings settings,
			CancelChecker cancelChecker) {
		SearchQuery query = SearchQueryFactory.createQuery(document,
				settings, QueryDirection.BOTH);
		if (query != null) {
			// Search references in included files
			query.setSearchInIncludedFiles(true);
			
			final Map<XMLReferenceExpression, ReferenceLink> linksMap = new HashMap<>();
			SearchEngine.getInstance().search(query,
					(fromSearchNode, toSearchNode, expression) -> {
						ReferenceLink link = linksMap.get(expression);
						if (link == null) {
							link = new ReferenceLink(expression);
							linksMap.put(expression, link);
						}
						if (fromSearchNode != null) {
							link.addFrom(fromSearchNode);
						}
						if (toSearchNode != null) {
							link.addTo(toSearchNode);
						}
					}, cancelChecker);
			return linksMap.values();
		}
		return Collections.emptyList();
	}

	/**
	 * Perform the search in the given DOM document.
	 * 
	 * @param document      the DOM document.
	 * @param query         the search query.
	 * @param collector     the collector used to collect attribute, text nodes.
	 * @param visitedURIs   visited URis used to avoid document loading recursion
	 *                      when document contains some xi:include.
	 * @param cancelChecker the cancel checker.
	 */
	private void searchInDocument(DOMDocument document, SearchQuery query, IXMLReferenceCollector collector,
			Set<String> visitedURIs, CancelChecker cancelChecker) {

		// Perform the search by using the DOM document
		Set<String> externalURIsForDocument = query.isSearchInIncludedFiles() ? new HashSet<>() : null;
		searchInNode(document, query, collector, externalURIsForDocument, cancelChecker);

		if (externalURIsForDocument != null && !externalURIsForDocument.isEmpty()) {
			// The search for the document has collected some external document, URIs,
			// perform the search for each of them.
			URIResolverExtensionManager resolverExtensionManager = document.getResolverExtensionManager();
			for (String externalURI : externalURIsForDocument) {
				String baseURI = document.getDocumentURI();
				String resourceURI = resolverExtensionManager.resolve(baseURI, null, externalURI);
				if (URIUtils.isFileResource(resourceURI) && canPerformSearch(resourceURI, visitedURIs)) {
					// The search was never done for the exterlam document, perform the search by
					// using this external document.
					if (visitedURIs != null) {
						visitedURIs.add(baseURI);
					}
					DOMDocument externalDocument = DOMUtils.loadDocument(resourceURI,
							document.getResolverExtensionManager());
					if (externalDocument != null) {
						searchInDocument(externalDocument, query, collector, visitedURIs, cancelChecker);
					}
				}
			}
		}
	}

	/**
	 * Returns true if the search can be performed for the given document URI and
	 * false otherwise.
	 * 
	 * @param documentURI the document URI.
	 * @param visitedURIs the visited URIs.
	 * 
	 * @return true if the search can be performed for the given document URI and
	 *         false otherwise.
	 */
	private boolean canPerformSearch(String documentURI, Set<String> visitedURIs) {
		return visitedURIs == null || !visitedURIs.contains(documentURI);
	}

	/**
	 * Perform the search in the given DOM node.
	 * 
	 * @param node          the DOM node.
	 * @param query         the search query.
	 * @param collector     the collector used to collect attribute, text nodes.
	 * @param externalURIs  the collected external URIs (xi:include/@href)
	 * @param cancelChecker the cancel checker.
	 */
	private void searchInNode(DOMNode node, SearchQuery query, IXMLReferenceCollector collector,
			Set<String> externalURIs, CancelChecker cancelChecker) {
		// Stop the search if required
		if (cancelChecker != null) {
			cancelChecker.checkCanceled();
		}

		if (node.isElement()) {
			// Search in the attributes of the element
			DOMElement element = (DOMElement) node;
			searchInAttributes(element, query, collector, cancelChecker);
			if (externalURIs != null) {
				if (isInclude(element)) {
					// collect xi:include
					String includedFile = element.getAttribute(HREF_ATTR);
					if (includedFile != null) {
						externalURIs.add(includedFile);
					}
				}
			}

		} else if (node.isText()) {
			// Search in the text
			searchInText((DOMText) node, query, collector, cancelChecker);
		}
		if (node.hasChildNodes()) {
			// Search in the children
			for (DOMNode child : node.children()) {
				searchInNode(child, query, collector, externalURIs, cancelChecker);
			}
		}
	}

	/**
	 * Perform the search in the given DOM text node.
	 * 
	 * @param text          the DOM text node.
	 * @param query         the search query.
	 * @param collector     the collector used to collect attribute, text nodes.
	 * @param cancelChecker the cancel checker.
	 */
	private void searchInText(DOMText text, SearchQuery query, IXMLReferenceCollector collector,
			CancelChecker cancelChecker) {
		if (query.isSearchInText()) {
			if (cancelChecker != null) {
				cancelChecker.checkCanceled();
			}
			collectNodes(text, query, collector);
		}
	}

	/**
	 * Perform the search in the attributes of the given DOM element node.
	 * 
	 * @param element       the DOM element node.
	 * @param query         the search query.
	 * @param collector     the collector used to collect attribute, text nodes.
	 * @param cancelChecker the cancel checker.
	 */
	private void searchInAttributes(DOMElement element, SearchQuery query, IXMLReferenceCollector collector,
			CancelChecker cancelChecker) {
		if (query.isSearchInAttribute()) {
			// Search to reference in attribute nodes
			if (element.hasAttributes()) {
				NamedNodeMap toAttributes = element.getAttributes();
				if (toAttributes != null) {
					for (int j = 0; j < toAttributes.getLength(); j++) {
						DOMAttr toAttr = (DOMAttr) toAttributes.item(j);
						if (cancelChecker != null) {
							cancelChecker.checkCanceled();
						}
						collectNodes(toAttr, query, collector);
					}
				}
			}
		}
	}

	private void collectNodes(DOMNode node, SearchQuery query, IXMLReferenceCollector collector) {
		QueryDirection queryDirection = query.getQueryDirection();
		// Loop for reference expressions of the query
		for (XMLReferenceExpression expression : query.getExpressions()) {
			Direction nodeDirection = getInversedDirection(node, expression, queryDirection);
			if (nodeDirection != null) {
				// The DOM node matches the XPath (from / to) declared in the reference
				// expression
				// get the search nodes for this attribute / text node
				List<SearchNode> searchNodes = findSearchNodes(node, expression, nodeDirection);
				for (SearchNode searchNode : searchNodes) {
					// Collect the current search node
					collect(query, searchNode, expression, collector);
				}
			}
		}
	}

	private void collect(SearchQuery query, SearchNode searchNode, XMLReferenceExpression expression,
			IXMLReferenceCollector collector) {
		SearchNode requestedNode = query.getSearchNode();
		if (query.isMatchNode()) {
			if (!requestedNode.matchesValue(searchNode)) {
				return;
			}
		}
		QueryDirection direction = query.getQueryDirection();
		switch (direction) {
			case FROM_2_TO:
				collector.collect(requestedNode, searchNode, expression);
				break;
			case TO_2_FROM:
				collector.collect(searchNode, requestedNode, expression);
				break;
			default:
				Direction nodeDirection = searchNode.getDirection();
				if (nodeDirection == Direction.FROM) {
					collector.collect(searchNode, requestedNode, expression);
				} else {
					collector.collect(requestedNode, searchNode, expression);
				}
		}
	}

	private List<SearchNode> findSearchNodes(DOMNode node, XMLReferenceExpression expression, Direction nodeDirection) {
		if (nodeDirection == Direction.FROM) {
			return SearchNodeFactory.findSearchNodes(node, expression.getPrefix(), expression.isMultiple(),
					nodeDirection);
		}
		return SearchNodeFactory.findSearchNodes(node, null, false, nodeDirection);
	}

	/**
	 * Returns true if the given element is an include element (ex : xi:include) and
	 * false otherwise.
	 * 
	 * @param element the DOM element.
	 * 
	 * @return true if the given element is an include element (ex : xi:include) and
	 *         false otherwise.
	 */
	private static boolean isInclude(Element element) {
		return element != null && INCLUDE_TAG.equals(element.getLocalName());
	}
}
