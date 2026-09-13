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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lemminx.commons.progress.ProgressMonitor;
import org.eclipse.lemminx.commons.progress.ProgressSupport;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.model.InvalidGrammarException;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

/**
 * XML Command "xml.grammar.listRootElements" to list root elements from a
 * given grammar (XSD, DTD, RelaxNG, etc.).
 *
 * The command parameters {@link ExecuteCommandParams} must be filled with 1
 * parameter:
 *
 * <ul>
 * <li>grammar URI (String) : the grammar file URI.</li>
 * </ul>
 */
public class ListRootElementsCommand implements IDelegateCommandHandler {

	public static final String COMMAND_ID = "xml.grammar.listRootElements";

	private final ContentModelManager contentModelManager;

	private final ProgressSupport progressSupport;

	public ListRootElementsCommand(ContentModelManager contentModelManager, ProgressSupport progressSupport) {
		this.contentModelManager = contentModelManager;
		this.progressSupport = progressSupport;
	}

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String grammarURI = ArgumentsUtils.getArgAt(params, 0, String.class);

		ProgressMonitor monitor = progressSupport != null ? progressSupport.createProgressMonitor() : null;
		if (monitor != null) {
			monitor.begin("Loading grammar...", grammarURI, null, null);
		}

		try {
			return contentModelManager.loadCMDocumentAsync(grammarURI)
					.thenApply(cmDocument -> {
						try {
							if (cmDocument == null) {
								throw new ResponseErrorException(new ResponseError(
										ResponseErrorCode.InvalidParams,
										"The selected grammar could not be loaded.",
										grammarURI));
							}
							Collection<CMElementDeclaration> elements = cmDocument.getElements();
							String schemaNamespace = cmDocument.getNamespace();
							List<RootElementInfo> result = new ArrayList<>();
							for (CMElementDeclaration element : elements) {
								// Filter out elements from imported schemas
								String elementNs = element.getNamespace();
								if (schemaNamespace != null && !schemaNamespace.equals(elementNs)) {
									continue;
								}
								result.add(new RootElementInfo(element.getLocalName(), elementNs));
							}
							return result;
						} finally {
							if (monitor != null) {
								monitor.end("");
							}
						}
					});
		} catch (InvalidGrammarException e) {
			if (monitor != null) {
				monitor.end("");
			}
			throw new ResponseErrorException(new ResponseError(
					ResponseErrorCode.InvalidParams,
					e.getMessage(),
					grammarURI));
		}
	}

	/**
	 * Information about a root element declaration.
	 */
	public static class RootElementInfo {

		private final String name;

		private final String namespace;

		public RootElementInfo(String name, String namespace) {
			this.name = name;
			this.namespace = namespace;
		}

		public String getName() {
			return name;
		}

		public String getNamespace() {
			return namespace;
		}
	}
}
