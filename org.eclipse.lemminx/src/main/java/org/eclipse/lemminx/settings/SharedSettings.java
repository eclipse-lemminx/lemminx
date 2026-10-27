/*******************************************************************************
* Copyright (c) 2019-2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.settings;

import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;

/**
 * SharedSettings
 */
public class SharedSettings {

	private final XMLCompletionSettings completion;
	private boolean linkedEditingEnabled;
	private final XMLFoldingSettings foldings;
	private final XMLFormattingOptions format;
	private final XMLValidationRootSettings validationSettings;
	private final XMLSymbolSettings symbols;

	private final XMLCodeActionSettings codeActionSettings;
	private final XMLCodeLensSettings codeLens;
	private final XMLSemanticTokensSettings semanticTokens;
	private final XMLHoverSettings hoverSettings;
	private final XMLPreferences preferences;
	private final XMLWorkspaceSettings workspaceSettings;
	private boolean actionableNotificationSupport;
	private boolean openSettingsCommandSupport;
	private boolean bindingWizardSupport;

	public SharedSettings() {
		this.completion = new XMLCompletionSettings();
		this.foldings = new XMLFoldingSettings();
		this.format = new XMLFormattingOptions(true);
		this.validationSettings = new XMLValidationRootSettings();
		this.symbols = new XMLSymbolSettings();
		this.codeActionSettings = new XMLCodeActionSettings();
		this.codeLens = new XMLCodeLensSettings();
		this.semanticTokens = new XMLSemanticTokensSettings();
		this.hoverSettings = new XMLHoverSettings();
		this.preferences = new XMLPreferences();
		this.workspaceSettings = new XMLWorkspaceSettings();
		this.actionableNotificationSupport = false;
		this.openSettingsCommandSupport = false;
		this.linkedEditingEnabled = false;

	}

	public SharedSettings(SharedSettings newSettings) {
		this();
		this.completion.merge(newSettings.getCompletionSettings());
		this.foldings.merge(newSettings.getFoldingSettings());
		this.format.merge(newSettings.getFormattingSettings());
		this.validationSettings.merge(newSettings.getValidationSettings());
		this.symbols.merge(newSettings.getSymbolSettings());
		this.codeActionSettings.setCapabilities(newSettings.getCodeActionSettings().getCapabilities());
		this.codeLens.merge(newSettings.getCodeLensSettings());
		this.semanticTokens.merge(newSettings.getSemanticTokensSettings());
		this.hoverSettings.setCapabilities(newSettings.getHoverSettings().getCapabilities());
		this.preferences.merge(newSettings.getPreferences());
		this.workspaceSettings.setCapabilities(newSettings.getWorkspaceSettings().getCapabilities());
		this.actionableNotificationSupport = newSettings.isActionableNotificationSupport();
		this.openSettingsCommandSupport = newSettings.isOpenSettingsCommandSupport();
		this.bindingWizardSupport = newSettings.isBindingWizardSupport();
		this.linkedEditingEnabled = newSettings.isLinkedEditingEnabled();
	}

	public XMLCompletionSettings getCompletionSettings() {
		return completion;
	}

	public XMLFoldingSettings getFoldingSettings() {
		return foldings;
	}

	public XMLFormattingOptions getFormattingSettings() {
		return format;
	}

	public XMLValidationRootSettings getValidationSettings() {
		return validationSettings;
	}

	public XMLSymbolSettings getSymbolSettings() {
		return symbols;
	}

	public XMLCodeActionSettings getCodeActionSettings() {
		return codeActionSettings;
	}

	public XMLCodeLensSettings getCodeLensSettings() {
		return codeLens;
	}

	public XMLSemanticTokensSettings getSemanticTokensSettings() {
		return semanticTokens;
	}

	public XMLHoverSettings getHoverSettings() {
		return hoverSettings;
	}

	public XMLPreferences getPreferences() {
		return preferences;
	}

	public XMLWorkspaceSettings getWorkspaceSettings() {
		return workspaceSettings;
	}

	/**
	 * Returns true if the client supports actionable notifications and false
	 * otherwise
	 *
	 * See {@link org.eclipse.lemminx.customservice.ActionableNotification} and
	 * {@link org.eclipse.lemminx.customservice.XMLLanguageClientAPI}
	 *
	 * @return true if the client supports actionable notifications and false
	 *         otherwise
	 */
	public boolean isActionableNotificationSupport() {
		return actionableNotificationSupport;
	}

	/**
	 * Sets the actionableNotificationSupport boolean
	 *
	 * @param actionableNotificationSupport
	 */
	public void setActionableNotificationSupport(boolean actionableNotificationSupport) {
		this.actionableNotificationSupport = actionableNotificationSupport;
	}

	/**
	 * Returns true if the client supports the open settings command and false
	 * otherwise
	 *
	 * See {@link org.eclipse.lemminx.client.ClientCommands#OPEN_SETTINGS}
	 *
	 * @return true if the client supports the open settings command and false
	 *         otherwise
	 */
	public boolean isOpenSettingsCommandSupport() {
		return openSettingsCommandSupport;
	}

	/**
	 * Sets the openSettingsCommandSupport boolean
	 *
	 * @param openSettingsCommandSupport
	 */
	public void setOpenSettingsCommandSupport(boolean openSettingsCommandSupport) {
		this.openSettingsCommandSupport = openSettingsCommandSupport;
	}

	/**
	 * Returns true if the client supports the `xml.open.binding.wizard` command
	 * using dropdown and false otherwise
	 *
	 * @return bindingWizardSupport
	 */
	public boolean isBindingWizardSupport() {
		return this.bindingWizardSupport;
	}

	/**
	 * Sets the bindingWizardSupport boolean
	 *
	 * @param bindingWizardSupport
	 */
	public void setBindingWizardSupport(boolean bindingWizardSupport) {
		this.bindingWizardSupport = bindingWizardSupport;
	}

	public void setLinkedEditingEnabled(boolean linkedEditingEnabled) {
		this.linkedEditingEnabled = linkedEditingEnabled;
	}

	public boolean isLinkedEditingEnabled() {
		return linkedEditingEnabled;
	}
}
