/*******************************************************************************
* Copyright (c) 2020 Łukasz Kwiatkowski
* All rights reserved.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
* @author Lukasz Kwiatkowski <woocash@eikberg.com> - initial API and implementation
*******************************************************************************/

package org.eclipse.lemminx.extensions.xsl.model;

import java.util.*;
import java.util.logging.*;
import java.util.function.Predicate;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.xsl.model.XSLElement.*;
import org.eclipse.lemminx.extensions.xsl.utils.XSLUtils;


/**
 * XSL document model.
 * 
 * @author Lukasz Kwiatkowski <woocash@eikberg.com> 
 *
 */
public class XSLDocument{

	private static final Logger LOGGER = Logger.getLogger(XSLDocument.class.getName());

	private DOMDocument document;
	private List<XSLDocument> includedXSLDocuments;
	private XSLElementCollector collector = new XSLElementCollector();
	private boolean parsed = false;


	public XSLDocument(DOMDocument document){
		this.document = document;
	}

	public String getDocumentURI(){
		return document.getDocumentURI();
	}


	public boolean parseDocument(Predicate<DOMNode> shouldStop){
		if(parsed || document==null){
			return false;
		}
		return traversUpFromElement(document.getDocumentElement(), shouldStop);
	}


	private DOMNode getStartingElementForTraversUp(DOMElement originElement){
		// all top level elements should be scanned therefore if origin is document or origin parent is document 
		// then start from last child otherwise directly from previous sibling
		if(originElement.isDocumentElement()){
			return originElement.getLastChild();
		}else if(originElement.getParentElement() != null && originElement.getParentElement().isDocumentElement()){
			return originElement.getParentElement().getLastChild();
		}else{
			return originElement.getPreviousNonTextSibling();
		}
	}


	public boolean traversUpFromElement(DOMElement originElement, Predicate<DOMNode> shouldStop){
		if(parsed || originElement==null){
			return false;
		}
		DOMNode node = getStartingElementForTraversUp(originElement);
		while(node != null){
			if(node.isElement()){
				collector.add(new XSLElement((DOMElement) node));
				if(shouldStop.test(node)){
					LOGGER.log(Level.FINER,"Early exit for node {0}",node.getNodeName());
					parsed = true;
					return true;
				}
			}
			node = node.getPreviousNonTextSibling();
		}
		if(originElement.getParentElement() != null && !originElement.isChildOfOwnerDocument()){
			return traversUpFromElement(originElement.getParentElement(), shouldStop);
		}
		parsed = true;
		return false;
	}
 
	public boolean traversUpFromElementWithInclusions(DOMElement originElement){
		return traversUpFromElementWithInclusions(originElement, node->{return false;});
	}

	public boolean traversUpFromElementWithInclusions(DOMElement originElement, Predicate<DOMNode> shouldStop){
		return traversUpFromElementWithInclusions(originElement, shouldStop, new HashSet<String>());
	}

	public boolean traversUpFromElementWithInclusions(DOMElement originElement, Predicate<DOMNode> shouldStop,
			Set<String> visitedURIs){
		if(parsed || visitedURIs==null || visitedURIs.contains(getDocumentURI())){
			return false;
		}
		if(traversUpFromElement(originElement, shouldStop)){
			return true; // shouldStop yield true - no more parsing equired
		}
		visitedURIs.add(getDocumentURI());
		XSLElementList inclusions = collector.getElementsListByType(XSLElementType.INCLUDE);
		inclusions.addAll(collector.getElementsListByType(XSLElementType.IMPORT));
		for(int i = inclusions.size(); i-- >0;){ //reverse order to comply with xslt last template wins rule
			DOMDocument externalDocument = resolveIncludedURI(inclusions.get(i));
			if (externalDocument != null ){
				if(parseInclusion(externalDocument, shouldStop, visitedURIs)){
					return true; // shouldStop yield true - no more parsing equired
				}
			}
		}
		return false;
	}

