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

import java.util.function.BiConsumer;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.ICompletionResponse;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.extensions.xsl.*;
import org.eclipse.lemminx.extensions.xsl.model.*;
import org.eclipse.lemminx.extensions.xsl.utils.*;
import org.eclipse.lsp4j.*;

public class XSLCompletionParticipant extends CompletionParticipantAdapter {

	@Override
	public void onAttributeValue(String valuePrefix, ICompletionRequest request, ICompletionResponse response)
		throws Exception {
		DOMNode node = request.getNode();
		DOMDocument document = node.getOwnerDocument();
		if (!DOMUtils.isXSL(document)) {
			return;
		}
		Range fullRange = request.getReplaceRange();
		DOMAttr originAttr = node.findAttrAt(request.getOffset());
		findSuggestionsForAttribute(valuePrefix, originAttr, (attrName, element) -> {
			CompletionItem item = new CompletionItem();
			item.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, makeNamedTemplateDocumentation(element)));
			String value = element.getAttribute(attrName);
			String insertText = request.getInsertAttrValue(value);
			item.setLabel(value);
			item.setKind(CompletionItemKind.Value);
			item.setFilterText(insertText);
			item.setTextEdit(new TextEdit(fullRange, insertText));
			response.addCompletionItem(item);
		});
	}


	private void findSuggestionsForAttribute(String valuePrefix, DOMAttr originAttr, BiConsumer<String, DOMElement> collector){
		String variableName = XSLUtils.getVariableNameFromPrefix(valuePrefix);
		if(variableName != null){
			collectForVariableName(variableName, originAttr, collector);
		}else if(XSLElement.isXSLNamedTemplateCallNameAttr(originAttr)){ // template name completon
			collectForTemplateName(originAttr, collector);	
		}else if("name".equals(originAttr.getLocalName()) 
				&& XSLElement.isXSLWithParamAttr(originAttr)){ //with-param name completion
			collectForTemplateParamName(
					XSLUtils.findNamedTemplateDefinition(originAttr.getOwnerElement().getParentElement()), collector);
		}else if("mode".equals(originAttr.getLocalName()) 
				&& XSLElement.isXSLTemplateCall(originAttr.getOwnerElement())){ //apply-templates mode completion
			collectForApplyTemplatesMode(originAttr, collector);
		}
	}


	private void collectForVariableName(String variable, DOMAttr originAttr, BiConsumer<String, DOMElement> collector){
		XSLUtils.getVarsDefinitions(originAttr).forEach((k, n) -> {
			collector.accept("name", n.getDOMElement());
		});
	}

	private void collectForTemplateName(DOMAttr originAttr, BiConsumer<String, DOMElement> collector){
		XSLUtils.getNamedTemplatesDefinitions(originAttr).forEach((k, n) -> {
			collector.accept("name", n.getDOMElement());
		});
	}

	private void collectForApplyTemplatesMode(DOMAttr originAttr, BiConsumer<String, DOMElement> collector){
		XSLUtils.getDefinedModes(originAttr).forEach(n -> {
			collector.accept("mode", n.getDOMElement());
		});
	}

	private void collectForTemplateParamName(XSLElement template, BiConsumer<String, DOMElement> collector){
		if(template == null){
			return;
		}
		template.getDOMElement().getChildren().forEach(element->{
			if(element.isElement() && "param".equals(element.getLocalName())){
				collector.accept(XSLURIResolverExtension.XSL_NAMESPACE_URI, (DOMElement)element);
			}
		});
	}

	private static String makeNamedTemplateDocumentation(DOMElement element){
		StringBuilder doc = new StringBuilder();
		doc.append("Location: ");
		doc.append(element.getOwnerDocument().getDocumentURI());
		return doc.toString();
	}

}
