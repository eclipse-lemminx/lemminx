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
 * Code action for the {@code missing-name} error — inserts a name
 * placeholder after an {@code element} or {@code attribute} keyword.
 *
 * <p>Use case: the user writes {@code element} without a name.
 * The code action inserts {@code name} (or a snippet placeholder
 * {@code ${1:name}}) after the keyword.</p>
 */
public class missing_nameCodeAction extends AbstractRNCInsertCodeAction {

	@Override
	protected String getTitle() {
		return "Add name";
	}

	@Override
	protected String getInsertText(boolean snippetSupported) {
		if (!snippetSupported) {
			return " name";
		}
		StringBuilder sb = new StringBuilder(" ");
		SnippetsBuilder.placeholders(1, "name", sb);
		return sb.toString();
	}
}
