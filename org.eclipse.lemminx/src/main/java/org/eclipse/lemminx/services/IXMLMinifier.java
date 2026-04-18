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
package org.eclipse.lemminx.services;

import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML minifier API.
 *
 */
public interface IXMLMinifier {

	/**
	 * Minify the given text document.
	 *
	 * @param text           the text.
	 * @param uri            the uri.
	 * @param sharedSettings the shared settings.
	 * @param cancelChecker  the cancel checker.
	 *
	 * @return the minified text document.
	 */
	String minify(String text, String uri, SharedSettings sharedSettings, CancelChecker cancelChecker);
}
