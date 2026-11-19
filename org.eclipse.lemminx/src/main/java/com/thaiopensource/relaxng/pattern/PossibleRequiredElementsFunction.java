/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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

import java.util.Set;

class PossibleRequiredElementsFunction extends AbstractPatternFunction<Void> {

	private final Set<String> requiredElementNames;

	public PossibleRequiredElementsFunction(Set<String> requiredElementNames) {
		this.requiredElementNames = requiredElementNames;
	}

	@Override
	public Void caseOther(Pattern p) {
		return null;
	}

	@Override
	public Void caseElement(ElementPattern p) {
		return caseNamed(p.getNameClass());
	}

	@Override
	public Void caseGroup(GroupPattern p) {
		return union(p);
	}

	@Override
	public Void caseChoice(ChoicePattern p) {
		return union(p);
	}

	@Override
	public Void caseInterleave(InterleavePattern p) {
		return union(p);
	}

	@Override
	public Void caseAfter(AfterPattern p) {
		return p.getOperand1().apply(this);
	}

	@Override
	public Void caseOneOrMore(OneOrMorePattern p) {
		return p.getOperand().apply(this);
	}

	private Void caseNamed(NameClass nc) {
		if (!(nc instanceof SimpleNameClass))
			return null;
		requiredElementNames.add(((SimpleNameClass) nc).getName().getLocalName());
		return null;
	}
	
	private Void union(BinaryPattern p) {
		Pattern p1 = p.getOperand1();
		if (!p1.isNullable()) {
			p1.apply(this);
		}
		Pattern p2 = p.getOperand2();
		if (!p2.isNullable()) {
			p2.apply(this);
		}
		return null;
	}
}
