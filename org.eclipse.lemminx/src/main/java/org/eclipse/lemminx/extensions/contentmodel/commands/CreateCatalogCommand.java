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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.catalog.create" that returns the content for a new
 * empty XML catalog file. The client is responsible for writing the file
 * to disk and registering it in the xml.catalogs setting.
 *
 * Parameters:
 * <ul>
 * <li>catalogURI (String) : the URI where the catalog will be created</li>
 * </ul>
 *
 * Returns the catalog file content as a String.
 */
public class CreateCatalogCommand implements IDelegateCommandHandler {

	public static final String COMMAND_ID = "xml.catalog.create";

	private static final String CATALOG_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" //
			+ "</catalog>\n";

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		// catalogURI is accepted for future use but currently unused
		// since we just return the template content
		ArgumentsUtils.getArgAt(params, 0, String.class);
		return CATALOG_TEMPLATE;
	}
}
