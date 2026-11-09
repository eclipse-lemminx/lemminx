/**
 *  Copyright (c) 2022 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import java.util.Locale;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

/**
 * DTD diagnostics services tests with DTD Entity.
 *
 */
public class DTDEntityDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void EntityDeclUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!ENTITY copyright \"Copyright W3Schools.\"  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 20, 42, DTDErrorCode.EntityDeclUnterminated));
	}

	@Test
	public void EntityNotDeclaredAddToSubset() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";

		Diagnostic d = d(5, 1, 5, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 29, 2, 29, "\r\n\t<!ENTITY nbsp \"entity-value\">")));
	}

	@Test
	public void EntityNotDeclaredAddToSubsetOneChar() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&a;\r\n" + //
				"</article>";

		Diagnostic d = d(5, 1, 5, 4, DTDErrorCode.EntityNotDeclared,
				"The entity \"a\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 29, 2, 29, "\r\n\t<!ENTITY a \"entity-value\">")));
	}

	@Test
	public void EntityNotDeclaredNoPrologNoDoctype() throws Exception {
		String xml = "<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";

		Diagnostic d = d(1, 1, 1, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d,
				ca(d, te(0, 0, 0, 0, "<!DOCTYPE article [\r\n" + "\t<!ENTITY nbsp \"entity-value\">\r\n" + "]>\r\n")));
	}

	@Test
	public void EntityNotDeclaredWithPrologNoDoctype() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";

		Diagnostic d = d(2, 1, 2, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d,
				te(0, 38, 0, 38, "\r\n<!DOCTYPE article [\r\n" + "\t<!ENTITY nbsp \"entity-value\">\r\n" + "]>")));
	}

	@Test
	public void EntityNotDeclaredWithPrologWithRootSameLine() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><text1>\n" + "<text2>\n" + "\t&c;\n" + "</text2>\n"
				+ "</text1>";

		Diagnostic d = d(2, 1, 2, 4, DTDErrorCode.EntityNotDeclared,
				"The entity \"c\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d,
				ca(d, te(0, 38, 0, 38, "\n<!DOCTYPE text1 [\n" + "\t<!ENTITY c \"entity-value\">\n" + "]>\n")));
	}

	@Test
	public void EntityNotDeclaredDoctypeNoSubset() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article >\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(3, 1, 3, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d, te(1, 18, 1, 18, "[\n\t<!ENTITY nbsp \"entity-value\">\n]")));
	}

	@Test
	public void EntityNotDeclaredDoctypeNoSubsetNoSpace() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article>\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(3, 1, 3, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d, te(1, 17, 1, 17, " [\n\t<!ENTITY nbsp \"entity-value\">\n]")));
	}

	@Test
	public void EntityNotDeclaredDoctypeNoSubsetEndBracketNewLine() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article\n" + //
				">\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(4, 1, 4, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d, te(2, 0, 2, 0, "[\n\t<!ENTITY nbsp \"entity-value\">\n]")));
	}

	@Test
	public void Issue862() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article\n" + //
				">\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(4, 1, 4, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article\n" + //
				">\n" + //
				"<article>\n" + //
				"	&|nbsp;\n" + // set the range
				"</article>";
		testCodeActionsFor(xml, d, ca(d, te(2, 0, 2, 0, "[\n\t<!ENTITY nbsp \"entity-value\">\n]")));
	}

	@Test
	public void EntityNotDeclaredDoctypeEmptySubset() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article []>\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(3, 1, 3, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d, te(1, 19, 1, 19, "\n\t<!ENTITY nbsp \"entity-value\">\n")));
	}

	@Test
	public void EntityNotDeclaredDoctypeEmptySubsetWithNewline() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<!DOCTYPE article [\n" + //
				"]>\n" + //
				"<article>\n" + //
				"	&nbsp;\n" + //
				"</article>";

		Diagnostic d = d(4, 1, 4, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);

		testCodeActionsFor(xml, d, ca(d, te(2, 0, 2, 0, "\t<!ENTITY nbsp \"entity-value\">\n")));
	}

	@Test
	public void EntityNotDeclaredSingleQuotes() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		settings.getFormattingSettings().setInsertSpaces(false);
		Diagnostic d = d(5, 1, 5, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, settings, ca(d, te(2, 29, 2, 29, "\r\n\t<!ENTITY nbsp \'entity-value\'>")));
	}

	@Test
	public void NotationDeclUnterminated() throws Exception {
		String xml = "<?xml version = \"1.0\"?>\r\n" + //
				"<!DOCTYPE Person [\r\n" + //
				"	<!NOTATION png PUBLIC \"PNG 1.0\" \"image/png\"  \r\n" + //
				"]>\r\n" + //
				"<Person Likes=\"\" />"; // <- error on @Likes value
		XMLAssert.testDiagnosticsFor(xml, d(2, 33, 44, DTDErrorCode.NotationDeclUnterminated));
	}

	@Test
	public void EntityNotDeclared() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";

		XMLAssert.testDiagnosticsFor(xml, d(5, 1, 7, DTDErrorCode.EntityNotDeclared));
	}

	@Test
	public void EntityNotDeclaredRespectsIndentSettings1() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		settings.getFormattingSettings().setInsertSpaces(true);
		settings.getFormattingSettings().setTabSize(6);
		Diagnostic d = d(5, 1, 5, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, settings, ca(d, te(2, 29, 2, 29, "\r\n      <!ENTITY nbsp \'entity-value\'>")));
	}

	@Test
	public void EntityNotDeclaredRespectsIndentSettings2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<!DOCTYPE article [\r\n" + //
				"	<!ELEMENT article (#PCDATA)>\r\n" + //
				"]>\r\n" + //
				"<article>\r\n" + //
				"	&nbsp;\r\n" + //
				"</article>";
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		settings.getFormattingSettings().setInsertSpaces(true);
		settings.getFormattingSettings().setTabSize(3);
		Diagnostic d = d(5, 1, 5, 7, DTDErrorCode.EntityNotDeclared,
				"The entity \"nbsp\" was referenced, but not declared.");
		XMLAssert.testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, settings, ca(d, te(2, 29, 2, 29, "\r\n   <!ENTITY nbsp \'entity-value\'>")));
	}

	@Test
	public void defaultEntityExpansionLimit() {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setResolveExternalEntities(true);
		settings.setValidation(validationSettings);

		Locale defaultLocale = Locale.getDefault();
		try {
			// Set local as English for formatting integer in error message with ','
			// See 64,000 in "The parser has encountered more than \"64,000\" entity
			// expansions in this document; this is the limit imposed by the application."
			Locale.setDefault(Locale.ENGLISH);
			String xml = "<?xml version=\"1.0\"?>\r\n" + //
					"<!DOCTYPE lolz [\r\n" + //
					"    <!ENTITY lol \"lol\">\r\n" + //
					"    <!ELEMENT lolz (#PCDATA)>\r\n" + //
					"    <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\r\n" + //
					"    <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\r\n" + //
					"    <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\r\n" + //
					"    <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\r\n" + //
					"    <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\r\n" + //
					"    <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\r\n" + //
					"    <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\r\n" + //
					"    <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\r\n" + //
					"    <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\r\n" + //
					"]>\r\n" + //
					"<lolz>&lol9;</lolz>";

			Diagnostic diagnostic = d(14, 6, 14, 12, DTDErrorCode.EntityExpansionLimitExceeded, //
					"The parser has encountered more than \"64,000\" entity expansions in this document; this is the limit imposed by the application.",
					"xml", DiagnosticSeverity.Error);
			XMLAssert.testDiagnosticsFor(new XMLLanguageService(), xml, null, null, null, false, settings, diagnostic);
		} finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void customEntityExpansionLimit() {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setResolveExternalEntities(true);
		settings.setValidation(validationSettings);

		try {
			System.setProperty("jdk.xml.entityExpansionLimit", "10");

			String xml = "<?xml version=\"1.0\"?>\r\n" + //
					"<!DOCTYPE lolz [\r\n" + //
					"    <!ENTITY lol \"lol\">\r\n" + //
					"    <!ELEMENT lolz (#PCDATA)>\r\n" + //
					"    <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\r\n" + //
					"    <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\r\n" + //
					"    <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\r\n" + //
					"    <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\r\n" + //
					"    <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\r\n" + //
					"    <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\r\n" + //
					"    <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\r\n" + //
					"    <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\r\n" + //
					"    <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\r\n" + //
					"]>\r\n" + //
					"<lolz>&lol9;</lolz>";

			Diagnostic diagnostic = d(14, 6, 14, 12, DTDErrorCode.EntityExpansionLimitExceeded, //
					"The parser has encountered more than \"10\" entity expansions in this document; this is the limit imposed by the application.",
					"xml", DiagnosticSeverity.Error);
			XMLAssert.testDiagnosticsFor(new XMLLanguageService(), xml, null, null, null, false, settings, diagnostic);

		} finally {
			System.setProperty("jdk.xml.entityExpansionLimit", "");
		}
	}

}
