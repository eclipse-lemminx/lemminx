/*******************************************************************************
* Copyright (c) 2026 IBM Corp. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corp. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.settings;

/**
 * Utility methods to support {@link FileAssociationVariableSupport} and {@link CatalogVariableSupport}
 */
public final class VariableSupportUtils {

	private VariableSupportUtils() {
	}

	static final String ENV_PREFIX = "${env:";
	static final String VARIABLE_PREFIX = "${";

	static String resolveEnvVariables(String value) {
		int startIndex = 0;
		while (true) {
			int envStart = value.indexOf(VariableSupportUtils.ENV_PREFIX, startIndex);
			if (envStart < 0) {
				break;
			}
			int envEnd = value.indexOf('}', envStart + VariableSupportUtils.ENV_PREFIX.length());
			if (envEnd < 0) {
				break;
			}
			String envName = value.substring(envStart + VariableSupportUtils.ENV_PREFIX.length(), envEnd);
			String envValue = System.getenv(envName);
			if (envValue != null) {
				value = value.substring(0, envStart) + envValue + value.substring(envEnd + 1);
				startIndex = envStart + envValue.length();
			} else {
				startIndex = envEnd + 1;
			}
		}
		return value;
	}

	/**
	 * Returns true if the given value contains variable references.
	 *
	 * @param value the value to check.
	 * @return true if the value contains {@code ${...}} variable references.
	 */
	public static boolean hasVariables(String value) {
		return value != null && value.contains(VariableSupportUtils.VARIABLE_PREFIX);
	}

}
