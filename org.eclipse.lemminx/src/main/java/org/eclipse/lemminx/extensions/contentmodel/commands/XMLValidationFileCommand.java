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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import java.util.Map;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.IXMLValidationService;
import org.eclipse.lemminx.services.extensions.commands.AbstractDOMDocumentCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.JSONUtility;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonObject;

/**
 * XML Command "xml.validation.current.file" to revalidate a give XML file which
 * means:
 *
 * <ul>
 * <li>remove the referenced grammar in the XML file from the Xerces grammar
 * pool (used by the Xerces validation) and the content model documents cache
 * (used by the XML completion/hover based on the grammar)</li>
 * <li>trigger the validation for the given XML file</li>
 * </ul>
 *
 * @author Angelo ZERR
 *
 */
public class XMLValidationFileCommand extends AbstractDOMDocumentCommandHandler {

	public static final String COMMAND_ID = "xml.validation.current.file";

	private final ContentModelManager contentModelManager;

	private final IXMLValidationService validationService;

	public XMLValidationFileCommand(ContentModelManager contentModelManager, IXMLDocumentProvider documentProvider,
			IXMLValidationService validationService) {
		super(documentProvider);
		this.contentModelManager = contentModelManager;
		this.validationService = validationService;
	}

	@Override
	protected Object executeCommand(DOMDocument document, ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		// Validation args can contains the external resource ('url' as key map) to
		// force do download.
		JsonObject validationArgs = getJsonObject(params, 1);
		JsonObject validationSettings = getJsonObject(params, 2);
		// 1. remove the referenced grammar in the XML file from the Xerces grammar pool
		// (used by the Xerces validation) and the content model documents cache (used
		// by the XML completion/hover based on the grammar)
		contentModelManager.evictCacheFor(document);
		// 2. trigger the validation for the given XML file
		Map map = JSONUtility.toModel(validationArgs, Map.class);
		XMLValidationRootSettings settings = JSONUtility.toModel(validationSettings, XMLValidationRootSettings.class);
		validationService.validate(document, map, settings);
		return null;
	}

	private static JsonObject getJsonObject(ExecuteCommandParams params, int index) {
		if (params.getArguments().size() <= index) {
			return null;
		}
		Object result = params.getArguments().get(index);
		return result instanceof JsonObject ? (JsonObject) result : null;
	}

}