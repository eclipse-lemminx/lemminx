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

/**
 * RNC formatting settings, mapped from {@code xml.rnc.format.*}.
 */
public class RNCFormattingSettings {

	private int blankLinesBetweenDefines = 1;

	public int getBlankLinesBetweenDefines() {
		return blankLinesBetweenDefines;
	}

	public void setBlankLinesBetweenDefines(int blankLinesBetweenDefines) {
		this.blankLinesBetweenDefines = blankLinesBetweenDefines;
	}
}
