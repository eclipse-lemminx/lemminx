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
package org.eclipse.lemminx.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.XMLTextDocumentService;
import org.eclipse.lemminx.client.ExtendedClientCapabilities;
import org.eclipse.lemminx.customservice.ActionableNotification;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.MessageParams;

/**
 * Test class for notification tests
 */
public abstract class AbstractNotifierTest {

	protected MockXMLLanguageServer languageServer;

	public void before() {
		this.languageServer = new MockXMLLanguageServer();
	}

	protected void setSupportCapabilities(boolean supportActionableNotification, boolean supportOpenSettings) {
		ExtendedClientCapabilities capabilities = new ExtendedClientCapabilities();
		capabilities.setActionableNotificationSupport(supportActionableNotification);
		capabilities.setOpenSettingsCommandSupport(supportOpenSettings);
		getTextDocumentService().updateClientCapabilities(new ClientCapabilities(), capabilities);
	}

	protected XMLTextDocumentService getTextDocumentService() {
		return languageServer.getXMLTextDocumentService();
	}

	/**
	 * Asserts "actionable notification" and "send message" counts
	 * 
	 * @param actionableNotification expected "actionable notification" count
	 * @param sendMessage            expected "send message" count
	 */
	protected void assertCounts(int actionableNotification, int sendMessage) {
		List<ActionableNotification> actionableNotifications = languageServer.getActionableNotifications();
		List<MessageParams> showMessages = languageServer.getShowMessages();
		assertEquals(actionableNotification, actionableNotifications.size());
		assertEquals(sendMessage, showMessages.size());
	}

	/**
	 * Asserts last show message
	 * 
	 * @param expectedMessage the expected message
	 */
	protected void assertLastShowMessage(String expectedMessage) {
		List<MessageParams> showMessages = languageServer.getShowMessages();
		assertEquals(expectedMessage,
				showMessages.size() > 0 ? showMessages.get(showMessages.size() - 1).getMessage() : null);
	}

	/**
	 * Assert last actionable notification message
	 * 
	 * @param expectedMessage the expected message
	 */
	protected void assertLastActionableNotificationMessage(String expectedMessage) {
		List<ActionableNotification> actionableNotifications = languageServer.getActionableNotifications();
		assertEquals(expectedMessage,
				actionableNotifications.size() > 0
						? actionableNotifications.get(actionableNotifications.size() - 1).getMessage()
						: null);
	}

}