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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.XMLBuilder;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import org.eclipse.lemminx.commons.TextEditUtils;

/**
 * XML Command "xml.catalog.entry.add" to add a new entry to an existing
 * XML catalog file.
 *
 * Parameters:
 * <ul>
 * <li>catalogURI (String) : the catalog file URI</li>
 * <li>entryType (String) : "public", "system", or "uri"</li>
 * <li>identifier (String) : the publicId, systemId, or name value</li>
 * <li>grammarURI (String) : the grammar file URI</li>
 * </ul>
 */
public class AddCatalogEntryCommand implements IDelegateCommandHandler {

	public static final String COMMAND_ID = "xml.catalog.entry.add";

	private final IXMLDocumentProvider documentProvider;

	public AddCatalogEntryCommand(IXMLDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
	}

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String catalogURI = ArgumentsUtils.getArgAt(params, 0, String.class);
		String entryType = ArgumentsUtils.getArgAt(params, 1, String.class);
		String identifier = ArgumentsUtils.getArgAt(params, 2, String.class);
		String grammarURI = ArgumentsUtils.getArgAt(params, 3, String.class);

		DOMDocument document = documentProvider != null ? documentProvider.getDocument(catalogURI) : null;
		if (document == null) {
			document = DOMUtils.loadDocument(catalogURI, null);
		}
		if (document == null) {
			throw new Exception("Cannot load catalog file: " + catalogURI);
		}

		Path effectiveBaseDir = getEffectiveBaseDir(document, catalogURI);
		String relativeGrammarURI = effectiveBaseDir != null
				? FilesUtils.getRelativeURI(grammarURI, effectiveBaseDir)
				: FilesUtils.getRelativeURI(grammarURI, catalogURI);

		String delimiter = document.getTextDocument().lineDelimiter(0);
		String entryXml = buildEntryXml(entryType, identifier, relativeGrammarURI, sharedSettings, delimiter);

