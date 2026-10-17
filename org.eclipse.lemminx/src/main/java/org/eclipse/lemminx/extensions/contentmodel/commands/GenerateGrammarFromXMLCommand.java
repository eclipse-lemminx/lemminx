/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.NoGrammarConstraintsCodeAction;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorManager;
import org.eclipse.lemminx.extensions.generators.xml2dtd.DTDGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2relaxng.RelaxNGGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2rnc.RelaxNGCompactGeneratorSettings;
import org.eclipse.lemminx.extensions.generators.xml2xsd.XMLSchemaGeneratorSettings;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.IComponentProvider;
import org.eclipse.lemminx.services.extensions.commands.AbstractDOMDocumentCommandHandler;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.settings.CompositeSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.JSONUtility;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.grammar.generateFromXml" to generate a grammar file (XSD,
 * DTD, RelaxNG) from a given XML document and optionally bind it.
 *
 * <p>
 * The command parameters {@link ExecuteCommandParams} must be filled with:
 * </p>
 * <ul>
 * <li>arg 0: document URI ({@link org.eclipse.lsp4j.TextDocumentIdentifier}) -
 * the XML document.</li>
 * <li>arg 1: grammar type (String) - "xsd", "dtd", or "rng".</li>
 * <li>arg 2: grammar file URI (String) - the target grammar file URI.</li>
 * <li>arg 3: binding type (String, optional) -
 * "xsi:noNamespaceSchemaLocation", "xsi:schemaLocation", "xml-model",
 * "DOCTYPE", or null for file association (client handles binding).</li>
 * </ul>
 *
 * <p>
 * Returns a {@link GenerateGrammarResult} with the generated grammar content
 * and an optional binding text edit.
 * </p>
 */
public class GenerateGrammarFromXMLCommand extends AbstractDOMDocumentCommandHandler {

	public static final String COMMAND_ID = "xml.grammar.generateFromXml";

	private final IComponentProvider componentProvider;

	public GenerateGrammarFromXMLCommand(IXMLDocumentProvider documentProvider,
			IComponentProvider componentProvider) {
		super(documentProvider);
		this.componentProvider = componentProvider;
	}

	@Override
	protected Object executeCommand(DOMDocument document, ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String grammarType = ArgumentsUtils.getArgAt(params, 1, String.class);
		String grammarFileURI = ArgumentsUtils.getArgAt(params, 2, String.class);

		// arg 3 is optional (null for file association)
		String bindingType = null;
		Object bindingTypeObj = ArgumentsUtils.getArgAt(params.getArguments(), 3);
		if (bindingTypeObj != null) {
			bindingType = JSONUtility.toModel(bindingTypeObj, String.class);
		}

		// arg 4 is optional formatting options (tabSize, insertSpaces)
		Object formattingOptionsObj = ArgumentsUtils.getArgAt(params.getArguments(), 4);
		if (formattingOptionsObj != null) {
			FormattingOptions formattingOptions = JSONUtility.toModel(formattingOptionsObj,
					FormattingOptions.class);
			if (formattingOptions != null) {
				sharedSettings = new CompositeSettings(sharedSettings, formattingOptions);
			}
		}

		// Generate grammar content
		FileContentGeneratorManager generator = componentProvider
				.getComponent(FileContentGeneratorManager.class);
		String grammarContent = generator.generate(document, sharedSettings,
				createGeneratorSettings(grammarType), cancelChecker);

		// Generate binding edit if needed
		TextDocumentEdit bindingEdit = null;
		if (bindingType != null) {
			validateBindingType(grammarType, bindingType);
			String grammarRelativeURI = FilesUtils.getRelativeURI(grammarFileURI, document.getDocumentURI());
			bindingEdit = createBindingEdit(bindingType, grammarRelativeURI, document, sharedSettings);
		}

		return new GenerateGrammarResult(grammarContent, bindingEdit);
	}

	private static org.eclipse.lemminx.extensions.generators.FileContentGeneratorSettings createGeneratorSettings(
			String grammarType) {
		switch (grammarType) {
		case "xsd":
			return new XMLSchemaGeneratorSettings();
		case "dtd":
			return new DTDGeneratorSettings();
		case "rng":
			return new RelaxNGGeneratorSettings();
		case "rnc":
			return new RelaxNGCompactGeneratorSettings();
		default:
			throw new UnsupportedOperationException("Unknown grammar type: " + grammarType);
		}
	}

	private static void validateBindingType(String grammarType, String bindingType) {
		switch (bindingType) {
		case "xsi:schemaLocation":
		case "xsi:noNamespaceSchemaLocation":
			if (!"xsd".equals(grammarType)) {
				throw new UnsupportedOperationException(
						"Binding type '" + bindingType + "' is only compatible with XSD grammar type");
			}
			break;
		case "DOCTYPE":
			if (!"dtd".equals(grammarType)) {
				throw new UnsupportedOperationException(
						"Binding type 'DOCTYPE' is only compatible with DTD grammar type");
			}
			break;
		}
	}

	private static TextDocumentEdit createBindingEdit(String bindingType, String grammarRelativeURI,
			DOMDocument document, SharedSettings sharedSettings) throws BadLocationException {
		switch (bindingType) {
		case "xsi:noNamespaceSchemaLocation":
			return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(grammarRelativeURI,
					document, sharedSettings);
		case "xsi:schemaLocation": {
			String targetNamespace = getTargetNamespaceFromDocument(document);
			if (StringUtils.isEmpty(targetNamespace)) {
				return NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(grammarRelativeURI,
						document, sharedSettings);
			}
			return NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(grammarRelativeURI,
					targetNamespace, document, sharedSettings);
		}
		case "DOCTYPE":
			return NoGrammarConstraintsCodeAction.createDocTypeEdit(grammarRelativeURI, document,
					sharedSettings);
		case "xml-model": {
			String targetNamespace = getTargetNamespaceFromDocument(document);
			return NoGrammarConstraintsCodeAction.createXmlModelEdit(grammarRelativeURI, targetNamespace,
					document, sharedSettings);
		}
		default:
			throw new UnsupportedOperationException("Unknown binding type: " + bindingType);
		}
	}

	private static String getTargetNamespaceFromDocument(DOMDocument document) {
		DOMElement documentElement = document.getDocumentElement();
		if (documentElement == null) {
			return null;
		}
		return documentElement.getNamespaceURI();
	}


	/**
	 * Result returned by the generate grammar command.
	 */
	public static class GenerateGrammarResult {

		private final String grammarContent;
		private final TextDocumentEdit bindingEdit;

		public GenerateGrammarResult(String grammarContent, TextDocumentEdit bindingEdit) {
			this.grammarContent = grammarContent;
			this.bindingEdit = bindingEdit;
		}

		public String getGrammarContent() {
			return grammarContent;
		}

		public TextDocumentEdit getBindingEdit() {
			return bindingEdit;
		}
	}
}
