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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.NoGrammarConstraintsCodeAction;
import org.eclipse.lemminx.extensions.xerces.xmlmodel.XMLModelConstants;
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

	private final ContentModelManager contentModelManager;

	public AssociateGrammarCommand(IXMLDocumentProvider documentProvider) {
		this(documentProvider, null);
	}

	public AssociateGrammarCommand(IXMLDocumentProvider documentProvider, ContentModelManager contentModelManager) {
		super(documentProvider);
		this.contentModelManager = contentModelManager;
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
		List<String> rootElementNames = getRootElementNames(fullPathGrammarURI);
		boolean snippetSupport = sharedSettings.getWorkspaceSettings().isSnippetEditSupported();

		if (GrammarBindingType.STANDARD.getName().equals(bindingType)) {
			if (isXSD) {
				String targetNamespace = getTargetNamespace(fullPathGrammarURI);
				if (StringUtils.isEmpty(targetNamespace)) {
					// XSD without targetNamespace:
					// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd" />
					return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(grammarURI, document,
							sharedSettings, rootElementNames, snippetSupport);
				}
				// XSD with targetNamespace:
				// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns xsd/team.xsd" />
				return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(grammarURI, targetNamespace,
						document, sharedSettings, rootElementNames, snippetSupport);
			} else {
				// DTD: <foo /> -> <!DOCTYPE foo SYSTEM "dtd/note.dtd">\n<foo />
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(grammarURI, document, sharedSettings,
						rootElementNames, snippetSupport);
			}
		} else if (GrammarBindingType.XML_MODEL.getName().equals(bindingType)) {
			// xml-model: <foo /> -> <?xml-model href="grammar.xsd"?>\n<foo />
			String targetNamespace = isXSD ? getTargetNamespace(fullPathGrammarURI) : null;
			return NoGrammarConstraintsCodeAction.createXmlModelEdit(grammarURI, targetNamespace,
					null, document, sharedSettings, rootElementNames, snippetSupport);
		}
		throw new UnknownBindingTypeException(bindingType);
	}

	/**
	 * Handle catalog-based grammar binding.
	 *
	 * <p>
	 * Use cases:
	 * </p>
	 * <ul>
	 * <li><b>public entry</b>: inserts
	 * {@code <!DOCTYPE root PUBLIC "publicId" "systemId">}</li>
	 * <li><b>system entry with XSD</b>: inserts
	 * {@code xsi:noNamespaceSchemaLocation} or {@code xsi:schemaLocation}</li>
	 * <li><b>system entry with DTD</b>: inserts
	 * {@code <!DOCTYPE root SYSTEM "systemId">}</li>
	 * <li><b>uri entry with XSD</b>: inserts {@code xsi:*schemaLocation}</li>
	 * <li><b>uri entry with RNG/RNC</b>: inserts
	 * {@code <?xml-model href="..." schematypens="...">}</li>
	 * <li><b>uri entry with DTD</b>: inserts {@code <!DOCTYPE ...>}</li>
	 * </ul>
	 *
	 * <p>
	 * When the grammar has no existing document element, root element names are
	 * resolved from the grammar's content model. If snippet editing is supported,
	 * a choice placeholder {@code ${1|elem1,elem2,...|}} is generated for
	 * multiple root elements.
	 * </p>
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
		List<String> rootElementNames = getRootElementNames(targetURI);
		boolean snippetSupport = sharedSettings.getWorkspaceSettings().isSnippetEditSupported();

		switch (entryType) {
			case "public":
				// <foo /> -> <!DOCTYPE foo PUBLIC "-//Example//DTD//EN" "dtd/note.dtd">\n<foo />
				String systemId = FilesUtils.getRelativeURI(targetURI, document.getDocumentURI());
				return NoGrammarConstraintsCodeAction.createPublicDocTypeEdit(identifier, systemId, document,
						sharedSettings, rootElementNames, snippetSupport);
			case "system": {
				boolean isXSD = DOMUtils.isXSD(targetURI);
				if (isXSD) {
					String targetNamespace = getTargetNamespace(targetURI);
					if (StringUtils.isEmpty(targetNamespace)) {
						// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="systemId" />
						return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(identifier,
								document, sharedSettings, rootElementNames, snippetSupport);
					}
					// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns systemId" />
					return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(identifier, targetNamespace,
							document, sharedSettings, rootElementNames, snippetSupport);
				}
				// <foo /> -> <!DOCTYPE foo SYSTEM "systemId">\n<foo />
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document, sharedSettings,
						rootElementNames, snippetSupport);
			}
			case "uri": {
				boolean isXSD = DOMUtils.isXSD(targetURI);
				boolean isRNG = DOMUtils.isRelaxNGUri(targetURI);
				if (isXSD) {
					String targetNamespace = getTargetNamespace(targetURI);
					if (StringUtils.isEmpty(targetNamespace)) {
						// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="identifier" />
						return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(identifier,
								document, sharedSettings, rootElementNames, snippetSupport);
					}
					// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns identifier" />
					return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(identifier, targetNamespace,
							document, sharedSettings, rootElementNames, snippetSupport);
				} else if (isRNG) {
					// <foo /> -> <?xml-model href="identifier" schematypens="http://relaxng.org/..."?>\n<foo />
					String schematypens = getSchematypens(targetURI);
					return NoGrammarConstraintsCodeAction.createXmlModelEdit(identifier, null,
							schematypens, document, sharedSettings, rootElementNames, snippetSupport);
				}
				// DTD fallback: <foo /> -> <!DOCTYPE foo SYSTEM "identifier">\n<foo />
				return NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document, sharedSettings,
						rootElementNames, snippetSupport);
			}
			default:
				throw new IllegalArgumentException("Unknown catalog entry type: " + entryType);
		}
	}


	/**
	 * Returns the list of root element names declared by the grammar at the given
	 * URI. Uses a non-blocking lookup: if the grammar is not yet loaded (e.g.
	 * remote download in progress), returns an empty list so the binding edit can
	 * still be generated with a placeholder element name.
	 *
	 * <p>
	 * For XSD grammars with a target namespace, imported elements (from a different
	 * namespace) are filtered out so that only the schema's own top-level elements
	 * are returned.
	 * </p>
	 *
	 * @param grammarURI the grammar file URI (XSD, DTD, RNG, or RNC).
	 * @return the list of root element local names, or an empty list.
	 */
	private List<String> getRootElementNames(String grammarURI) {
		if (contentModelManager == null || grammarURI == null) {
			return Collections.emptyList();
		}
		try {
			CMDocument cmDocument = contentModelManager.loadCMDocumentAsync(grammarURI).getNow(null);
			if (cmDocument == null) {
				return Collections.emptyList();
			}
			Collection<CMElementDeclaration> elements = cmDocument.getElements();
			String schemaNamespace = cmDocument.getNamespace();
			List<String> names = new ArrayList<>();
			for (CMElementDeclaration element : elements) {
				String elementNs = element.getNamespace();
				if (schemaNamespace != null && !schemaNamespace.equals(elementNs)) {
					continue;
				}
				names.add(element.getLocalName());
			}
			return names;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static String getSchematypens(String grammarURI) {
		if (grammarURI == null) {
			return null;
		}
		String lower = grammarURI.toLowerCase();
		if (lower.endsWith(".rng") || lower.endsWith(".rnc")) {
			return XMLModelConstants.RELAXNG_SCHEMATYPENS;
		}
		return null;
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
