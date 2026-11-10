/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.ds;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.si;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testDocumentSymbolsFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testSymbolInformationsFor;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.Test;

public class RNCDocumentSymbolsTest {

	// -------------------- Document Symbols (hierarchical) --------------------

	@Test
	public void testDocumentSymbolsDefines() {
		String value = "start = root" + LS
				+ "root = element root {" + LS
				+ "  text" + LS
				+ "}" + LS
				+ "name = element name { text }";
		testDocumentSymbolsFor(value,
				ds("start", SymbolKind.Event, r(0, 0, 0, 12), r(0, 0, 0, 5)),
				ds("root", SymbolKind.Variable, r(1, 0, 3, 1), r(1, 0, 1, 4),
						ds("element root", SymbolKind.Field, r(1, 7, 3, 1), r(1, 15, 1, 19))),
				ds("name", SymbolKind.Variable, r(4, 0, 4, 28), r(4, 0, 4, 4),
						ds("element name", SymbolKind.Field, r(4, 7, 4, 28), r(4, 15, 4, 19))));
	}

	@Test
	public void testDocumentSymbolsNamespace() {
		String value = "namespace a = \"http://example.com\"";
		testDocumentSymbolsFor(value,
				ds("namespace a", SymbolKind.Namespace, r(0, 0, 0, 34), r(0, 10, 0, 11)));
	}

	@Test
	public void testDocumentSymbolsDefaultNamespace() {
		String value = "default namespace = \"http://example.com\"";
		testDocumentSymbolsFor(value,
				ds("default namespace", SymbolKind.Namespace, r(0, 0, 0, 40), r(0, 0, 0, 17)));
	}

	@Test
	public void testDocumentSymbolsDatatypes() {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"";
		testDocumentSymbolsFor(value,
				ds("datatypes xsd", SymbolKind.Namespace, r(0, 0, 0, 60), r(0, 10, 0, 13)));
	}

	@Test
	public void testDocumentSymbolsInclude() {
		String value = "include \"base.rnc\"";
		testDocumentSymbolsFor(value,
				ds("base.rnc", SymbolKind.Module, r(0, 0, 0, 18), r(0, 8, 0, 18)));
	}

	@Test
	public void testDocumentSymbolsElement() {
		testDocumentSymbolsFor("element name { text }",
				ds("element name", SymbolKind.Field, r(0, 0, 0, 21), r(0, 8, 0, 12)));
	}

	@Test
	public void testDocumentSymbolsAttribute() {
		testDocumentSymbolsFor("attribute id { text }",
				ds("attribute id", SymbolKind.Property, r(0, 0, 0, 21), r(0, 10, 0, 12)));
	}

	@Test
	public void testDocumentSymbolsEmpty() {
		testDocumentSymbolsFor("");
	}

	@Test
	public void testDocumentSymbolsFullSchema() {
		String value = "default namespace = \"http://example.com\"" + LS
				+ "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS
				+ "start = root" + LS
				+ "root = element root {" + LS
				+ "  text" + LS
				+ "}";
		testDocumentSymbolsFor(value,
				ds("default namespace", SymbolKind.Namespace, r(0, 0, 0, 40), r(0, 0, 0, 17)),
				ds("datatypes xsd", SymbolKind.Namespace, r(1, 0, 1, 60), r(1, 10, 1, 13)),
				ds("start", SymbolKind.Event, r(2, 0, 2, 12), r(2, 0, 2, 5)),
				ds("root", SymbolKind.Variable, r(3, 0, 5, 1), r(3, 0, 3, 4),
						ds("element root", SymbolKind.Field, r(3, 7, 5, 1), r(3, 15, 3, 19))));
	}

	@Test
	public void testDocumentSymbolsMultipleElements() {
		String value = "element root {" + LS
				+ "  text" + LS
				+ "}" + LS
				+ "element child { empty }";
		testDocumentSymbolsFor(value,
				ds("element root", SymbolKind.Field, r(0, 0, 2, 1), r(0, 8, 0, 12)),
				ds("element child", SymbolKind.Field, r(3, 0, 3, 23), r(3, 8, 3, 13)));
	}

