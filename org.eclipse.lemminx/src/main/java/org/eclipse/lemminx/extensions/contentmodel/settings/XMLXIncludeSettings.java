/**
 *  Copyright (c) 2022 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.settings;

import org.eclipse.lemminx.commons.config.ConfigurationScope;
import org.eclipse.lemminx.commons.config.Scope;

/**
 * XML xi:include settings.
 *
 */
public class XMLXIncludeSettings {

	public XMLXIncludeSettings() {
		setEnabled(false);
	}

	public XMLXIncludeSettings(XMLXIncludeSettings other) {
		this.enabled = other.enabled;
	}

	@Scope(ConfigurationScope.RESOURCE)
	private boolean enabled;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (enabled ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XMLXIncludeSettings other = (XMLXIncludeSettings) obj;
		if (enabled != other.enabled) {
			return false;
		}
		return true;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
