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
 * Code action for the {@code missing-assign} error — inserts {@code =}
 * after a define or start keyword.
 *
 * <p>Use case: the user writes {@code start root} instead of
 * {@code start = root}. The code action inserts {@code " = "} after
 * {@code start}.</p>
 */
public class missing_assignCodeAction extends AbstractRNCInsertCodeAction {

	@Override
	protected String getTitle() {
		return "Add '='";
	}

	@Override
	protected String getInsertText(boolean snippetSupported) {
		if (!snippetSupported) {
			return " =";
		}
		StringBuilder sb = new StringBuilder(" =");
		SnippetsBuilder.tabstops(0, sb);
		return sb.toString();
	}
}
