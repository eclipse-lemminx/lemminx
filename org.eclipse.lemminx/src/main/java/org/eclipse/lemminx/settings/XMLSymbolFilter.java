/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.settings;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.xpath.matcher.IXPathNodeMatcher.MatcherType;

/**
 * XML symbol filter used to show/hide DOM attributes, text nodes specified with
 * XPath expressions:
 * 
 * <pre>
 * "xml.symbols.filters": [
   // Declaration of symbols filter for maven 'pom.xml' to show all text nodes in the Outline.
   {
      "pattern": "pom.xml",
      "expressions" :[
         {
            "xpath": "//text()"
         }
      ]
   },
   // Declaration of symbols filter for Spring beans to show all @id of the elements in the Outline.
   {
      "pattern": "bean*.xml",
      "expressions" :[
         {
            "xpath": "//@id"
         }
      ]
   },
   // Declaration of symbols filter for ant/phing build.xml files to show all target names and property
   // names/files in the Outline.
   {
      "pattern": "build*.xml",
      "expressions" :[
         {
            // keep the value of the attribute "name" on the same line
            // as the "target" element, and only show the attribute value
            "xpath": "//target/@name",
            "inlineAttribute" : true,
            "showAttributeName": false
         },
         // show "unless" and "depends" as nested attributes for
         // "target" elements
         {
            "xpath": "//target/@unless"
         },
         {
            "xpath": "//target/@depends"
         },
         {
            // keep the value of the attribute "name" on the same line
            // as the "property" element, and only show the attribute value
            "xpath": "//property/@name",
            "inlineAttribute" : true,
            "showAttributeName": false
         },
         {
            // keep the value of the attribute "file" on the same line
            // as the "property" element, and show the attribute name
            // along with the value to distinguish it from the more common
            // "name" attribute
            "xpath": "//property/@file",
            "inlineAttribute" : true,
            "showAttributeName": true
         }
      }
   ]
 * </pre>
 */
public class XMLSymbolFilter extends DocumentMatcher {

	public static final XMLSymbolFilter DEFAULT;

	static {
		DEFAULT = new XMLSymbolFilter();
	}

	private XMLSymbolExpressionFilter[] expressions;

	/**
	 * Set the expression list filter.
	 * 
	 * @param expressions the expression list filter.
	 */
	public void setExpressions(XMLSymbolExpressionFilter[] expressions) {
		this.expressions = expressions;
	}

	/**
	 * Returns the expression list filter.
	 * 
	 * @return the expression list filter.
	 */
	public XMLSymbolExpressionFilter[] getExpressions() {
		return expressions;
	}

	/**
	 * Gets the first matched attribute node that is set as an
	 * inline attribute for an element.
	 * 
	 * @param attrNode the DOMElement attribute node to check for.
	 * 
	 * @return the first matched attribute node that is set as an
	 *         inline attribute for an element, or null if there
	 *         isn't one.
	 */
	public XMLSymbolExpressionFilter getFilterForInlineAttr(DOMAttr attrNode){
		if (expressions != null && expressions.length > 0) {
			for (XMLSymbolExpressionFilter expression : expressions) {
				if (expression.match(attrNode) && expression.isInlineAttribute()) {
					return expression;
				}
			}
		}

		return null;
	}

	/**
	 * Returns true if the given node is a symbol and false otherwise.
	 * 
	 * @param node the DOM node.
	 * 
	 * @return true if the given node is a symbol and false otherwise.
	 */
	public boolean isNodeSymbol(DOMNode node) {
		if (node == null) {
			return false;
		}
		if (expressions != null && expressions.length > 0) {
			// loop for list of XPath expression and returns the first expression which
			// matches the given node.
			for (XMLSymbolExpressionFilter expression : expressions) {
				if (expression.match(node)) {
					return !expression.isExcluded();
				}
			}
		}
		// By default DOM attributes and Text nodes are excluded.
		return node.isElement() || //
				node.isDoctype() || //
				node.isProcessingInstruction() || //
				node.isProlog() || //
				node.isDTDElementDecl() || //
				node.isDTDAttListDecl() || //
				node.isDTDEntityDecl() || //
				node.isDTDNotationDecl();
	}

	/**
	 * Returns true if the filter have an expression for the given type (element,
	 * attribute, text) and false otherwise.
	 * 
	 * @param matcherType the matcher type.
	 * 
	 * @return true if the filter have an expression for the given type (element,
	 *         attribute, text) and false otherwise.
	 */
	public boolean hasFilterFor(MatcherType matcherType) {
		if (expressions == null) {
			return false;
		}
		for (XMLSymbolExpressionFilter expression : expressions) {
			if (expression.isFilterFor(matcherType)) {
				return true;
			}
		}
		return false;
	}
}
