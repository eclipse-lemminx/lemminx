/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLBuilder;

/**
 * Generates a complete XML document from a grammar (XSD, DTD, RelaxNG, RNC).
 *
 * <p>
 * This generator creates a well-formed XML document that includes:
 * </p>
 * <ul>
 * <li>XML declaration ({@code <?xml version="1.0" encoding="UTF-8"?>})</li>
 * <li>Grammar binding depending on the grammar type:
 * <ul>
 * <li>XSD: {@code xsi:schemaLocation} or
 * {@code xsi:noNamespaceSchemaLocation}</li>
 * <li>DTD: {@code <!DOCTYPE root SYSTEM "uri">}</li>
 * <li>RelaxNG/RNC: {@code <?xml-model href="uri"?>}</li>
 * </ul>
 * </li>
 * <li>XML body with required elements and attributes generated via
 * {@link XMLElementGenerator}</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * XMLDocumentGenerator generator = new XMLDocumentGenerator(sharedSettings);
 * String xml = generator.generate(cmDocument, grammarURI, "rootElement");
 * </pre>
 */
public class XMLDocumentGenerator {

	private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	private final SharedSettings sharedSettings;

	private final XMLGenerationSettings generationSettings;

	/**
	 * Creates a new XML document generator with default generation settings.
	 *
	 * @param sharedSettings the shared settings containing formatting options
	 *                       (tab size, insert spaces, etc.).
	 */
	public XMLDocumentGenerator(SharedSettings sharedSettings) {
		this(sharedSettings, new XMLGenerationSettings());
	}

	/**
	 * Creates a new XML document generator with the given generation settings.
	 *
	 * @param sharedSettings     the shared settings containing formatting options.
	 * @param generationSettings the generation settings (maxDepth, optionalElements,
	 *                           etc.).
	 */
	public XMLDocumentGenerator(SharedSettings sharedSettings, XMLGenerationSettings generationSettings) {
		this.sharedSettings = sharedSettings;
		this.generationSettings = generationSettings;
	}

	/**
	 * Generates a complete XML document using the first root element declared in the
	 * grammar.
	 *
	 * @param cmDocument the content model document loaded from the grammar.
	 * @param grammarURI the grammar file URI (used for grammar binding).
	 * @return the generated XML document as a string, or empty string if the grammar
	 *         has no elements.
	 */
	public String generate(CMDocument cmDocument, String grammarURI) {
		if (cmDocument == null) {
			return "";
		}
		// Use the first declared element that belongs to this schema (skip imported elements)
		String schemaNamespace = cmDocument.getNamespace();
		for (CMElementDeclaration element : cmDocument.getElements()) {
			if (schemaNamespace == null || schemaNamespace.equals(element.getNamespace())) {
				return generate(cmDocument, grammarURI, element);
			}
		}
		return "";
	}

	/**
	 * Generates a complete XML document from the given grammar with the specified
	 * root element name.
	 *
	 * @param cmDocument      the content model document loaded from the grammar.
	 * @param grammarURI      the grammar file URI (used for grammar binding).
	 * @param rootElementName the local name of the root element to generate.
	 * @return the generated XML document as a string, or empty string if the root
	 *         element is not found.
	 */
	public String generate(CMDocument cmDocument, String grammarURI, String rootElementName) {
		if (cmDocument == null) {
			return "";
		}

		// Find the root element declaration by name
		CMElementDeclaration rootElement = findRootElement(cmDocument, rootElementName);
		if (rootElement == null) {
			return "";
		}

		return generate(cmDocument, grammarURI, rootElement);
	}

	/**
	 * Generates a complete XML document from the given grammar and root element
	 * declaration.
	 *
	 * @param cmDocument  the content model document loaded from the grammar.
	 * @param grammarURI  the grammar file URI (used for grammar binding).
	 * @param rootElement the root element declaration to generate.
	 * @return the generated XML document as a string.
	 */
	private String generate(CMDocument cmDocument, String grammarURI, CMElementDeclaration rootElement) {
		String lineDelimiter = System.lineSeparator();

		XMLElementGenerator generator = new XMLElementGenerator(sharedSettings,
				"", lineDelimiter,
				false, generationSettings.getMaxDepth());
		generator.setUseTypeDefaults(generationSettings.isTypeDefaults());
		generator.setForDocumentGeneration(true);

		// Collect namespace prefixes for child elements from imported schemas
		String rootNamespace = rootElement.getNamespace();
		Map<String, String> namespacePrefixes = collectNamespacePrefixes(rootElement, rootNamespace);
		if (!namespacePrefixes.isEmpty()) {
			generator.setNamespacePrefixes(rootNamespace, namespacePrefixes);
		}

		boolean generateOnlyRequired = !generationSettings.isOptionalElements();
		String xmlContent = generator.generate(rootElement, null, true, false, 0, generateOnlyRequired);

		// Add grammar binding (schemaLocation, DOCTYPE, or xml-model) to the root element
		xmlContent = addGrammarBinding(xmlContent, grammarURI, cmDocument, rootElement, namespacePrefixes,
				lineDelimiter);

		// Assemble the final document: XML declaration + body
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xml.append(lineDelimiter);
		xml.append(xmlContent);
		xml.append(lineDelimiter);

		return xml.toString();
	}

