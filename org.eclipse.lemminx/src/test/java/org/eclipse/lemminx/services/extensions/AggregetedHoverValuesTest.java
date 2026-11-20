/**
 *  Copyright (c) 2018 Angelo ZERR. Seiphon Wang.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *  Seiphon Wang <seiphon.wang@liferay.com>
 */
package org.eclipse.lemminx.services.extensions;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.utils.MarkupContentFactory.MARKDOWN_SEPARATOR;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.extensions.hover.HoverParticipantAdapter;
import org.eclipse.lemminx.services.extensions.hover.IHoverRequest;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.Test;

/**
 * XML aggregated hover values tests
 *
 */
public class AggregetedHoverValuesTest extends AbstractCacheBasedTest {
	private static final String TEST_FOR_TAG_HOVER = "test for tag hover";
	private static final String TEST_FOR_ATTRIBUTENAME_HOVER = "test for attribute name hover";

	@Test
	public void testTagHover() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("spring-beans-3.0.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ "	<bea|n>";
		assertHover(xml, TEST_FOR_TAG_HOVER + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				MARKDOWN_SEPARATOR + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Defines a single (usually named) bean." + //
				System.lineSeparator() + System.lineSeparator() + //
				"A bean definition may contain nested tags for constructor arguments, property values, lookup methods, and replaced methods. Mixing constructor injection and setter injection on the same bean is explicitly supported."
				+ //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [spring-beans-3.0.xsd](" + schemaURI + ")", r(2, 2, 2, 6));
	};

	@Test
	public void testAttributeNameHover() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("spring-beans-3.0.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ "	<bean clas|s=>";
		assertHover(xml, TEST_FOR_ATTRIBUTENAME_HOVER + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				MARKDOWN_SEPARATOR + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"The fully qualified name of the bean's class, except if it serves only as a parent definition for child bean definitions."
				+ //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [spring-beans-3.0.xsd](" + schemaURI + ")", null);
	};

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverOffset)
			throws BadLocationException {
		XMLAssert.assertHover(new AggregatedHoverLanguageService(), value, "src/test/resources/catalogs/catalog.xml",
				null, expectedHoverLabel, expectedHoverOffset);
	}

	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true).replace("///",
				"/");
	}

	private static class AggregatedHoverLanguageService extends XMLLanguageService {

		public AggregatedHoverLanguageService() {
			super.registerHoverParticipant(new AggregatedHoverParticipant());
		}

		class AggregatedHoverParticipant extends HoverParticipantAdapter {

			@Override
			public Hover onTag(IHoverRequest request, CancelChecker cancelChecker) throws Exception {
				if ("bean".equals(request.getCurrentTag())) {
					return createHover(TEST_FOR_TAG_HOVER);
				}
				return null;
			}

			@Override
			public Hover onAttributeName(IHoverRequest request, CancelChecker cancelChecker) throws Exception {
				if ("class".equals(request.getCurrentAttributeName())) {
					return createHover(TEST_FOR_ATTRIBUTENAME_HOVER);
				}
				return null;
			}

			private Hover createHover(String value) {
				return new Hover(new MarkupContent(MarkupKind.PLAINTEXT, value));
			}

		}
	}
}
