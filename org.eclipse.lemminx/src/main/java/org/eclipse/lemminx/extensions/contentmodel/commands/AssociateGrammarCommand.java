/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.commands;

import static org.eclipse.lemminx.extensions.xsd.utils.XSDUtils.TARGET_NAMESPACE_ATTR;

import java.net.URL;
import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.NoGrammarConstraintsCodeAction;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.commands.AbstractDOMDocumentCommandHandler;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Command "xml.associate.grammar.insert" to associate a grammar to a given
 * DOM document.
 *
 * The command parameters {@link ExecuteCommandParams} must be filled with 3
 * parameters:
 *
 * <ul>
 * <li>document URI (String) : the DOM document file URI to bind with a grammar.
 * </li>
 * <li>grammar URI (String) : the XSD, DTD file URI to bind with the DOM
 * document.</li>
 * <li>binding type (String) : which can takes values "standard", "xml-model" to
 * know which binding type must be inserted in the DOM document.</li>
 * </ul>
 *
 * @author Angelo ZERR
 *
 */
public class AssociateGrammarCommand extends AbstractDOMDocumentCommandHandler {

	public static final String COMMAND_ID = "xml.associate.grammar.insert";

	public AssociateGrammarCommand(IXMLDocumentProvider documentProvider) {
		super(documentProvider);
	}

	public enum GrammarBindingType {

		STANDARD("standard"), // xsi:schemaLocation, xsi:noNamespaceSchemaLocation, DOCTYPE
		XML_MODEL("xml-model"), // xml-model processing instruction
		CATALOG("catalog"); // catalog-based binding (PUBLIC DOCTYPE, SYSTEM DOCTYPE, or namespace)

		private String name;

		private GrammarBindingType(String name) {
			this.name = name != null ? name : name();
		}

		public String getName() {
			return name;
		}
	}

	@SuppressWarnings({ "serial" })
	public static class UnknownBindingTypeException extends Exception {

		private final static String MESSAGE = "Unknown binding type ''{0}''. Allowed values are " + //
				Stream.of(GrammarBindingType.values()) //
						.map(GrammarBindingType::getName) //
						.collect(Collectors.joining(", ", "[", "]"));

		public UnknownBindingTypeException(String bindingType) {
			super(MessageFormat.format(MESSAGE, bindingType));
		}

	}

	@Override
	protected Object executeCommand(DOMDocument document, ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String bindingType = ArgumentsUtils.getArgAt(params, 2, String.class);

		if (GrammarBindingType.CATALOG.getName().equals(bindingType)) {
			return executeCatalogBinding(document, params, sharedSettings);
		}

		String fullPathGrammarURI = ArgumentsUtils.getArgAt(params, 1, String.class);
		String grammarURI = FilesUtils.getRelativeURI(fullPathGrammarURI, document.getDocumentURI());
		boolean isXSD = DOMUtils.isXSD(fullPathGrammarURI);

		if (GrammarBindingType.STANDARD.getName().equals(bindingType)) {
			if (isXSD) {
				// XSD file
				// Check if XSD to bind declares a target namespace
				String targetNamespace = getTargetNamespace(fullPathGrammarURI);
				if (StringUtils.isEmpty(targetNamespace)) {
					// Insert inside <foo /> ->
					// xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance"
					// xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"
					return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(grammarURI, document, sharedSettings);
				}
				// Insert inside <foo /> ->
				// xmlns="team_namespace"
				// xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				// xsi:schemaLocation="team_namespace xsd/team.xsd"
				return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(grammarURI, targetNamespace,
						document, sharedSettings);
			} else {
				// DTD file
				// Insert before <foo /> -> <!DOCTYPE foo SYSTEM "dtd/tag.dtd">
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(grammarURI, document, sharedSettings);
			}
		} else if (GrammarBindingType.XML_MODEL.getName().equals(bindingType)) {
			String targetNamespace = isXSD ? getTargetNamespace(fullPathGrammarURI) : null;
			// Insert before <foo /> -> <?xml-model href=\"dtd/tag.dtd\"?>
			return NoGrammarConstraintsCodeAction.createXmlModelEdit(grammarURI, targetNamespace, document,
					sharedSettings);
		}
		throw new UnknownBindingTypeException(bindingType);
	}

	/**
	 * Handle catalog-based grammar binding.
	 *
	 * Parameters for catalog binding:
	 * <ul>
	 * <li>param 1: entryType (String) — "public", "system", or "uri"</li>
	 * <li>param 2: "catalog" (bindingType)</li>
	 * <li>param 3: identifier (String) — the publicId, systemId, or name</li>
	 * <li>param 4: targetURI (String) — the resolved grammar URI</li>
	 * <li>param 5: uriValue (String) — the raw uri attribute from catalog entry</li>
	 * </ul>
	 */
	private Object executeCatalogBinding(DOMDocument document, ExecuteCommandParams params,
			SharedSettings sharedSettings) throws Exception {
		String entryType = ArgumentsUtils.getArgAt(params, 1, String.class);
		String identifier = ArgumentsUtils.getArgAt(params, 3, String.class);
		String targetURI = ArgumentsUtils.getArgAt(params, 4, String.class);
		String uriValue = ArgumentsUtils.getArgAt(params, 5, String.class);

		switch (entryType) {
			case "public":
				// <!DOCTYPE root PUBLIC "publicId" "systemId">
				// Use the raw uri attribute value as the SYSTEM identifier
				String systemId = uriValue != null ? uriValue : targetURI;
				return NoGrammarConstraintsCodeAction.createPublicDocTypeEdit(identifier, systemId, document,
						sharedSettings);
			case "system": {
				boolean isXSD = DOMUtils.isXSD(targetURI);
				if (isXSD) {
					return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(identifier, document,
							sharedSettings);
				}
				// <!DOCTYPE root SYSTEM "systemId">
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document, sharedSettings);
			}
			case "uri": {
				boolean isXSD = targetURI != null && DOMUtils.isXSD(targetURI);
				boolean isRNG = targetURI != null
						&& (targetURI.endsWith(".rng") || targetURI.endsWith(".rnc"));
				if (isXSD) {
					String targetNamespace = getTargetNamespace(targetURI);
					if (StringUtils.isEmpty(targetNamespace)) {
						return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(identifier,
								document, sharedSettings);
					}
					return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(identifier, targetNamespace,
							document, sharedSettings);
				} else if (isRNG) {
					return NoGrammarConstraintsCodeAction.createXmlModelEdit(identifier, null, document,
							sharedSettings);
				}
				// Fallback for uri entries pointing to DTD
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document, sharedSettings);
			}
			default:
				throw new IllegalArgumentException("Unknown catalog entry type: " + entryType);
		}
	}


	private static String getTargetNamespace(String xsdURI) {
		TargetNamespaceHandler handler = new TargetNamespaceHandler();
		try {
			SAXParserFactory factory = DOMUtils.newSAXParserFactory();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new URL(xsdURI).openStream(), handler);
		} catch (Exception e) {

		}
		return handler.getTargetNamespace();
	}

	/**
	 * SAX handler which extract the targetNamespace attribute from the xs:schema
	 * root tag element and null otherwise.
	 *
	 */
	private static class TargetNamespaceHandler extends DefaultHandler {

		private String targetNamespace;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.targetNamespace = attributes.getValue(TARGET_NAMESPACE_ATTR);
			throw new SAXException();
		}

		public String getTargetNamespace() {
			return targetNamespace;
		}
	}

}
