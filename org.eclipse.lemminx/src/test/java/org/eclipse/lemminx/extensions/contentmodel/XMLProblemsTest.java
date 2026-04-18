/**
 *  Copyright (c) 2018 Angelo ZERR
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

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.createFile;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.teOp;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;
import static org.eclipse.lemminx.XMLAssert.testResolveCodeActionsFor;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_BINDING_WIZARD;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSyntaxErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.GenerateDocTypeCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.GenerateRelaxNGSchemaCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.GenerateXMLModelWithDTDCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.GenerateXMLModelWithXSDCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.GenerateXSINoNamespaceSchemaCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.extensions.minify.participants.MinifyCodeActionResolver;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionResolveSupportCapabilities;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * XML problems like noGrammar.
 *
 */
public class XMLProblemsTest extends AbstractCacheBasedTest {

	@Test
	public void noGrammarIgnore() throws BadLocationException {
		String xml = "<root /> ";
		testDiagnosticsFor(xml);
	}

	@Test
	public void noGrammarHint() throws BadLocationException {
		noGrammarHint(false);
	}

	@Test
	public void noGrammarHintSelfClose() throws BadLocationException {
		noGrammarHint(true);
	}

	private static void noGrammarHint(boolean selfClose) throws BadLocationException {
		String xml = selfClose ? "<root/>" : "<root></root>";
		Diagnostic d = new Diagnostic(r(0, 1, 0, 5), "No grammar constraints (DTD or XML Schema).",
				DiagnosticSeverity.Hint, "xml", XMLSyntaxErrorCode.NoGrammarConstraints.name());
		// Set noGrammar has 'hint'
		ContentModelSettings contentModelSettings = new ContentModelSettings();
		XMLValidationRootSettings problems = new XMLValidationRootSettings();
		problems.setNoGrammar("hint");
		contentModelSettings.setValidation(problems);

		testDiagnosticsFor(xml, null, null, null, false, contentModelSettings, d);

		SharedSettings settings = createSharedSettings();

		// Code action to generate DTD, XSD
		String schemaTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\" />" + lineSeparator() + //
				"</xs:schema>";
		String dtdTemplate = "<!ELEMENT root EMPTY>";
		String rngTemplate = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		testCodeActionsFor(xml, d, null, settings,
				// XSD with xsi:noNamespaceSchemaLocation
				ca(d, //
						createFile("test.xsd", false), //
						teOp("test.xsd", 0, 0, 0, 0, //
								schemaTemplate), //
						teOp("test.xml", 0, 5, 0, 5, //
								" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lineSeparator() + //
										" xsi:noNamespaceSchemaLocation=\"test.xsd\"")),
				// XSD with xml-model
				ca(d, //
						createFile("test.xsd", false), //
						teOp("test.xsd", 0, 0, 0, 0, //
								schemaTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.xsd\"?>" + lineSeparator())),
				// DTD with DOCTYPE
				ca(d, //
						createFile("test.dtd", false), //
						teOp("test.dtd", 0, 0, 0, 0, //
								dtdTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<!DOCTYPE root SYSTEM \"test.dtd\">" + lineSeparator())),
				// DTD with xml-model
				ca(d, //
						createFile("test.dtd", false), //
						teOp("test.dtd", 0, 0, 0, 0, //
								dtdTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.dtd\"?>" + lineSeparator())),
				// RelaxNG with xml-model
				ca(d, //
						createFile("test.rng", false), //
						teOp("test.rng", 0, 0, 0, 0, //
								rngTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.rng\"?>" + lineSeparator())),
				// Open binding wizard command
				ca(d, new Command("Bind to existing grammar/schema", OPEN_BINDING_WIZARD,
						Arrays.asList(new Object[] { "test.xml" }))));

	}

