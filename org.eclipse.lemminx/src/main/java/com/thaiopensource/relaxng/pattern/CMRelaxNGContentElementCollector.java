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
package com.thaiopensource.relaxng.pattern;

import com.thaiopensource.util.VoidValue;

/**
 * RelaxNG collector that resolves choice groups by only collecting elements from
 * the first alternative. Used for XML generation to produce valid content.
 *
 * <p>
 * Unlike {@link CMRelaxNGElementDeclarationCollector} which collects ALL
 * elements from all choice branches, this collector picks only the first branch
 * of each {@code <choice>} group.
 * </p>
 */
public class CMRelaxNGContentElementCollector extends CMRelaxNGElementDeclarationCollector {

	public CMRelaxNGContentElementCollector(CMRelaxNGDocument document, Pattern pattern) {
		super(document, pattern);
	}

	@Override
	public VoidValue caseChoice(ChoicePattern p) {
		p.getOperand1().apply(this);
		return VoidValue.VOID;
	}
}
