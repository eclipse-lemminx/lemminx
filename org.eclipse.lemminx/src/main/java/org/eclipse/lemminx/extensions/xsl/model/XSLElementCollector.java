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
import org.eclipse.lemminx.extensions.xsl.model.XSLElement.XSLElementType;

/**
 * XSL element collector - holds collections of parsed and recognized XSL elements.
 *
 */
public class XSLElementCollector{

	private HashMap<XSLElementType,IXSLElementCollection> elements = new HashMap<>();


	private IXSLElementCollection produceCollection(XSLElementType type){
		if(type.needsMap()){
			return new XSLElementMap();
		}else{
			return new XSLElementList();
		}
	}
	
	private IXSLElementCollection getCollection(XSLElementType type){
		if(elements.get(type) == null){
			elements.put(type, produceCollection(type));
		}
		return elements.get(type);
	}

	public boolean add(XSLElement element){
		if(element.getType().isVariable()){
			getCollection(XSLElementType.VARS).add(element);
		}
		return getCollection(element.getType()).add(element);
	}

	public XSLElementList getElementsListByType(XSLElementType type){
		return getCollection(type) instanceof XSLElementList ? (XSLElementList)getCollection(type) : null;
	}

	public XSLElementMap getElementsMapByType(XSLElementType type){
		return getCollection(type) instanceof XSLElementMap ? (XSLElementMap)getCollection(type) : null;
	}

	public XSLElement getElementByTypeName(XSLElementType type, String name){
		if(getCollection(type) instanceof XSLElementMap && ((XSLElementMap)getCollection(type)).containsKey(name)){
			return ((XSLElementMap)getCollection(type)).get(name);
		}else{
			return null;
		}
	}
}
