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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.CharSequenceReader;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.xml.sax.InputSource;

import com.thaiopensource.datatype.DatatypeLibraryLoader;
import com.thaiopensource.relaxng.parse.IllegalSchemaException;
import com.thaiopensource.relaxng.parse.compact.CompactParseable;
import com.thaiopensource.relaxng.pattern.AnnotationsImpl;
import com.thaiopensource.relaxng.pattern.CommentListImpl;
import com.thaiopensource.relaxng.pattern.NameClass;
import com.thaiopensource.relaxng.pattern.Pattern;
import com.thaiopensource.relaxng.pattern.SchemaBuilderImpl;
import com.thaiopensource.relaxng.pattern.SchemaPatternBuilder;
import com.thaiopensource.resolver.BasicResolver;
import com.thaiopensource.resolver.xml.sax.SAX;
import com.thaiopensource.resolver.xml.sax.SAXResolver;
import com.thaiopensource.util.VoidValue;

/**
 * Validates RNC (RELAX NG Compact Syntax) documents in two phases.
 *
 * <p><strong>Phase 1</strong> — Syntax validation via {@link RNCParser}: detects
 * structural errors (missing braces, missing assigns, unclosed blocks) and
 * produces diagnostics with precise error ranges and specific error codes.</p>
 *
 * <p><strong>Phase 2</strong> — Semantic validation via Jing's
 * {@code CompactParseable}: detects schema-level errors (undefined patterns,
 * duplicate definitions, missing start element) that require full schema
 * analysis.</p>
 *
 * <p>If Phase 1 finds errors, Phase 2 is skipped to avoid confusing duplicate
 * diagnostics from Jing's less precise error reporting.</p>
 *
 * <p>Use case: the editor validates the RNC document on each save or change,
 * showing red squiggles under the problematic tokens.</p>
 *
 * @see RNCErrorCode
 * @see LSPErrorReporterForRNC
 */
public class RNCValidator {

	private static final Logger LOGGER = Logger.getLogger(RNCValidator.class.getName());

	private static final String SOURCE = "rnc";

	public static List<Diagnostic> validate(TextDocument document, CancelChecker cancelChecker) {
		return validate(document, true, cancelChecker);
	}

	public static List<Diagnostic> validate(TextDocument document, boolean unnecessaryTagSupported,
			CancelChecker cancelChecker) {
		List<Diagnostic> diagnostics = new ArrayList<>();

		cancelChecker.checkCanceled();

		// Phase 1: Parse with RNCParser for syntax errors (precise ranges + specific codes)
		RNCDocument rncDocument = RNCParser.getInstance().parse(document.getTextSequence(), document.getUri());
		boolean hasParserErrors = !rncDocument.getErrors().isEmpty();

		if (hasParserErrors) {
			for (RNCDocument.RNCError error : rncDocument.getErrors()) {
				Range range = RNCPositionUtils.createRange(document, error.getStart(), error.getEnd());
				if (range != null) {
					Diagnostic d = new Diagnostic(range, error.getMessage(), DiagnosticSeverity.Error, SOURCE);
					d.setCode(error.getErrorCode().getCode());
					diagnostics.add(d);
				}
			}
		}

		// Phase 1b: Semantic validation on our AST (missing start, etc.)
		if (!hasParserErrors) {
			validateSemantics(rncDocument, document, unnecessaryTagSupported, diagnostics);
		}

		// Phase 2: Run Jing for semantic validation (undefined-pattern, duplicate-definition)
		// Skip Jing when the schema has no meaningful content (e.g., only comments or namespaces)
		boolean hasContent = rncDocument.getStartDecl() != null
				|| !rncDocument.getDefines().isEmpty()
				|| !rncDocument.getIncludes().isEmpty()
				|| rncDocument.hasTopLevelPattern();

		if (hasContent) {
			List<Diagnostic> jingDiagnostics = new ArrayList<>();
			LSPErrorReporterForRNC reporter = new LSPErrorReporterForRNC(document, jingDiagnostics, cancelChecker);

			try {
				cancelChecker.checkCanceled();

				InputSource inputSource = new InputSource();
				inputSource.setCharacterStream(new CharSequenceReader(document.getTextSequence()));
				inputSource.setSystemId(document.getUri());

				SAXResolver saxResolver = new SAXResolver(BasicResolver.getInstance());

				CompactParseable<Pattern, NameClass, org.xml.sax.Locator, VoidValue, CommentListImpl, AnnotationsImpl> parseable = new CompactParseable<>(
						SAX.createInput(inputSource), saxResolver.getResolver(), reporter);

				SchemaPatternBuilder spb = new SchemaPatternBuilder();
				SchemaBuilderImpl.parse(parseable, reporter, new DatatypeLibraryLoader(), spb, false);
			} catch (IllegalSchemaException e) {
				// Schema-level errors are already reported via the ErrorHandler.
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error validating RNC document: " + document.getUri(), e);
			}

			// Merge Jing diagnostics: skip errors we detect natively (MissingStartElement),
			// and when parser errors exist, only keep semantic Jing errors.
			for (Diagnostic jingDiag : jingDiagnostics) {
				RNCErrorCode code = jingDiag.getCode() != null
						? RNCErrorCode.fromCode(jingDiag.getCode().getLeft())
						: null;
				if (code == RNCErrorCode.MissingStartElement) {
					continue;
				}
				if (hasParserErrors) {
					if (code == RNCErrorCode.UndefinedPattern || code == RNCErrorCode.DuplicateDefinition) {
						diagnostics.add(jingDiag);
					}
				} else {
					diagnostics.add(jingDiag);
				}
			}
		}

		diagnostics.sort((a, b) -> {
			int lineCmp = a.getRange().getStart().getLine() - b.getRange().getStart().getLine();
			if (lineCmp != 0) {
				return lineCmp;
			}
			return a.getRange().getStart().getCharacter() - b.getRange().getStart().getCharacter();
		});

		return diagnostics;
	}

