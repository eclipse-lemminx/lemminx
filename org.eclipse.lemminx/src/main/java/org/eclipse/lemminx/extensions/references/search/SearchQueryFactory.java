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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.references.search.SearchNode.Direction;
import org.eclipse.lemminx.extensions.references.search.SearchQuery.QueryDirection;
import org.eclipse.lemminx.extensions.references.settings.XMLReferenceExpression;
import org.eclipse.lemminx.extensions.references.settings.XMLReferences;
import org.eclipse.lemminx.extensions.references.settings.XMLReferencesSettings;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML references search Query factory.
 * 
 * @author Angelo ZERR
 *
 */
public class SearchQueryFactory {

	/**
	 * Returns the inversed direction of the given node according the given
	 * expression and
	 * query direction.
	 * 
	 * @param node           the DOM node.
	 * @param expression     the reference expression.
	 * @param queryDirection the query direction.
	 * 
	 * @return the inversed direction of the given node according the given
	 *         expression and
	 *         query direction.
	 */
	public static Direction getInversedDirection(DOMNode node, XMLReferenceExpression expression,
			QueryDirection direction) {
		switch (direction) {
			case FROM_2_TO:
				if (expression.matchTo(node)) {
					return Direction.TO;
				}
				return null;
			case TO_2_FROM:
				if (expression.matchFrom(node)) {
					return Direction.FROM;
				}
				return null;
			default:
				if (expression.matchFrom(node)) {
					return Direction.FROM;
				}
				if (expression.matchTo(node)) {
					return Direction.TO;
				}
				return null;
		}
	}

	/**
	 * Returns the direction of the given node according the given expression and
	 * query direction.
	 * 
	 * @param node           the DOM node.
	 * @param expression     the reference expression.
	 * @param queryDirection the query direction.
	 * 
	 * @return the direction of the given node according the given expression and
	 *         query direction.
	 */
	public static Direction getDirection(DOMNode node, XMLReferenceExpression expression,
			QueryDirection queryDirection) {
		switch (queryDirection) {
			case TO_2_FROM:
				if (expression.matchTo(node)) {
					return Direction.TO;
				}
				return null;
			case FROM_2_TO:
				if (expression.matchFrom(node)) {
					return Direction.FROM;
				}
				return null;
			default:
				if (expression.matchFrom(node)) {
					return Direction.FROM;
				}
				if (expression.matchTo(node)) {
					return Direction.TO;
				}
				return null;
		}
	}

	public static SearchQuery createQuery(DOMNode node, int offset, XMLReferencesSettings settings) {
		DOMNode adjustedNode = findAttrOrTextNode(node, offset);
		if (adjustedNode == null) {
			return null;
		}
		SearchQuery query = internalCreateQuery(adjustedNode, offset, settings, QueryDirection.FROM_2_TO);
		if (query != null) {
			return query;
		}
		return internalCreateQuery(adjustedNode, offset, settings, QueryDirection.TO_2_FROM);
	}

	public static SearchQuery createFromQuery(DOMNode node, int offset, XMLReferencesSettings settings) {
		return createQuery(node, offset, settings, QueryDirection.FROM_2_TO);
	}

	public static SearchQuery createToQuery(DOMNode node, int offset, XMLReferencesSettings settings) {
		return createQuery(node, offset, settings, QueryDirection.TO_2_FROM);
	}

	public static SearchQuery createToQueryByRetrievingToBefore(DOMNode node, int offset,
			XMLReferencesSettings settings, CancelChecker cancelChecker) {
		SearchQuery query = createQuery(node, offset, settings, QueryDirection.TO_2_FROM);
		if (query != null) {
			return query;
		}
		query = createQuery(node, offset, settings, QueryDirection.FROM_2_TO);
		if (query != null) {
			query.setMatchNode(true);
			query.setSearchInIncludedFiles(true);
			AtomicReference<SearchNode> to = new AtomicReference<>();
			try {
				SearchEngine.getInstance().search(query,
						(fromSearchNode, toSearchNode, expression) -> {
							to.set(toSearchNode);
							throw new CancellationException();
						}, cancelChecker);
			} catch (CancellationException e) {
				if (cancelChecker != null) {
					cancelChecker.checkCanceled();
				}
			}
			SearchNode searchToNode = to.get();
			if (searchToNode != null) {
				QueryDirection queryDirection = QueryDirection.TO_2_FROM;
				List<XMLReferenceExpression> expressions = findExpressions(searchToNode.getNode(), settings,
						queryDirection);
				return new SearchQuery(searchToNode, expressions, queryDirection);
			}
		}
		return null;
	}

