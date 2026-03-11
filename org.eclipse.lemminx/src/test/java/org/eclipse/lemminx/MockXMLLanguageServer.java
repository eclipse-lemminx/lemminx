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
package org.eclipse.lemminx;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lemminx.customservice.ActionableNotification;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

/**
 * Mock XML Language server which helps to track show messages, actionable
 * notification and commands.
 * 
 * @author Angelo ZERR
 *
 */
public class MockXMLLanguageServer extends XMLLanguageServer {

	public MockXMLLanguageServer() {
		super.setClient(new MockXMLLanguageClient());
	}

	public List<PublishDiagnosticsParams> getPublishDiagnostics() {
		return getLanguageClient().getPublishDiagnostics();
	}

	public void waitForDiagnosticCount(int expectedCount, long timeoutMs) {
		getLanguageClient().waitForDiagnosticCount(expectedCount, timeoutMs);
	}

	@Override
	public MockXMLLanguageClient getLanguageClient() {
		return (MockXMLLanguageClient) super.getLanguageClient();
	}

	public List<MessageParams> getShowMessages() {
		return getLanguageClient().getShowMessages();
	}

	public List<ActionableNotification> getActionableNotifications() {
		return getLanguageClient().getActionableNotifications();
	}

	public void registerCommand(String commandId, IDelegateCommandHandler handler) {
		getXMLLanguageService().getCommandService().registerCommand(commandId, handler);
	}

	public void unregisterCommand(String commandId) {
		getXMLLanguageService().getCommandService().unregisterCommand(commandId);
	}

	public CompletableFuture<Object> executeCommand(String command, Object... arguments) {
		getXMLLanguageService().initializeIfNeeded();
		ExecuteCommandParams params = new ExecuteCommandParams(command, Arrays.asList(arguments));
		return getWorkspaceService().executeCommand(params);
	}

	public TextDocumentIdentifier didOpen(String fileURI, String xml) {
		TextDocumentIdentifier xmlIdentifier = new TextDocumentIdentifier(fileURI);
		DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(
				new TextDocumentItem(xmlIdentifier.getUri(), "xml", 1, xml));
		XMLTextDocumentService textDocumentService = (XMLTextDocumentService) super.getTextDocumentService();
		textDocumentService.didOpen(params);
		try {
			// Force the parse of DOM document
			textDocumentService.getDocument(params.getTextDocument().getUri()).getModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlIdentifier;
	}

	public TextDocumentIdentifier didChange(String fileURI, List<TextDocumentContentChangeEvent> contentChanges) {
		TextDocumentIdentifier xmlIdentifier = new TextDocumentIdentifier(fileURI);
		DidChangeTextDocumentParams params = new DidChangeTextDocumentParams(
				new VersionedTextDocumentIdentifier(xmlIdentifier.getUri(), 1), contentChanges);
		XMLTextDocumentService textDocumentService = (XMLTextDocumentService) super.getTextDocumentService();
		textDocumentService.didChange(params);
		try {
			// Force the parse of DOM document
			textDocumentService.getDocument(params.getTextDocument().getUri()).getModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlIdentifier;
	}

	public TextDocumentIdentifier didClose(String fileURI) {
		TextDocumentIdentifier xmlIdentifier = new TextDocumentIdentifier(fileURI);
		DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(xmlIdentifier);
		XMLTextDocumentService textDocumentService = (XMLTextDocumentService) super.getTextDocumentService();
		textDocumentService.didClose(params);
		return xmlIdentifier;
	}

	public TextDocumentIdentifier didSave(String fileURI) {
		TextDocumentIdentifier xmlIdentifier = new TextDocumentIdentifier(fileURI);
		DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(xmlIdentifier);
		XMLTextDocumentService textDocumentService = (XMLTextDocumentService) super.getTextDocumentService();
		textDocumentService.didSave(params);
		try {
			// Force the parse of DOM document
			textDocumentService.getDocument(params.getTextDocument().getUri()).getModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return xmlIdentifier;
	}
}
