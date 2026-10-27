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
package org.eclipse.lemminx.settings;

/**
 * XML semantic tokens settings
 */
public class XMLSemanticTokensSettings {

	private boolean enabled = true;

	// Not deserialized from JSON settings; set from client capabilities
	private transient boolean refreshSupport;

	/**
	 * Returns true if semantic tokens service is enabled and false otherwise.
	 *
	 * @return true if semantic tokens service is enabled and false otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set true if semantic tokens service is enabled and false otherwise.
	 *
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns true if the client supports workspace/semanticTokens/refresh and
	 * false otherwise.
	 *
	 * @return true if the client supports workspace/semanticTokens/refresh and
	 *         false otherwise.
	 */
	public boolean isRefreshSupport() {
		return refreshSupport;
	}

	/**
	 * Set true if the client supports workspace/semanticTokens/refresh and false
	 * otherwise.
	 *
	 * @param refreshSupport
	 */
	public void setRefreshSupport(boolean refreshSupport) {
		this.refreshSupport = refreshSupport;
	}

	public void merge(XMLSemanticTokensSettings newSettings) {
		this.setEnabled(newSettings.isEnabled());
	}
}