	public static SearchQuery createQuery(DOMNode node, XMLReferencesSettings settings,
			QueryDirection direction) {
		return createQuery(node, -1, settings, direction);
	}

	public static SearchQuery createQuery(DOMNode node, int offset, XMLReferencesSettings settings,
			QueryDirection direction) {
		DOMNode adjustedNode = findAttrOrTextNode(node, offset);
		if (adjustedNode == null) {
			return null;
		}
		return internalCreateQuery(adjustedNode, offset, settings, direction);
	}

	private static SearchQuery internalCreateQuery(DOMNode adjustedNode, int offset, XMLReferencesSettings settings,
			QueryDirection direction) {
		List<XMLReferenceExpression> expressions = findExpressions(adjustedNode, settings,
				direction);
		if (expressions == null) {
			return null;
		}
		return new SearchQuery(adjustedNode, offset, expressions, direction);
	}

	private static DOMNode findAttrOrTextNode(DOMNode node, int offset) {
		if (node == null || node.isText() || node.isAttribute() || node.isOwnerDocument()) {
			return node;
		}
		if (node.isElement()) {
			DOMText text = ((DOMElement) node).findTextAt(offset);
			if (text != null) {
				return text;
			}
			DOMAttr attr = node.findAttrAt(offset);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}

	/**
	 * Returns XML references expressions which match the given DOM
	 * <code>document</code> and null otherwise.
	 * 
	 * @param document              the DOM document.
	 * @param xmlReferencesSettings the XML references settings which hosts all
	 *                              references expressions.
	 * 
	 * @return XML references expressions which match the given DOM
	 *         <code>document</code> and null otherwise.
	 */
	private static List<XMLReferenceExpression> findExpressions(DOMNode node,
			XMLReferencesSettings xmlReferencesSettings, QueryDirection queryDirection) {
		List<XMLReferences> allReferences = xmlReferencesSettings != null ? xmlReferencesSettings.getReferences()
				: null;
		if (allReferences == null) {
			return null;
		}
		DOMDocument document = node.getOwnerDocument();
		List<XMLReferenceExpression> matchedExpressions = null;
		for (XMLReferences references : allReferences) {
			// Given this XML references sample

			/**
			 * <code>
			 * "xml.references": [
			 * // references for docbook.xml files
			 * {
			 *   "pattern": "*.xml",
			 *   "expressions": [
			 *     {
			 *       "from": "xref/@linkend",
			 *       "to": "@id"
			 *     }
			 *   ]
			 * }
			 *]
			 * 
			 * </code>
			 *
			 */
			if (references.matches(document)) {
				List<XMLReferenceExpression> expressions = references.getExpressions();
				if (expressions != null) {
					if (node.isOwnerDocument()) {
						if (matchedExpressions == null) {
							matchedExpressions = new ArrayList<>();
						}
						matchedExpressions.addAll(expressions);
					} else {
						for (XMLReferenceExpression expression : expressions) {
							// Given this XML reference expression sample
							/**
							 * <code>
							 * "expressions": [
							 *   {
							 *     "from": "xref/@linkend",
							 *     "to": "@id"
							 *   }
							 * ]
							 * 
							 * </code>
							 *
							 */
							Direction direction = getDirection(node, expression, queryDirection);
							if (direction != null) {
								// here the attribute matches xref/@linkend
								if (matchedExpressions == null) {
									matchedExpressions = new ArrayList<>();
								}
								matchedExpressions.add(expression);
							}
						}
					}
				}
			}
		}
		return matchedExpressions;
	}

}
