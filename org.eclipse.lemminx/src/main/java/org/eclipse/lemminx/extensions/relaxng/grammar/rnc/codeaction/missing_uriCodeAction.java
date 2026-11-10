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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction;

import org.eclipse.lemminx.commons.SnippetsBuilder;

/**
 * Code action for the {@code missing-uri} error — inserts a URI string
 * literal after an {@code include} keyword.
 *
 * <p>Use case: the user writes {@code include} without a file path.
 * The code action inserts {@code ""} with the cursor inside the quotes.</p>
 */
public class missing_uriCodeAction extends AbstractRNCInsertCodeAction {

	@Override
	protected String getTitle() {
		return "Add URI";
	}

	@Override
	protected String getInsertText(boolean snippetSupported) {
		if (!snippetSupported) {
			return " \"\"";
		}
		StringBuilder sb = new StringBuilder(" \"");
		SnippetsBuilder.tabstops(1, sb);
		sb.append("\"");
		SnippetsBuilder.tabstops(0, sb);
		return sb.toString();
	}
}
