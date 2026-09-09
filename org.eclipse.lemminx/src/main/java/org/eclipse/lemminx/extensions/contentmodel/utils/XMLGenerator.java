/**
 *  Copyright (c) 2018-2020 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.MarkupContentFactory;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLBuilder;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;

/**
 * XML generator used to generate an XML fragment with formatting from a given
 * element declaration (XML Schema element declaration, DTD element, etc).
 */
public class XMLGenerator {

	private final SharedSettings sharedSettings;
	private final String whitespacesIndent;
	private final String lineDelimiter;
	private final boolean canSupportSnippets;
	private final boolean autoCloseTags;
	private int maxLevel;
	private final DOMNode node;

	/**
	 * XML generator constructor.
	 * 
	 * @param sharedSettings     the settings containing formatting options (uses
	 *                           spaces or tabs for indentation, etc) and
	 *                           preferences
	 * @param whitespacesIndent  the whitespaces to use to indent XML children
	 *                           elements.
	 * @param lineDelimiter      the line delimiter to use when several XML elements
	 *                           must be generated.
	 * @param canSupportSnippets true if snippets can be supported and false
	 *                           otherwise.
	 */
	public XMLGenerator(SharedSettings sharedSettings, String whitespacesIndent, String lineDelimiter,
			boolean canSupportSnippets, int maxLevel) {
		this(sharedSettings, true, whitespacesIndent, lineDelimiter, canSupportSnippets, maxLevel, null);
	}

	public XMLGenerator(SharedSettings sharedSettings, boolean autoCloseTags, String whitespacesIndent,
			String lineDelimiter, boolean canSupportSnippets, int maxLevel, DOMNode node) {
		this.sharedSettings = sharedSettings;
		this.autoCloseTags = autoCloseTags;
		this.whitespacesIndent = whitespacesIndent;
		this.lineDelimiter = lineDelimiter;
		this.canSupportSnippets = canSupportSnippets;
		this.maxLevel = maxLevel;
		this.node = node;
	}

	public String generateMissingElements(ContentModelManager contentModelManager, DOMElement element,
			boolean generateOnlyRequired) {

		StringBuilder generatedXml = new StringBuilder();

		for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
			CMElementDeclaration matchesCMElement = cmDocument.findCMElement(element);
			if (matchesCMElement != null) {
				generatedXml
						.append(generate(matchesCMElement, null, true, true, 0,
								generateOnlyRequired));
			}
		}