	/**
	 * Collects namespace prefixes for child elements whose namespace differs from
	 * the root namespace. Traverses the element tree recursively.
	 */
	private Map<String, String> collectNamespacePrefixes(CMElementDeclaration element, String rootNamespace) {
		Map<String, String> prefixes = new LinkedHashMap<>();
		collectNamespaces(element, rootNamespace, prefixes, new HashSet<>());
		return prefixes;
	}

	private void collectNamespaces(CMElementDeclaration element, String rootNamespace,
			Map<String, String> prefixes, Set<CMElementDeclaration> visited) {
		if (visited.contains(element)) {
			return;
		}
		visited.add(element);
		for (CMElementDeclaration child : element.getElements()) {
			String childNs = child.getNamespace();
			if (childNs != null && !childNs.equals(rootNamespace) && !prefixes.containsKey(childNs)) {
				prefixes.put(childNs, derivePrefix(childNs));
			}
			collectNamespaces(child, rootNamespace, prefixes, visited);
		}
	}

	private static String derivePrefix(String namespace) {
		int lastSlash = namespace.lastIndexOf('/');
		if (lastSlash >= 0 && lastSlash < namespace.length() - 1) {
			return namespace.substring(lastSlash + 1);
		}
		return "ns1";
	}

	/**
	 * Adds the appropriate grammar binding to the generated XML content based on the
	 * grammar file extension.
	 *
	 * @param xmlContent         the generated XML body.
	 * @param grammarURI         the grammar file URI.
	 * @param cmDocument         the content model document (used for XSD target
	 *                           namespace).
	 * @param rootElement        the root element declaration.
	 * @param namespacePrefixes  mapping from imported namespace URI to prefix.
	 * @param lineDelimiter      the line delimiter to use.
	 * @return the XML content with grammar binding added.
	 */
	private String addGrammarBinding(String xmlContent, String grammarURI,
			CMDocument cmDocument, CMElementDeclaration rootElement,
			Map<String, String> namespacePrefixes, String lineDelimiter) {
		String rootName = rootElement.getLocalName();
		// Insert grammar attributes right after the root element name in the start tag
		int insertPos = xmlContent.indexOf(rootName) + rootName.length();
		String lowerURI = grammarURI.toLowerCase();

		if (lowerURI.endsWith(".xsd")) {
			// XSD: add xmlns, xmlns:xsi, and xsi:schemaLocation or xsi:noNamespaceSchemaLocation
			return addXSDBinding(xmlContent, grammarURI, cmDocument, rootElement, namespacePrefixes, insertPos, lineDelimiter);
		}
		if (lowerURI.endsWith(".dtd")) {
			// DTD: add <!DOCTYPE root SYSTEM "uri"> before the root element
			return addDTDBinding(xmlContent, grammarURI, rootName, lineDelimiter);
		}
		// RelaxNG (.rng) and RelaxNG Compact (.rnc): add <?xml-model?> processing instruction
		// Add namespace binding if the root element has a namespace (from RelaxNG ns="...")
		String rootNs = rootElement.getNamespace();
		if (!StringUtils.isEmpty(rootNs)) {
			xmlContent = addNamespaceBinding(xmlContent, rootNs, namespacePrefixes, insertPos, lineDelimiter);
		}
		return addXMLModelBinding(xmlContent, grammarURI, lineDelimiter);
	}

