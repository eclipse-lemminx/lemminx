/**
 *  Copyright (c) 2018 Angelo ZERR.
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
package org.eclipse.lemminx.extensions.xsd;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.caSnippet;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.xsd.participants.XSDErrorCode;
import org.eclipse.lemminx.extensions.xsd.participants.diagnostics.XSDValidator;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * XSD diagnostics tests which test the {@link XSDValidator}.
 *
 */
public class XSDValidationExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void cos_all_limited_2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema \r\n" + //
				"	xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"testEle1\" minOccurs=\"2\" maxOccurs=\"unbounded\" type=\"xs:string\"/>\r\n"
				+ //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(6, 55, 6, 66, XSDErrorCode.cos_all_limited_2));
	}

	@Test
	public void cos_all_limited_2_multiple() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema \r\n" + //
				"	xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"testEle1\" minOccurs=\"2\" maxOccurs=\"unbounded\" type=\"xs:string\"/>\r\n"
				+ //
				"			<xs:element name=\"testEle2\" minOccurs=\"2\" maxOccurs=\"unbounded\" type=\"xs:string\"/>\r\n"
				+ //
				"			<xs:element name=\"test3\" minOccurs=\"2\" maxOccurs=\"unbounded\" type=\"xs:string\"/>\r\n"
				+ //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";

		Diagnostic first = d(6, 55, 6, 66, XSDErrorCode.cos_all_limited_2);
		Diagnostic second = d(7, 55, 7, 66, XSDErrorCode.cos_all_limited_2);
		Diagnostic third = d(8, 52, 8, 63, XSDErrorCode.cos_all_limited_2);
		testDiagnosticsFor(xml, first, second, third);
	}

	@Test
	public void ct_props_correct_3_1() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	<xs:complexType name=\"fullpersoninfo\">\r\n" + //
				"		<xs:complexContent>\r\n" + //
				"			<xs:extension base=\"fullpersoninfo\">\r\n" + //
				"			</xs:extension>\r\n" + //
				"		</xs:complexContent>\r\n" + //
				"	</xs:complexType>\r\n" + "</xs:schema>";
		testDiagnosticsFor(xml, d(4, 22, 4, 38, XSDErrorCode.ct_props_correct_3));
	}

	@Test
	public void ct_props_correct_3_2() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:complexType name=\"aComplexType\">\r\n" + //
				"		<xs:complexContent>\r\n" + //
				"			<xs:extension base=\"xs:aComplexType\"></xs:extension>\r\n" + //
				"		</xs:complexContent>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(4, 22, 4, 39, XSDErrorCode.ct_props_correct_3));
	}

	@Test
	public void emptyTargetNamespace() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" xml:lang=\"EN\"\r\n" + //
				"	targetNamespace=\"\"\r\n" + //
				"	version=\"1.0\">\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(2, 17, 2, 19, XSDErrorCode.EmptyTargetNamespace));
	}

	@Test
	public void p_props_correct_2_1() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"testEle\" minOccurs=\"1\" maxOccurs=\"0\" type=\"xs:string\"/>\r\n" + //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(4, 30, 4, 43, XSDErrorCode.p_props_correct_2_1));
	}

	@Test
	public void p_props_correct_2_1_multiple() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"testEle\" minOccurs=\"1\" maxOccurs=\"0\" type=\"xs:string\"/>\r\n" + //
				"			<xs:element name=\"test\" minOccurs=\"5\" maxOccurs=\"0\" type=\"xs:string\"/>\r\n" + //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";

		Diagnostic first = d(4, 30, 4, 43, XSDErrorCode.p_props_correct_2_1);
		Diagnostic second = d(5, 27, 5, 40, XSDErrorCode.p_props_correct_2_1);
		testDiagnosticsFor(xml, first, second);
	}

	@Test
	public void s4s_elt_invalid_content_1() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<foo></foo>\r\n" + // <- error foo doesn't exist
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 2, 3, 5, XSDErrorCode.s4s_elt_invalid_content_1));
	}

	@Test
	public void s4s_elt_character() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<xs:element name=\"foo\">bar</xs:element>\r\n" + // <- error with bar text
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 24, 3, 27, XSDErrorCode.s4s_elt_character));
	}

	@Test
	public void s4s_elt_must_match_2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\r\n" + //
				"	<xs:simpleType name=\"X\"></xs:simpleType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(2, 2, 2, 15, XSDErrorCode.s4s_elt_must_match_2));
	}

	@Test
	public void s4s_att_must_appear() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<xs:element></xs:element>\r\n" + // <- error with @name missing
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 2, 3, 12, XSDErrorCode.s4s_att_must_appear));
	}

	@Test
	public void s4s_att_not_allowed() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<xs:element foo=\"bar\" ></xs:element>\r\n" + // <- error with foo attribute which is not allowed
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 13, 3, 16, XSDErrorCode.s4s_att_not_allowed),
				d(3, 2, 3, 12, XSDErrorCode.s4s_att_must_appear));
	}

	@Test
	public void s4s_att_invalid_value() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<xs:element name=\"\" ></xs:element>\r\n" + // <- error with @name which is empty
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 18, 3, 20, XSDErrorCode.s4s_att_invalid_value),
				d(3, 2, 3, 12, XSDErrorCode.s4s_att_must_appear));
	}

	@Test
	public void emptyElementNameNoNPE() throws BadLocationException {
		// Test for https://github.com/eclipse-lemminx/lemminx/issues/437
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema \r\n" + //
				"	xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"\" minOccurs=\"1\" maxOccurs=\"2\" type=\"xs:string\"/>\r\n" + //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(5, 20, 5, 22, XSDErrorCode.s4s_att_invalid_value),
				d(5, 4, 5, 14, XSDErrorCode.src_element_2_1));
	}

	@Test
	public void s4s_elt_invalid_content_3WithClosingTag() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\">\r\n" + //
				"	<xs:element name=\"project\" type=\"xs:string\"></xs:element>\r\n" + //
				"	<xs:import></xs:import>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(2, 2, 2, 11, XSDErrorCode.s4s_elt_invalid_content_3);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 1, 2, 24, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 1, 2, 24, "$0")));
	}

	@Test
	public void s4s_elt_invalid_content_3WithSelfClosingTag() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\">\r\n" + //
				"	<xs:element name=\"project\" type=\"xs:string\"></xs:element>\r\n" + //
				"	<xs:import/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(2, 2, 2, 11, XSDErrorCode.s4s_elt_invalid_content_3);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 1, 2, 13, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 1, 2, 13, "$0")));
	}

	@Test
	public void src_annotation_invalid_tag_() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"        <xs:appInfo>\r\n" + // 
				"        </xs:appInfo>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(2, 9, 2, 19, XSDErrorCode.src_annotation);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
			ca(d,
				te(2, 9, 2, 19, "xs:appinfo"),
				te(3, 10, 3, 20,"xs:appinfo")
			),
			ca(d,
				te(2, 9, 2, 19, "xs:documentation"),
				te(3, 10, 3, 20,"xs:documentation")
			)
		);
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
			caSnippet(d,
				sne(2, 9, 2, 19, "xs:appinfo$0"),
				sne(3, 10, 3, 20, "xs:appinfo$0")
			),
			caSnippet(d,
				sne(2, 9, 2, 19, "xs:documentation$0"),
				sne(3, 10, 3, 20, "xs:documentation$0")
			)
		);
	}

	@Test
	public void sch_props_correct_2() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:element name=\"elt1\" />\r\n" + //
				"	<xs:element name=\"elt1\" />\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(3, 18, 3, 24, XSDErrorCode.sch_props_correct_2));
	}

	@Test
	public void src_ct_1() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	<xs:complexType name=\"fullpersoninfo\">\r\n" + //
				"		<xs:complexContent>\r\n" + //
				"			<xs:extension base=\"xs:string\">\r\n" + //
				"			</xs:extension>\r\n" + //
				"		</xs:complexContent>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(4, 22, 4, 33, XSDErrorCode.src_ct_1));
	}

	@Test
	public void src_ct_2_1() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:element name=\"childA\">\r\n" + //
				"		<xs:complexType>\r\n" + //
				"			<xs:simpleContent>\r\n" + //
				"				<xs:restriction base=\"xs:string\" />\r\n" + //
				"			</xs:simpleContent>\r\n" + //
				"			<xs:attributeGroup ref=\"\" />\r\n" + //
				"		</xs:complexType>\r\n" + //
				"	</xs:element>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(5, 25, 5, 36, XSDErrorCode.src_ct_2_1));
	}

	@Test
	public void src_resolve() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<xs:element name=\"A\">\r\n" + //
				"		<xs:complexType>\r\n" + //
				"			<xs:sequence>\r\n" + //
				"				<xs:element name=\"A.1\" type=\"xs:string\" />\r\n" + //
				"				<xs:element name=\"A.2\" type=\"XXXXX\" /> \r\n" + // <- error with XXXXX
				"			</xs:sequence>\r\n" + //
				"		</xs:complexType>\r\n" + //
				"	</xs:element> \r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(7, 32, 7, 39, XSDErrorCode.src_resolve));
	}

	@Test
	public void src_resolve2() throws BadLocationException {
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"\r\n" + //
				"	<xs:simpleType name=\"carType\">\r\n" + //
				"		<xs:restriction base=\"xs:string\">\r\n" + //
				"			<xs:enumeration value=\"Audi\" />\r\n" + //
				"			<xs:enumeration value=\"Golf\" />\r\n" + //
				"			<xs:enumeration value=\"BMW\" />\r\n" + //
				"		</xs:restriction>\r\n" + //
				"	</xs:simpleType>\r\n" + //
				"\r\n" + //
				"	<xs:element name=\"car\" type=\"carType\" />\r\n" + //
				"	<xs:element name=\"foo\" type=\"fooType\" />\r\n" + // <- error with fooType which doesn't exists
				"\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(13, 29, 13, 38, XSDErrorCode.src_resolve));
	}

	@Test
	public void src_element_2_1() throws BadLocationException {
		String xml = "<?xml version='1.0'?>\r\n" + //
				"<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\r\n" + //
				"    <xs:element name='note'>\r\n" + //
				"        <xs:complexType>\r\n" + //
				"            <xs:sequence>\r\n" + //
				"                <xs:element nhame='to' type='xs:string' nillable='false' />\r\n" + // <- error nhame
																									// doesn't exists
				"                <xs:element name='from' type='xs:string' />\r\n" + //
				"                <xs:element name='heading' type='xs:string' />\r\n" + //
				"                <xs:element name='body' type='xs:string' nillable='false' />\r\n" + //
				"            </xs:sequence>\r\n" + //
				"        </xs:complexType>\r\n" + //
				"    </xs:element>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(5, 28, 5, 33, XSDErrorCode.s4s_att_not_allowed),
				d(5, 17, 5, 27, XSDErrorCode.src_element_2_1));
	}

	@Test
	public void src_element_3() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"  <xs:element name=\"a\" type=\"xs:integer\">\r\n" + //
				"    <xs:complexType>\r\n" + //
				"      <xs:sequence>\r\n" + //
				"        <xs:element name=\"b\"></xs:element>\r\n" + //
				"      </xs:sequence>\r\n" + //
				"    </xs:complexType>\r\n" + //
				"  </xs:element>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(1, 3, 1, 13, XSDErrorCode.src_element_3));
	}

	@Test
	public void src_import_1_2xs() throws BadLocationException {
		String xml = "<?xml version=\'1.0\'?>\r\n" + //
				"<xs:schema xmlns:xs=\'http://www.w3.org/2001/XMLSchema\'>\r\n" + //
				"	<xs:import></xs:import>\r\n" + //
				"</xs:schema>";

		Diagnostic d = d(2, 2, 2, 11, XSDErrorCode.src_import_1_2);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 11, 2, 11, " namespace=\"\"")),
				ca(d, te(1, 54, 1, 54, " targetNamespace=\"\"")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 11, 2, 11, " namespace=\"$1\"")),
				caSnippet(d, sne(1, 54, 1, 54, " targetNamespace=\"$1\"")));
	}

	@Test
	public void src_import_1_2() throws BadLocationException {
		String xml = "<?xml version=\'1.0\'?>\r\n" + //
				"<schema xmlns=\'http://www.w3.org/2001/XMLSchema\'>\r\n" + //
				"	<import></import>\r\n" + //
				"</schema>";

		Diagnostic d = d(2, 2, 2, 8, XSDErrorCode.src_import_1_2);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 8, 2, 8, " namespace=\"\"")),
				ca(d, te(1, 48, 1, 48, " targetNamespace=\"\"")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 8, 2, 8, " namespace=\"$1\"")),
				caSnippet(d, sne(1, 48, 1, 48, " targetNamespace=\"$1\"")));
	}

	@Test
	public void src_import_1_2_different_range() throws BadLocationException {
		String xml = "<?xml version=\'1.0\'?>\r\n" + //
				"<xs:schema xmlns:xs=\'http://www.w3.org/2001/XMLSchema\'>\r\n" + //
				"	<xs:imp|ort></xs:import>\r\n" + //
				"</xs:schema>";

		Diagnostic d = d(2, 2, 2, 11, XSDErrorCode.src_import_1_2);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(2, 11, 2, 11, " namespace=\"\"")),
				ca(d, te(1, 54, 1, 54, " targetNamespace=\"\"")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 11, 2, 11, " namespace=\"$1\"")),
				caSnippet(d, sne(1, 54, 1, 54, " targetNamespace=\"$1\"")));
	}

	@Test
	public void schema_reference_4_BadSchemaLocation() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\r\n" + //
				"<xs:import namespace='http://foo' schemaLocation='bad.xsd' />\r\n" + //
				"<xs:element name='foo'></xs:element>\r\n" + //
				"</xs:schema>";

		Diagnostic d = d(1, 50, 1, 57, XSDErrorCode.schema_reference_4);
		testDiagnosticsFor(xsd, d);
	}

	@Test
	public void schema_reference_4_GoodSchemaLocation() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\r\n" + //
				"<xs:import namespace='team_namespace' schemaLocation='src/test/resources/xsd/team.xsd' />\r\n" + //
				"<xs:element name='foo'></xs:element>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xsd);
	}

	@Test
	public void schema_reference_4_IncludeErrorRange() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\r\n" + //
				"<xs:include schemaLocation='bad.xsd' />\r\n" + //
				"<xs:element name='foo'></xs:element>\r\n" + //
				"</xs:schema>";

		Diagnostic d = d(1, 28, 1, 35, XSDErrorCode.schema_reference_4);
		testDiagnosticsFor(xsd, d);
	}

	@Test
	public void src_import_3_1_BadNamespaceWithEmptySchema() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs=\'http://www.w3.org/2001/XMLSchema\'>\n" + //
				"<xs:import namespace=\'BAD_NAMESPACE\' schemaLocation=\'src/test/resources/xsd/empty.xsd\'/>\n" + //
				"<xs:element name=\'foo\'></xs:element>\n" + //
				"</xs:schema>";

		Diagnostic d = d(1, 21, 1, 36, XSDErrorCode.src_import_3_1);
		testDiagnosticsFor(xsd, d);
	}

	@Test
	public void src_import_3_1_BadNamespaceWithSchemaContent() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs=\'http://www.w3.org/2001/XMLSchema\'>\n" + //
				"<xs:import namespace=\'BAD_NAMESPACE\' schemaLocation=\'src/test/resources/xsd/baseSchema.xsd\'/>\n" + //
				"<xs:element name=\'foo\'></xs:element>\n" + //
				"</xs:schema>";

		Diagnostic d = d(1, 21, 1, 36, XSDErrorCode.src_import_3_1);
		testDiagnosticsFor(xsd, d);
	}

	@Test
	public void src_import_3_2_NoNamespaceFound() throws BadLocationException {
		String xsd = "<xs:schema\n" + //
				"xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" + //
				"targetNamespace=\"http://example.org/my-example\"\n" + //
				"xmlns:NS=\"http://example.org/my-example\">\n" + //
				"<xs:import schemaLocation=\"src/test/resources/xsd/baseSchema.xsd\"/>\n" + //
				"</xs:schema>";

		Diagnostic d = d(4, 26, 4, 65, XSDErrorCode.src_import_3_2);
		testDiagnosticsFor(xsd, d);
	}

	@Test
	public void schema_reference_4() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"  <xs:import\r\n" + //
				"      namespace=\"abcd\"\r\n" + //
				"      schemaLocation=\"unkown1.xsd\" />\r\n" + // <-- error with unkown1.xsd
				"  <xs:include schemaLocation=\"unkown2.xsd\" />\r\n" + // <-- error with unkown2.xsd
				"</xs:schema>";

		Diagnostic d1 = d(3, 22, 3, 33, XSDErrorCode.schema_reference_4);
		Diagnostic d2 = d(4, 30, 4, 41, XSDErrorCode.schema_reference_4);
		testDiagnosticsFor(xsd, d1, d2);
	}

	@Test
	public void aggregateErrorFromReferencedSchema() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://java.sun.com/xml/ns/j2ee\">\r\n"
				+ //
				"  <xs:import\r\n" + //
				"      schemaLocation=\"src/test/resources/xsd/foo-invalid-schema.xsd\" />\r\n" + // <-- error base.xml
																									// is not a XSD
				"  <xs:include schemaLocation=\"src/test/resources/xsd/srcElement3.xsd\" />\r\n" + // error
				"</xs:schema>";

		Diagnostic d1 = d(2, 21, 2, 68, null);
		Diagnostic d2 = d(3, 29, 3, 69, null);
		testDiagnosticsFor(xsd, d1, d2);
	}

	// ---------- Code action tests for new registrations ----------

	@Test
	public void s4s_elt_invalid_content_1CodeAction() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<foo></foo>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(3, 2, 3, 5, XSDErrorCode.s4s_elt_invalid_content_1);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(3, 1, 3, 12, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(3, 1, 3, 12, "$0")));
	}

	@Test
	public void s4s_elt_invalid_content_1CodeActionSelfClosing() throws BadLocationException {
		String xml = "<?xml version=\"1.1\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n" + //
				"	<foo/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(3, 2, 3, 5, XSDErrorCode.s4s_elt_invalid_content_1);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(3, 1, 3, 7, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(3, 1, 3, 7, "$0")));
	}

	@Test
	public void s4s_att_not_allowedCodeAction() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:element name=\"foo\" bar=\"baz\" type=\"xs:string\"/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(1, 24, 1, 27, XSDErrorCode.s4s_att_not_allowed);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(1, 23, 1, 33, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 23, 1, 33, "$0")));
	}

	@Test
	public void emptyTargetNamespaceCodeAction() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	targetNamespace=\"\">\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(1, 17, 1, 19, XSDErrorCode.EmptyTargetNamespace);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(1, 0, 1, 19, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 0, 1, 19, "$0")));
	}

	@Test
	public void p_props_correct_2_1CodeAction() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:complexType name=\"testType\">\r\n" + //
				"		<xs:all>\r\n" + //
				"			<xs:element name=\"testEle\" minOccurs=\"1\" maxOccurs=\"0\" type=\"xs:string\"/>\r\n" + //
				"		</xs:all>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(4, 30, 4, 43, XSDErrorCode.p_props_correct_2_1);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(4, 29, 4, 43, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(4, 29, 4, 43, "$0")));
	}

	@Test
	public void src_element_1() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:element name=\"foo\" type=\"xs:string\" default=\"a\" fixed=\"b\"/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(1, 2, 1, 12, XSDErrorCode.src_element_1);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(1, 40, 1, 52, "")),
				ca(d, te(1, 52, 1, 62, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 40, 1, 52, "$0")),
				caSnippet(d, sne(1, 52, 1, 62, "$0")));
	}

	@Test
	public void src_attribute_1() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:attribute name=\"foo\" type=\"xs:string\" default=\"a\" fixed=\"b\"/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(1, 2, 1, 14, XSDErrorCode.src_attribute_1);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d,
				ca(d, te(1, 42, 1, 54, "")),
				ca(d, te(1, 54, 1, 64, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(1, 42, 1, 54, "$0")),
				caSnippet(d, sne(1, 54, 1, 64, "$0")));
	}

	@Test
	public void src_single_facet_value() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"	<xs:simpleType name=\"myType\">\r\n" + //
				"		<xs:restriction base=\"xs:integer\">\r\n" + //
				"			<xs:minInclusive value=\"0\"/>\r\n" + //
				"			<xs:minInclusive value=\"1\"/>\r\n" + //
				"		</xs:restriction>\r\n" + //
				"	</xs:simpleType>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(4, 4, 4, 19, XSDErrorCode.src_single_facet_value);
		testDiagnosticsFor(xml, d);
		// Without snippet support
		testCodeActionsFor(xml, d, ca(d, te(4, 3, 4, 31, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(4, 3, 4, 31, "$0")));
	}

	// ---------- cos-applicable-facets ----------

	@Test
	public void cos_applicable_facets() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:simpleType name=\"badFacetType\">\r\n" + //
				"        <xs:restriction base=\"xs:string\">\r\n" + //
				"            <xs:totalDigits value=\"5\"/>\r\n" + //
				"        </xs:restriction>\r\n" + //
				"    </xs:simpleType>\r\n" + //
				"    <xs:element name=\"root\" type=\"badFacetType\"/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(4, 13, 4, 27, XSDErrorCode.cos_applicable_facets);
		testDiagnosticsFor(xml, d);
		// Without snippet support - remove facet element
		testCodeActionsFor(xml, d, ca(d, te(4, 12, 4, 39, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(4, 12, 4, 39, "$0")));
	}

	// ---------- ag-props-correct ----------

	@Test
	public void ag_props_correct_2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:attributeGroup name=\"myGroup\">\r\n" + //
				"        <xs:attribute name=\"attr1\" type=\"xs:string\"/>\r\n" + //
				"        <xs:attribute name=\"attr1\" type=\"xs:integer\"/>\r\n" + //
				"    </xs:attributeGroup>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(4, 28, 4, 33, XSDErrorCode.ag_props_correct_2);
		testDiagnosticsFor(xml, d);
		// Without snippet support - remove duplicate element
		testCodeActionsFor(xml, d, ca(d, te(4, 8, 4, 54, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(4, 8, 4, 54, "$0")));
	}

	@Test
	public void ag_props_correct_3() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:attributeGroup name=\"myGroup\">\r\n" + //
				"        <xs:attribute name=\"id1\" type=\"xs:ID\"/>\r\n" + //
				"        <xs:attribute name=\"id2\" type=\"xs:ID\"/>\r\n" + //
				"    </xs:attributeGroup>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(4, 39, 4, 44, XSDErrorCode.ag_props_correct_3);
		testDiagnosticsFor(xml, d);
		// Without snippet support - remove 'type' attribute
		testCodeActionsFor(xml, d, ca(d, te(4, 32, 4, 45, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(4, 32, 4, 45, "$0")));
	}

	// ---------- ct-props-correct ----------

	@Test
	public void ct_props_correct_5() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:element name=\"root\">\r\n" + //
				"        <xs:complexType>\r\n" + //
				"            <xs:attribute name=\"id1\" type=\"xs:ID\"/>\r\n" + //
				"            <xs:attribute name=\"id2\" type=\"xs:ID\"/>\r\n" + //
				"        </xs:complexType>\r\n" + //
				"    </xs:element>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(5, 43, 5, 48, XSDErrorCode.ct_props_correct_5);
		testDiagnosticsFor(xml, d);
		// Without snippet support - remove 'type' attribute
		testCodeActionsFor(xml, d, ca(d, te(5, 36, 5, 49, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(5, 36, 5, 49, "$0")));
	}

	// ---------- a-props-correct ----------

	@Test
	public void a_props_correct_3() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"    <xs:attribute name=\"badIdAttr\" type=\"xs:ID\" default=\"defaultId\"/>\r\n" + //
				"</xs:schema>";
		Diagnostic d = d(2, 48, 2, 55, XSDErrorCode.a_props_correct_3);
		testDiagnosticsFor(xml, d);
		// Without snippet support - remove 'default' attribute
		testCodeActionsFor(xml, d, ca(d, te(2, 47, 2, 67, "")));
		// With snippet support
		testCodeActionsFor(xml, d, createSnippetEditSettings(),
				caSnippet(d, sne(2, 47, 2, 67, "$0")));
	}

	// ---------- Error range tests for new toLSPRange categories ----------

	@Test
	public void src_import_1_1() throws BadLocationException {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	targetNamespace=\"http://example.org\">\r\n" + //
				"	<xs:import namespace=\"http://example.org\"/>\r\n" + //
				"</xs:schema>";
		testDiagnosticsFor(xml, d(2, 22, 2, 42, XSDErrorCode.src_import_1_1));
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) throws BadLocationException {
		XMLAssert.testDiagnosticsFor(xml, null, null, "test.xsd", expected);
	}

	private static SharedSettings createSnippetEditSettings() {
		return XMLAssert.createSnippetEditSettings();
	}
}
