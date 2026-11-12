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

import org.xml.sax.Locator;

import com.thaiopensource.relaxng.pattern.AttributePattern;
import com.thaiopensource.relaxng.pattern.ElementPattern;
import com.thaiopensource.relaxng.pattern.NameClass;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.relaxng.pattern.SchemaPatternBuilder;

/**
 * This class extends {@link SchemaPatternBuilder} to copy the {@link Locator}
 * when {@link ElementPattern} and {@link AttributePattern} are created, because
 * by default the received locator instance is a singleton and we loose the
 * correct line/character position.
 * 
 * @author Angelo ZERR
 *
 */
public class MySchemaPatternBuilder extends SchemaPatternBuilder {

	private static class MyLocator implements Locator {

		private final String publicId;
		private final String systemId;
		private final int lineNumber;
		private final int columnNumber;

		public MyLocator(Locator loc) {
			this.publicId = loc.getPublicId();
			this.systemId = loc.getSystemId();
			this.lineNumber = loc.getLineNumber();
			this.columnNumber = loc.getColumnNumber();
		}

		@Override
		public String getPublicId() {
			return publicId;
		}

		@Override
		public String getSystemId() {
			return systemId;
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		@Override
		public int getColumnNumber() {
			return columnNumber;
		}
	}

	@Override
	public Pattern makeElement(NameClass nameClass, Pattern content, Locator loc) {
		// Don't call super.makeElement() which interns the pattern.
		// Interning collapses ElementPatterns with the same name and content,
		// losing the locator of subsequent definitions.
		// See https://github.com/eclipse-lemminx/lemminx/issues/1745
		return new ElementPattern(nameClass, content, copy(loc));
	}

	@Override
	public Pattern makeAttribute(NameClass nameClass, Pattern value, Locator loc) {
		return super.makeAttribute(nameClass, value, copy(loc));
	}

	@Override
	public Pattern makeAttribute(NameClass nameClass, Pattern value, Locator loc, String defaultValue) {
		return super.makeAttribute(nameClass, value, copy(loc), defaultValue);
	}

	private static MyLocator copy(Locator loc) {
		return new MyLocator(loc);
	}
}
