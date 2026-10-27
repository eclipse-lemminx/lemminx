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
 * XML Inlay Hint settings.
 */
public class XMLInlayHintSettings {

	private boolean enabled;

	private boolean showElementType = true;

	private boolean showAttributeType = true;

	private boolean showDefaultValue = true;

	private transient boolean refreshSupport;

	/**
	 * Returns true if inlay hints are enabled.
	 *
	 * @return true if inlay hints are enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isShowElementType() {
		return showElementType;
	}

	public void setShowElementType(boolean showElementType) {
		this.showElementType = showElementType;
	}

	public boolean isShowAttributeType() {
		return showAttributeType;
	}

	public void setShowAttributeType(boolean showAttributeType) {
		this.showAttributeType = showAttributeType;
	}

	public boolean isShowDefaultValue() {
		return showDefaultValue;
	}

	public void setShowDefaultValue(boolean showDefaultValue) {
		this.showDefaultValue = showDefaultValue;
	}

	/**
	 * Returns true if the client supports workspace/inlayHint/refresh and false
	 * otherwise.
	 *
	 * @return true if the client supports workspace/inlayHint/refresh and false
	 *         otherwise.
	 */
	public boolean isRefreshSupport() {
		return refreshSupport;
	}

	/**
	 * Set true if the client supports workspace/inlayHint/refresh and false
	 * otherwise.
	 *
	 * @param refreshSupport
	 */
	public void setRefreshSupport(boolean refreshSupport) {
		this.refreshSupport = refreshSupport;
	}

	public void merge(XMLInlayHintSettings newSettings) {
		this.enabled = newSettings.isEnabled();
		this.showElementType = newSettings.isShowElementType();
		this.showAttributeType = newSettings.isShowAttributeType();
		this.showDefaultValue = newSettings.isShowDefaultValue();
	}
}
