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
import java.util.List;

import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;

import com.thaiopensource.util.VoidValue;
import com.thaiopensource.xml.util.Name;

/**
 * RelaxNG class used to collect content model elements children for a given
 * {@link ElementPatter}.
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
public class CMRelaxNGElementDeclarationCollector extends AbstractCMRelaxNGCollector {

	private final CMRelaxNGDocument document;

	private final Collection<CMElementDeclaration> elements;

	public CMRelaxNGElementDeclarationCollector(CMRelaxNGDocument document, Pattern pattern) {
		this.document = document;
		this.elements = new ArrayList<>();
		pattern.apply(this);
	}

	@Override
	public VoidValue caseElement(ElementPattern p) {
		NameClass nameClass = p.getNameClass();
		if (nameClass instanceof SimpleNameClass) {
			CMRelaxNGElementDeclaration elementDeclaration = document.getPatternElement(p);
			elements.add(elementDeclaration);
		} else {
			List<Name> names = new ArrayList<>();
			collectSimpleNames(nameClass, names);
			for (Name name : names) {
				CMRelaxNGElementDeclaration elementDeclaration = document.createPatternElement(p, name);
				elements.add(elementDeclaration);
			}
		}
		return VoidValue.VOID;
	}

	private static void collectSimpleNames(NameClass nameClass, List<Name> names) {
		nameClass.accept(new NameClassVisitor() {
			@Override
			public void visitChoice(NameClass nc1, NameClass nc2) {
				collectSimpleNames(nc1, names);
				collectSimpleNames(nc2, names);
			}

			@Override
			public void visitName(Name name) {
				names.add(name);
			}

			@Override
			public void visitNsName(String ns) {
			}

			@Override
			public void visitNsNameExcept(String ns, NameClass nc) {
			}

			@Override
			public void visitAnyName() {
			}

			@Override
			public void visitAnyNameExcept(NameClass nc) {
			}

			@Override
			public void visitNull() {
			}

			@Override
			public void visitError() {
			}
		});
	}

	public Collection<CMElementDeclaration> getElements() {
		return elements;
	}
}
