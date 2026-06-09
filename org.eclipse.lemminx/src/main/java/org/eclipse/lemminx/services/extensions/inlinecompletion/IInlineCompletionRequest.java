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

import org.eclipse.lemminx.services.extensions.IPositionRequest;
import org.eclipse.lemminx.services.extensions.ISharedSettingsRequest;
import org.eclipse.lsp4j.InlineCompletionContext;

/**
 * Inline completion request API.
 */
public interface IInlineCompletionRequest extends IPositionRequest, ISharedSettingsRequest {

	/**
	 * Returns the inline completion context.
	 * 
	 * @return the inline completion context
	 */
	InlineCompletionContext getContext();
}