	@Test
	public void noCodeActionResolverSupport() throws BadLocationException {
		String xml = "<root></root>";
		Diagnostic d = new Diagnostic(r(0, 1, 0, 5), "No grammar constraints (DTD or XML Schema).",
				DiagnosticSeverity.Hint, "xml", XMLSyntaxErrorCode.NoGrammarConstraints.name());

		SharedSettings settings = createSharedSettings();

		// Code action to generate DTD, XSD
		String schemaTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\" />" + lineSeparator() + //
				"</xs:schema>";
		String dtdTemplate = "<!ELEMENT root EMPTY>";
		String rngTemplate = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"" + lineSeparator() + //
				"  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">" + lineSeparator() + //
				"  <start>" + lineSeparator() + //
				"    <ref name=\"rootContent\" />" + lineSeparator() + //
				"  </start>" + lineSeparator() + //
				"  <define name=\"rootContent\">" + lineSeparator() + //
				"    <element name=\"root\">" + lineSeparator() + //
				"      <empty />" + lineSeparator() + //
				"    </element>" + lineSeparator() + //
				"  </define>" + lineSeparator() + //
				"</grammar>";
		testCodeActionsFor(xml, d, null, settings,
				// XSD with xsi:noNamespaceSchemaLocation
				ca(d, //
						createFile("test.xsd", false), //
						teOp("test.xsd", 0, 0, 0, 0, //
								schemaTemplate), //
						teOp("test.xml", 0, 5, 0, 5, //
								" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lineSeparator() + //
										" xsi:noNamespaceSchemaLocation=\"test.xsd\"")),
				// XSD with xml-model
				ca(d, //
						createFile("test.xsd", false), //
						teOp("test.xsd", 0, 0, 0, 0, //
								schemaTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.xsd\"?>" + lineSeparator())),
				// DTD with DOCTYPE
				ca(d, //
						createFile("test.dtd", false), //
						teOp("test.dtd", 0, 0, 0, 0, //
								dtdTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<!DOCTYPE root SYSTEM \"test.dtd\">" + lineSeparator())),
				// DTD with xml-model
				ca(d, //
						createFile("test.dtd", false), //
						teOp("test.dtd", 0, 0, 0, 0, //
								dtdTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.dtd\"?>" + lineSeparator())),
				// RelaxNG with xml-model
				ca(d, //
						createFile("test.rng", false), //
						teOp("test.rng", 0, 0, 0, 0, //
								rngTemplate), //
						teOp("test.xml", 0, 0, 0, 0, //
								"<?xml-model href=\"test.rng\"?>" + lineSeparator())),
				// Open binding wizard command
				ca(d, new Command("Bind to existing grammar/schema", OPEN_BINDING_WIZARD,
						Arrays.asList(new Object[] { "test.xml" }))));

	}

