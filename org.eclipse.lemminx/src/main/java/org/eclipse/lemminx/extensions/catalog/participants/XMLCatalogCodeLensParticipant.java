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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.client.ClientCommands;
import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.commons.config.ConfigurationItemEdit;
import org.eclipse.lemminx.commons.config.ConfigurationItemEditType;
import org.eclipse.lemminx.commons.config.ConfigurationItemValueKind;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexCodeLensSupport;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensParticipant;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensRequest;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.URIUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code lens participant for XML catalog files. Provides two kinds of lenses:
 * <ul>
 *   <li><b>Register/Unregister Catalog</b> — shown on the root element, allows the
 *       user to add or remove the catalog from {@code xml.catalogs} settings.</li>
 *   <li><b>N references</b> — shown on each {@code uri}, {@code system}, and
 *       {@code public} entry when the workspace index is available. Counts XML
 *       instance files that reference the entry via its catalog identifier
 *       (namespace URI, system ID, or public ID). Clicking the lens triggers
 *       {@code textDocument/references} on the catalog file so that
 *       {@code WorkspaceIndexCatalogReferenceParticipant} can resolve references
 *       by catalog identifier.</li>
 * </ul>
 */
public class XMLCatalogCodeLensParticipant implements ICodeLensParticipant {

	private static final Set<String> RELEVANT_ENTRIES = Set.of("uri", "system", "public");

	private final ContentModelManager contentModelManager;
	private final XMLExtensionsRegistry registry;

	public XMLCatalogCodeLensParticipant(ContentModelManager contentModelManager, XMLExtensionsRegistry registry) {
		this.contentModelManager = contentModelManager;
		this.registry = registry;
	}

	@Override
	public void doCodeLens(ICodeLensRequest request, List<CodeLens> lenses, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		if (!DOMUtils.isCatalog(document)) {
			return;
		}
		createRegisterCatalogLenses(request, lenses);
		createWorkspaceReferenceLenses(request, lenses, cancelChecker);
	}

	private void createRegisterCatalogLenses(ICodeLensRequest request, List<CodeLens> lenses) {
		DOMDocument document = request.getDocument();
		String documentURI = contentModelManager.expandSystemId(document.getDocumentURI());
		Range range = XMLPositionUtility.selectRootStartTag(document);
		if (!contentModelManager.isRegisteredCatalog(document.getDocumentURI())) {
			ConfigurationItemEdit configurationItemEdit = new ConfigurationItemEdit("xml.catalogs",
					documentURI, ConfigurationItemEditType.Add, ConfigurationItemValueKind.File);
			Command command = new Command("Register Catalog", ClientCommands.UPDATE_CONFIGURATION,
					Arrays.asList(configurationItemEdit));
			lenses.add(new CodeLens(range, command, null));
		} else {
			ConfigurationItemEdit configurationItemEdit = new ConfigurationItemEdit("xml.catalogs",
					documentURI, ConfigurationItemEditType.Delete, ConfigurationItemValueKind.File);
			Command command = new Command("Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION,
					Arrays.asList(configurationItemEdit));
			lenses.add(new CodeLens(range, command, null));
		}
	}

	private void createWorkspaceReferenceLenses(ICodeLensRequest request, List<CodeLens> lenses,
			CancelChecker cancelChecker) {
		WorkspaceIndex workspaceIndex = registry.getComponent(WorkspaceIndex.class);
		if (workspaceIndex == null) {
			return;
		}
		DOMDocument document = request.getDocument();
		// Only show references code lenses for catalogs registered in xml.catalogs.
		// An unregistered catalog is not used by LemMinX so its entries never produce
		// cross-file references, and showing counts would be misleading.
		if (!contentModelManager.isRegisteredCatalog(document.getDocumentURI())) {
			return;
		}
		boolean supportedByClient = request.isSupportedByClient(CodeLensKind.References);
		for (CatalogEntry entry : CatalogUtils.getCatalogEntries(document)) {
			cancelChecker.checkCanceled();
			if (!RELEVANT_ENTRIES.contains(entry.getEntryElement().getNodeName())) {
				continue;
			}
			String resolvedLocation = CatalogUtils.getResolvedLocation(document, entry);
			if (resolvedLocation == null || !URIUtils.isFileResource(resolvedLocation)) {
				continue;
			}
			Range range = XMLPositionUtility.selectStartTagName(entry.getEntryElement());
			String catalogIdentifier = CatalogUtils.getCatalogIdentifier(entry);
			// Pass the catalog document URI (not the grammar file URI) so that clicking
			// the code lens triggers textDocument/references on the catalog file, which
			// WorkspaceIndexCatalogReferenceParticipant resolves by catalog identifier.
			CodeLens lens = WorkspaceIndexCodeLensSupport.buildCatalogWorkspaceReferenceLens(
					workspaceIndex, document.getDocumentURI(), catalogIdentifier, range, supportedByClient,
					cancelChecker);
			if (lens != null) {
				lenses.add(lens);
			}
		}
	}
}
