/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.contentmodel;

import com.thaiopensource.relaxng.pattern.AbstractPatternFunction;
import com.thaiopensource.relaxng.pattern.AfterPattern;
import com.thaiopensource.relaxng.pattern.AttributePattern;
import com.thaiopensource.relaxng.pattern.BinaryPattern;
import com.thaiopensource.relaxng.pattern.ChoicePattern;
import com.thaiopensource.relaxng.pattern.ElementPattern;
import com.thaiopensource.relaxng.pattern.GroupPattern;
import com.thaiopensource.relaxng.pattern.InterleavePattern;
import com.thaiopensource.relaxng.pattern.OneOrMorePattern;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.util.VoidValue;

/**
 * Abstract class to collect {@link ElementPattern}, {@link AttributePattern},
 * etc.
 * 
 * <p>
 * NOTE : this class is hosted in 'com.thaiopensource.relaxng.pattern' because
 * {@link Pattern} implementation like {@link ElementPattern} are not public.
 * Once https://github.com/relaxng/jing-trang/issues/271 will be fixed we could
 * move this class in 'org.eclipse.lemminx.extensions.relaxng.contentmodel'
 * package.
 * </p>
 * 
 * @author Angelo ZERR
 *
 */
public abstract class AbstractCMRelaxNGCollector extends AbstractPatternFunction<VoidValue> {

	@Override
	public VoidValue caseElement(ElementPattern p) {
		return VoidValue.VOID;
	}

	public VoidValue caseAfter(AfterPattern p) {
		return p.getOperand1().apply(this);
	}

	public VoidValue caseBinary(BinaryPattern p) {
		p.getOperand1().apply(this);
		p.getOperand2().apply(this);
		return VoidValue.VOID;
	}

	public VoidValue caseChoice(ChoicePattern p) {
		return caseBinary(p);
	}

	@Override
	public VoidValue caseGroup(GroupPattern p) {
		return caseBinary(p);
	}

	public VoidValue caseInterleave(InterleavePattern p) {
		return caseBinary(p);
	}

	public VoidValue caseOneOrMore(OneOrMorePattern p) {
		return p.getOperand().apply(this);
	}

	@Override
	public VoidValue caseAttribute(AttributePattern p) {
		return VoidValue.VOID;
	}

	public VoidValue caseOther(Pattern p) {
		return VoidValue.VOID;
	}

}
