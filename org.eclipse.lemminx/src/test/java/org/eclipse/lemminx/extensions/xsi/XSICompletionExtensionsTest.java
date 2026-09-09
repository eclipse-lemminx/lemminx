/**
 *  Copyright (c) 2019 Red Hat, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.xsi;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.xsl.XSLURIResolverExtension;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * XSL completion tests which test the {@link XSLURIResolverExtension}.
 *
 */
public class XSICompletionExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void completion() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=|>";
		testCompletionFor(xml,
				c("true", te(1, 71, 1, 71, "\"true\""), "\"true\""),
				c("false", te(1, 71, 1, 71, "\"false\""), "\"false\""));
	}

	@Test
	public void completionItemDefaults() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=|>";
		testCompletionFor(xml, true,
				c("true", te(1, 71, 1, 71, "\"true\""), "\"true\""),
				c("false", te(1, 71, 1, 71, "\"false\""), "\"false\""));
	}

	@Test
	public void completion2() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"|\">";
		testCompletionFor(xml,
				c("true", te(1, 72, 1, 72, "true"), "true"),
				c("false", te(1, 72, 1, 72, "false"), "false"));
	}

	@Test
	public void completion3() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\r\n" +
				"  <a xsi:nil=|> </a> ";
		testCompletionFor(xml,
				c("true", te(2, 13, 2, 13, "\"true\""), "\"true\""),
				c("false", te(2, 13, 2, 13, "\"false\""), "\"false\""));
	}

	@Test
	public void completion3NNamespace() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project >\r\n" +
				"  <a xsi:nil=|> </a> ";
		testCompletionFor(xml);
	}

	@Test
	public void completion4() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\r\n" +
				"  <a xsi:nil=|> </a> ";

		testCompletionFor(xml, singleQuotesSharedSettings(),
				c("true", te(2, 13, 2, 13, "\'true\'"), "\'true\'"),
				c("false", te(2, 13, 2, 13, "\'false\'"), "\'false\'"));
	}

	@Test
	public void completionXMLNSXSIValue() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=| >\r\n" +
				"  <a> </a> \r\n"+
				"</project>";
		testCompletionFor(xml,
				c("http://www.w3.org/2001/XMLSchema-instance", te(1, 19, 1, 19, "\"http://www.w3.org/2001/XMLSchema-instance\""), "\"http://www.w3.org/2001/XMLSchema-instance\"")
				); // coming from stylesheet children
	}

	@Test
	public void completionXMLNSXSIValueSingleQuotes() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:xsi=| >\r\n" +
				"  <a> </a> \r\n"+
				"</project>";
		testCompletionFor(xml, singleQuotesSharedSettings(),
				c("http://www.w3.org/2001/XMLSchema-instance", te(1, 19, 1, 19, "\'http://www.w3.org/2001/XMLSchema-instance\'"), "\'http://www.w3.org/2001/XMLSchema-instance\'")
				); // coming from stylesheet children
	}

	@Test
	public void completionXMLNSXSIWhole() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project xmlns:x| >\r\n" +
				"  <a> </a> \r\n"+
				"</project>";
		testCompletionFor(xml,
				c("xmlns:xsi", te(1, 9, 1, 16, "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""), "xmlns:xsi")
				); // coming from stylesheet children
	}

	@Test
	public void completionXMLNS() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project x| >\r\n" +
				"  <a> </a> \r\n"+
				"</project>";
		testCompletionFor(xml,
				c("xmlns", te(1, 9, 1, 10, "xmlns=\"\""), "xmlns")
				); // coming from stylesheet children
	}

	@Test
	public void completionXMLNSOnlyInRoot() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<project>\r\n" +
				"  <a x|> </a> \r\n"+
				"</project>";
		testCompletionFor(xml
				); // coming from stylesheet children
	}

	@Test
	public void xsiTypeValueCompletionWithPrefix() throws BadLocationException {
		String xml = "<root xmlns=\"http://example.com/test\"\r\n" + //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"      xmlns:tns=\"http://example.com/test\"\r\n" + //
				"      xsi:schemaLocation=\"http://example.com/test xsd/xsitype-ns.xsd\">\r\n" + //
				"  <item xsi:type=\"|\">\r\n" + //
				"  </item>\r\n" + //
				"</root>";
		testCompletionFor(xml, "src/test/resources/xsitype-ns.xml",
				c("tns:DerivedType", te(4, 18, 4, 18, "tns:DerivedType"), "tns:DerivedType"));
	}

	@Test
	public void xsiTypeValueCompletionWithDefaultNamespace() throws BadLocationException {
		String xml = "<root xmlns=\"http://example.com/test\"\r\n" + //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"      xsi:schemaLocation=\"http://example.com/test xsd/xsitype-ns.xsd\">\r\n" + //
				"  <item xsi:type=\"|\">\r\n" + //
				"  </item>\r\n" + //
				"</root>";
		testCompletionFor(xml, "src/test/resources/xsitype-ns.xml",
				c("DerivedType", te(3, 18, 3, 18, "DerivedType"), "DerivedType"));
	}

	@Test
	public void xsiTypeValueCompletionWithAbstractType() throws BadLocationException {
		// Schema with no namespace, abstract base type 'Character',
		// and two derived types 'Teacher' and 'Student'
		String xml = "<root\r\n" + //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"      xsi:noNamespaceSchemaLocation=\"xsd/xsitype-abstract.xsd\">\r\n" + //
				"  <Character xsi:type=\"|\" Name=\"test\">\r\n" + //
				"  </Character>\r\n" + //
				"</root>";
		testCompletionFor(xml, "src/test/resources/xsitype-abstract.xml",
				c("Teacher", te(3, 23, 3, 23, "Teacher"), "Teacher"),
				c("Student", te(3, 23, 3, 23, "Student"), "Student"));
	}

	private SharedSettings singleQuotesSharedSettings() {
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		return settings;
	}

	private void testCompletionFor(String xml, CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(xml, null, expectedItems);
	}

	private void testCompletionFor(String xml, String fileURI, CompletionItem... expectedItems)
			throws BadLocationException {
		XMLAssert.testCompletionFor(xml, null, fileURI, null, expectedItems);
	}

	private void testCompletionFor(String xml, SharedSettings sharedSettings, CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(new XMLLanguageService(), xml, null, null, null, null, sharedSettings, expectedItems);
	}

	private void testCompletionFor(String xml, boolean enableItemDefaults, CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(xml, null, enableItemDefaults, expectedItems);
	}
}
