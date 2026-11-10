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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings;

import org.eclipse.lemminx.utils.JSONUtility;

/**
 * RNC settings, mapped from {@code xml.rnc.*}.
 *
 * <p>
 * Follows the {@code ContentModelSettings} pattern: a settings class
 * deserialized from the client's {@code xml} JSON object via
 * {@link JSONUtility#toModel(Object, Class)}.
 * </p>
 */
public class RNCSettings {

	private RNCFormattingSettings format;

	public RNCFormattingSettings getFormat() {
		return format;
	}

	public void setFormat(RNCFormattingSettings format) {
		this.format = format;
	}

	/**
	 * Extracts RNC settings from the given initialization options.
	 *
	 * <p>
	 * The {@code initSettings} object is the deserialized JSON under the
	 * {@code xml} key (after {@code AllXMLSettings.getAllXMLSettings()}).
	 * This method extracts the {@code rnc} sub-object from it.
	 * </p>
	 *
	 * @param initSettings the settings object (content under {@code xml}).
	 * @return the RNC settings, or {@code null} if not present.
	 */
	public static RNCSettings getRNCSettings(Object initSettings) {
		Wrapper wrapper = JSONUtility.toModel(initSettings, Wrapper.class);
		return wrapper != null ? wrapper.rnc : null;
	}

	private static class Wrapper {
		@SuppressWarnings("unused")
		RNCSettings rnc;
	}
}
