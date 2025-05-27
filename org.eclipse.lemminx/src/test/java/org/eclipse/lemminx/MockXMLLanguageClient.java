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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.lemminx.customservice.ActionableNotification;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;

/**
 * Mock XML Language client which helps to track show messages, actionable
 * notification and commands.
 * 
 * @author Angelo ZERR
 *
 */
public class MockXMLLanguageClient implements XMLLanguageClientAPI {

	private final List<PublishDiagnosticsParams> publishDiagnostics;

	private final List<MessageParams> showMessages;

	private final List<ActionableNotification> actionableNotifications;

	private final List<MessageParams> logMessages;

	public MockXMLLanguageClient() {
		publishDiagnostics = new CopyOnWriteArrayList<>();
		showMessages = new ArrayList<>();
		logMessages = new ArrayList<>();
		actionableNotifications = new ArrayList<>();
	}

	@Override
	public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
		return null;
	}

	@Override
	public void showMessage(MessageParams messageParams) {
		showMessages.add(messageParams);
	}

	@Override
	public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		publishDiagnostics.add(diagnostics);
	}

	@Override
	public void logMessage(MessageParams message) {
		logMessages.add(message);
	}

	@Override
	public void telemetryEvent(Object object) {

	}

	@Override
	public void actionableNotification(ActionableNotification notification) {
		actionableNotifications.add(notification);
	}

	@Override
	public CompletableFuture<Void> registerCapability(RegistrationParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Object> executeClientCommand(ExecuteCommandParams params) {
		throw new UnsupportedOperationException();
	}

	public List<PublishDiagnosticsParams> getPublishDiagnostics() {
		return publishDiagnostics;
	}

	public List<MessageParams> getLogMessages() {
		return logMessages;
	}

	public List<MessageParams> getShowMessages() {
		return showMessages;
	}

	public List<ActionableNotification> getActionableNotifications() {
		return actionableNotifications;
	}

}