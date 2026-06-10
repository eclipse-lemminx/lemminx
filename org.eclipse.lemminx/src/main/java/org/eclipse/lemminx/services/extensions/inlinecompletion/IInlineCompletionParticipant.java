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

import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Inline completion participant API.
 * 
 * <p>
 * This participant is called when inline completion is requested to provide
 * context-aware suggestions as users type.
 * </p>
 */
public interface IInlineCompletionParticipant {

	/**
	 * Called when inline completion is requested.
	 *
	 * @param request       the inline completion request
	 * @param response      the response to add inline completion items to
	 * @param cancelChecker the cancel checker
	 */
	void onInlineCompletion(IInlineCompletionRequest request,
	                        IInlineCompletionResponse response,
	                        CancelChecker cancelChecker);
}