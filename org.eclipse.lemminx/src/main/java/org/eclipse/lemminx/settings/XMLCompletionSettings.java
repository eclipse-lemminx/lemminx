/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.settings;

import org.eclipse.lemminx.settings.capabilities.CompletionResolveSupportProperty;
import org.eclipse.lsp4j.CompletionCapabilities;

/**
 * A wrapper around LSP {@link CompletionCapabilities}.
 *
 */
public class XMLCompletionSettings {

	private CompletionCapabilities completionCapabilities;

	private boolean autoCloseTags;

	private boolean autoCloseRemovesContent;

	public XMLCompletionSettings(boolean autoCloseTags, boolean autoCloseRemovesContent) {
		this.autoCloseTags = autoCloseTags;
		this.autoCloseRemovesContent = autoCloseRemovesContent;
	}

	public XMLCompletionSettings() {
		this(true, true);
	}

	public void setCapabilities(CompletionCapabilities completionCapabilities) {
		this.completionCapabilities = completionCapabilities;
	}

	public CompletionCapabilities getCompletionCapabilities() {
		return completionCapabilities;
	}

	/**
	 * Tag should be autoclosed with an end tag.
	 *
	 * @param autoCloseTags
	 */
	public void setAutoCloseTags(boolean autoCloseTags) {
		this.autoCloseTags = autoCloseTags;
	}

	/**
	 * If tag should be autoclosed with an end tag.
	 *
	 * @return
	 */
	public boolean isAutoCloseTags() {
		return autoCloseTags;
	}

	/**
	 * If turning a start tag into a self closing tag should remove the content of
	 * the element
	 *
	 * @param autoCloseRemovesContent
	 */
	public void setAutoCloseRemovesContent(boolean autoCloseRemovesContent) {
		this.autoCloseRemovesContent = autoCloseRemovesContent;
	}

	/**
	 * Returns true if turning a start tag into a self closing tag should remove the
	 * content of the element and false otherwise
	 *
	 * @return true if turning a start tag into a self closing tag should remove the
	 *         content of the element and false otherwise
	 */
	public boolean isAutoCloseRemovesContent() {
		return autoCloseRemovesContent;
	}

	/**
	 * Returns <code>true</code> if the client support snippet and
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if the client support snippet and
	 *         <code>false</code> otherwise.
	 */
	public boolean isCompletionSnippetsSupported() {
		return completionCapabilities != null && completionCapabilities.getCompletionItem() != null
				&& completionCapabilities.getCompletionItem().getSnippetSupport() != null
				&& completionCapabilities.getCompletionItem().getSnippetSupport();
	}

	/**
	 * Returns true if the client supports resolving the given
	 * {@link CompletionResolveSupportProperty} property in
	 * completionItem/resolve and false otherwise.
	 *
	 * @return true if the client supports resolving the given
	 *         {@link CompletionResolveSupportProperty} property in
	 *         completionItem/resolve and false otherwise
	 */
	public boolean isCompletionResolveSupported(CompletionResolveSupportProperty property) {
		return completionCapabilities != null && completionCapabilities.getCompletionItem() != null
				&& completionCapabilities.getCompletionItem().getResolveSupport() != null
				&& completionCapabilities.getCompletionItem().getResolveSupport().getProperties() != null
				&& completionCapabilities.getCompletionItem().getResolveSupport().getProperties()
						.contains(property.name());
	}

	/**
	 * Returns <code>true</code> if the client support completion list itemDefaults given the field and
	 * <code>false</code> otherwise.
	 *
	 * @param field the completionList itemDefaults field
	 * 
	 * @return <code>true</code> if the client support completion list itemDefaults given the field and
	 * <code>false</code> otherwise.
	 */
	public boolean isCompletionListItemDefaultsSupport(String field) {
		return completionCapabilities != null && completionCapabilities.getCompletionList() != null
			&& completionCapabilities.getCompletionList().getItemDefaults() != null
			&& completionCapabilities.getCompletionList().getItemDefaults().contains(field);
	}

	/**
	 * Returns whether the client supports the given markup kind in completion
	 * item documentation.
	 *
	 * @param kind the markup kind (e.g. {@code MarkupKind.MARKDOWN}).
	 * @return {@code true} if the kind is supported, {@code false} otherwise.
	 */
	public boolean canSupportMarkupKind(String kind) {
		return completionCapabilities != null
				&& completionCapabilities.getCompletionItem() != null
				&& completionCapabilities.getCompletionItem().getDocumentationFormat() != null
				&& completionCapabilities.getCompletionItem().getDocumentationFormat().contains(kind);
	}

	/**
	 * Merge only the given completion settings (and not the capability) in the
	 * settings.
	 *
	 * @param newCompletion the new settings to merge.
	 */
	public void merge(XMLCompletionSettings newCompletion) {
		this.setAutoCloseTags(newCompletion.isAutoCloseTags());
		this.setAutoCloseRemovesContent(newCompletion.isAutoCloseRemovesContent());
	}
}