/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLParseException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.diagnostics.LSPErrorReporterForXML;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.extensions.relaxng.contentmodel.MySchemaPatternBuilder;
import org.eclipse.lemminx.extensions.relaxng.jing.SchemaProvider;
import org.eclipse.lemminx.extensions.xerces.ReferencedGrammarDiagnosticsInfo;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.xml.sax.XMLReaderCreator;

/**
 * RNG validator utilities class.
 *
 */
public class RNGValidator {

	private static final Logger LOGGER = Logger.getLogger(RNGValidator.class.getName());

	public static void doDiagnostics(DOMDocument document, XMLEntityResolver entityResolver,
			List<Diagnostic> diagnostics, XMLValidationSettings validationSettings,
			ContentModelManager contentModelManager, CancelChecker monitor) {

		Map<String, ReferencedGrammarDiagnosticsInfo> referencedGrammarDiagnosticsInfoCache = new HashMap<>();
		// When referenced grammar (XSD, DTD) have an error (ex : syntax error), the
		// error must be reported.
		// We create a reporter for grammar since Xerces reporter stores the XMLLocator
		// for XML and Grammar.
		LSPErrorReporterForXML reporterForRNG = new LSPErrorReporterForXML(document, diagnostics, contentModelManager,
				validationSettings != null ? validationSettings.isRelatedInformation() : false,
				referencedGrammarDiagnosticsInfoCache);
		try {
			InputSource input = DOMUtils.createInputSource(document);
			XMLReaderCreator creator = new RNGXMLReaderCreator(reporterForRNG);
			SchemaProvider.loadSchema(input, entityResolver, reporterForRNG,
					new MySchemaPatternBuilder(), creator);
		} catch (IOException | SAXException | CancellationException | XMLParseException
				| IncorrectSchemaException exception) {
			// ignore error
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unexpected RNG Validator error", e);
		} finally {
			reporterForRNG.endReport();
		}
	}

}
