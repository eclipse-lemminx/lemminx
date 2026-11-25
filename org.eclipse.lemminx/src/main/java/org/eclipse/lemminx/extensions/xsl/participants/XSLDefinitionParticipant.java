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

package org.eclipse.lemminx.extensions.xsl.participants;

import java.util.List;
import java.util.logging.*;

import org.eclipse.lemminx.extensions.xsl.model.*;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.xsl.utils.XSLUtils;
import org.eclipse.lemminx.services.extensions.AbstractDefinitionParticipant;
import org.eclipse.lemminx.services.extensions.IDefinitionRequest;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

public class XSLDefinitionParticipant extends AbstractDefinitionParticipant {

	private static final Logger LOGGER = Logger.getLogger(XSLDefinitionParticipant.class.getName());

	@Override
	protected boolean match(DOMDocument document) {
		return DOMUtils.isXSL(document);
	}

	@Override
	protected void doFindDefinition(IDefinitionRequest request, List<LocationLink> locations,
			CancelChecker cancelChecker) {

		// - xsl:template name="###"
		DOMNode node = request.getNode();
		if (node.isAttribute()){ //in attribute
			DOMAttr originAttr = (DOMAttr)node;
			String n = XSLUtils.getVariableNameAtOffset(request.getXMLDocument().getText(), request.getOffset());
			if(n!=null){
				findVariableDeclaration(originAttr, n, locations);
			}else if(XSLElement.isXSLNamedTemplateCallNameAttr(originAttr)) {
				//in name attribute of call-template element - goto named-template definition
				findNamedTemplateDefinition(originAttr.getOwnerElement(),locations);
			}else if (XSLElement.isXSLWithParam(originAttr.getOwnerElement())) {
				//in attribute of with-param element nested in call-template element - goto named-template definition
				findNamedTemplateDefinition(originAttr.getOwnerElement().getParentElement(),locations);
			}else if (XSLElement.isXSLInclusion(originAttr.getOwnerElement())) {
				//in attribute of include/import element - do to included file
				findInclusion(originAttr.getOwnerElement(),locations);
			}else if(XSLElement.isXSLTemplateCall(originAttr.getOwnerElement()) && "mode".equals(originAttr.getLocalName())){
				//in mode attribute of apply-templates element - find all definitions with that mode
				findTemplateDefinitionForMode(originAttr,locations);
			}
		}else if(node.isElement()){
			DOMElement originElement = (DOMElement)node;
			if(XSLElement.isXSLNamedTemplateCall(originElement)){
				//in named-template element - goto named-template definitiont
				findNamedTemplateDefinition(originElement,locations);
			}else if(XSLElement.isXSLWithParam(originElement)){
				//in with-param element nested in call-template element - goto named-template definition
				findNamedTemplateDefinition(originElement.getParentElement(),locations);
			}else if(XSLElement.isXSLInclusion(originElement)){
				//in include or import element - goto included document
				findInclusion((DOMElement)node , locations);
			}
		}
	}

	private void findNamedTemplateDefinition(DOMElement callTemplate, List<LocationLink> locations){
		if(callTemplate == null){
			return;
		}
		LOGGER.log(Level.FINE, "searching for definition of {0}", callTemplate.getAttribute("name"));
		XSLElement template = XSLUtils.findNamedTemplateDefinition(callTemplate);
		if(template != null){
			LocationLink location = XMLPositionUtility.createLocationLink(
				callTemplate,//.getAttributeNode("name").getNodeAttrValue(), 
				template.getDOMElement().getAttributeNode("name").getNodeAttrValue());
			locations.add(location);
		};
	}

	private void findInclusion(DOMElement inclusion, List<LocationLink> locations){
		DOMDocument targetDoc = XSLDocument.resolveIncludedURI(new XSLElement(inclusion));
		if(targetDoc != null){
			LocationLink location = XMLPositionUtility.createLocationLink(inclusion, targetDoc.getDocumentElement());
			locations.add(location);
		}
	}

	private void findVariableDeclaration(DOMAttr originAttr, String varName, List<LocationLink> locations){
		XSLElement template = XSLUtils.findVariableDefinition(originAttr,varName);
		if(template != null){
			LocationLink location = XMLPositionUtility.createLocationLink(
				originAttr,//.getAttributeNode("name").getNodeAttrValue(), 
				template.getDOMElement().getAttributeNode("name").getNodeAttrValue());
			locations.add(location);
		};
	}

	private void findTemplateDefinitionForMode(DOMAttr originAttr, List<LocationLink> locations){
		XSLUtils.getModeDefinitions(originAttr).forEach(n->{
			LocationLink location = XMLPositionUtility.createLocationLink(
				originAttr,
				n.getDOMElement().getAttributeNode("mode").getNodeAttrValue());
			locations.add(location);
		});
	}

}