	private static void validateSemantics(RNCDocument rncDocument, TextDocument document,
			boolean unnecessaryTagSupported, List<Diagnostic> diagnostics) {
		List<RNCDefine> defines = rncDocument.getDefines();
		if (rncDocument.getStartDecl() == null && rncDocument.getIncludes().isEmpty()
				&& !rncDocument.hasTopLevelPattern()) {
			Range range;
			if (!defines.isEmpty()) {
				RNCDefine firstDefine = defines.get(0);
				range = firstDefine.getNameStart() != -1
						? RNCPositionUtils.createRange(document, firstDefine.getNameStart(), firstDefine.getNameEnd())
						: null;
			} else {
				range = RNCPositionUtils.ZERO_RANGE;
			}
			if (range != null) {
				Diagnostic d = new Diagnostic(range, "missing \"start\" element",
						DiagnosticSeverity.Error, SOURCE);
				d.setCode(RNCErrorCode.MissingStartElement.getCode());
				diagnostics.add(d);
			}
		}

		validateIncludes(rncDocument, document, diagnostics);
		validateUnusedDefines(rncDocument, document, unnecessaryTagSupported, diagnostics);
	}

	private static void validateIncludes(RNCDocument rncDocument, TextDocument document,
			List<Diagnostic> diagnostics) {
		String documentURI = rncDocument.getUri();
		if (documentURI == null || !documentURI.startsWith("file:")) {
			return;
		}
		for (RNCNode child : rncDocument.getChildren()) {
			if (child instanceof RNCInclude) {
				RNCInclude include = (RNCInclude) child;
				String uri = include.getUri();
				if (uri != null && !uri.isEmpty() && include.getUriStart() != -1) {
					String resolvedURI = resolveURI(documentURI, uri);
					if (resolvedURI != null) {
						try {
							Path path = Paths.get(new URI(resolvedURI));
							if (!Files.exists(path)) {
								Range range = RNCPositionUtils.selectUri(document, include);
								if (range != null) {
									Diagnostic d = new Diagnostic(range,
											"File \"" + uri + "\" not found.",
											DiagnosticSeverity.Error, SOURCE);
									d.setCode(RNCErrorCode.FileNotFound.getCode());
									diagnostics.add(d);
								}
							}
						} catch (Exception e) {
							// ignore
						}
					}
				}
			}
		}
	}

	private static void validateUnusedDefines(RNCDocument rncDocument, TextDocument document,
			boolean unnecessaryTagSupported, List<Diagnostic> diagnostics) {
		if (!rncDocument.getIncludes().isEmpty()) {
			return;
		}
		if (rncDocument.getStartDecl() == null) {
			return;
		}
		CharSequence text = document.getTextSequence();
		for (RNCDefine define : rncDocument.getDefines()) {
			if (define.getNameStart() == -1) {
				continue;
			}
			List<Location> references = RNCSearchUtils.findReferences(document, text, define, false);
			if (references.isEmpty()) {
				Range range = RNCPositionUtils.createRange(document, define.getNameStart(), define.getNameEnd());
				if (range != null) {
					Diagnostic d = new Diagnostic(range,
							"'" + define.getName() + "' is declared but never used",
							unnecessaryTagSupported ? DiagnosticSeverity.Hint : DiagnosticSeverity.Warning,
							SOURCE);
					d.setCode(RNCErrorCode.UnusedDefine.getCode());
					if (unnecessaryTagSupported) {
						d.setTags(Arrays.asList(DiagnosticTag.Unnecessary));
					}
					diagnostics.add(d);
				}
			}
		}
	}

	private static String resolveURI(String documentURI, String location) {
		try {
			return XMLEntityManager.expandSystemId(location, documentURI, false);
		} catch (MalformedURIException e) {
			return null;
		}
	}
}
