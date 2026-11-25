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

package org.eclipse.lemminx.extensions.xsl.utils;

import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.logging.*;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.xsl.model.XSLDocument;
import org.eclipse.lemminx.extensions.xsl.model.XSLElement;
import org.eclipse.lemminx.extensions.xsl.model.XSLElementMap;
import org.eclipse.lemminx.extensions.xsl.model.XSLElement.XSLElementType;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.URIUtils;


/**
 * XSL utilities.
 */
public class XSLUtils {

	private static final Logger LOGGER = Logger.getLogger(XSLUtils.class.getName());
	private static final Predicate<Character> VARIABLE_NAME_PREDICATE = ch -> {
		return ch == '_' || ch == ':' || ch == '.' || ch == '-' || Character.isLetterOrDigit(ch);
	};

	public static DOMDocument resolveURI(DOMDocument base, String uri){
		URIResolverExtensionManager resolverExtensionManager = base.getResolverExtensionManager();
		String resourceURI = resolverExtensionManager.resolve(base.getDocumentURI(), null, uri);
		return (URIUtils.isFileResource(resourceURI)) ?
			(DOMDocument) DOMUtils.loadDocument(resourceURI, base.getResolverExtensionManager()) :null;
	}

	public static XSLElement getDefinitionForReference(DOMElement origin, XSLElementType type,  String name, 
			Predicate<DOMNode> shouldStop){
		
		XSLDocument dm = new XSLDocument(origin.getOwnerDocument());
		dm.parseDocumentWithInclusions(shouldStop);
		return dm.getDefinition(type,name);
	}

	public static XSLElement findNamedTemplateDefinition(DOMElement callTemplate){
		if(callTemplate==null || !callTemplate.hasAttribute("name")){
			return null;
		}
		return getDefinitionForReference(
				callTemplate, 
				XSLElementType.NAMED_TEMPLATE_DEFINITION, 
				callTemplate.getAttribute("name"),
				node->{
					return node != null && node.isElement() && XSLElement.isXSLNamedTemplateDefinition((DOMElement)node) 
						&& callTemplate.getAttribute("name").equals(node.getAttribute("name"));
				}
		);
	}
	
	public static XSLElement findVariableDefinition(DOMAttr originAttrib, String name){
		if(originAttrib==null){
			return null;
		}
		return getDefinitionForReference(
				originAttrib.getOwnerElement(), 
				XSLElementType.VARS, 
				name, 
				node->{
					return node != null && node.isElement() && XSLElement.isXSLVariableOrParam((DOMElement)node) 
						&& name.equals(node.getAttribute("name"));
				}
			);
	}
	
	public static int getVariableReferenceStartOffset(String text, int offset) {// based on getEntityReferenceStartOffset
		// adjust offset to get the left character of the offset
		offset--;
		if (offset < 0) {
			// case where offset is on the first character
			return -1;
		}
		if (text.charAt(offset) == '$') {
			// case with $|abcd
			return offset;
		}
		if (offset == 0) {
			// case with a|bcd -> there are no '$'
			return -1;
		}
		int startVariableOffset = StringUtils.findStartWord(text, offset, VARIABLE_NAME_PREDICATE);
		if (startVariableOffset <= 0) {
			return -1;
		}
		// check if the left character is '$'
		if (text.charAt(startVariableOffset - 1) != '$') {
			return -1;
		}
		return startVariableOffset - 1;
	}

	public static int getVariableReferenceEndOffset(String text, int offset) {
		int endVariableOffset = StringUtils.findEndWord(text, offset, VARIABLE_NAME_PREDICATE);
		if(endVariableOffset == -1 && endVariableOffset >= text.length()) {
			return -1;
		}
		return endVariableOffset;
	}

	public static String getVariableNameAtOffset(String text, int offset){
		int s = getVariableReferenceStartOffset(text, offset);
		if(s == -1){
			return null;
		}
		int e = getVariableReferenceEndOffset(text, offset);
		if(e == -1){
			return null;
		}
		return text.substring(s+1, e);
	}

	public static String getVariableNameFromPrefix(String prefix){
		int so = getVariableReferenceStartOffset(prefix, prefix.length());
		if(so==-1){
			return null;
		}else{
			return prefix.substring(so);
		}
	}

	public static XSLElementMap getNamedElementsDefinitions(XSLElementType type ,DOMAttr originAttr){
		XSLDocument dm = new XSLDocument(originAttr.getOwnerDocument());
		dm.traversUpFromElementWithInclusions(originAttr.getOwnerElement());
		return dm.getDefinitions(type);
	}

	public static XSLElementMap getNamedTemplatesDefinitions(DOMAttr originAttr){
		return getNamedElementsDefinitions(XSLElementType.NAMED_TEMPLATE_DEFINITION, originAttr);
	}

	public static XSLElementMap getVarsDefinitions(DOMAttr originAttr){
		return getNamedElementsDefinitions(XSLElementType.VARS, originAttr);
	}

	public static Stream<XSLElement> getDefinedModes(DOMAttr originAttr){
		XSLDocument dm = new XSLDocument(originAttr.getOwnerDocument());
		dm.parseDocumentWithInclusions();
		return dm.getElements(XSLElementType.TEMPLATE_DEFINITION).stream().filter(e->{
			LOGGER.log(Level.FINEST,"filtering element {0}",e);
			return e!=null && e.hasAttribute("mode") && !e.getAttribute("mode").isEmpty();
		});
	}

	public static Stream<XSLElement> getModeDefinitions(DOMAttr originAttr){
		LOGGER.log(Level.FINE,"finding definitions for mode: {0}",originAttr.getValue());
		return getDefinedModes(originAttr).filter(m->{
			return originAttr.getValue().equals(m.getAttribute("mode"));
		});
	}
}


