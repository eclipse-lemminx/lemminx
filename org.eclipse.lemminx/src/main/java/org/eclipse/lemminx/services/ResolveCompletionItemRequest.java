/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.services.extensions.completion.ICompletionItemResolverRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.capabilities.CompletionResolveSupportProperty;
import org.eclipse.lsp4j.CompletionItem;

/**
 * Implementation of {@code ICompletionItemResolverRequest}
 */
public class ResolveCompletionItemRequest implements ICompletionItemResolverRequest {

	private final CompletionItem unresolved;
	private final String participantId;
	private final DOMDocument document;
	private final IComponentProvider componentProvider;
	private final SharedSettings sharedSettings;

	public ResolveCompletionItemRequest(CompletionItem unresolved, DOMDocument document,
			IComponentProvider componentProvider, SharedSettings sharedSettings) {
		this.unresolved = unresolved;
		this.participantId = DataEntryField.getParticipantId(unresolved.getData());
		this.document = document;
		this.componentProvider = componentProvider;
		this.sharedSettings = sharedSettings;
	}

	@Override
	public CompletionItem getUnresolved() {
		return unresolved;
	}

	@Override
	public String getParticipantId() {
		return participantId;
	}

	@Override
	public String getDataProperty(String fieldName) {
		return DataEntryField.getProperty(unresolved.getData(), fieldName);
	}

	@Override
	public boolean canSupportMarkupKind(String kind) {
		return getSharedSettings().getCompletionSettings().canSupportMarkupKind(kind);
	}

	@Override
	public Integer getDataPropertyAsInt(String fieldName) {
		return DataEntryField.getPropertyAsInt(unresolved.getData(), fieldName);
	}

	@Override
	public Boolean getDataPropertyAsBoolean(String fieldName) {
		return DataEntryField.getPropertyAsBoolean(unresolved.getData(), fieldName);
	}
	
	@Override
	public <T> T getComponent(Class clazz) {
		return componentProvider.getComponent(clazz);
	}

	@Override
	public DOMDocument getDocument() {
		return document;
	}

	@Override
	public SharedSettings getSharedSettings() {
		return sharedSettings;
	}

	@Override
	public boolean isResolveDocumentationSupported() {
		return getSharedSettings().getCompletionSettings()
				.isCompletionResolveSupported(CompletionResolveSupportProperty.documentation);
	}

	@Override
	public boolean isResolveAdditionalTextEditsSupported() {
		return getSharedSettings().getCompletionSettings()
				.isCompletionResolveSupported(CompletionResolveSupportProperty.additionalTextEdits);
	}

	@Override
	public Integer getCompletionOffset() {
		return getDataPropertyAsInt(DataEntryField.OFFSET_FIELD);
	}

}
