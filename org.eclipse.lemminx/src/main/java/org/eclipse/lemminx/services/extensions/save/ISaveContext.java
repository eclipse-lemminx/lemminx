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
 */
package org.eclipse.lemminx.services.extensions.save;

import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.ICodeLensRefresher;
import org.eclipse.lemminx.services.extensions.IDiagnosticsRefresher;

/**
 * Save context API.
 *
 */
public interface ISaveContext extends IXMLDocumentProvider, IDiagnosticsRefresher, ICodeLensRefresher {

	/**
	 * The save context type:
	 *
	 * <ul>
	 * <li>DOCUMENT: a document is saved.</li>
	 * <li>SETTINGS: a settings is saved.</li>
	 * </ul>
	 */
	public enum SaveContextType {
		DOCUMENT, SETTINGS;
	}

	/**
	 * Returns the save context type.
	 *
	 * @return the save context type.
	 */
	SaveContextType getType();

	/**
	 * Returns the document URI which was saved and null if it's a settings which
	 * was saved.
	 *
	 * @return the document URI which was saved and null if it's a settings which
	 *         was saved.
	 */
	String getUri();

	/**
	 * Returns the settings which has changed and null otherwise.
	 *
	 * @return the settings which has changed and null otherwise.
	 */
	Object getSettings();

}
