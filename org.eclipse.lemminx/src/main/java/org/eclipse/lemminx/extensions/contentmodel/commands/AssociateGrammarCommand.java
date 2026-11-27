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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
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
		boolean snippetSupport = sharedSettings.getWorkspaceSettings().isSnippetEditSupported();

		return getGrammarInfoForDocument(document, fullPathGrammarURI) //
				.thenApply(grammarInfo -> {
					List<String> rootElementNames = grammarInfo.rootElementNames;
					String targetNamespace = grammarInfo.targetNamespace;
					try {
						if (GrammarBindingType.STANDARD.getName().equals(bindingType)) {
							if (isXSD) {
								if (StringUtils.isEmpty(targetNamespace)) {
									// XSD without targetNamespace:
									// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd"
									// />
									return (Object) NoGrammarConstraintsCodeAction
											.createXSINoNamespaceSchemaLocationEdit(grammarURI, document,
													sharedSettings, rootElementNames, snippetSupport);
								}
								// XSD with targetNamespace:
								// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns
								// xsd/team.xsd" />
								return (Object) NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(grammarURI,
										targetNamespace, document, sharedSettings, rootElementNames, snippetSupport);
							} else {
								// DTD: <foo /> -> <!DOCTYPE foo SYSTEM "dtd/note.dtd">\n<foo />
								return (Object) NoGrammarConstraintsCodeAction.createDocTypeEdit(grammarURI, document,
										sharedSettings, rootElementNames, snippetSupport);
							}
						} else if (GrammarBindingType.XML_MODEL.getName().equals(bindingType)) {
							// xml-model: <foo /> -> <?xml-model href="grammar.xsd"?>\n<foo />
							return (Object) NoGrammarConstraintsCodeAction.createXmlModelEdit(grammarURI,
									targetNamespace, null, document, sharedSettings, rootElementNames, snippetSupport);
						}
						throw new UnknownBindingTypeException(bindingType);
					} catch (Exception e) {
						throw new CompletionException(e);
					}
				});
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
	 * resolved from the grammar's content model. If snippet editing is supported, a
	 * choice placeholder {@code ${1|elem1,elem2,...|}} is generated for multiple
	 * root elements.
	 * </p>
	 *
	 * Parameters for catalog binding:
	 * <ul>
	 * <li>param 1: entryType (String) — "public", "system", or "uri"</li>
	 * <li>param 2: "catalog" (bindingType)</li>
	 * <li>param 3: identifier (String) — the publicId, systemId, or name</li>
	 * <li>param 4: targetURI (String) — the resolved grammar URI</li> entry</li>
	 * </ul>
	 */
	private Object executeCatalogBinding(DOMDocument document, ExecuteCommandParams params,
			SharedSettings sharedSettings) throws Exception {
		String entryType = ArgumentsUtils.getArgAt(params, 1, String.class);
		String identifier = ArgumentsUtils.getArgAt(params, 3, String.class);
		String targetURI = ArgumentsUtils.getArgAt(params, 4, String.class);
		boolean snippetSupport = sharedSettings.getWorkspaceSettings().isSnippetEditSupported();

		return getGrammarInfoForDocument(document, targetURI)//
				.thenApply(grammarInfo -> {
					try {
						List<String> rootElementNames = grammarInfo.rootElementNames;
						String targetNamespace = grammarInfo.targetNamespace;
						switch (entryType) {
						case "public":
							// <foo /> -> <!DOCTYPE foo PUBLIC "-//Example//DTD//EN" "dtd/note.dtd">\n<foo
							// />
							// Try to find a corresponding system entry with the same URI
							String systemId = findSystemIdForUri(targetURI);
							if (systemId == null) {
								// Fallback: extract JAR path or use relative URI
								if (targetURI != null && targetURI.contains("jar:")) {
									systemId = extractPathFromJarUri(targetURI);
								} else {
									systemId = FilesUtils.getRelativeURI(targetURI, document.getDocumentURI());
								}
							}
							return (Object) NoGrammarConstraintsCodeAction.createPublicDocTypeEdit(identifier, systemId,
									document, sharedSettings, rootElementNames, snippetSupport);
						case "system": {
							boolean isXSD = DOMUtils.isXSD(targetURI);
							if (isXSD) {
								if (StringUtils.isEmpty(targetNamespace)) {
									// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="systemId" />
									return (Object) NoGrammarConstraintsCodeAction
											.createXSINoNamespaceSchemaLocationEdit(identifier, document,
													sharedSettings, rootElementNames, snippetSupport);
								}
								// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns systemId"
								// />
								return (Object) NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(identifier,
										targetNamespace, document, sharedSettings, rootElementNames, snippetSupport);
							}
							// <foo /> -> <!DOCTYPE foo SYSTEM "systemId">\n<foo />
							return (Object) NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document,
									sharedSettings, rootElementNames, snippetSupport);
						}
						case "uri": {
							boolean isXSD = DOMUtils.isXSD(targetURI);
							boolean isRNG = DOMUtils.isRelaxNGUri(targetURI);
							if (isXSD) {
								if (StringUtils.isEmpty(targetNamespace)) {
									// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="identifier" />
									return (Object) NoGrammarConstraintsCodeAction
											.createXSINoNamespaceSchemaLocationEdit(identifier, document,
													sharedSettings, rootElementNames, snippetSupport);
								}
								// <foo /> -> <foo xmlns="ns" xmlns:xsi="..." xsi:schemaLocation="ns identifier"
								// />
								return (Object) NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(identifier,
										targetNamespace, document, sharedSettings, rootElementNames, snippetSupport);
							} else if (isRNG) {
								// <foo /> -> <?xml-model href="identifier"
								// schematypens="http://relaxng.org/..."?>\n<foo />
								String schematypens = getSchematypens(targetURI);
								return (Object) NoGrammarConstraintsCodeAction.createXmlModelEdit(identifier, null,
										schematypens, document, sharedSettings, rootElementNames, snippetSupport);
							}
							// DTD fallback: <foo /> -> <!DOCTYPE foo SYSTEM "identifier">\n<foo />
							return (Object) NoGrammarConstraintsCodeAction.createDocTypeEdit(identifier, document,
									sharedSettings, rootElementNames, snippetSupport);
						}
						default:
							throw new IllegalArgumentException("Unknown catalog entry type: " + entryType);
						}
					} catch (Exception e) {
						throw new CompletionException(e);
					}
				});
	}

	/**
	 * Holds both root element names and target namespace from a grammar document.
	 */
	private static class GrammarInfo {
		List<String> rootElementNames;
		String targetNamespace;

		GrammarInfo(List<String> rootElementNames, String targetNamespace) {
			this.rootElementNames = rootElementNames;
			this.targetNamespace = targetNamespace;
		}
	}

	/**
	 * Returns grammar info (root elements and target namespace) for a binding edit.
	 * When the document already has a named root element, root element names are not
	 * extracted (the builders use the existing element name). For XSD schemas, the
	 * target namespace is loaded since catalog XSD branches use it to determine
	 * xsi:schemaLocation vs xsi:noNamespaceSchemaLocation. For DTD, no metadata is
	 * loaded since it is not needed for the edit.
	 */
	private CompletableFuture<GrammarInfo> getGrammarInfoForDocument(DOMDocument document, String grammarURI) {
		DOMElement documentElement = document.getDocumentElement();
		if (documentElement != null && documentElement.hasTagName()) {
			// For XSD, load namespace; for DTD or other types, return empty
			if (DOMUtils.isXSD(grammarURI)) {
				return getNamespaceAsync(grammarURI);
			}
			return CompletableFuture.completedFuture(new GrammarInfo(Collections.emptyList(), null));
		}
		return getGrammarInfoAsync(grammarURI);
	}

	private CompletableFuture<GrammarInfo> getGrammarInfoAsync(String grammarURI) {
		if (contentModelManager == null || grammarURI == null) {
			return CompletableFuture.completedFuture(new GrammarInfo(Collections.emptyList(), null));
		}
		try {
			return contentModelManager.loadCMDocumentAsync(grammarURI) //
					.thenApply(cmDocument -> {
						List<String> rootElements = extractRootElementNames(cmDocument);
						String namespace = cmDocument != null ? cmDocument.getNamespace() : null;
						return new GrammarInfo(rootElements, namespace);
					}).exceptionally(e -> new GrammarInfo(Collections.emptyList(), null));
		} catch (Exception e) {
			return CompletableFuture.completedFuture(new GrammarInfo(Collections.emptyList(), null));
		}
	}

	private CompletableFuture<GrammarInfo> getNamespaceAsync(String grammarURI) {
		if (contentModelManager == null || grammarURI == null) {
			return CompletableFuture.completedFuture(new GrammarInfo(Collections.emptyList(), null));
		}
		try {
			return contentModelManager.loadCMDocumentAsync(grammarURI) //
					.thenApply(cmDocument -> {
						String namespace = cmDocument != null ? cmDocument.getNamespace() : null;
						return new GrammarInfo(Collections.emptyList(), namespace);
					}).exceptionally(e -> new GrammarInfo(Collections.emptyList(), null));
		} catch (Exception e) {
			return CompletableFuture.completedFuture(new GrammarInfo(Collections.emptyList(), null));
		}
	}

	private List<String> extractRootElementNames(CMDocument cmDocument) {
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
	}

	private static String getSchematypens(String grammarURI) {
		if (grammarURI == null) {
			return null;
		}
		if (DOMUtils.isRelaxNGUri(grammarURI)) {
			return XMLModelConstants.RELAXNG_SCHEMATYPENS;
		}
		return null;
	}

	/**
	 * Extracts the internal path from a JAR URI. For example:
	 * {@code jar:file:/path/to/my.jar!/dtd/xhtml.dtd} → {@code dtd/xhtml.dtd}
	 * If the URI is not a JAR URI, returns the URI unchanged.
	 */
	private static String extractPathFromJarUri(String uri) {
		if (uri != null && uri.contains("jar:")) {
			int sepIndex = uri.indexOf("!");
			if (sepIndex != -1) {
				String path = uri.substring(sepIndex + 1);
				// Remove leading slash if present
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				return path;
			}
		}
		return uri;
	}

	/**
	 * Searches all configured catalogs for a system entry that points to the same
	 * target URI as the given URI. Returns the system entry's systemId if found,
	 * or null otherwise. This allows public entries to use the HTTP URL from a
	 * corresponding system entry for DOCTYPE declarations.
	 */
	private String findSystemIdForUri(String targetUri) {
		if (contentModelManager == null || targetUri == null) {
			return null;
		}
		String[] catalogs = contentModelManager.getCatalogs();
		if (catalogs == null || catalogs.length == 0) {
			return null;
		}
		java.util.Set<String> visited = new java.util.HashSet<>();
		for (String catalogUri : catalogs) {
			String result = searchSystemIdInCatalog(catalogUri, targetUri, visited);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private String searchSystemIdInCatalog(String catalogUri, String targetUri, java.util.Set<String> visited) {
		if (catalogUri == null || !visited.add(catalogUri)) {
			return null;
		}
		try {
			// Load catalog with timeout to avoid blocking on unresponsive HTTPS catalogs
			ExecutorService executor = Executors.newSingleThreadExecutor();
			try {
				DOMDocument catalog = executor.submit(() -> DOMUtils.loadDocument(catalogUri, null))
						.get(5, TimeUnit.SECONDS);
				if (catalog == null || !DOMUtils.isCatalog(catalog)) {
					return null;
				}
				return searchSystemIdInDocument(catalog, targetUri, visited);
			} finally {
				executor.shutdown();
			}
		} catch (Exception e) {
			// Timeout or other error: skip this catalog and continue search
			return null;
		}
	}

	private String searchSystemIdInDocument(DOMDocument catalog, String targetUri, java.util.Set<String> visited) {
		for (CatalogEntry entry : CatalogUtils.getCatalogEntries(catalog)) {
			DOMElement child = entry.getEntryElement();
			String localName = child.getLocalName();
			if ("system".equals(localName)) {
				String resolvedUri = CatalogUtils.getResolvedLocation(catalog, entry);
				if (resolvedUri != null && resolvedUri.equals(targetUri)) {
					return child.getAttribute("systemId");
				}
			} else if ("nextCatalog".equals(localName)) {
				String resolvedNextUri = CatalogUtils.getResolvedLocation(catalog, entry);
				if (resolvedNextUri != null) {
					String result = searchSystemIdInCatalog(resolvedNextUri, targetUri, visited);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

}
