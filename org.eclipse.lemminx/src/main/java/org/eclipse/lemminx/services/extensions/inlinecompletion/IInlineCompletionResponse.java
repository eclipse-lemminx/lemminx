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
package org.eclipse.lemminx.services.extensions.inlinecompletion;

import org.eclipse.lsp4j.InlineCompletionItem;

/**
 * Inline completion response API.
 * 
 * <p>
 * This interface provides methods to add inline completion items to the response.
 * It follows the same pattern as {@link org.eclipse.lemminx.services.extensions.completion.ICompletionResponse}
 * to maintain API consistency.
 * </p>
 */
public interface IInlineCompletionResponse {

	/**
	 * Add an inline completion item to the response.
	 * 
	 * @param item the inline completion item to add
	 */
	void addInlineCompletionItem(InlineCompletionItem item);
}