/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/

package org.eclipse.lemminx.extensions.catalog.participants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.extensions.catalog.XMLCatalogErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.extensions.index.GrammarReference;
import org.eclipse.lemminx.extensions.index.GrammarReferenceLocator;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexReferenceParticipant;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Validates XML catalog documents:
 * <ul>
 *   <li>Reports an error when the {@code uri} attribute points to a file that does
 *       not exist on disk.</li>
 *   <li>Reports a warning (or hint when {@link DiagnosticTag#Unnecessary} is supported)
 *       on the {@code <catalog>} root element when the catalog file is not registered
 *       in {@code xml.catalogs}.</li>
 *   <li>Reports a warning (or hint when {@link DiagnosticTag#Unnecessary} is supported)
 *       on individual {@code <uri>}, {@code <system>}, and {@code <public>} entries that
 *       are never referenced by any XML file in the workspace. This check is only
 *       performed when the workspace index scan has completed and the catalog is
 *       registered in {@code xml.catalogs}.</li>
 * </ul>
 */
public class XMLCatalogDiagnosticsParticipant implements IDiagnosticsParticipant {

	private static final String ERROR_STRING = "The file ''{0}'' cannot be found.";

	private static final Set<String> RELEVANT_ENTRIES = Set.of("uri", "system", "public");

	private final ContentModelManager contentModelManager;
	private final XMLExtensionsRegistry registry;

	public XMLCatalogDiagnosticsParticipant(ContentModelManager contentModelManager, XMLExtensionsRegistry registry) {
		this.contentModelManager = contentModelManager;
		this.registry = registry;
	}

	@Override
	public void doDiagnostics(DOMDocument xmlDocument, List<Diagnostic> diagnostics,
			XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
		if (!DOMUtils.isCatalog(xmlDocument)) {
			return;
		}

		for (CatalogEntry catalogEntry : CatalogUtils.getCatalogEntries(xmlDocument)) {
			String path = CatalogUtils.getResolvedLocation(xmlDocument, catalogEntry);
			if (path != null && !FilesUtils.isValidPath(FilesUtils.getPath(path)) && URIUtils.isFileResource(path)) {
				Range range = XMLPositionUtility.selectValueWithoutQuote(catalogEntry.getLinkRange());
				String msg = MessageFormat.format(ERROR_STRING, catalogEntry.getResolvedURI());
				diagnostics.add(new Diagnostic(range, msg, DiagnosticSeverity.Error, xmlDocument.getDocumentURI(),
						XMLCatalogErrorCode.catalog_uri.name()));
			}
		}

		checkUnregisteredCatalog(xmlDocument, diagnostics, validationSettings);
		checkUnusedEntries(xmlDocument, diagnostics, validationSettings, cancelChecker);
	}

	private void checkUnregisteredCatalog(DOMDocument xmlDocument, List<Diagnostic> diagnostics,
			XMLValidationSettings validationSettings) {
		if (contentModelManager.isRegisteredCatalog(xmlDocument.getDocumentURI())) {
			return;
		}
		DOMElement catalogElement = xmlDocument.getDocumentElement();
		if (catalogElement == null) {
			return;
		}
		Range range = XMLPositionUtility.selectStartTagName(catalogElement);
		if (range == null) {
			return;
		}
		boolean unnecessarySupported = validationSettings != null
				&& validationSettings.isTagSupported(DiagnosticTag.Unnecessary);
		Diagnostic diagnostic = new Diagnostic(range,
				"This XML catalog is not registered in 'xml.catalogs'. It will not be used for grammar resolution.",
				unnecessarySupported ? DiagnosticSeverity.Hint : DiagnosticSeverity.Warning,
				xmlDocument.getDocumentURI(), XMLCatalogErrorCode.catalog_not_registered.name());
		if (unnecessarySupported) {
			diagnostic.setTags(Arrays.asList(DiagnosticTag.Unnecessary));
		}
		diagnostics.add(diagnostic);
	}

	private void checkUnusedEntries(DOMDocument xmlDocument, List<Diagnostic> diagnostics,
			XMLValidationSettings validationSettings, CancelChecker cancelChecker) {
		WorkspaceIndex workspaceIndex = registry.getComponent(WorkspaceIndex.class);
		if (workspaceIndex == null || !workspaceIndex.isScanCompleted()) {
			return;
		}
		if (!contentModelManager.isRegisteredCatalog(xmlDocument.getDocumentURI())) {
			return;
		}
		boolean unnecessarySupported = validationSettings != null
				&& validationSettings.isTagSupported(DiagnosticTag.Unnecessary);
		for (CatalogEntry entry : CatalogUtils.getCatalogEntries(xmlDocument)) {
			cancelChecker.checkCanceled();
			if (!RELEVANT_ENTRIES.contains(entry.getEntryElement().getNodeName())) {
				continue;
			}
			// Count by catalog identifier (systemId / publicId / name), not by filename.
			// Filename-based counting produces false positives from xs:include/xs:import in
			// XSD files that happen to reference the same grammar file directly.
			String catalogIdentifier = CatalogUtils.getCatalogIdentifier(entry);
			if (catalogIdentifier == null) {
				continue;
			}
			// Catalog identifiers always appear in the first 64 KB — skip partially-indexed fallback.
			List<String> candidates = workspaceIndex.findReferencingFiles(catalogIdentifier, false);
			int count = countIdentifierReferences(catalogIdentifier, candidates, workspaceIndex, cancelChecker);
			if (count == 0) {
				DOMElement entryElement = entry.getEntryElement();
				Range range = XMLPositionUtility.createRange(entryElement.getStart(), entryElement.getEnd(),
						xmlDocument);
				if (range == null) {
					continue;
				}
				String message = "Catalog entry '" + catalogIdentifier
						+ "' is never used by any XML file in the workspace.";
				Diagnostic diagnostic = new Diagnostic(range, message,
						unnecessarySupported ? DiagnosticSeverity.Hint : DiagnosticSeverity.Warning,
						xmlDocument.getDocumentURI(),
						XMLCatalogErrorCode.catalog_entry_unused.getCode());
				if (unnecessarySupported) {
					diagnostic.setTags(Arrays.asList(DiagnosticTag.Unnecessary));
				}
				diagnostics.add(diagnostic);
			}
		}
	}

	/**
	 * Counts XML instance files (not grammar files) that reference {@code identifier}
	 * via an exact value match — i.e., via the catalog identifier itself, not by filename.
	 */
	private static int countIdentifierReferences(String identifier, List<String> candidates,
			WorkspaceIndex workspaceIndex, CancelChecker cancelChecker) {
		List<IGrammarReferenceContributor> contributors = workspaceIndex.getContributors();
		int total = 0;
		for (String candidateUri : candidates) {
			cancelChecker.checkCanceled();
			if (WorkspaceIndexReferenceParticipant.isGrammarFile(candidateUri)) {
				continue;
			}
			Path candidatePath = FilesUtils.getPathOrNull(candidateUri);
			if (candidatePath == null || !Files.exists(candidatePath)) {
				continue;
			}
			try {
				String content = Files.readString(candidatePath);
				List<GrammarReference> allRefs = GrammarReferenceLocator.locate(content, contributors);
				if (!GrammarReferenceLocator.locateReferencesWithValue(allRefs, identifier).isEmpty()) {
					total++;
				}
			} catch (IOException e) {
				// skip unreadable files
			}
		}
		return total;
	}
}
