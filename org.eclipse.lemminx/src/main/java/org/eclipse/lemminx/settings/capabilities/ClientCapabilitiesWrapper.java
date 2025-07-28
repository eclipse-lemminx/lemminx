/**
 *  Copyright (c) 2018 Red Hat, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.settings.capabilities;

import java.util.Optional;

import org.eclipse.lemminx.client.ExtendedClientCapabilities;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DynamicRegistrationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;

/**
 * Determines if a client supports a specific capability dynamically
 */
public class ClientCapabilitiesWrapper {
	private boolean v3Supported;

	public ClientCapabilities capabilities;

	private final ExtendedClientCapabilities extendedCapabilities;

	public ClientCapabilitiesWrapper() {
		this(new ClientCapabilities(), null);
	}

	public ClientCapabilitiesWrapper(ClientCapabilities capabilities, ExtendedClientCapabilities extendedCapabilities) {
		this.capabilities = capabilities;
		this.v3Supported = capabilities != null ? capabilities.getTextDocument() != null : false;
		this.extendedCapabilities = extendedCapabilities;
	}

	/**
	 * IMPORTANT
	 *
	 * This should be up to date with all Server supported capabilities
	 *
	 */

	public boolean isCompletionDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCompletion());
	}

	public boolean isLinkDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDocumentLink());
	}

	public boolean isColorDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getColorProvider());
	}
	
	public boolean isRangeFoldingDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getFoldingRange());
	}

	public boolean isDocumentSyncDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getSynchronization());
	}

	public boolean isFormattingDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getFormatting());
	}

	public boolean isRangeFormattingDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getRangeFormatting());
	}

	public boolean isRenameDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getRename());
	}

	public boolean isDocumentSymbolDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDocumentSymbol());
	}

	public boolean isCodeLensDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCodeLens());
	}

	public boolean isDefinitionDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDefinition());
	}

	public boolean isTypeDefinitionDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getTypeDefinition());
	}

	public boolean isReferencesDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getReferences());
	}

	public boolean isCodeActionDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCodeAction());
	}

	public boolean isHoverDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getHover());
	}

	public boolean isDocumentHighlightDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDocumentHighlight());
	}

	public boolean isSelectionRangeDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getSelectionRange());
	}

	public boolean isDidChangeWatchedFilesRegistered() {
		return v3Supported && isDynamicRegistrationSupported(capabilities.getWorkspace().getDidChangeWatchedFiles());
	}

    public boolean isExecuteCommandRegistered() {
        return v3Supported && isDynamicRegistrationSupported(capabilities.getWorkspace().getExecuteCommand());
    }

	public boolean isLinkedEditingRangeDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getLinkedEditingRange());
	}

	private boolean isDynamicRegistrationSupported(DynamicRegistrationCapabilities capability) {
		return capability != null && capability.getDynamicRegistration() != null
				&& capability.getDynamicRegistration().booleanValue();
	}

	public TextDocumentClientCapabilities getTextDocument() {
		return this.capabilities.getTextDocument();
	}

	public ExtendedClientCapabilities getExtendedCapabilities() {
		return extendedCapabilities;
	}

	public boolean isWorkspaceFoldersSupported() {
		return Optional.ofNullable(this.capabilities) //
				.map(ClientCapabilities::getWorkspace) //
				.map(WorkspaceClientCapabilities::getWorkspaceFolders) //
				.filter(Boolean.TRUE::equals).isPresent();
	}

	/**
	 * Returns true if the client should exit on shutdown() request and avoid
	 * waiting for an exit() request
	 *
	 * @return true if the language server should exit on shutdown() request
	 */
	public boolean shouldLanguageServerExitOnShutdown() {
		if (extendedCapabilities == null) {
			return false;
		}
		return extendedCapabilities.shouldLanguageServerExitOnShutdown();
	}
	
	public boolean isWorkDoneProgressSupported() {
		return v3Supported && capabilities.getWindow() != null && capabilities.getWindow().getWorkDoneProgress() != null
				&& capabilities.getWindow().getWorkDoneProgress().booleanValue();
	}
}