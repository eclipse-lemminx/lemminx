/*******************************************************************************
* Copyright (c) 2022, 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGCodeLensParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGCompletionParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGDefinitionParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGDiagnosticsParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGDocumentLinkParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGHighlightingParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGReferenceParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RNGRenameParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rng.RelaxNGURIResolverExtension;
import org.eclipse.lemminx.extensions.relaxng.xml.contentmodel.CMRelaxNGContentModelProvider;
import org.eclipse.lemminx.services.extensions.IDefinitionParticipant;
import org.eclipse.lemminx.services.extensions.IDocumentLinkParticipant;
import org.eclipse.lemminx.services.extensions.IHighlightingParticipant;
import org.eclipse.lemminx.services.extensions.IReferenceParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensParticipant;
import org.eclipse.lemminx.services.extensions.completion.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.rename.IRenameParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.InitializeParams;

/**
 * RelaxNG plugin.
 */
public class RelaxNGPlugin implements IXMLExtension {

	private ContentModelManager contentModelManager;
	private URIResolverExtension uiResolver;

	private final ICompletionParticipant completionParticipant;

	private final IDefinitionParticipant definitionParticipant;

	private final IReferenceParticipant referenceParticipant;
	private final ICodeLensParticipant codeLensParticipant;
	private final IHighlightingParticipant highlightingParticipant;
	private final IRenameParticipant renameParticipant;
	private final IDocumentLinkParticipant documentLinkParticipant;

	private final IDiagnosticsParticipant diagnosticsParticipant;

	public RelaxNGPlugin() {
		completionParticipant = new RNGCompletionParticipant();
		definitionParticipant = new RNGDefinitionParticipant();
		referenceParticipant = new RNGReferenceParticipant();
		codeLensParticipant = new RNGCodeLensParticipant();
		highlightingParticipant = new RNGHighlightingParticipant();
		renameParticipant = new RNGRenameParticipant();
		documentLinkParticipant = new RNGDocumentLinkParticipant();
		this.diagnosticsParticipant = new RNGDiagnosticsParticipant(this);
	}

	@Override
	public void doSave(ISaveContext context) {
		String documentURI = context.getUri();
		DOMDocument document = context.getDocument(documentURI);
		if (DOMUtils.isRelaxNG(document)) {
			context.refreshDiagnostics(d -> {
				DOMDocument xml = context.getDocument(d.getDocumentURI());
				return contentModelManager.dependsOnGrammar(xml, context.getUri());
			});
		}
	}

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		uiResolver = new RelaxNGURIResolverExtension();
		registry.getResolverExtensionManager().registerResolver(uiResolver);
		// register RelaxNG content model provider
		ContentModelProvider modelProvider = new CMRelaxNGContentModelProvider(registry.getResolverExtensionManager());
		this.contentModelManager = registry.getComponent(ContentModelManager.class);
		this.contentModelManager.registerModelProvider(modelProvider);
		// rng participant
		registry.registerCompletionParticipant(completionParticipant);
		registry.registerDefinitionParticipant(definitionParticipant);
		registry.registerReferenceParticipant(referenceParticipant);
		registry.registerCodeLensParticipant(codeLensParticipant);
		registry.registerHighlightingParticipant(highlightingParticipant);
		registry.registerRenameParticipant(renameParticipant);
		registry.registerDocumentLinkParticipant(documentLinkParticipant);
		registry.registerDiagnosticsParticipant(diagnosticsParticipant);
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.getResolverExtensionManager().unregisterResolver(uiResolver);
		// rng participant
		registry.unregisterCompletionParticipant(completionParticipant);
		registry.unregisterDefinitionParticipant(definitionParticipant);
		registry.unregisterReferenceParticipant(referenceParticipant);
		registry.unregisterCodeLensParticipant(codeLensParticipant);
		registry.unregisterHighlightingParticipant(highlightingParticipant);
		registry.unregisterRenameParticipant(renameParticipant);
		registry.unregisterDocumentLinkParticipant(documentLinkParticipant);
		registry.unregisterDiagnosticsParticipant(diagnosticsParticipant);
	}

	public ContentModelManager getContentModelManager() {
		return contentModelManager;
	}
}
