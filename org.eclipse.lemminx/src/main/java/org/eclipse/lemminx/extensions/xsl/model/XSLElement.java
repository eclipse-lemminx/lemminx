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


import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.xsl.*;
import org.w3c.dom.Element;


/**
 * XSL element model.
 */
public class XSLElement{

	public enum XSLElementType{
		INCLUDE,
		IMPORT,
		TEMPLATE_DEFINITION,
		TEMPLATE_CALL,
		NAMED_TEMPLATE_DEFINITION,
		NAMED_TEMPLATE_CALL,
		PARAM,
		VARIABLE,
		VARS,
		UNDEFINED,
		OTHER;

		public boolean isVariable(){
			return PARAM.equals(this) || VARIABLE.equals(this);
		}

		public boolean needsMap(){
			return NAMED_TEMPLATE_DEFINITION.equals(this) || VARS.equals(this) || this.isVariable();
		}
	}


	private DOMElement element;


	public XSLElement(DOMElement element){
		this.element = element;
	}

	public XSLElementType getType(){
		return getXSLElementType(element);
	}

	public boolean isInclude(){
		return isXSLInclude(element);
	}

	public boolean isImport(){
		return isXSLImport(element);
	}

	public boolean isInclusion(){
		return isXSLInclusion(element);
	}

	public boolean isTemplateDefinition(){
		return isXSLTemplateDefinition(element);
	}

	public boolean isNamedTemplateDefinition(){
		return isXSLNamedTemplateDefinition(element);
	}

	public boolean isTemplateCall(){
		return isXSLTemplateCall(element);
	}

	public boolean isVariable(){
		return isXSLVariable(element);
	}

	public boolean isParam(){
		return isXSLParam(element);
	}

	public boolean isVariableOrParam(){
		return isXSLVariableOrParam(element);
	}

	public String getAttribute(String name){
		return element.getAttribute(name);
	}

	public boolean hasAttribute(String name){
		return element.hasAttribute(name);
	}

	public DOMElement getDOMElement(){
		return this.element;
	}

	public static XSLElementType getXSLElementType(Element element){
		if(!isXSLElement(element)){
			return XSLElementType.UNDEFINED;
		}else if("include".equals(element.getLocalName())){
			return XSLElementType.INCLUDE;
		}else if("import".equals(element.getLocalName())){
			return XSLElementType.IMPORT;
		}else if("template".equals(element.getLocalName()) && element.hasAttribute("name")){
			return XSLElementType.NAMED_TEMPLATE_DEFINITION;
		}else if("template".equals(element.getLocalName())){
			return XSLElementType.TEMPLATE_DEFINITION;
		}else if("apply-templates".equals(element.getLocalName())){
			return XSLElementType.TEMPLATE_CALL;
		}else if("call-template".equals(element.getLocalName())){
			return XSLElementType.NAMED_TEMPLATE_CALL;
		}else if("param".equals(element.getLocalName())){
			return XSLElementType.PARAM;
		}else if("variable".equals(element.getLocalName())){
			return XSLElementType.VARIABLE;
		}else{
			return XSLElementType.OTHER;
		}
	}

	public static boolean isXSLElement(Element element){
		return element != null && XSLURIResolverExtension.XSL_NAMESPACE_URI.equals(element.getNamespaceURI());
	}

	public static boolean isXSLInclude(Element element){
		return getXSLElementType(element) == XSLElementType.INCLUDE;
	}

	public static boolean isXSLImport(Element element){
		return getXSLElementType(element) == XSLElementType.IMPORT;
	}

	public static boolean isXSLInclusion(Element element){
		return isXSLImport(element) || isXSLInclude(element);
	}

	public static boolean isXSLTemplateDefinition(Element element){
		return getXSLElementType(element) == XSLElementType.TEMPLATE_DEFINITION;
	}

	public static boolean isXSLNamedTemplateDefinition(Element element){
		return getXSLElementType(element) == XSLElementType.NAMED_TEMPLATE_DEFINITION;
	}

	public static boolean isXSLTemplateCall(Element element){
		return getXSLElementType(element) == XSLElementType.TEMPLATE_CALL;
	}

	public static boolean isXSLNamedTemplateCall(Element element){
		return getXSLElementType(element) == XSLElementType.NAMED_TEMPLATE_CALL;
	}

	public static boolean isXSLParam(Element element){
		return getXSLElementType(element) == XSLElementType.PARAM;
	}

	public static boolean isXSLVariable(Element element){
		return getXSLElementType(element) == XSLElementType.VARIABLE;
	}

	public static boolean isXSLVariableOrParam(Element element){
		return getXSLElementType(element) == XSLElementType.VARIABLE || getXSLElementType(element) == XSLElementType.PARAM;
	}

	public static boolean isXSLWithParam(Element element){
		return isXSLElement(element) && "with-param".equals(element.getLocalName());
	}

	public static boolean isXSLWithParamAttr(DOMAttr originAttr){
		return originAttr != null && isXSLWithParam(originAttr.getOwnerElement());
	}

	public static boolean isXSLNamedTemplateCallNameAttr(DOMAttr originAttr){
		return originAttr != null && "name".equals(originAttr.getLocalName())
			&& isXSLNamedTemplateCall(originAttr.getOwnerElement());
	}

	public static boolean isXSLAttribInCallTemplate(DOMAttr originAttr){
		return originAttr != null && isXSLNamedTemplateCall(originAttr.getOwnerElement());
	}

}
