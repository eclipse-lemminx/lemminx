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
package org.eclipse.lemminx.extensions.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.extensions.index.scanner.IndexScanner;
import org.eclipse.lemminx.extensions.index.scanner.IndexTokenType;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;
import org.eclipse.lemminx.services.extensions.IIndexFileScannerContributor;

/**
 * Single-pass streaming scanner that populates both a {@link GrammarReference}
 * list and a bloom-filter name set from an XML file in one forward read.
 *
 * <p>Replaces the previous two-scan approach used by {@link WorkspaceIndex}
 * ({@code GrammarReferenceLocator} then {@code XMLReferenceCollector}), combining
 * both into one {@link IndexScanner} pass. Peak heap per file = the reader's
 * internal buffer (~8 KB) plus the small strings captured per token; the whole
 * file is never loaded into memory.
 *
 * <p>An optional {@code maxChars} limit stops scanning early for performance
 * (not memory). Files that hit the limit are marked partial and always returned
 * as candidates by {@link WorkspaceIndex#findReferencingFiles}, avoiding false
 * negatives.
 */
final class IndexFileScanner {

	// --- Attribute kind constants (mirror XMLReferenceCollector) ---
	private static final int ATTR_NONE                  = 0;
	private static final int ATTR_SCHEMA_LOCATION_XSI   = 1;
	private static final int ATTR_NO_NS_SCHEMA_LOCATION = 2;
	private static final int ATTR_SCHEMA_LOCATION_PLAIN = 3;
	private static final int ATTR_HREF                  = 4;
	private static final int ATTR_TYPE                  = 5;
	private static final int ATTR_REF                   = 6;
	private static final int ATTR_BASE                  = 7;
	private static final int ATTR_NAME                  = 8;
	private static final int ATTR_SUBSTITUTION_GROUP    = 9;
	private static final int ATTR_CONTRIBUTOR_PATH      = 10;
	private static final int ATTR_CONTRIBUTOR_PAIRS     = 11;
	private static final int ATTR_XMLNS                 = 12;

	private IndexFileScanner() { }

	/**
	 * Scans {@code path} with an optional character limit and populates
	 * {@code refs} and {@code names} in a single streaming pass.
	 *
	 * @param path         file to scan.
	 * @param maxChars     maximum characters to consume; {@link Integer#MAX_VALUE} = no limit.
	 * @param contributors extension-provided grammar-reference attribute contributors.
	 * @param refs         output list — located grammar references with line/col positions.
	 * @param names        output set — token names for the bloom filter.
	 * @return {@code true} if scanning stopped early due to the {@code maxChars} limit.
	 * @throws IOException if the file cannot be opened or read.
	 */
	static boolean scan(Path path, int maxChars,
			List<IGrammarReferenceContributor> contributors,
			List<IIndexFileScannerContributor> fileScannerContributors,
			List<GrammarReference> refs, Set<String> names) throws IOException {
		for (IIndexFileScannerContributor sc : fileScannerContributors) {
			if (sc.canScan(path)) {
				return sc.scan(path, maxChars, refs, names);
			}
		}
		try (IndexScanner scanner = new IndexScanner(Files.newBufferedReader(path), maxChars)) {
			scanInternal(scanner, contributors, refs, names);
			return scanner.isPartial();
		}
	}

