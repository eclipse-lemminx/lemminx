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
package org.eclipse.lemminx.extensions.relaxng.xml.completion;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.BaseFileTempTest;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

/**
 * XML completion tests based on RelaxNG with XML catalog.
 *
 */
public class XMLCompletionBasedOnRelaxNGWithCatalogTest extends BaseFileTempTest {

	@Test
	public void completionInRoot() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"<|\r\n" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				1 + XMLAssert.CDATA_SNIPPETS + XMLAssert.COMMENT_SNIPPETS, //, //
				c("teiHeader", te(1, 0, 1, 1, "<teiHeader></teiHeader>"), "<teiHeader"));
	}

	@Test
	public void completionInDocumentElement() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader>\r\n" + //
				"      <|\r\n" + //
				"    </teiHeader>" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("fileDesc", te(2, 6, 2, 7, "<fileDesc></fileDesc>"), "<fileDesc"));
	}

	@Test
	public void completionInAnElementAfter() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader>\r\n" + //
				"      <fileDesc>\r\n" + //
				"         <titleStmt>\r\n" + //
				"         </titleStmt>\r\n" + //
				"         <|\r\n" + //
				"      </fileDesc>  \r\n" + //
				"   </teiHeader>" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("publicationStmt", te(5, 9, 5, 10, "<publicationStmt></publicationStmt>"), "<publicationStmt"), //
				c("editionStmt", te(5, 9, 5, 10, "<editionStmt></editionStmt>"), "<editionStmt"), //
				c("extent", te(5, 9, 5, 10, "<extent></extent>"), "<extent"));
	}

	@Test
	public void completionInAnElement() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader>\r\n" + //
				"      <fileDesc>\r\n" + //
				"         <titleStmt>\r\n" + //
				"            <title>\r\n" + //
				"               <|\r\n" + //
				"            </title>\r\n" + //
				"         </titleStmt>\r\n" + //
				"      </fileDesc>  \r\n" + //
				"   </teiHeader>" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("terrain", te(5, 15, 5, 16, "<terrain></terrain>"), "<terrain"), //
				c("abbr", te(5, 15, 5, 16, "<abbr></abbr>"), "<abbr"));
	}

	@Test
	public void completionWithRequiredAttributes() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader>\r\n" + //
				"      <fileDesc>\r\n" + //
				"      </fileDesc>\r\n" + //
				"      <encodingDesc>\r\n" + //
				"         <appInfo>\r\n" + //
				"            <|\r\n" + //
				"         </appInfo>\r\n" + //
				"      </encodingDesc>\r\n" + //
				"   </teiHeader>" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("application", //
						te(6, 12, 6, 13, "<application ident=\"\" version=\"\"></application>"), "<application"));
	}

	@Test
	public void completionForAttributeNames() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" |>\r\n" + //
				"\r\n" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("rend", te(0, 41, 0, 41, "rend=\"\""), "rend"), //
				c("xml:space", te(0, 41, 0, 41, "xml:space=\"default\""), "xml:space"));
	}

	@Test
	public void completionForAttributeValues() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader>\r\n" + //
				"     <fileDesc cert=\"|\">\r\n" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				4, //
				c("high", te(2, 21, 2, 21, "high"), "high"), //
				c("medium", te(2, 21, 2, 21, "medium"), "medium"), //
				c("low", te(2, 21, 2, 21, "low"), "low"), //
				c("unknown", te(2, 21, 2, 21, "unknown"), "unknown"));
	}

	@Test
	public void completionForXMLSpaceAttributeValues() throws BadLocationException {
		// completion on <|
		String xml = "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"   <teiHeader xml:space=\"|\">\r\n" + //
				"</TEI>";
		testCompletionWithCatalogFor(xml, //
				c("preserve", te(1, 25, 1, 25, "preserve"), "preserve"));
	}

	private static void testCompletionWithCatalogFor(String value, CompletionItem... expectedItems)
			throws BadLocationException {
		testCompletionWithCatalogFor(value, null, expectedItems);
	}

	private static void testCompletionWithCatalogFor(String value, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(new XMLLanguageService(), value, "src/test/resources/relaxng/catalog-relaxng.xml",
				null, "src/test/resources/relaxng/test.xml", expectedCount, true, expectedItems);
	}
}