		return insertEntryInCatalog(document, entryType, identifier, entryXml);
	}

	/**
	 * Returns the effective base directory for resolving relative grammar URIs.
	 * If the {@code <catalog>} root element has an {@code xml:base} attribute,
	 * the base directory is computed relative to the catalog file's parent
	 * directory; otherwise returns {@code null} to use the catalog URI directly.
	 *
	 * @param document   the parsed catalog DOM document.
	 * @param catalogURI the catalog file URI.
	 * @return the effective base directory, or {@code null}.
	 */
	private static Path getEffectiveBaseDir(DOMDocument document, String catalogURI) {
		DOMElement catalogElement = findCatalogElement(document);
		if (catalogElement != null) {
			String xmlBase = catalogElement.getAttribute("xml:base");
			if (xmlBase != null && !xmlBase.isEmpty()) {
				try {
					Path catalogDir = FilesUtils.getPath(catalogURI).getParent();
					return catalogDir.resolve(xmlBase).normalize();
				} catch (Exception e) {
					// Fall back to null (will use catalogURI)
				}
			}
		}
		return null;
	}

	/**
	 * Returns the XML attribute name that holds the identifier for the given
	 * catalog entry type: {@code publicId} for "public", {@code systemId} for
	 * "system", {@code name} for "uri".
	 *
	 * @param entryType the catalog entry type.
	 * @return the identifier attribute name.
	 */
	private static String getIdentifierAttributeName(String entryType) {
		switch (entryType) {
			case "public":
				return "publicId";
			case "system":
				return "systemId";
			case "uri":
				return "name";
			default:
				throw new IllegalArgumentException("Unknown entry type: " + entryType);
		}
	}

	/**
	 * Builds the XML text for a catalog entry element, e.g.
	 * {@code <uri name="..." uri="..." />}.
	 *
	 * @param entryType      the catalog entry type ("public", "system", or "uri").
	 * @param identifier     the identifier value (publicId, systemId, or name).
	 * @param grammarURI     the relative grammar URI for the {@code uri} attribute.
	 * @param sharedSettings the shared settings for XML formatting.
	 * @param delimiter      the line delimiter to use.
	 * @return the formatted entry XML string with indentation.
	 */
	private static String buildEntryXml(String entryType, String identifier, String grammarURI,
			SharedSettings sharedSettings, String delimiter) {
		XMLBuilder builder = new XMLBuilder(sharedSettings, null, delimiter);
		builder.indent(1);
		builder.startElement(entryType, false);
		builder.addSingleAttribute(getIdentifierAttributeName(entryType), identifier, true);
		builder.addSingleAttribute("uri", grammarURI, true);
		builder.selfCloseElement();
		return builder.toString();
	}

	/**
	 * Inserts or replaces a catalog entry in the document.
	 *
	 * <p>
	 * Use cases:
	 * </p>
	 * <ul>
	 * <li><b>Duplicate identifier</b>: if the catalog already contains an entry
	 * of the same type with the same identifier, it is replaced in-place (XML
	 * Catalog resolution uses the first match, so a duplicate would be
	 * ineffective).</li>
	 * <li><b>Self-closed catalog</b>: {@code <catalog .../>} is expanded to
	 * {@code <catalog ...>entry</catalog>}.</li>
	 * <li><b>Empty catalog</b>: the entry is inserted after the opening tag.</li>
	 * <li><b>Non-empty catalog</b>: the entry is appended after the last child
	 * element.</li>
	 * </ul>
	 *
	 * @param document  the parsed catalog DOM document.
	 * @param entryType the catalog entry type ("public", "system", or "uri").
	 * @param identifier the identifier value to check for duplicates.
	 * @param entryXml  the formatted entry XML to insert.
	 * @return the {@link TextDocumentEdit} for the insertion or replacement.
	 * @throws BadLocationException if no {@code <catalog>} root element is found.
	 */
	private static TextDocumentEdit insertEntryInCatalog(DOMDocument document, String entryType,
			String identifier, String entryXml) throws BadLocationException {
		DOMElement catalogElement = findCatalogElement(document);
		if (catalogElement == null) {
			throw new BadLocationException("No <catalog> root element found");
		}

		// Duplicate: replace existing entry in-place (XML Catalog uses first match)
		// <uri name="id" uri="old.xsd" /> -> <uri name="id" uri="new.xsd" />
		DOMElement existingEntry = findExistingEntry(catalogElement, entryType, identifier);
		if (existingEntry != null) {
			Position startPos = document.positionAt(existingEntry.getStart());
			Position endPos = document.positionAt(existingEntry.getEnd());
			TextEdit edit = new TextEdit(new Range(startPos, endPos), entryXml.strip());
			VersionedTextDocumentIdentifier versionedId = new VersionedTextDocumentIdentifier(
					document.getDocumentURI(), document.getTextDocument().getVersion());
			return new TextDocumentEdit(versionedId,
					TextEditUtils.toEitherTextEdits(Collections.singletonList(edit)));
		}

		String delimiter = document.getTextDocument().lineDelimiter(0);

		// Self-closed: <catalog .../> -> <catalog ...>\n  <entry />\n</catalog>
		if (catalogElement.isSelfClosed()) {
			int replaceStart = catalogElement.getOffsetBeforeCloseOfStartTag();
			int replaceEnd = catalogElement.getEnd();
			String insertText = ">" + delimiter + entryXml + delimiter + "</catalog>";
			Position startPos = document.positionAt(replaceStart);
			Position endPos = document.positionAt(replaceEnd);
			TextEdit edit = new TextEdit(new Range(startPos, endPos), insertText);
			return new TextDocumentEdit(
					new VersionedTextDocumentIdentifier(document.getDocumentURI(),
							document.getTextDocument().getVersion()),
					TextEditUtils.toEitherTextEdits(Collections.singletonList(edit)));
		}

		// Non-empty: append after last child
		// Empty: insert after opening tag
		int insertOffset;
		if (catalogElement.hasChildNodes()) {
			DOMNode lastChild = catalogElement.getLastChild();
			insertOffset = lastChild.getEnd();
		} else {
			insertOffset = catalogElement.getStartTagCloseOffset() + 1;
		}

		String insertText = delimiter + entryXml;
		Position position = document.positionAt(insertOffset);
		TextEdit edit = new TextEdit(new Range(position, position), insertText);

		VersionedTextDocumentIdentifier versionedId = new VersionedTextDocumentIdentifier(
				document.getDocumentURI(), document.getTextDocument().getVersion());
		return new TextDocumentEdit(versionedId,
				TextEditUtils.toEitherTextEdits(Collections.singletonList(edit)));
	}

	/**
	 * Searches the catalog element's children for an existing entry of the given
	 * type with the given identifier. Used to detect duplicates before inserting.
	 *
	 * @param catalogElement the {@code <catalog>} root element.
	 * @param entryType      the entry type to match (e.g. "uri", "system").
	 * @param identifier     the identifier value to match.
	 * @return the matching entry element, or {@code null} if not found.
	 */
	private static DOMElement findExistingEntry(DOMElement catalogElement, String entryType, String identifier) {
		String idAttrName = getIdentifierAttributeName(entryType);
		for (DOMNode node : catalogElement.getChildren()) {
			if (node.isElement() && entryType.equals(node.getNodeName())) {
				DOMElement element = (DOMElement) node;
				String value = element.getAttribute(idAttrName);
				if (identifier.equals(value)) {
					return element;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the {@code <catalog>} root element in the document.
	 *
	 * @param document the catalog DOM document.
	 * @return the catalog element, or {@code null} if not found.
	 */
	private static DOMElement findCatalogElement(DOMDocument document) {
		for (DOMNode node : document.children()) {
			if (node.isElement() && "catalog".equals(node.getNodeName())) {
				return (DOMElement) node;
			}
		}
		return null;
	}
}
