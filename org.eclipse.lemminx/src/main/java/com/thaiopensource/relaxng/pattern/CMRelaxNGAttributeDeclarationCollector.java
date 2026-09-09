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
package com.thaiopensource.relaxng.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;

import com.thaiopensource.util.VoidValue;
import com.thaiopensource.xml.util.Name;

/**
 * RelaxNG class used to collect content model attributes for a given
 * {@link ElementPattern}.
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
public class CMRelaxNGAttributeDeclarationCollector extends AbstractCMRelaxNGCollector {

	private final CMRelaxNGElementDeclaration elementDeclaration;

	// Use a map keyed by attribute name to deduplicate attributes that appear
	// in multiple branches of a <choice>. When the same attribute name is found
	// in different branches, their enumeration values are merged via
	// CMRelaxNGAttributeDeclaration#addMergedPattern.
	private final Map<Name, CMRelaxNGAttributeDeclaration> attributeMap;

	private final Collection<CMAttributeDeclaration> attributes;

	public CMRelaxNGAttributeDeclarationCollector(CMRelaxNGElementDeclaration elementDeclaration, Pattern pattern) {
		this.elementDeclaration = elementDeclaration;
		this.attributeMap = new LinkedHashMap<>();
		pattern.apply(this);
		this.attributes = new ArrayList<>(attributeMap.values());
		if (!attributes.isEmpty()) {
			RequiredAttributesFunction attributesFunction = new RequiredAttributesFunction();
			Set<Name> requiredAttributeNames = pattern.apply(attributesFunction);
			for (Name requiredAttributeName : requiredAttributeNames) {
				CMRelaxNGAttributeDeclaration rngAttribute = attributeMap.get(requiredAttributeName);
				if (rngAttribute != null) {
					rngAttribute.setRequired(true);
				}
			}
		}
	}

	@Override
	public VoidValue caseAttribute(AttributePattern p) {
		NameClass nameClass = p.getNameClass();
		if (nameClass instanceof SimpleNameClass) {
			Name name = ((SimpleNameClass) nameClass).getName();
			CMRelaxNGAttributeDeclaration existing = attributeMap.get(name);
			if (existing != null) {
				// Same attribute name from another <choice> branch: merge its values
				existing.addMergedPattern(p);
			} else {
				CMRelaxNGAttributeDeclaration attributeDeclaration = new CMRelaxNGAttributeDeclaration(
						elementDeclaration, p);
				attributeMap.put(name, attributeDeclaration);
			}
		}
		return VoidValue.VOID;
	}

	public Collection<CMAttributeDeclaration> getAttributes() {
		return attributes;
	}
}
