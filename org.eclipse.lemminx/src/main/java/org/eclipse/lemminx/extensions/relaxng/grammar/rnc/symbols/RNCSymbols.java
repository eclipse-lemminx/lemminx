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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAttribute;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypesDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCElement;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNamespaceDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides document symbols (outline) for RNC (RELAX NG Compact Syntax)
 * documents.
 *
 * <p>Produces a hierarchical list of {@link DocumentSymbol} objects for the
 * editor's Outline view and breadcrumb navigation. Top-level symbols include
 * namespace declarations, datatypes declarations, start assignments, define
 * statements, div blocks, and include directives.</p>
 *
 * <p>Use case: the user opens the Outline panel and sees a tree of all
 * defines and declarations in the RNC file, enabling quick navigation.</p>
 *
 * <p>Example outline for:</p>
 * <pre>
 * namespace rng = "http://relaxng.org/ns/structure/1.0"
 * start = root
 * root = element root {
 *   inline*
 * }
 * </pre>
 * <p>Produces:</p>
 * <ul>
 *   <li>namespace rng (Package)</li>
 *   <li>start (Event)</li>
 *   <li>root → element root (Variable → Class)</li>
 * </ul>
 */
public class RNCSymbols {

	/**
	 * Computes hierarchical document symbols for the editor's Outline view.
	 *
	 * <p>Use case: populates the Outline panel and breadcrumb bar with a
	 * tree of namespace/datatypes declarations, start, defines, includes,
	 * and nested element/attribute patterns.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort computation if the document changes.
	 * @return a list of {@link DocumentSymbol} objects forming a tree.
	 */
	public List<DocumentSymbol> findDocumentSymbols(RNCDocument rncDocument, CancelChecker cancelChecker) {
		if (rncDocument == null) {
			return Collections.emptyList();
		}

		TextDocument textDocument = rncDocument.getTextDocument();
		CharSequence text = textDocument.getTextSequence();

		List<RNCNode> declarations = collectDeclarations(rncDocument);
		List<DocumentSymbol> symbols = new ArrayList<>();

		for (int i = 0; i < declarations.size(); i++) {
			cancelChecker.checkCanceled();
			RNCNode decl = declarations.get(i);

			int consumed = 0;
			if (decl instanceof RNCDefine || decl instanceof RNCStart) {
				int j = i + 1;
				while (j < declarations.size()
						&& (declarations.get(j) instanceof RNCElement
								|| declarations.get(j) instanceof RNCAttribute)) {
					j++;
				}
				consumed = j - i - 1;
			}

			int rangeEnd;
			int afterIndex = i + 1 + consumed;
			if (afterIndex < declarations.size()) {
				rangeEnd = trimTrailingWhitespace(text, declarations.get(afterIndex).getStart());
			} else {
				rangeEnd = trimTrailingWhitespace(text, text.length());
			}

			Range range = RNCPositionUtils.createRange(textDocument, decl.getStart(), rangeEnd);
			if (range == null) {
				continue;
			}

			DocumentSymbol symbol = null;

			if (decl instanceof RNCNamespaceDecl) {
				symbol = createNamespaceSymbol(textDocument, (RNCNamespaceDecl) decl, range);
			} else if (decl instanceof RNCDatatypesDecl) {
				symbol = createDatatypesSymbol(textDocument, (RNCDatatypesDecl) decl, range);
			} else if (decl instanceof RNCStart) {
				Range selectionRange = RNCPositionUtils.createRange(textDocument, decl.getStart(),
						decl.getStart() + 5);
				if (selectionRange != null) {
					symbol = new DocumentSymbol(RNCKeywords.START, SymbolKind.Event, range, selectionRange);
					for (int j = i + 1; j <= i + consumed; j++) {
						addElementOrAttributeChild(textDocument, declarations.get(j), symbol);
					}
				}
			} else if (decl instanceof RNCDefine) {
				RNCDefine define = (RNCDefine) decl;
				String name = define.getName();
				if (name == null) {
					continue;
				}
				Range selectionRange = RNCPositionUtils.createRange(textDocument, define.getNameStart(),
						define.getNameEnd());
				if (selectionRange != null) {
					symbol = new DocumentSymbol(name, SymbolKind.Variable, range, selectionRange);
					for (int j = i + 1; j <= i + consumed; j++) {
						addElementOrAttributeChild(textDocument, declarations.get(j), symbol);
					}
				}
			} else if (decl instanceof RNCInclude) {
				symbol = createIncludeSymbol(textDocument, (RNCInclude) decl, range);
			} else if (decl instanceof RNCElement) {
				symbol = createElementSymbol(textDocument, (RNCElement) decl);
			} else if (decl instanceof RNCAttribute) {
				symbol = createAttributeSymbol(textDocument, (RNCAttribute) decl);
			}

			i += consumed;

			if (symbol != null) {
				symbols.add(symbol);
			}
		}
		return symbols;
	}

