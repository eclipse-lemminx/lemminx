/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missinggrammar;

import static org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missinggrammar.MissingGrammarDataConstants.DATA_FILE_FIELD;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.commons.DiagnosticUtils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorManager;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorSettings;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonObject;

/**
 * Abstract code action participant which manages missing referenced grammar file (DTD, XSD).
 */
public abstract class AbstractFixMissingGrammarCodeAction implements ICodeActionParticipant {

	private static final String FILE_SCHEME = "'file:///";

	private final Map<String, ICodeActionResolvesParticipant> resolveCodeActionParticipants;

	public AbstractFixMissingGrammarCodeAction() {
		// Register available resolvers.
		resolveCodeActionParticipants = new HashMap<>();
		resolveCodeActionParticipants.put(GenerateDTDCodeActionResolver.PARTICIPANT_ID,
				new GenerateDTDCodeActionResolver());
		resolveCodeActionParticipants.put(GenerateXSDCodeActionResolver.PARTICIPANT_ID,
				new GenerateXSDCodeActionResolver());
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		String missingFilePath = getPathFromDiagnostic(diagnostic);
		if (StringUtils.isEmpty(missingFilePath)) {
			return;
		}
		Path p = Paths.get(missingFilePath);
		if (p.toFile().exists()) {
			return;
		}

		CodeAction makeSchemaFile = createGenerateFileCodeAction(request, diagnostic, missingFilePath, p,
				cancelChecker);
		codeActions.add(makeSchemaFile);
	}

	public ICodeActionResolvesParticipant getResolveCodeActionParticipant(String participantId) {
		return resolveCodeActionParticipants.get(participantId);
	}

	private CodeAction createGenerateFileCodeAction(ICodeActionRequest request, Diagnostic diagnostic,
			String missingFilePath, Path p, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		String title = "Generate missing file '" + p.toFile().getName() + "'";

		if (request.canSupportResolve()) {
			CodeAction makeSchemaFile = new CodeAction(title);
			makeSchemaFile.setDiagnostics(Collections.singletonList(diagnostic));
			makeSchemaFile.setKind(CodeActionKind.QuickFix);

			JsonObject data = DataEntryField.createData(document.getDocumentURI(), getParticipantId());
			data.addProperty(DATA_FILE_FIELD, missingFilePath);
			makeSchemaFile.setData(data);

			return makeSchemaFile;
		}

		SharedSettings sharedSettings = request.getSharedSettings();
		// Generate XSD / DTD from the DOM document
		FileContentGeneratorManager generator = request.getComponent(FileContentGeneratorManager.class);
		String schemaTemplate = generator.generate(document, sharedSettings, getFileContentGeneratorSettings(),
				cancelChecker);

		// Create code action to create the XSD file with the generated XSD content
		CodeAction makeSchemaFile = CodeActionFactory.createFile(title, "file:///" + missingFilePath, schemaTemplate,
				diagnostic);
		return makeSchemaFile;
	}

	protected abstract String getParticipantId();

	/**
	 * Extract the file to create from the diagnostic. Example diagnostic:
	 * schema_reference.4: Failed to read schema document
	 * 'file:///home/dthompson/Documents/TestFiles/potationCoordination.xsd',
	 * because 1) could not find the document; 2) the document could not be read; 3)
	 * the root element of the document is not <xsd:schema>.
	 */
	private String getPathFromDiagnostic(Diagnostic diagnostic) {
		String message = DiagnosticUtils.getDiagnosticMessage(diagnostic);
		int startIndex = message.indexOf(FILE_SCHEME);
		if (startIndex != -1) {
			int endIndex = message.lastIndexOf("'");
			return message.substring(startIndex + FILE_SCHEME.length(), endIndex);
		}

		return null;
	}

	/**
	 * Returns the grammar settings used to generate the missing grammar file (XSD,
	 * DTD).
	 * 
	 * @return the grammar settings used to generate the missing grammar file (XSD,
	 *         DTD).
	 */
	protected abstract FileContentGeneratorSettings getFileContentGeneratorSettings();

}