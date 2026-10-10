/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.filepath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.filepath.participants.FilePathCompletionParticipant;
import org.eclipse.lemminx.extensions.filepath.settings.FilePathExpression;
import org.eclipse.lemminx.extensions.filepath.settings.FilePathMapping;
import org.eclipse.lemminx.extensions.filepath.settings.FilePathSupportSettings;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.InitializeParams;

/**
 * File Path support plugin.
 */
public class FilePathPlugin implements IXMLExtension {

	private static final Logger LOGGER = Logger.getLogger(FilePathPlugin.class.getName());

	private final FilePathCompletionParticipant completionParticipant;
	private FilePathSupportSettings filePathsSettings;

	private List<IFilePathSupportParticipant> filePathSupportParticipants;

	public FilePathPlugin() {
		completionParticipant = new FilePathCompletionParticipant(this);
	}

	@Override
	public void doSave(ISaveContext context) {
		if (context.getType() != ISaveContext.SaveContextType.DOCUMENT) {
			// Settings
			updateSettings(context);
		}
	}

	private void updateSettings(ISaveContext saveContext) {
		Object initializationOptionsSettings = saveContext.getSettings();
		FilePathSupportSettings settings = FilePathSupportSettings
				.getFilePathsSettings(initializationOptionsSettings);
		updateSettings(settings, saveContext);
	}

	private void updateSettings(FilePathSupportSettings settings, ISaveContext context) {
		this.filePathsSettings = settings;
	}

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		registry.registerCompletionParticipant(completionParticipant);
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.unregisterCompletionParticipant(completionParticipant);
	}

	public FilePathSupportSettings getFilePathsSettings() {
		return filePathsSettings;
	}

	/**
	 * Return the list of {@link FilePathExpression} for the given document and an
	 * empty list otherwise.
	 * 
	 * @param xmlDocument the DOM document
	 * 
	 * @return the list of {@link FilePathExpression} for the given document and an
	 *         empty list otherwise.
	 */
	public List<IFilePathExpression> findFilePathExpressions(DOMDocument xmlDocument) {
		List<IFilePathExpression> expressions = new ArrayList<>();
		fillFromParticipants(xmlDocument, expressions);
		fillFromUserSettings(xmlDocument, expressions);
		return expressions;
	}

	private void fillFromParticipants(DOMDocument xmlDocument, List<IFilePathExpression> allExpressions) {
		for (IFilePathSupportParticipant participant : getFilePathSupportParticipants()) {
			List<IFilePathExpression> expressions = participant.collectFilePathExpressions(xmlDocument);
			if (expressions != null && !expressions.isEmpty()) {
				allExpressions.addAll(expressions);
			}
		}
	}

	private void fillFromUserSettings(DOMDocument xmlDocument, List<IFilePathExpression> expressions) {
		FilePathSupportSettings settings = getFilePathsSettings();
		if (settings == null) {
			return;
		}

		List<FilePathMapping> mappings = settings.getFilePathMappings();
		fillFromMappings(xmlDocument, mappings, expressions);
	}

	private static void fillFromMappings(DOMDocument xmlDocument, List<FilePathMapping> mappings,
			List<IFilePathExpression> expressions) {
		if (mappings == null) {
			return;
		}

		for (FilePathMapping filePaths : mappings) {
			if (filePaths.matches(xmlDocument)) {
				expressions.addAll(filePaths.getExpressions());
			}
		}
	}

	public List<IFilePathSupportParticipant> getFilePathSupportParticipants() {
		if (filePathSupportParticipants == null) {
			loadFilePathSupportParticipants();
		}
		return filePathSupportParticipants;
	}

	private synchronized void loadFilePathSupportParticipants() {
		if (filePathSupportParticipants != null) {
			return;
		}
		List<IFilePathSupportParticipant> participants = new ArrayList<>();
		Iterator<IFilePathSupportParticipant> extensions = ServiceLoader.load(IFilePathSupportParticipant.class)
				.iterator();
		while (extensions.hasNext()) {
			try {
				participants.add(extensions.next());
			} catch (ServiceConfigurationError e) {
				LOGGER.log(Level.SEVERE, "Error while instantiating file path support participant", e);
			}
		}
		filePathSupportParticipants = participants;
	}
}