	private void addElementOrAttributeChild(TextDocument textDocument, RNCNode node, DocumentSymbol parentSymbol) {
		DocumentSymbol childSymbol = null;
		if (node instanceof RNCElement) {
			childSymbol = createElementSymbol(textDocument, (RNCElement) node);
		} else if (node instanceof RNCAttribute) {
			childSymbol = createAttributeSymbol(textDocument, (RNCAttribute) node);
		}
		if (childSymbol != null) {
			List<DocumentSymbol> children = parentSymbol.getChildren();
			if (children == null) {
				children = new ArrayList<>();
				parentSymbol.setChildren(children);
			}
			children.add(childSymbol);
		}
	}

	private DocumentSymbol createElementSymbol(TextDocument textDocument, RNCElement element) {
		String name = element.getName();
		if (name == null) {
			return null;
		}
		Range range = RNCPositionUtils.createRange(textDocument, element.getStart(), element.getEnd());
		Range selectionRange = RNCPositionUtils.createRange(textDocument, element.getNameStart(),
				element.getNameEnd());
		if (range == null || selectionRange == null) {
			return null;
		}
		DocumentSymbol symbol = new DocumentSymbol("element " + name, SymbolKind.Field, range, selectionRange);
		collectChildSymbols(textDocument, element, symbol);
		return symbol;
	}

	private DocumentSymbol createAttributeSymbol(TextDocument textDocument, RNCAttribute attribute) {
		String name = attribute.getName();
		if (name == null) {
			return null;
		}
		Range range = RNCPositionUtils.createRange(textDocument, attribute.getStart(), attribute.getEnd());
		Range selectionRange = RNCPositionUtils.createRange(textDocument, attribute.getNameStart(),
				attribute.getNameEnd());
		if (range == null || selectionRange == null) {
			return null;
		}
		return new DocumentSymbol("attribute " + name, SymbolKind.Property, range, selectionRange);
	}

	private void collectChildSymbols(TextDocument textDocument, RNCNode parent, DocumentSymbol parentSymbol) {
		List<DocumentSymbol> children = parentSymbol.getChildren();
		for (RNCNode child : parent.getChildren()) {
			DocumentSymbol childSymbol = null;
			if (child instanceof RNCElement) {
				childSymbol = createElementSymbol(textDocument, (RNCElement) child);
			} else if (child instanceof RNCAttribute) {
				childSymbol = createAttributeSymbol(textDocument, (RNCAttribute) child);
			}
			if (childSymbol != null) {
				if (children == null) {
					children = new ArrayList<>();
					parentSymbol.setChildren(children);
				}
				children.add(childSymbol);
			}
		}
	}

	/**
	 * Computes flat symbol information for workspace symbol search.
	 *
	 * <p>Use case: allows the user to search for RNC defines and declarations
	 * across the workspace using "Go to Symbol in Workspace" (Ctrl+T).</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort computation if the document changes.
	 * @return a flat list of {@link SymbolInformation} objects.
	 */
	public List<SymbolInformation> findSymbolInformations(RNCDocument rncDocument, CancelChecker cancelChecker) {
		if (rncDocument == null) {
			return Collections.emptyList();
		}

		String uri = rncDocument.getUri();
		List<SymbolInformation> symbols = new ArrayList<>();
		collectFlatSymbols(rncDocument, cancelChecker, (name, kind, range) -> {
			Location location = new Location(uri, range);
			symbols.add(new SymbolInformation(name, kind, location));
		});
		return symbols;
	}

	@FunctionalInterface
	private interface FlatSymbolCollector {
		void accept(String name, SymbolKind kind, Range range);
	}