	public boolean parseDocumentWithInclusions(){
		return parseDocumentWithInclusions(node->{return false;});
	}

	public boolean parseDocumentWithInclusions(Predicate<DOMNode> shouldStop){
		return parseDocumentWithInclusions(shouldStop, new HashSet<String>());
	}

	public boolean parseDocumentWithInclusions(Predicate<DOMNode> shouldStop, Set<String> visitedURIs){
		return traversUpFromElementWithInclusions(document.getDocumentElement(), shouldStop, visitedURIs);
	}

	private boolean parseInclusion(DOMDocument externalDocument, Predicate<DOMNode> shouldStop, Set<String> visitedURIs){
		LOGGER.log(Level.FINER, "parsing inclusion {0}", externalDocument.getDocumentURI());
		if (visitedURIs==null || visitedURIs.contains(externalDocument.getDocumentURI())) {
			return false;
		}
		XSLDocument dm = new XSLDocument(externalDocument);
		addIncludedXSLDocument(dm);
		if(dm.parseDocumentWithInclusions(shouldStop, visitedURIs)){
			return true; // shouldStop yield true - no more parsing equired
		}
		visitedURIs.add(dm.getDocumentURI());
		return false;
	}

	private void addIncludedXSLDocument(XSLDocument dm){
		if(includedXSLDocuments == null){
			includedXSLDocuments = new Vector<XSLDocument>();
		}
		includedXSLDocuments.add(dm);
	}

	public static String getIncludedURI(XSLElement inclusionElement){
		return inclusionElement.isInclusion() ? inclusionElement.getAttribute("href") : null;
	}

	public static DOMDocument resolveIncludedURI(XSLElement inclusionElement){
		return XSLUtils.resolveURI(inclusionElement.getDOMElement().getOwnerDocument(), getIncludedURI(inclusionElement));
	}


	public XSLElement getDefinition(XSLElementType type, String name){
		XSLElement def = collector.getElementByTypeName(type, name);
		if(def != null){
			return def;
		}
		if(includedXSLDocuments == null){
			return null;
		}
		for(int i = includedXSLDocuments.size(); i-- >0;){ //reverse order to comply with xslt last template wins rule
			def = includedXSLDocuments.get(i).getDefinition(type, name);
			if(def != null){
				return def;
			}
		}
		return null;
	}

	public void getDefinitions(XSLElementType type, XSLElementMap definitions){
		collector.getElementsMapByType(type).forEach(
			(URI,element)->definitions.putIfAbsent(URI,element)
		);
		if(includedXSLDocuments == null){
			return;
		}
		for(int i = includedXSLDocuments.size(); i-- >0;){ //reverse order to comply with xslt last template wins rule
			includedXSLDocuments.get(i).getDefinitions(type,definitions);
		}
	}

	public XSLElementMap getDefinitions(XSLElementType type){
		XSLElementMap definitions = new XSLElementMap();
		getDefinitions(type, definitions);
		return definitions;
	}

	private void getElements(XSLElementType type, XSLElementList elements){
		elements.addAll(collector.getElementsListByType(type));
		LOGGER.log(Level.FINER,"found elements ".concat(getDocumentURI()).concat(" : {0}"),elements.size());
		if(includedXSLDocuments==null){
			return;
		}
		for(int i = includedXSLDocuments.size(); i-- >0;){ //reverse order to comply with xslt last template wins rule
			includedXSLDocuments.get(i).getElements(type, elements);
		}
	}

	public XSLElementList getElements(XSLElementType type){
		XSLElementList elements = new XSLElementList();
		getElements(type, elements);
		return elements;
	}

	public static boolean isXSLStylesheet(DOMDocument document){
		if (document == null){
			return false;
		}
		DOMElement de = document.getDocumentElement();
		return XSLElement.isXSLElement(de) && "stylesheet".equals(de.getLocalName());
	}

}