	/**
	 * Adds XSD binding attributes to the root element.
	 * <p>
	 * For schemas with a target namespace:
	 * {@code xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns uri"}
	 * </p>
	 * <p>
	 * For schemas without a target namespace:
	 * {@code xmlns:xsi="..." xsi:noNamespaceSchemaLocation="uri"}
	 * </p>
	 * <p>
	 * When the root element's namespace differs from the schema's target namespace
	 * (e.g., element defined in an imported schema), the element's namespace is
	 * used for {@code xmlns} and the schema's target namespace for
	 * {@code xsi:schemaLocation}.
	 * </p>
	 */
	private String addXSDBinding(String xmlContent, String grammarURI,
			CMDocument cmDocument, CMElementDeclaration rootElement,
			Map<String, String> namespacePrefixes, int insertPos, String lineDelimiter) {
		XMLBuilder attrs = new XMLBuilder(sharedSettings, "", lineDelimiter);
		int lineStart = xmlContent.lastIndexOf('\n', insertPos - 1);
		attrs.setInitialLineWidth(lineStart < 0 ? insertPos : insertPos - lineStart - 1);
		String elementNamespace = rootElement.getNamespace();
		String schemaNamespace = cmDocument.getNamespace();
		boolean hasElementNamespace = !StringUtils.isEmpty(elementNamespace);
		boolean hasSchemaNamespace = !StringUtils.isEmpty(schemaNamespace);

		// Add default namespace declaration for the root element's namespace
		if (hasElementNamespace) {
			attrs.addAttribute("xmlns", elementNamespace, 0, true);
		}

		// Only add xmlns:xsi if not already declared (e.g. by xsi:type generation
		// for elements with abstract types)
		if (!xmlContent.contains("xmlns:xsi")) {
			attrs.addAttribute("xmlns:xsi", XSI_NAMESPACE, 0, true);
		}

		// Add xmlns:prefix declarations for imported namespaces
		for (Map.Entry<String, String> entry : namespacePrefixes.entrySet()) {
			attrs.addAttribute("xmlns:" + entry.getValue(), entry.getKey(), 0, true);
		}

		// Add schema location attribute using the schema's target namespace
		// (not the element's namespace, which may differ for imported elements)
		if (hasSchemaNamespace) {
			attrs.addAttribute("xsi:schemaLocation", schemaNamespace + " " + grammarURI, 0, true);
		} else if (hasElementNamespace) {
			attrs.addAttribute("xsi:schemaLocation", elementNamespace + " " + grammarURI, 0, true);
		} else {
			attrs.addAttribute("xsi:noNamespaceSchemaLocation", grammarURI, 0, true);
		}

		return xmlContent.substring(0, insertPos) + attrs + xmlContent.substring(insertPos);
	}

	/**
	 * Adds a DOCTYPE declaration before the root element for DTD grammars.
	 * <p>
	 * Example: {@code <!DOCTYPE note SYSTEM "note.dtd">}
	 * </p>
	 */
	private String addDTDBinding(String xmlContent, String grammarURI,
			String rootName, String lineDelimiter) {
		return "<!DOCTYPE " + rootName + " SYSTEM \"" + grammarURI + "\">" + lineDelimiter + xmlContent;
	}

	/**
	 * Adds namespace binding attributes ({@code xmlns="..."} and any imported
	 * namespace prefixes) to the root element. Used for grammars like RelaxNG
	 * where the namespace is not part of the grammar binding mechanism.
	 */
	private String addNamespaceBinding(String xmlContent, String namespace,
			Map<String, String> namespacePrefixes, int insertPos, String lineDelimiter) {
		XMLBuilder attrs = new XMLBuilder(sharedSettings, "", lineDelimiter);
		int lineStart = xmlContent.lastIndexOf('\n', insertPos - 1);
		attrs.setInitialLineWidth(lineStart < 0 ? insertPos : insertPos - lineStart - 1);

		attrs.addAttribute("xmlns", namespace, 0, true);

		for (Map.Entry<String, String> entry : namespacePrefixes.entrySet()) {
			attrs.addAttribute("xmlns:" + entry.getValue(), entry.getKey(), 0, true);
		}

		return xmlContent.substring(0, insertPos) + attrs + xmlContent.substring(insertPos);
	}

	/**
	 * Adds an {@code <?xml-model?>} processing instruction before the root element
	 * for RelaxNG (.rng) and RelaxNG Compact (.rnc) grammars.
	 * <p>
	 * Example: {@code <?xml-model href="addressBook.rng"?>}
	 * </p>
	 */
	private String addXMLModelBinding(String xmlContent, String grammarURI, String lineDelimiter) {
		return "<?xml-model href=\"" + grammarURI + "\"?>" + lineDelimiter + xmlContent;
	}

	/**
	 * Finds a root element declaration by name in the content model document.
	 *
	 * @param cmDocument      the content model document.
	 * @param rootElementName the local name of the root element.
	 * @return the element declaration, or {@code null} if not found.
	 */
	private CMElementDeclaration findRootElement(CMDocument cmDocument, String rootElementName) {
		Collection<CMElementDeclaration> elements = cmDocument.getElements();
		for (CMElementDeclaration element : elements) {
			if (rootElementName.equals(element.getLocalName())) {
				return element;
			}
		}
		return null;
	}
}
