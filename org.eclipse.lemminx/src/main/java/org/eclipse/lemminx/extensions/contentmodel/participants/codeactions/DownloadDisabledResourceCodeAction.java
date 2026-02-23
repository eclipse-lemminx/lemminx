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
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.commands.XMLValidationFileCommand;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonObject;

/**
 * Code action to force download of an external resource.
 * 
 * @author Angelo ZERR
 *
 */
public class DownloadDisabledResourceCodeAction implements ICodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(DownloadDisabledResourceCodeAction.class.getName());

	private static final String URL_KEY = "url";
	private static final String FORCE_DOWNLOAD_TITLE = "Force download of ''{0}''.";

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		try {
			Range diagnosticRange = diagnostic.getRange();
			int start = document.offsetAt(diagnosticRange.getStart());
			int end = document.offsetAt(diagnosticRange.getEnd());
			String url = document.getTextSequence().subSequence(start, end).toString();

			String title = MessageFormat.format(FORCE_DOWNLOAD_TITLE, url);
			CodeAction codeAction = new CodeAction(title);
			codeAction.setKind(CodeActionKind.QuickFix);

			String documentURI = document.getDocumentURI();

			Command command = createDownloadCommand(title, url, documentURI);

			codeAction.setCommand(command);
			codeAction.setDiagnostics(Arrays.asList(diagnostic));
			codeActions.add(codeAction);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while creating download disabled resource code action.", e);
		}
	}

	public static Command createDownloadCommand(String title, String url, String documentURI) {
		JsonObject args = new JsonObject();
		args.addProperty(URL_KEY, url);
		TextDocumentIdentifier identifier = new TextDocumentIdentifier(documentURI);
		List<Object> arguments = Arrays.asList(identifier, args);
		return new Command(title, XMLValidationFileCommand.COMMAND_ID, arguments);
	}

	/**
	 * Returns the url to force to download and null otherwise.
	 * 
	 * @param validationArgs the validation arguments.
	 * 
	 * @return the url to force to download and null otherwise.
	 */
	public static String getUrlToForceToDownload(Map<String, Object> validationArgs) {
		if (validationArgs == null) {
			return null;
		}
		return (String) validationArgs.get(URL_KEY);
	}

}