	private void collectFlatSymbols(RNCDocument rncDocument, CancelChecker cancelChecker,
			FlatSymbolCollector collector) {
		TextDocument textDocument = rncDocument.getTextDocument();
		CharSequence text = textDocument.getTextSequence();

		List<RNCNode> declarations = collectDeclarations(rncDocument);

		for (int i = 0; i < declarations.size(); i++) {
			cancelChecker.checkCanceled();
			RNCNode decl = declarations.get(i);

			int consumed = 0;
			if (decl instanceof RNCDefine || decl instanceof RNCStart) {
				int j = i + 1;
				while (j < declarations.size()
						&& (declarations.get(j) instanceof RNCElement
								|| declarations.get(j) instanceof RNCAttribute)) {
					j++;
				}
				consumed = j - i - 1;
			}

			int rangeEnd;
			int afterIndex = i + 1 + consumed;
			if (afterIndex < declarations.size()) {
				rangeEnd = trimTrailingWhitespace(text, declarations.get(afterIndex).getStart());
			} else {
				rangeEnd = trimTrailingWhitespace(text, text.length());
			}

			Range range = RNCPositionUtils.createRange(textDocument, decl.getStart(), rangeEnd);
			if (range == null) {
				continue;
			}

			if (decl instanceof RNCNamespaceDecl) {
				RNCNamespaceDecl ns = (RNCNamespaceDecl) decl;
				String name = ns.isDefault() ? "default namespace" : "namespace " + ns.getPrefix();
				collector.accept(name, SymbolKind.Namespace, range);
			} else if (decl instanceof RNCDatatypesDecl) {
				RNCDatatypesDecl dt = (RNCDatatypesDecl) decl;
				String prefix = dt.getPrefix();
				String name = "datatypes " + (prefix != null ? prefix : "");
				collector.accept(name, SymbolKind.Namespace, range);
			} else if (decl instanceof RNCStart) {
				collector.accept(RNCKeywords.START, SymbolKind.Event, range);
			} else if (decl instanceof RNCDefine) {
				RNCDefine define = (RNCDefine) decl;
				String name = define.getName();
				if (name != null) {
					collector.accept(name, SymbolKind.Variable, range);
				}
			} else if (decl instanceof RNCInclude) {
				RNCInclude include = (RNCInclude) decl;
				String incUri = include.getUri();
				String name = incUri != null && !incUri.isEmpty() ? incUri : "include";
				collector.accept(name, SymbolKind.Module, range);
			} else if (decl instanceof RNCElement) {
				RNCElement element = (RNCElement) decl;
				String name = element.getName();
				if (name != null) {
					collector.accept("element " + name, SymbolKind.Field, range);
				}
			} else if (decl instanceof RNCAttribute) {
				RNCAttribute attribute = (RNCAttribute) decl;
				String name = attribute.getName();
				if (name != null) {
					collector.accept("attribute " + name, SymbolKind.Property, range);
				}
			}

			i += consumed;
		}
	}

	private static List<RNCNode> collectDeclarations(RNCDocument rncDocument) {
		List<RNCNode> declarations = new ArrayList<>();
		for (RNCNode child : rncDocument.getChildren()) {
			if (child instanceof RNCStart || child instanceof RNCDefine
					|| child instanceof RNCNamespaceDecl || child instanceof RNCDatatypesDecl
					|| child instanceof RNCInclude || child instanceof RNCElement
					|| child instanceof RNCAttribute) {
				declarations.add(child);
			}
		}
		return declarations;
	}

	private DocumentSymbol createNamespaceSymbol(TextDocument textDocument, RNCNamespaceDecl ns, Range range) {
		String name = ns.isDefault() ? "default namespace" : "namespace " + ns.getPrefix();
		Range selectionRange;
		if (ns.getPrefixStart() != -1) {
			selectionRange = RNCPositionUtils.createRange(textDocument, ns.getPrefixStart(), ns.getPrefixEnd());
		} else {
			// "default namespace" — use keyword span as selectionRange
			selectionRange = RNCPositionUtils.createRange(textDocument, ns.getStart(), ns.getStart() + 17);
		}
		if (selectionRange == null) {
			return null;
		}
		DocumentSymbol symbol = new DocumentSymbol(name, SymbolKind.Namespace, range, selectionRange);
		if (ns.getUri() != null) {
			symbol.setDetail(ns.getUri());
		}
		return symbol;
	}

	private DocumentSymbol createDatatypesSymbol(TextDocument textDocument, RNCDatatypesDecl dt, Range range) {
		String prefix = dt.getPrefix();
		String name = "datatypes " + (prefix != null ? prefix : "");
		Range selectionRange;
		if (dt.getPrefixStart() != -1) {
			selectionRange = RNCPositionUtils.createRange(textDocument, dt.getPrefixStart(), dt.getPrefixEnd());
		} else {
			selectionRange = range;
		}
		if (selectionRange == null) {
			return null;
		}
		DocumentSymbol symbol = new DocumentSymbol(name, SymbolKind.Namespace, range, selectionRange);
		if (dt.getUri() != null) {
			symbol.setDetail(dt.getUri());
		}
		return symbol;
	}

	private DocumentSymbol createIncludeSymbol(TextDocument textDocument, RNCInclude include, Range range) {
		String uri = include.getUri();
		String name = uri != null && !uri.isEmpty() ? uri : "include";
		Range selectionRange;
		if (include.getUriStart() != -1) {
			selectionRange = RNCPositionUtils.createRange(textDocument, include.getUriStart(),
					include.getUriEnd());
		} else {
			selectionRange = range;
		}
		if (selectionRange == null) {
			return null;
		}
		return new DocumentSymbol(name, SymbolKind.Module, range, selectionRange);
	}

	private static int trimTrailingWhitespace(CharSequence text, int offset) {
		while (offset > 0) {
			char ch = text.charAt(offset - 1);
			if (ch != '\n' && ch != '\r' && ch != ' ' && ch != '\t') {
				break;
			}
			offset--;
		}
		return offset;
	}
}
