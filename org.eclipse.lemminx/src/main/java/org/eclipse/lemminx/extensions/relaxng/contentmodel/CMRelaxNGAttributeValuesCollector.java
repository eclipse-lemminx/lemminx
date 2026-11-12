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

import java.util.ArrayList;
import java.util.List;

import com.thaiopensource.relaxng.pattern.AttributePattern;
import com.thaiopensource.relaxng.pattern.ElementPattern;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.relaxng.pattern.ValuePattern;
import com.thaiopensource.util.VoidValue;

/**
 * RelaxNG class used to collect enumeration values for a given
 * {@link AttributePattern}.
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
public class CMRelaxNGAttributeValuesCollector extends AbstractCMRelaxNGCollector {

	private final List<String> values;

	public CMRelaxNGAttributeValuesCollector(Pattern pattern) {
		this.values = new ArrayList<>();
		pattern.apply(this);
	}

	@Override
	public VoidValue caseValue(ValuePattern p) {
		values.add(p.getStringValue());
		return super.caseValue(p);
	}

	public List<String> getValues() {
		return values;
	}
}
