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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion;

import org.eclipse.lemminx.commons.snippets.ISnippetRegistryLoader;
import org.eclipse.lemminx.commons.snippets.SnippetRegistry;

/**
 * Loads RNC-specific code snippets from the bundled JSON resource file.
 *
 * <p>Use case: when the user triggers completion in an RNC file, built-in
 * snippets (e.g., a full grammar skeleton, common patterns) are available
 * alongside keyword and define completions.</p>
 */
public class RNCSnippetRegistryLoader implements ISnippetRegistryLoader {

	@Override
	public String getLanguageId() {
		return "rnc";
	}

	@Override
	public void load(SnippetRegistry registry) throws Exception {
		registry.registerSnippets(
				RNCSnippetRegistryLoader.class.getResourceAsStream("new-rnc-snippets.json"));
	}
}