		return generatedXml.toString();
	}

	/**
	 * Returns the line delimiter.
	 * 
	 * @return the line delimiter.
	 */
	public String getLineDelimiter() {
		return lineDelimiter;
	}

	/**
	 * Returns the whitespaces indentation.
	 * 
	 * @return the whitespaces indentation.
	 */
	public String getWhitespacesIndent() {
		return whitespacesIndent;
	}

	/**
	 * Returns the XML generated from the given element declaration.
	 * 
	 * @param elementDeclaration
	 * @param prefix
	 * @return the XML generated from the given element declaration.
	 */
	public String generate(CMElementDeclaration elementDeclaration, String prefix, boolean generateEndTag) {
		return generate(elementDeclaration, prefix, generateEndTag, false, 0, false);
	}

	/**
	 * Returns the XML generated from the given element declaration given option to
	 * generate only children and only required elements.
	 * 
	 * @param elementDeclaration
	 * @param prefix
	 * @param generateEndTag
	 * @param level                the level at which the first element will be
	 *                             generated at
	 * @param generateOnlyChildren true if only is children of the given element is
	 *                             to be generated
	 * @param generateOnlyRequired true if only the required elements is to be
	 *                             generated
	 * @return the XML generated from the given element declaration given option to
	 *         generate only children.
	 */
	public String generate(CMElementDeclaration elementDeclaration, String prefix, boolean generateEndTag,
			boolean generateOnlyChildren, int level, boolean generateOnlyRequired) {
		XMLBuilder xml = new XMLBuilder(sharedSettings, whitespacesIndent, lineDelimiter);
		generate(elementDeclaration, prefix, generateEndTag, generateOnlyChildren, level, 0, xml,
				new HashMap<CMElementDeclaration, List<CMElementDeclaration>>(), generateOnlyRequired);
		if (canSupportSnippets) {
			xml.addContent(SnippetsBuilder.tabstops(0)); // "$0"
		}
		if (!generateOnlyChildren && level > 0) {
			xml.linefeed();
			xml.indent(level - 1);
		}
		return xml.toString();
	}

	private int generate(CMElementDeclaration elementDeclaration, String prefix, boolean generateEndTag,
			boolean generateOnlyChildren, int level, int snippetIndex, XMLBuilder xml,
			HashMap<CMElementDeclaration, List<CMElementDeclaration>> generatedElements, boolean generateOnlyRequired) {
		Collection<CMElementDeclaration> children = generateOnlyRequired ? elementDeclaration.getRequiredElements()
				: elementDeclaration.getElements();
		if (generateOnlyChildren) {
			for (CMElementDeclaration child : children) {
				List<CMElementDeclaration> parents = generatedElements.get(child) == null ? new ArrayList<>()
						: generatedElements.get(child);
				parents.add(elementDeclaration);
				generatedElements.put(child, parents);
				snippetIndex = generate(child, prefix, true, false, level + 1, snippetIndex, xml, generatedElements,
						generateOnlyRequired);

			}
			xml.linefeed();
			xml.indent(level);
			return snippetIndex;
		}
		if (isAlreadyGenerated(elementDeclaration, children, generatedElements)) {
			return snippetIndex;
		}
		boolean autoCloseTags = this.autoCloseTags && generateEndTag;
		if (level > 0) {
			xml.linefeed();
			xml.indent(level);
		}
		xml.startElement(prefix, elementDeclaration.getLocalName(), false);
		// For elements with abstract types, add xsi:type attribute first with
		// derived type choices (e.g. xsi:type="${1|Teacher,Student|}")
		Collection<String> derivedTypeNames = elementDeclaration.getDerivedTypeNames();
		boolean hasXsiType = !derivedTypeNames.isEmpty();
		if (hasXsiType) {
			snippetIndex = generateXsiTypeAttribute(derivedTypeNames, prefix, level, snippetIndex, xml);
		}
		// Attributes
		Collection<CMAttributeDeclaration> attributes = elementDeclaration.getAttributes();
		snippetIndex = generate(attributes, level, snippetIndex, xml, elementDeclaration.getLocalName(), hasXsiType);
		// Elements children
		if (children.size() > 0) {
			xml.closeStartElement();
			if ((level < maxLevel)) {
				for (CMElementDeclaration child : children) {
					level++;
					List<CMElementDeclaration> parents = generatedElements.get(child) == null ? new ArrayList<>()
							: generatedElements.get(child);
					parents.add(elementDeclaration);
					generatedElements.put(child, parents);
					snippetIndex = generate(child, prefix, true, false, level, snippetIndex, xml, generatedElements,
							generateOnlyRequired);
					level--;
				}
				xml.linefeed();
				xml.indent(level);
			} else {
				if (generateEndTag && canSupportSnippets) {
					snippetIndex++;
					xml.addContent(SnippetsBuilder.tabstops(snippetIndex));
				}
			}
			if (generateEndTag) {
				xml.endElement(prefix, elementDeclaration.getLocalName());
			}
		} else if (elementDeclaration.isEmpty() && autoCloseTags && !hasXsiType) {
			// Don't self-close elements with abstract types: the concrete type
			// chosen via xsi:type may have child elements
			xml.selfCloseElement();
		} else {
			xml.closeStartElement();
			Collection<String> values = elementDeclaration.getEnumerationValues();
			if (!values.isEmpty()) {
				// The Element Text node has xs:enumeration.
				if (canSupportSnippets) {
					// Generate LSP choice.
					// Ex : <skill>${1|Java,Node,XML|}$2</skill>$0"
					snippetIndex++;
					xml.addContent(SnippetsBuilder.choice(snippetIndex, values));
				} else {
					// Generate the first item
					// Ex : <skill>Java</skill>"
					xml.addContent(values.iterator().next());
				}
			}
			if (canSupportSnippets) {
				snippetIndex++;
				xml.addContent(SnippetsBuilder.tabstops(snippetIndex));
			}
			if (generateEndTag) {
				xml.endElement(prefix, elementDeclaration.getLocalName());
			}
		}
		return snippetIndex;
	}

	public String generate(Collection<CMAttributeDeclaration> attributes, String tagName) {
		XMLBuilder xml = new XMLBuilder(sharedSettings, whitespacesIndent, lineDelimiter);
		generate(attributes, 0, 0, xml, tagName, false);
		return xml.toString();
	}

	private int generate(Collection<CMAttributeDeclaration> attributes, int level, int snippetIndex, XMLBuilder xml,
			String tagName, boolean hasExtraAttributes) {
		Map<String /* namespaceURI */, String /* prefix */> prefixes = null;
		List<CMAttributeDeclaration> requiredAttributes = new ArrayList<>();
		// Loop for attributes to collect :
		// - required attributes
		// - mapping between namespace / prefix for required attributes
		boolean generateXmlnsAttr = false;
		for (CMAttributeDeclaration att : attributes) {
			// required attributes
			if (att.isRequired()) {
				requiredAttributes.add(att);
				// mapping between namespace / prefix for attributes
				String namespace = att.getNamespace();
				if (!StringUtils.isEmpty(namespace)) {
					// Attribute has a namespace, get the prefix from the XML DOM document or from
					// the grammar.
					String prefix = prefixes != null ? prefixes.get(namespace) : null;
					if (prefix == null) {
						// Find the prefix from the DOM node
						prefix = findPrefixFromDOMNode(namespace);
						if (prefix == null) {
							// Find the prefix from the grammar
							prefix = att.getOwnerElementDeclaration().getPrefix(namespace);
							if (prefix != null) {
								if (!"xml".equals(prefix)) {
									// Generate an xmlns:prefix attribute to declare the namespace.
									xml.addAttribute("xmlns:" + prefix, namespace, level, true);
									generateXmlnsAttr = true;
								}
							}
						}
					}
					if (prefix != null) {
						if (prefixes == null) {
							prefixes = new HashMap<>();
						}
						prefixes.put(namespace, prefix);
					}
				}
			}
		}
		int attributesSize = requiredAttributes.size();
		for (CMAttributeDeclaration attributeDeclaration : requiredAttributes) {
			if (canSupportSnippets) {
				snippetIndex++;
			}
			String defaultValue = attributeDeclaration.getDefaultValue();
			Collection<String> enumerationValues = attributeDeclaration.getEnumerationValues();
			String value = generateAttributeValue(defaultValue, enumerationValues, canSupportSnippets, snippetIndex,
					false, sharedSettings);
			String attrName = attributeDeclaration.getName(prefixes);
			if (attributesSize != 1 || generateXmlnsAttr || hasExtraAttributes) {
				xml.addAttribute(attrName, value, level, true);
			} else {
				xml.addSingleAttribute(attrName, value, true);
			}
		}
		return snippetIndex;
	}

	/**
	 * Generates the xsi:type attribute with a snippet choice listing the given
	 * derived type names. Type names are qualified with the element's prefix
	 * when the element belongs to a namespace.
	 */
	private int generateXsiTypeAttribute(Collection<String> derivedTypeNames, String elementPrefix,
			int level, int snippetIndex, XMLBuilder xml) {
		if (canSupportSnippets) {
			snippetIndex++;
		}
		// Qualify type names with the element's prefix if it has a namespace
		Collection<String> qualifiedTypes;
		if (elementPrefix != null) {
			qualifiedTypes = new ArrayList<>();
			for (String typeName : derivedTypeNames) {
				qualifiedTypes.add(elementPrefix + ":" + typeName);
			}
		} else {
			qualifiedTypes = derivedTypeNames;
		}
		String value = generateAttributeValue(null, qualifiedTypes, canSupportSnippets, snippetIndex,
				false, sharedSettings);
		xml.addSingleAttribute("xsi:type", value, true);
		return snippetIndex;
	}

	/**
	 * Returns true if the given element has already been generated.
	 * 
	 * @param elementDeclaration the element to be checked
	 * @param children           the children of the element
	 * @param generatedElements  the list containing generated elements
	 * @return
	 */
	private boolean isAlreadyGenerated(CMElementDeclaration elementDeclaration,
			Collection<CMElementDeclaration> children,
			HashMap<CMElementDeclaration, List<CMElementDeclaration>> generatedElements) {
		for (CMElementDeclaration child : children) {
			if (generatedElements.get(child) != null
					&& generatedElements.get(child).contains(elementDeclaration)) {
				return true;
			}
		}
		return false;
	}

	private String findPrefixFromDOMNode(String namespace) {
		if (node == null) {
			return null;
		}
		DOMElement element = null;
		if (node.isAttribute()) {
			element = ((DOMAttr) node).getOwnerElement();
		} else if (node.isElement()) {
			element = (DOMElement) node;
		} else if (node.isText()) {
			element = node.getParentElement();
		}
		if (element != null) {
			return element.getPrefix(namespace);
		}
		return null;
	}

	/**
	 * Creates the string value for a CompletionItem TextEdit
	 * 
	 * Can create an enumerated TextEdit if given a collection of values.
	 */
	public static String generateAttributeValue(String defaultValue, Collection<String> enumerationValues,
			boolean canSupportSnippets, int snippetIndex, boolean withQuote, SharedSettings sharedSettings) {
		StringBuilder value = new StringBuilder();
		String quotation = sharedSettings.getPreferences().getQuotationAsString();
		if (withQuote) {
			value.append("=").append(quotation);
		}
		if (!canSupportSnippets) {
			if (defaultValue != null) {
				value.append(defaultValue);
			}
		} else {
			// Snippets syntax support
			if (enumerationValues != null && !enumerationValues.isEmpty()) {
				SnippetsBuilder.choice(snippetIndex, enumerationValues, value);
			} else {
				if (defaultValue != null) {
					SnippetsBuilder.placeholders(snippetIndex, defaultValue, value);
				} else {
					SnippetsBuilder.tabstops(snippetIndex, value);
				}
			}
		}
		if (withQuote) {
			value.append(quotation);
			if (canSupportSnippets) {
				SnippetsBuilder.tabstops(0, value); // "$0"
			}
		}
		return value.toString();
	}

	/**
	 * Returns a properly formatted documentation string with source.
	 * 
	 * If there is no content then null is returned.
	 * 
	 * @param documentation
	 * @param schemaURI
	 * @return
	 */
	public static String generateDocumentation(String documentation, String schemaURI, boolean html) {
		if (StringUtils.isBlank(documentation)) {
			return null;
		}
		StringBuilder doc = new StringBuilder(documentation);
		if (schemaURI != null) {
			doc.append(System.lineSeparator());
			doc.append(System.lineSeparator());
			if (html) {
				doc.append("<p>");
			}
			doc.append("Source: ");
			if (html) {
				doc.append("<a href=\"");
				doc.append(schemaURI);
				doc.append("\">");
			}
			doc.append(getFileName(schemaURI));
			if (html) {
				doc.append("</a>");
				doc.append("</p>");
			}
		}
		return doc.toString();
	}

	/**
	 * Returns the file name from the given schema URI
	 * 
	 * @param schemaURI the schema URI
	 * @return the file name from the given schema URI
	 */
	private static String getFileName(String schemaURI) {
		int index = schemaURI.lastIndexOf('/');
		if (index == -1) {
			index = schemaURI.lastIndexOf('\\');
		}
		if (index == -1) {
			return schemaURI;
		}
		return schemaURI.substring(index + 1, schemaURI.length());
	}

	/**
	 * Returns a markup content for element documentation and null otherwise.
	 * 
	 * @param cmElement
	 * @param support
	 * @return a markup content for element documentation and null otherwise.
	 */
	public static MarkupContent createMarkupContent(CMElementDeclaration cmElement, ISharedSettingsRequest support) {
		String documentation = XMLGenerator.generateDocumentation(cmElement.getDocumentation(support),
				cmElement.getDocumentURI(), support.canSupportMarkupKind(MarkupKind.MARKDOWN));
		if (documentation != null) {
			return MarkupContentFactory.createMarkupContent(documentation, MarkupKind.MARKDOWN, support);
		}
		return null;
	}

	/**
	 * Returns a markup content for attribute name documentation and null otherwise.
	 * 
	 * @param cmAttribute  the attribute declaration
	 * @param ownerElement the owner element declaration
	 * @param request      the request
	 * @return a markup content for attribute name documentation and null otherwise.
	 */
	public static MarkupContent createMarkupContent(CMAttributeDeclaration cmAttribute,
			CMElementDeclaration ownerElement, ISharedSettingsRequest request) {
		String documentation = XMLGenerator.generateDocumentation(cmAttribute.getAttributeNameDocumentation(request),
				ownerElement.getDocumentURI(), request.canSupportMarkupKind(MarkupKind.MARKDOWN));
		if (documentation != null) {
			return MarkupContentFactory.createMarkupContent(documentation, MarkupKind.MARKDOWN, request);
		}
		return null;
	}

	/**
	 * Returns a markup content for attribute value documentation and null
	 * otherwise.
	 * 
	 * @param cmAttribute
	 * @param attributeValue
	 * @param ownerElement
	 * @param support
	 * @return a markup content for attribute value documentation and null
	 *         otherwise.
	 */
	public static MarkupContent createMarkupContent(CMAttributeDeclaration cmAttribute, String attributeValue,
			CMElementDeclaration ownerElement, ISharedSettingsRequest support) {
		String documentation = XMLGenerator.generateDocumentation(
				cmAttribute.getAttributeValueDocumentation(attributeValue, support), ownerElement.getDocumentURI(),
				support.canSupportMarkupKind(MarkupKind.MARKDOWN));
		if (documentation != null) {
			return MarkupContentFactory.createMarkupContent(documentation, MarkupKind.MARKDOWN, support);
		}
		return null;
	}

	/**
	 * Returns a markup content for element text documentation and null otherwise.
	 * 
	 * @param cmElement   element declaration.
	 * @param textContent the text content.
	 * @param support     markup kind support.
	 * 
	 * @return a markup content for element text documentation and null otherwise.
	 */
	public static MarkupContent createMarkupContent(CMElementDeclaration cmElement, String textContent,
			ISharedSettingsRequest support) {
		String documentation = XMLGenerator.generateDocumentation(cmElement.getTextDocumentation(textContent, support),
				cmElement.getDocumentURI(), support.canSupportMarkupKind(MarkupKind.MARKDOWN));
		if (documentation != null) {
			return MarkupContentFactory.createMarkupContent(documentation, MarkupKind.MARKDOWN, support);
		}
		return null;
	}
}
