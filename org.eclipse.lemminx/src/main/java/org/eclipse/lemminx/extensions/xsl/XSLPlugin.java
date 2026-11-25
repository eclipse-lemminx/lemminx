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
 *  Lukasz Kwiatkowski <lk@eikberg.com> - completion handling
 */
package org.eclipse.lemminx.extensions.xsl;

import org.eclipse.lemminx.services.extensions.*;
import org.eclipse.lemminx.extensions.xsl.participants.*;
import org.eclipse.lsp4j.InitializeParams;

/**
 * XSL plugin.
 */
public class XSLPlugin implements IXMLExtension {

	private XSLURIResolverExtension uiResolver;
	private final ICompletionParticipant completionParticipant;
	private final IDefinitionParticipant definitionParticipant;

	public XSLPlugin(){
		completionParticipant = new XSLCompletionParticipant();
		definitionParticipant = new XSLDefinitionParticipant();
	}

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		uiResolver = new XSLURIResolverExtension(registry.getDocumentProvider());
		registry.getResolverExtensionManager().registerResolver(uiResolver);
		registry.registerCompletionParticipant(completionParticipant);
		registry.registerDefinitionParticipant(definitionParticipant);
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.getResolverExtensionManager().unregisterResolver(uiResolver);
		registry.unregisterCompletionParticipant(completionParticipant);
		registry.unregisterDefinitionParticipant(definitionParticipant);
	}
}