	@Test
	public void withCodeActionResolverSupport() throws BadLocationException {
		String xml = "<root></root>";
		Diagnostic d = new Diagnostic(r(0, 1, 0, 5), "No grammar constraints (DTD or XML Schema).",
				DiagnosticSeverity.Hint, "xml", XMLSyntaxErrorCode.NoGrammarConstraints.name());

		SharedSettings settings = createSharedSettings(true);

		XMLLanguageService ls = new XMLLanguageService();

		List<CodeAction> actual = testCodeActionsFor(xml, d, null, settings, ls,
				// XSD with xsi:noNamespaceSchemaLocation
				ca(d, createData("test.xml", GenerateXSINoNamespaceSchemaCodeActionResolver.PARTICIPANT_ID,
						"test.xsd")),
				// XSD with xml-model
				ca(d, createData("test.xml", GenerateXMLModelWithXSDCodeActionResolver.PARTICIPANT_ID, "test.xsd")),
				// DTD with DOCTYPE
				ca(d, createData("test.xml", GenerateDocTypeCodeActionResolver.PARTICIPANT_ID, "test.dtd")),
				// DTD with xml-model
				ca(d, createData("test.xml", GenerateXMLModelWithDTDCodeActionResolver.PARTICIPANT_ID, "test.dtd")),
				// RelaxNG with xml-model
				ca(d, createData("test.xml", GenerateRelaxNGSchemaCodeActionResolver.PARTICIPANT_ID, "test.rng")),
				// Open binding wizard command
				ca(d, new Command("Bind to existing grammar/schema", OPEN_BINDING_WIZARD,
						Arrays.asList(new Object[] { "test.xml" }))), //
				// XML Minify
				ca(null, createData("test.xml", MinifyCodeActionResolver.PARTICIPANT_ID, null)));

		// Test resolve of
		// Code action to generate DTD, XSD
		String schemaTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\" />" + lineSeparator() + //
				"</xs:schema>";
		String dtdTemplate = "<!ELEMENT root EMPTY>";

		// XSD with xsi:noNamespaceSchemaLocation
		CodeAction unresolved1 = actual.get(0);
		testResolveCodeActionsFor(xml, unresolved1, settings, ls, ca(d, //
				createData("test.xml", GenerateXSINoNamespaceSchemaCodeActionResolver.PARTICIPANT_ID, "test.xsd"), //
				createFile("test.xsd", false), //
				teOp("test.xsd", 0, 0, 0, 0, //
						schemaTemplate), //
				teOp("test.xml", 0, 5, 0, 5, //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + lineSeparator() + //
								" xsi:noNamespaceSchemaLocation=\"test.xsd\"")));

		// XSD with xml-model
		CodeAction unresolved2 = actual.get(1);
		testResolveCodeActionsFor(xml, unresolved2, settings, ls, ca(d, //
				createData("test.xml", GenerateXMLModelWithXSDCodeActionResolver.PARTICIPANT_ID, "test.xsd"), //
				createFile("test.xsd", false), //
				teOp("test.xsd", 0, 0, 0, 0, //
						schemaTemplate), //
				teOp("test.xml", 0, 0, 0, 0, //
						"<?xml-model href=\"test.xsd\"?>" + lineSeparator())));

		// DTD with DOCTYPE
		CodeAction unresolved3 = actual.get(2);
		testResolveCodeActionsFor(xml, unresolved3, settings, ls, ca(d, //
				createData("test.xml", GenerateDocTypeCodeActionResolver.PARTICIPANT_ID, "test.dtd"), //
				createFile("test.dtd", false), //
				teOp("test.dtd", 0, 0, 0, 0, //
						dtdTemplate), //
				teOp("test.xml", 0, 0, 0, 0, //
						"<!DOCTYPE root SYSTEM \"test.dtd\">" + lineSeparator())));

		// DTD with xml-model
		CodeAction unresolved4 = actual.get(3);
		testResolveCodeActionsFor(xml, unresolved4, settings, ls, ca(d, //
				createData("test.xml", GenerateXMLModelWithDTDCodeActionResolver.PARTICIPANT_ID, "test.dtd"), //
				createFile("test.dtd", false), //
				teOp("test.dtd", 0, 0, 0, 0, //
						dtdTemplate), //
				teOp("test.xml", 0, 0, 0, 0, //
						"<?xml-model href=\"test.dtd\"?>" + lineSeparator())));

	}

	private JsonObject createData(String uri, String particpantId, String file) {
		JsonObject data = DataEntryField.createData(uri, particpantId);
		if (file != null) {
			data.addProperty("file", file);
		}
		return data;
	}

	private static SharedSettings createSharedSettings() {
		return createSharedSettings(false);
	}

	private static SharedSettings createSharedSettings(boolean resolveCodeAction) {
		SharedSettings settings = new SharedSettings();
		WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
		WorkspaceEditCapabilities workspaceEdit = new WorkspaceEditCapabilities();
		workspaceEdit.setResourceOperations(Arrays.asList(ResourceOperationKind.Create));
		workspace.setWorkspaceEdit(workspaceEdit);
		settings.getWorkspaceSettings().setCapabilities(workspace);
		// Expose `xml.open.binding.wizard` command
		settings.setBindingWizardSupport(true);

		if (resolveCodeAction) {
			CodeActionCapabilities codeAction = new CodeActionCapabilities();
			codeAction.setResolveSupport(new CodeActionResolveSupportCapabilities());
			settings.getCodeActionSettings().setCapabilities(codeAction);
		}

		return settings;
	}
}
