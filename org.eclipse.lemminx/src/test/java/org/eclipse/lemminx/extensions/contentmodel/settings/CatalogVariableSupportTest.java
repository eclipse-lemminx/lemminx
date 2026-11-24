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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogVariableSupport}
 */
public class CatalogVariableSupportTest {

	@Test
	public void resolveEnvVariable() {
		String pathValue = System.getenv("PATH");
		if (pathValue == null) {
			return;
		}
		String result = CatalogVariableSupport.resolve(
				"${env:PATH}");
		assertEquals(pathValue, result);
	}

	@Test
	public void resolveUnknownEnvVariable() {
		String result = CatalogVariableSupport.resolve(
				"${env:UNLIKELY_VAR_NAME_12345}");
		assertEquals("${env:UNLIKELY_VAR_NAME_12345}", result);
	}
	
}
