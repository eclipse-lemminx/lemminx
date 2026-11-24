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
 * Resolves VS Code-style predefined variables in catalog uris
 *
 * <p>
 * Supported variables:
 * </p>
 * <ul>
 * <li>{@code ${env:Name}} — environment variable</li>
 * </ul>
 */
public class CatalogVariableSupport {

	static String resolve(String value) {
		if (!VariableSupportUtils.hasVariables(value)) {
			return value;
		}
		String result = value;
		result = VariableSupportUtils.resolveEnvVariables(value);
		return result;
	}

}