	private static void scanInternal(IndexScanner scanner,
			List<IGrammarReferenceContributor> contributors,
			List<GrammarReference> refs, Set<String> names) throws IOException {
		boolean isXmlModel = false;
		String currentElement = null;
		int attrKind = ATTR_NONE;

		IndexTokenType token;
		while ((token = scanner.next()) != IndexTokenType.EOS) {
			switch (token) {

				case PI_NAME:
					isXmlModel = "xml-model".equals(scanner.getValue());
					break;

				case PI_CONTENT:
					if (isXmlModel) {
						extractXmlModelHref(scanner, scanner.getValue(), refs, names);
					}
					isXmlModel = false;
					break;

				case START_TAG:
					// IndexScanner already strips the namespace prefix from tag names.
					currentElement = scanner.getValue();
					names.add(currentElement);
					break;

				case ATTRIBUTE_NAME:
					attrKind = classifyAttribute(scanner.getValue(), currentElement, contributors);
					break;

				case ATTRIBUTE_VALUE:
					if (attrKind != ATTR_NONE) {
						processAttributeValue(scanner, scanner.getValue(), attrKind, refs, names);
					}
					attrKind = ATTR_NONE;
					break;

				case DOCTYPE_SYSTEM_ID: {
					String raw = scanner.getValue();
					String val = decode(raw);
					addFilePath(names, val);
					refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
							scanner.getStartChar(), scanner.getEndChar(),
							val, GrammarReference.Kind.DOCTYPE_SYSTEM));
					break;
				}

				case DOCTYPE_PUBLIC_ID: {
					String raw = scanner.getValue();
					String val = decode(raw);
					if (!val.isEmpty()) {
						names.add(val);
						refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
								scanner.getStartChar(), scanner.getEndChar(),
								val, GrammarReference.Kind.DOCTYPE_PUBLIC));
					}
					break;
				}

				default:
					break;
			}
		}
	}

	// ---- Attribute classification ----

	private static int classifyAttribute(String fullName, String elementName,
			List<IGrammarReferenceContributor> contributors) {
		int colon = fullName.lastIndexOf(':');
		String local;
		String prefix;
		boolean hasPrefix;
		if (colon >= 0) {
			prefix = fullName.substring(0, colon);
			local  = fullName.substring(colon + 1);
			hasPrefix = true;
		} else {
			prefix = null;
			local  = fullName;
			hasPrefix = false;
		}

		// xmlns / xmlns:prefix
		if (!hasPrefix && "xmlns".equals(local))  return ATTR_XMLNS;
		if (hasPrefix  && "xmlns".equals(prefix)) return ATTR_XMLNS;

		if ("schemaLocation".equals(local)) {
			if (hasPrefix) return ATTR_SCHEMA_LOCATION_XSI;
			return isXsdIncludeOrImport(elementName) ? ATTR_SCHEMA_LOCATION_PLAIN : ATTR_NONE;
		}
		if ("noNamespaceSchemaLocation".equals(local)) return ATTR_NO_NS_SCHEMA_LOCATION;
		if ("href".equals(local))               return ATTR_HREF;
		if ("location".equals(local))           return "import".equals(elementName) ? ATTR_HREF : ATTR_NONE;
		if ("type".equals(local))               return ATTR_TYPE;
		if ("ref".equals(local))                return ATTR_REF;
		if ("base".equals(local))               return ATTR_BASE;
		if ("name".equals(local))               return ATTR_NAME;
		if ("substitutionGroup".equals(local))  return ATTR_SUBSTITUTION_GROUP;

		if (!contributors.isEmpty()) {
			for (IGrammarReferenceContributor c : contributors) {
				IGrammarReferenceContributor.ExtractionKind kind = c.classify(elementName, local, hasPrefix);
				if (kind != null) {
					return kind == IGrammarReferenceContributor.ExtractionKind.PATH
							? ATTR_CONTRIBUTOR_PATH : ATTR_CONTRIBUTOR_PAIRS;
				}
			}
		}
		return ATTR_NONE;
	}

	private static boolean isXsdIncludeOrImport(String localName) {
		return "include".equals(localName) || "import".equals(localName);
	}

	// ---- Attribute value processing ----

	private static void processAttributeValue(IndexScanner scanner, String rawValue,
			int attrKind, List<GrammarReference> refs, Set<String> names) {
		if (rawValue.isEmpty()) return;
		switch (attrKind) {
			case ATTR_SCHEMA_LOCATION_XSI:
			case ATTR_CONTRIBUTOR_PAIRS:
				processSchemaLocationPairs(rawValue, scanner.getOffset(), scanner.getLine(),
						scanner.getStartChar(), refs, names);
				break;
			case ATTR_NO_NS_SCHEMA_LOCATION: {
				String val = decode(rawValue);
				addFilePath(names, val);
				refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
						scanner.getStartChar(), scanner.getEndChar(),
						val, GrammarReference.Kind.NO_NS_SCHEMA_LOCATION));
				break;
			}
			case ATTR_SCHEMA_LOCATION_PLAIN: {
				String val = decode(rawValue);
				addFilePath(names, val);
				refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
						scanner.getStartChar(), scanner.getEndChar(),
						val, GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION));
				break;
			}
			case ATTR_HREF:
			case ATTR_CONTRIBUTOR_PATH: {
				String val = decode(rawValue);
				addFilePath(names, val);
				refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
						scanner.getStartChar(), scanner.getEndChar(),
						val, GrammarReference.Kind.HREF));
				break;
			}
			case ATTR_XMLNS: {
				String val = decode(rawValue);
				if (!val.isEmpty()) {
					names.add(val);
					refs.add(new GrammarReference(scanner.getOffset(), scanner.getLine(),
							scanner.getStartChar(), scanner.getEndChar(),
							val, GrammarReference.Kind.NAMESPACE_DECL));
				}
				break;
			}
			case ATTR_TYPE:
			case ATTR_REF:
			case ATTR_BASE:
			case ATTR_NAME:
			case ATTR_SUBSTITUTION_GROUP:
				addSymbolName(names, decode(rawValue));
				break;
			default:
				break;
		}
	}

	/**
	 * Splits a whitespace-separated namespace/path pair string (as found in
	 * {@code xsi:schemaLocation="ns1 path1 ns2 path2"}) into individual tokens,
	 * computes their line/column positions relative to {@code baseStartChar}, and
	 * adds each as a {@link GrammarReference} and a bloom-filter name.
	 *
	 * <p>Assumes the value is on a single line (true in practice for schema location
	 * attributes). Multi-line values would produce slightly inaccurate column positions
	 * beyond the first newline, which is acceptable for head-limited partial scanning.
	 */
	private static void processSchemaLocationPairs(String rawValue, int baseOffset,
			int baseLine, int baseStartChar, List<GrammarReference> refs, Set<String> names) {
		int pairIndex = 0;
		int tokenStart = -1;
		for (int i = 0; i <= rawValue.length(); i++) {
			boolean isWs = i == rawValue.length() || Character.isWhitespace(rawValue.charAt(i));
			if (!isWs && tokenStart < 0) {
				tokenStart = i;
			} else if (isWs && tokenStart >= 0) {
				String tok = decode(rawValue.substring(tokenStart, i));
				GrammarReference.Kind kind = pairIndex % 2 == 1
						? GrammarReference.Kind.SCHEMA_LOCATION
						: GrammarReference.Kind.NAMESPACE_DECL;
				refs.add(new GrammarReference(
						baseOffset + tokenStart, baseLine,
						baseStartChar + tokenStart, baseStartChar + i,
						tok, kind));
				if (pairIndex % 2 == 1) {
					addFilePath(names, tok);
				} else {
					names.add(tok); // namespace URI for catalog lookup
				}
				pairIndex++;
				tokenStart = -1;
			}
		}
	}

	/**
	 * Finds the {@code href} attribute value within {@code <?xml-model ...?>} PI content
	 * and adds it as a {@link GrammarReference.Kind#XML_MODEL_HREF} reference.
	 *
	 * <p>The PI content is captured by {@link IndexScanner} as a plain string starting
	 * right after the PI name whitespace (e.g. {@code href="schema.xsd" type="..."}).
	 * The position offsets in {@code scanner} are those of the first character of this
	 * content. The href value's position is computed by adding the intra-string offset.
	 */
	private static void extractXmlModelHref(IndexScanner scanner, String content,
			List<GrammarReference> refs, Set<String> names) {
		int piOffset    = scanner.getOffset();
		int piLine      = scanner.getLine();
		int piStartChar = scanner.getStartChar();

		int idx = content.indexOf("href");
		if (idx < 0) return;
		int pos = idx + 4; // past "href"
		while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) pos++;
		if (pos >= content.length() || content.charAt(pos) != '=') return;
		pos++; // past '='
		while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) pos++;
		if (pos >= content.length()) return;
		char q = content.charAt(pos);
		if (q != '"' && q != '\'') return;
		int valStart = pos + 1;
		int valEnd = content.indexOf(q, valStart);
		if (valEnd < 0) return;

		String val = decode(content.substring(valStart, valEnd));
		addFilePath(names, val);
		refs.add(new GrammarReference(
				piOffset + valStart, piLine,
				piStartChar + valStart, piStartChar + valEnd,
				val, GrammarReference.Kind.XML_MODEL_HREF));
	}

	// ---- Name helpers ----

	private static void addFilePath(Set<String> names, String path) {
		if (path == null || path.isEmpty()) return;
		names.add(path);
		int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < path.length() - 1) {
			names.add(path.substring(lastSlash + 1));
		}
	}

	private static void addSymbolName(Set<String> names, String val) {
		if (val.isEmpty()) return;
		int colon = val.indexOf(':');
		if (colon >= 0 && colon < val.length() - 1) {
			names.add(val.substring(colon + 1));
		}
		names.add(val);
	}

	private static String decode(String raw) {
		return XMLReferenceCollector.decodeXmlEntities(raw, 0, raw.length());
	}
}
