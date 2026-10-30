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

import java.util.Collections;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
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

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String catalogURI = ArgumentsUtils.getArgAt(params, 0, String.class);
		String entryType = ArgumentsUtils.getArgAt(params, 1, String.class);
		String identifier = ArgumentsUtils.getArgAt(params, 2, String.class);
		String grammarURI = ArgumentsUtils.getArgAt(params, 3, String.class);

		DOMDocument document = DOMUtils.loadDocument(catalogURI, null);
		if (document == null) {
			throw new Exception("Cannot load catalog file: " + catalogURI);
		}

		String relativeGrammarURI = FilesUtils.getRelativeURI(grammarURI, catalogURI);

		String entryXml = buildEntryXml(entryType, identifier, relativeGrammarURI);

		return insertEntryInCatalog(document, entryXml);
	}

	private static String buildEntryXml(String entryType, String identifier, String grammarURI) {
		switch (entryType) {
			case "public":
				return "  <public publicId=\"" + identifier + "\" uri=\"" + grammarURI + "\"/>";
			case "system":
				return "  <system systemId=\"" + identifier + "\" uri=\"" + grammarURI + "\"/>";
			case "uri":
				return "  <uri name=\"" + identifier + "\" uri=\"" + grammarURI + "\"/>";
			default:
				throw new IllegalArgumentException("Unknown entry type: " + entryType);
		}
	}

	private static TextDocumentEdit insertEntryInCatalog(DOMDocument document, String entryXml)
			throws BadLocationException {
		DOMElement catalogElement = findCatalogElement(document);
		if (catalogElement == null) {
			throw new BadLocationException("No <catalog> root element found");
		}

		String delimiter = document.getTextDocument().lineDelimiter(0);
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
				document.getDocumentURI(), 0);
		return new TextDocumentEdit(versionedId,
				TextEditUtils.toEitherTextEdits(Collections.singletonList(edit)));
	}

	private static DOMElement findCatalogElement(DOMDocument document) {
		for (DOMNode node : document.children()) {
			if (node.isElement() && "catalog".equals(node.getNodeName())) {
				return (DOMElement) node;
			}
		}
		return null;
	}
}
