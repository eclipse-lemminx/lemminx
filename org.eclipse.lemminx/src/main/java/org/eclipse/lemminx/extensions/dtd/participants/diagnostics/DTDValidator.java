/**
 *  Copyright (c) 2018 Angelo ZERR.
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
package org.eclipse.lemminx.extensions.dtd.participants.diagnostics;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.dtd.XMLDTDLoader;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.eclipse.lemminx.commons.text.CharSequenceUtils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.diagnostics.LSPErrorReporterForXML;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.extensions.xerces.LSPSecurityManager;
import org.eclipse.lemminx.extensions.xerces.LSPXMLEntityManager;
import org.eclipse.lemminx.uriresolver.CacheResourceException;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * DTD validator
 *
 */
public class DTDValidator {

	private static final Logger LOGGER = Logger.getLogger(DTDValidator.class.getName());

	public static void doDiagnostics(DOMDocument document, XMLEntityResolver entityResolver,
			List<Diagnostic> diagnostics, XMLValidationSettings validationSettings,
			ContentModelManager contentModelManager, CancelChecker monitor) {
		LSPErrorReporterForXML reporterForXML = new LSPErrorReporterForXML(document, diagnostics, contentModelManager,
				false, new HashMap<>());
		try {
			LSPXMLEntityManager entityManager = createXMLEntityManager(reporterForXML, entityResolver,
					validationSettings);

			XMLDTDLoader loader = new LSPXML11DTDProcessor(entityManager, reporterForXML, entityResolver);
			CharSequence content = document.getTextSequence();
			String uri = document.getDocumentURI();

			Reader inputStream = CharSequenceUtils.newReader(content);
			XMLInputSource source = new XMLInputSource(null, uri, uri, inputStream, null);
			loader.loadGrammar(source);
		} catch (IOException | CancellationException exception) {
			// ignore error
		} catch (CacheResourceException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unexpected DTDValidator error", e);
		} finally {
			reporterForXML.endReport();
		}
	}

	private static LSPXMLEntityManager createXMLEntityManager(LSPErrorReporterForXML reporterForXML,
			XMLEntityResolver entityResolver, XMLValidationSettings validationSettings) {
		LSPXMLEntityManager entityManager = new LSPXMLEntityManager(reporterForXML, null);
		// Update the resolver external entities capability
		boolean resolveExternalEntities = validationSettings != null ? validationSettings.isResolveExternalEntities()
				: false;
		entityManager.setResolveExternalEntities(resolveExternalEntities);
		// Update the resolver
		entityManager.setEntityResolver(entityResolver);
		// Update the security manager
		entityManager.setSecurityManager(LSPSecurityManager.getSecurityManager());
		return entityManager;
	}
}
