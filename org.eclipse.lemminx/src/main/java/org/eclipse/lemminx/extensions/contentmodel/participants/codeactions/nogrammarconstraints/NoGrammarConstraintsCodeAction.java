/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints;

import static org.eclipse.lemminx.client.ClientCommands.OPEN_BINDING_WIZARD;
import static org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.NoGrammarConstraintsDataConstants.DATA_FILE_FIELD;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorManager;
import org.eclipse.lemminx.extensions.generators.xml2dtd.DTDGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2relaxng.RelaxNGGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2rnc.RelaxNGCompactGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2xsd.XMLSchemaGeneratorSettings;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLBuilder;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonObject;

/**
 * Code Action to bind a XML to a grammar (DTD, XSD) by generating the grammar.
 */
public class NoGrammarConstraintsCodeAction implements ICodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(NoGrammarConstraintsCodeAction.class.getName());
	// FIXME: the element name should be derived from the content model if possible
	private static final String PLACEHOLDER_ELEMENT_NAME = "root-element";
	private final Map<String, ICodeActionResolvesParticipant> resolveCodeActionParticipants;

	public NoGrammarConstraintsCodeAction() {
		// Register available resolvers.
		resolveCodeActionParticipants = new HashMap<>();
		resolveCodeActionParticipants.put(GenerateXSINoNamespaceSchemaCodeActionResolver.PARTICIPANT_ID,
				new GenerateXSINoNamespaceSchemaCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateXMLModelWithXSDCodeActionResolver.PARTICIPANT_ID,
				new GenerateXMLModelWithXSDCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateDocTypeCodeActionResolver.PARTICIPANT_ID,
				new GenerateDocTypeCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateXMLModelWithDTDCodeActionResolver.PARTICIPANT_ID,
				new GenerateXMLModelWithDTDCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateRelaxNGSchemaCodeActionResolver.PARTICIPANT_ID,
				new GenerateRelaxNGSchemaCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateRelaxNGCompactSchemaCodeActionResolver.PARTICIPANT_ID,
				new GenerateRelaxNGCompactSchemaCodeActionResolver());
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		try {
			DOMElement documentElement = document.getDocumentElement();
			if (documentElement == null || !documentElement.hasTagName()) {
				return;
			}
			// ---------- XSD

			String schemaURI = getGrammarURI(document.getDocumentURI(), "xsd");
			String schemaFileName = getFileName(schemaURI);
			String schemaTemplate = null;
			if (!request.canSupportResolve()) {
				SharedSettings sharedSettings = request.getSharedSettings();
				FileContentGeneratorManager generator = request.getComponent(FileContentGeneratorManager.class);
				schemaTemplate = generator.generate(document, sharedSettings, new XMLSchemaGeneratorSettings(),
						cancelChecker);
			}

			// xsi:noNamespaceSchemaLocation
			// Create code action to create the XSD file with the generated XSD content
			CodeAction noNamespaceSchemaLocationAction = createNoNamespaceSchemaLocationCodeAction(schemaURI,
					schemaFileName, schemaTemplate, request, cancelChecker);
			codeActions.add(noNamespaceSchemaLocationAction);

			// xml-model
			CodeAction xsdWithXmlModelAction = createXmlModelCodeAction(schemaURI, schemaFileName, schemaTemplate,
					GenerateXMLModelWithXSDCodeActionResolver.PARTICIPANT_ID, request);
			codeActions.add(xsdWithXmlModelAction);

			// ---------- DTD

			String dtdURI = getGrammarURI(document.getDocumentURI(), "dtd");
			String dtdFileName = getFileName(dtdURI);
			String dtdTemplate = null;
			if (!request.canSupportResolve()) {
				SharedSettings sharedSettings = request.getSharedSettings();
				FileContentGeneratorManager generator = request.getComponent(FileContentGeneratorManager.class);
				dtdTemplate = generator.generate(document, sharedSettings, new DTDGeneratorSettings(), cancelChecker);
			}

			// <!DOCTYPE ${1:root-element} SYSTEM \"${2:file}.dtd\">
			CodeAction docTypeAction = createDocTypeCodeAction(dtdURI, dtdFileName, dtdTemplate, request,
					cancelChecker);
			codeActions.add(docTypeAction);

			// xml-model
			CodeAction dtdWithXmlModelAction = createXmlModelCodeAction(dtdURI, dtdFileName, dtdTemplate,
					GenerateXMLModelWithDTDCodeActionResolver.PARTICIPANT_ID, request);
			codeActions.add(dtdWithXmlModelAction);

			// ---------- Relax NG

			String rngURI = getGrammarURI(document.getDocumentURI(), "rng");
			String rngFileName = getFileName(rngURI);
			String rngTemplate = null;
			if (!request.canSupportResolve()) {
				SharedSettings sharedSettings = request.getSharedSettings();
				FileContentGeneratorManager generator = request.getComponent(FileContentGeneratorManager.class);
				rngTemplate = generator.generate(document, sharedSettings, new RelaxNGGeneratorSettings(), cancelChecker);
			}

			// xml-model
			CodeAction relaxNGWithXmlModelAction = createRelaxNGCodeAction(rngURI, rngFileName, rngTemplate, request,
					cancelChecker);
			codeActions.add(relaxNGWithXmlModelAction);

			// ---------- Relax NG Compact

			String rncURI = getGrammarURI(document.getDocumentURI(), "rnc");
			String rncFileName = getFileName(rncURI);
			String rncTemplate = null;
			if (!request.canSupportResolve()) {
				SharedSettings rncSharedSettings = request.getSharedSettings();
				FileContentGeneratorManager rncGenerator = request.getComponent(FileContentGeneratorManager.class);
				rncTemplate = rncGenerator.generate(document, rncSharedSettings, new RelaxNGCompactGeneratorSettings(),
						cancelChecker);
			}

			// xml-model
			CodeAction relaxNGCompactWithXmlModelAction = createRelaxNGCompactCodeAction(rncURI, rncFileName,
					rncTemplate, request, cancelChecker);
			codeActions.add(relaxNGCompactWithXmlModelAction);

			// ---------- Open Binding Wizard
			SharedSettings sharedSettings = request.getSharedSettings();
			if (sharedSettings.isBindingWizardSupport()) {
				String documentURI = document.getDocumentURI();
				String title = "Bind to existing grammar/schema";
				List<Object> commandParams = Arrays.asList(documentURI);
				CodeAction bindWithExistingGrammar = CodeActionFactory.createCommand(title, OPEN_BINDING_WIZARD,
						commandParams, diagnostic);
				codeActions.add(bindWithExistingGrammar);
			}

		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In NoGrammarConstraintsCodeAction position error", e);
		}
	}

	@Override
	public ICodeActionResolvesParticipant getResolveCodeActionParticipant(String participantId) {
		return resolveCodeActionParticipants.get(participantId);
	}

	private static CodeAction createNoNamespaceSchemaLocationCodeAction(String schemaURI, String schemaFileName,
			String schemaTemplate, ICodeActionRequest request, CancelChecker cancelChecker)
			throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Generate '" + schemaFileName + "' and bind with xsi:noNamespaceSchemaLocation";
		if (request.canSupportResolve()) {
			return createGenerateFileUnresolvedCodeAction(schemaURI, title, diagnostic, document,
					GenerateXSINoNamespaceSchemaCodeActionResolver.PARTICIPANT_ID);
		} else {
			TextDocumentEdit noNamespaceSchemaLocationEdit = createXSINoNamespaceSchemaLocationEdit(schemaFileName,
					document, request.getSharedSettings());
			return createGrammarFileAndBindIt(title, schemaURI, schemaTemplate, noNamespaceSchemaLocationEdit,
					diagnostic);
		}
	}

	private static CodeAction createXmlModelCodeAction(String fileURI, String fileName, String fileTemplate,
			String participantId, ICodeActionRequest request) throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Generate '" + fileName + "' and bind with xml-model";
		if (request.canSupportResolve()) {
			return createGenerateFileUnresolvedCodeAction(fileURI, title, diagnostic, document, participantId);
		}
		SharedSettings sharedSettings = request.getSharedSettings();
		TextDocumentEdit xsdWithXMLModelEdit = createXmlModelEdit(fileName, null, document, sharedSettings);
		CodeAction xsdWithXmlModelAction = createGrammarFileAndBindIt(title, fileURI, fileTemplate, xsdWithXMLModelEdit,
				diagnostic);
		return xsdWithXmlModelAction;
	}

	private CodeAction createDocTypeCodeAction(String dtdURI, String dtdFileName, String dtdTemplate,
			ICodeActionRequest request, CancelChecker cancelChecker) throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Generate '" + dtdFileName + "' and bind with DOCTYPE";
		if (request.canSupportResolve()) {
			return createGenerateFileUnresolvedCodeAction(dtdURI, title, diagnostic, document,
					GenerateDocTypeCodeActionResolver.PARTICIPANT_ID);
		}
		SharedSettings sharedSettings = request.getSharedSettings();
		TextDocumentEdit dtdWithDocType = createDocTypeEdit(dtdFileName, document, sharedSettings);
		return createGrammarFileAndBindIt(title, dtdURI, dtdTemplate, dtdWithDocType, diagnostic);
	}

	private CodeAction createRelaxNGCodeAction(String rngURI, String rngFileName, String rngTemplate,
			ICodeActionRequest request, CancelChecker cancelChecker) throws BadLocationException {
 		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Generate '" + rngFileName + "' and bind with RelaxNG";
		if (request.canSupportResolve()) {
			return createGenerateFileUnresolvedCodeAction(rngURI, title, diagnostic, document,
					GenerateRelaxNGSchemaCodeActionResolver.PARTICIPANT_ID);
		}
		SharedSettings sharedSettings = request.getSharedSettings();
		TextDocumentEdit relaxNGWithXMLModelEdit = createXmlModelEdit(rngFileName, null, document, sharedSettings);
		return createGrammarFileAndBindIt(title, rngURI, rngTemplate, relaxNGWithXMLModelEdit, diagnostic);
	}

	private CodeAction createRelaxNGCompactCodeAction(String rncURI, String rncFileName, String rncTemplate,
			ICodeActionRequest request, CancelChecker cancelChecker) throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Generate '" + rncFileName + "' and bind with RelaxNG Compact";
		if (request.canSupportResolve()) {
			return createGenerateFileUnresolvedCodeAction(rncURI, title, diagnostic, document,
					GenerateRelaxNGCompactSchemaCodeActionResolver.PARTICIPANT_ID);
		}
		SharedSettings sharedSettings = request.getSharedSettings();
		TextDocumentEdit relaxNGCompactWithXMLModelEdit = createXmlModelEdit(rncFileName, null, document,
				sharedSettings);
		return createGrammarFileAndBindIt(title, rncURI, rncTemplate, relaxNGCompactWithXMLModelEdit, diagnostic);
	}

	private static CodeAction createGenerateFileUnresolvedCodeAction(String generateFileURI, String title,
			Diagnostic diagnostic, DOMDocument document, String participantId) {
		CodeAction codeAction = new CodeAction(title);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);

		JsonObject data = DataEntryField.createData(document.getDocumentURI(), participantId);
		data.addProperty(DATA_FILE_FIELD, generateFileURI);
		codeAction.setData(data);
		return codeAction;
	}

	/**
	 * Returns the unique grammar URI file.
	 *
	 * @param documentURI   the XML document URI.
	 * @param fileExtension the grammar file extension.
	 *
	 * @return the unique grammar URI file.
	 */
	static String getGrammarURI(String documentURI, String fileExtension) {
		int index = documentURI.lastIndexOf('.');
		if (index > 1 && documentURI.charAt(index - 1) == '/') {
			// case with file which starts with '.' (ex .project, .classpath).
			index = -1;
		}
		String grammarWithoutExtension = index != -1 ? documentURI.substring(0, index) : documentURI;
		String grammarURI = grammarWithoutExtension + "." + fileExtension;
		int i = 1;
		try {
			while (Files.exists(Paths.get(new URI(grammarURI)))) {
				grammarURI = grammarWithoutExtension + (i++) + "." + fileExtension;
			}
		} catch (Exception e) {
			// Do nothing
		}
		return grammarURI;
	}

	static String getFileName(String schemaURI) {
		return new File(schemaURI).getName();
	}

	private static CodeAction createGrammarFileAndBindIt(String title, String grammarURI, String grammarContent,
			TextDocumentEdit boundEdit, Diagnostic diagnostic) {
		CodeAction codeAction = CodeActionFactory.createFile(title, grammarURI, grammarContent, diagnostic);
		codeAction.getEdit().getDocumentChanges().add(Either.forLeft(boundEdit));
		return codeAction;
	}

	public static TextDocumentEdit createXSINoNamespaceSchemaLocationEdit(String schemaFileName, DOMDocument document,
			SharedSettings sharedSettings)
			throws BadLocationException {
		String delimiter = document.getTextDocument().lineDelimiter(0);
		DOMElement documentElement = document.getDocumentElement();
		int beforeTagOffset = documentElement != null ? documentElement.getStartTagOpenOffset()
				: document.getLastChild() != null ? document.getLastChild().getEnd() : 0;
		int afterTagOffset = documentElement != null ? beforeTagOffset + 1 + documentElement.getTagName().length()
				: beforeTagOffset;

		XMLBuilder insertText = new XMLBuilder(sharedSettings, null, delimiter);

		if (documentElement == null) {
			generateStartTag(insertText, document);
		} else {
			insertText.initElementContext(documentElement.getTagName());
		}

		SplitAttributes splitAttr = sharedSettings.getFormattingSettings().getSplitAttributes();

		if (splitAttr.isExpandFirstAttribute()) {
			insertText.addAttributeOnNewLine("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", 0, true);
		} else {
			insertText.addSingleAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", true);
		}
		insertText.addAttributeOnNewLine("xsi:noNamespaceSchemaLocation", schemaFileName, 0, true);

		int endOffset = afterTagOffset;
		if (documentElement != null && documentElement.hasAttributes() && splitAttr.isAlwaysSplit()) {
			insertText.appendNewLineAttributeIndent(0);
			DOMAttr firstAttr = documentElement.getAttributeNodes().get(0);
			endOffset = firstAttr.getStart();
		}

		if (documentElement == null) {
			generateEndTag(insertText);
		}

		Position startPosition = document.positionAt(afterTagOffset);
		Position endPosition = document.positionAt(endOffset);
		return CodeActionFactory.insertEdits(document.getTextDocument(),
				Collections.singletonList(
						new TextEdit(new Range(startPosition, endPosition), insertText.toString())));
	}

	public static TextDocumentEdit createXSISchemaLocationEdit(String schemaFileName, String targetNamespace,
			DOMDocument document, SharedSettings sharedSettings) throws BadLocationException {
		String delimiter = document.getTextDocument().lineDelimiter(0);
		DOMElement documentElement = document.getDocumentElement();
		int beforeTagOffset = documentElement != null ? documentElement.getStartTagOpenOffset()
				: document.getLastChild() != null ? document.getLastChild().getEnd() : 0;
		int afterTagOffset = documentElement != null ? beforeTagOffset + 1 + documentElement.getTagName().length()
				: beforeTagOffset;

		XMLBuilder insertText = new XMLBuilder(sharedSettings, null, delimiter);

		if (documentElement == null) {
			generateStartTag(insertText, document);
		} else {
			insertText.initElementContext(documentElement.getTagName());
		}

		SplitAttributes splitAttr = sharedSettings.getFormattingSettings().getSplitAttributes();

		boolean hasExistingNamespace = documentElement != null
				&& !StringUtils.isEmpty(documentElement.getNamespaceURI());

		int insertOffset;
		if (hasExistingNamespace) {
			List<DOMAttr> attrs = documentElement.getAttributeNodes();
			DOMAttr lastAttr = attrs.get(attrs.size() - 1);
			insertOffset = lastAttr.getEnd();
		} else {
			insertOffset = afterTagOffset;
			if (splitAttr.isExpandFirstAttribute()) {
				insertText.addAttributeOnNewLine("xmlns", targetNamespace, 0, true);
			} else {
				insertText.addSingleAttribute("xmlns", targetNamespace, true);
			}
		}

		insertText.addAttributeOnNewLine("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", 0, true);
		insertText.addAttributeOnNewLine("xsi:schemaLocation", targetNamespace + " " + schemaFileName, 0, true);

		int endOffset = insertOffset;
		if (!hasExistingNamespace && documentElement != null && documentElement.hasAttributes()
				&& splitAttr.isAlwaysSplit()) {
			insertText.appendNewLineAttributeIndent(0);
			DOMAttr firstAttr = documentElement.getAttributeNodes().get(0);
			endOffset = firstAttr.getStart();
		}

		if (documentElement == null) {
			generateEndTag(insertText);
		}

		Position startPosition = document.positionAt(insertOffset);
		Position endPosition = document.positionAt(endOffset);
		return CodeActionFactory.insertEdits(document.getTextDocument(),
				Collections.singletonList(
						new TextEdit(new Range(startPosition, endPosition), insertText.toString())));
	}

	public static TextDocumentEdit createXmlModelEdit(String schemaFileName, String targetNamespace,
			DOMDocument document, SharedSettings sharedSettings) throws BadLocationException {
		String delimiter = document.getTextDocument().lineDelimiter(0);
		DOMElement documentElement = document.getDocumentElement();
		int beforeTagOffset = documentElement != null ? documentElement.getStartTagOpenOffset()
				: document.getLastChild() != null ? document.getLastChild().getEnd() : 0;

		// Insert Text edit for xml-model
		XMLBuilder xsdWithXmlModel = new XMLBuilder(sharedSettings, null, delimiter);
		xsdWithXmlModel.startPrologOrPI("xml-model");
		xsdWithXmlModel.addSingleAttribute("href", schemaFileName, true);
		xsdWithXmlModel.endPrologOrPI();
		xsdWithXmlModel.linefeed();

		String xmlModelInsertText = (documentElement == null && document.getLastChild() != null ? delimiter : "")
				+ xsdWithXmlModel.toString();
		Position xmlModelPosition = document.positionAt(beforeTagOffset);

		boolean hasExistingNamespace = documentElement != null
				&& !StringUtils.isEmpty(documentElement.getNamespaceURI());

		if (documentElement != null && (StringUtils.isEmpty(targetNamespace) || hasExistingNamespace)) {
			return CodeActionFactory.insertEdit(xmlModelInsertText, xmlModelPosition, document.getTextDocument());
		}

		// Generate root element (if needed) and insert namespace in root element (if
		// needed)
		XMLBuilder xmlNamespaceInsertText = new XMLBuilder(sharedSettings, null, delimiter);

		if (documentElement == null) {
			generateStartTag(xmlNamespaceInsertText, document);
		}

		if (!StringUtils.isEmpty(targetNamespace)) {
			xmlNamespaceInsertText.append(" xmlns=\"");
			xmlNamespaceInsertText.append(targetNamespace);
			xmlNamespaceInsertText.append("\"");
		}

		if (documentElement == null) {
			generateEndTag(xmlNamespaceInsertText);
		}

		int afterTagOffset = beforeTagOffset
				+ (documentElement != null ? 1 + documentElement.getTagName().length() : 0);
		Position xmlNamespacePosition = document.positionAt(afterTagOffset);

		return CodeActionFactory.insertEdits(document.getTextDocument(), //
				Arrays.asList(CodeActionFactory.insertEdit(xmlModelInsertText, xmlModelPosition), //
						CodeActionFactory.insertEdit(xmlNamespaceInsertText.toString(), xmlNamespacePosition)));
	}

	public static TextDocumentEdit createDocTypeEdit(String dtdFileName, DOMDocument document,
			SharedSettings sharedSettings) throws BadLocationException {
		String delimiter = document.getTextDocument().lineDelimiter(0);
		DOMElement documentElement = document.getDocumentElement();
		int beforeTagOffset = documentElement != null ? documentElement.getStartTagOpenOffset()
				: document.getLastChild() != null ? document.getLastChild().getEnd() : 0;

		XMLBuilder builder = new XMLBuilder(sharedSettings, null, delimiter);
		builder.startDoctype();
		if (documentElement != null) {
			builder.addParameter(documentElement.getLocalName());
		} else {
			builder.addParameter(PLACEHOLDER_ELEMENT_NAME);
		}
		builder.append(" SYSTEM \"");
		builder.append(dtdFileName);
		builder.append("\"");
		builder.endDoctype();
		builder.linefeed();

		if (documentElement == null) {
			builder.append("<");
			builder.append(PLACEHOLDER_ELEMENT_NAME);
			generateEndTag(builder);
		}

		String insertText = (documentElement == null && document.getLastChild() != null ? delimiter : "")
				+ builder.toString();
		Position position = document.positionAt(beforeTagOffset);
		return CodeActionFactory.insertEdit(insertText, position, document.getTextDocument());
	}

	private static void generateStartTag(XMLBuilder builder, DOMDocument document) {
		if (document.getLastChild() != null) {
			builder.linefeed();
		}
		builder.startElement(PLACEHOLDER_ELEMENT_NAME, false);
	}

	private static void generateEndTag(XMLBuilder builder) {
		builder.append("></");
		builder.append(PLACEHOLDER_ELEMENT_NAME);
		builder.append(">");
	}

}
