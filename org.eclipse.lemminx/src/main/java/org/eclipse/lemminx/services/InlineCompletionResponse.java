/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
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
package org.eclipse.lemminx.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionResponse;
import org.eclipse.lsp4j.InlineCompletionItem;

/**
 * Inline completion response implementation.
 */
class InlineCompletionResponse implements IInlineCompletionResponse {

	private final List<InlineCompletionItem> items;

	public InlineCompletionResponse() {
		this.items = new ArrayList<>();
	}

	@Override
	public void addInlineCompletionItem(InlineCompletionItem item) {
		items.add(item);
	}

	/**
	 * Returns the list of inline completion items.
	 * 
	 * @return the list of inline completion items
	 */
	public List<InlineCompletionItem> getItems() {
		return items;
	}
}