	@Test
	public void testDocumentSymbolsStartWithInlineElement() {
		testDocumentSymbolsFor("start = element root { text }",
				ds("start", SymbolKind.Event, r(0, 0, 0, 29), r(0, 0, 0, 5),
						ds("element root", SymbolKind.Field, r(0, 8, 0, 29), r(0, 16, 0, 20))));
	}

	@Test
	public void testDocumentSymbolsIncludeEmptyUri() {
		// include "" should use fallback name "include", not empty string
		String value = "include \"\"";
		testDocumentSymbolsFor(value,
				ds("include", SymbolKind.Module, r(0, 0, 0, 10), r(0, 8, 0, 10)));
	}

	@Test
	public void testDocumentSymbolsIncludeWithBody() {
		String value = "include \"base.rnc\" {" + LS
				+ "  start = foo" + LS
				+ "}";
		testDocumentSymbolsFor(value,
				ds("base.rnc", SymbolKind.Module, r(0, 0, 2, 1), r(0, 8, 0, 18)));
	}

	@Test
	public void testDocumentSymbolsNestedElements() {
		String value = "root = element root {" + LS
				+ "  element name { text }" + LS
				+ "}";
		testDocumentSymbolsFor(value,
				ds("root", SymbolKind.Variable, r(0, 0, 2, 1), r(0, 0, 0, 4),
						ds("element root", SymbolKind.Field, r(0, 7, 2, 1), r(0, 15, 0, 19),
								ds("element name", SymbolKind.Field, r(1, 2, 1, 23), r(1, 10, 1, 14)))));
	}

	@Test
	public void testDocumentSymbolsNestedElementAndAttribute() {
		String value = "root = element root {" + LS
				+ "  attribute id { text }," + LS
				+ "  element child { text }" + LS
				+ "}";
		testDocumentSymbolsFor(value,
				ds("root", SymbolKind.Variable, r(0, 0, 3, 1), r(0, 0, 0, 4),
						ds("element root", SymbolKind.Field, r(0, 7, 3, 1), r(0, 15, 0, 19),
								ds("attribute id", SymbolKind.Property, r(1, 2, 1, 23), r(1, 12, 1, 14)),
								ds("element child", SymbolKind.Field, r(2, 2, 2, 24), r(2, 10, 2, 15)))));
	}

	// -------------------- Symbol Informations (flat) --------------------

	@Test
	public void testSymbolInformationsDefines() {
		String value = "start = root" + LS
				+ "root = element root {" + LS
				+ "  text" + LS
				+ "}" + LS
				+ "name = element name { text }";
		testSymbolInformationsFor(value,
				si("start", SymbolKind.Event, r(0, 0, 0, 12)),
				si("root", SymbolKind.Variable, r(1, 0, 3, 1)),
				si("name", SymbolKind.Variable, r(4, 0, 4, 28)));
	}

	@Test
	public void testSymbolInformationsNamespace() {
		testSymbolInformationsFor("namespace a = \"http://example.com\"",
				si("namespace a", SymbolKind.Namespace, r(0, 0, 0, 34)));
	}

	@Test
	public void testSymbolInformationsElement() {
		testSymbolInformationsFor("element name { text }",
				si("element name", SymbolKind.Field, r(0, 0, 0, 21)));
	}

	@Test
	public void testSymbolInformationsAttribute() {
		testSymbolInformationsFor("attribute id { text }",
				si("attribute id", SymbolKind.Property, r(0, 0, 0, 21)));
	}

	@Test
	public void testSymbolInformationsEmpty() {
		testSymbolInformationsFor("");
	}

	@Test
	public void testSymbolInformationsFullSchema() {
		String value = "default namespace = \"http://example.com\"" + LS
				+ "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS
				+ "start = root" + LS
				+ "root = element root {" + LS
				+ "  text" + LS
				+ "}";
		testSymbolInformationsFor(value,
				si("default namespace", SymbolKind.Namespace, r(0, 0, 0, 40)),
				si("datatypes xsd", SymbolKind.Namespace, r(1, 0, 1, 60)),
				si("start", SymbolKind.Event, r(2, 0, 2, 12)),
				si("root", SymbolKind.Variable, r(3, 0, 5, 1)));
	}

	@Test
	public void testSymbolInformationsInclude() {
		testSymbolInformationsFor("include \"base.rnc\"",
				si("base.rnc", SymbolKind.Module, r(0, 0, 0, 18)));
	}
}
