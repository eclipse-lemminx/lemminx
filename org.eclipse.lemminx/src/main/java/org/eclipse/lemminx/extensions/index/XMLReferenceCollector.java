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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.dom.parser.Scanner;
import org.eclipse.lemminx.dom.parser.TokenType;
import org.eclipse.lemminx.dom.parser.XMLScanner;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;

/**
 * Collects grammar references and symbol names from XML content using
 * LemMinX's own {@link XMLScanner}. No DOM is built — only the relevant
 * token values are extracted via a lightweight token scan.
 *
 * <p>Uses the XMLScanner which is fault-tolerant (handles malformed XML)
 * and lighter than SAX (no factory, no JDK XML parser stack).
 *
 * <p>All comparisons on token text are done char-by-char on the source
 * {@link CharSequence} — no intermediate String allocations for checks.
 * Strings are only created for values that are actually collected into
 * the result set.
 *
 * <p>Collects:
 * <ul>
 *   <li>{@code <?xml-model href="..."?>} — grammar file path</li>
 *   <li>{@code xsi:schemaLocation} — grammar file paths (namespace-URI pairs)</li>
 *   <li>{@code xsi:noNamespaceSchemaLocation} — grammar file path</li>
 *   <li>{@code xs:include/xs:import schemaLocation} — grammar file path</li>
 *   <li>{@code <!DOCTYPE ... SYSTEM "...">} — DTD file path</li>
 *   <li>{@code href} and {@code location} attributes (XInclude, XSLT import/include, XLink, WSDL import)</li>
 *   <li>Element names (for symbol-level cross-file references)</li>
 *   <li>{@code type}, {@code ref}, {@code base}, {@code name}, {@code substitutionGroup}
 *       attribute values (for XSD cross-file)</li>
 * </ul>
 */
public class XMLReferenceCollector {

	private static final int ATTR_NONE = 0;
	private static final int ATTR_SCHEMA_LOCATION_XSI = 1;
	private static final int ATTR_NO_NS_SCHEMA_LOCATION = 2;
	private static final int ATTR_SCHEMA_LOCATION_PLAIN = 3;
	private static final int ATTR_HREF = 4;
	private static final int ATTR_TYPE = 5;
	private static final int ATTR_REF = 6;
	private static final int ATTR_BASE = 7;
	private static final int ATTR_NAME = 8;
	private static final int ATTR_SUBSTITUTION_GROUP = 9;
	private static final int ATTR_CONTRIBUTOR_PATH = 10;
	private static final int ATTR_CONTRIBUTOR_PAIRS = 11;
	private static final int ATTR_XMLNS = 12;

	/**
	 * Extracts all reference tokens from XML content and returns them as a name set.
	 *
	 * <p><b>Use case:</b> When indexing {@code order.xml} whose content is:
	 * <pre>
	 *   {@literal <?xml-model href="schemas/order.xsd"?>}
	 *   {@literal <root xsi:noNamespaceSchemaLocation="common.xsd"/>}
	 * </pre>
	 * returns {@code {"schemas/order.xsd", "order.xsd", "common.xsd", "root", …}}.
	 * This set is fed to {@link BloomFilter#create} so that later queries like
	 * {@code mightContain("order.xsd")} return {@code true} in O(1) time.
	 *
	 * @param text raw XML content, possibly truncated to the first 64 KB for large files.
	 * @return the set of collected names (file paths, element names, attribute values).
	 */
	public static Set<String> collect(CharSequence text) {
		return collect(text, Collections.emptyList());
	}

	public static Set<String> collect(CharSequence text, List<IGrammarReferenceContributor> contributors) {
		Set<String> names = new LinkedHashSet<>();
		Scanner scanner = XMLScanner.createScanner(text, 0);

		int currentAttrKind = ATTR_NONE;
		boolean inXmlModelPI = false;
		String currentElement = null;

		TokenType token = scanner.scan();
		while (token != TokenType.EOS) {
			switch (token) {
				case StartTag:
					currentElement = extractLocalName(text, scanner.getTokenOffset(), scanner.getTokenEnd());
					addElementName(names, text, scanner.getTokenOffset(), scanner.getTokenEnd());
					break;

				case EndTag:
					addElementName(names, text, scanner.getTokenOffset(), scanner.getTokenEnd());
					break;

				case PIName:
					inXmlModelPI = regionEquals(text, scanner.getTokenOffset(), scanner.getTokenEnd(), "xml-model");
					break;

				case PIContent:
					if (inXmlModelPI) {
						extractAndAddHref(names, text, scanner.getTokenOffset(), scanner.getTokenEnd());
						inXmlModelPI = false;
					}
					break;

				case AttributeName:
					currentAttrKind = classifyAttribute(text, scanner.getTokenOffset(), scanner.getTokenEnd(),
							currentElement, contributors);
					break;

				case AttributeValue:
					if (currentAttrKind != ATTR_NONE) {
						int start = scanner.getTokenOffset();
						int end = scanner.getTokenEnd();
						// Skip surrounding quotes
						if (end - start >= 2) {
							char first = text.charAt(start);
							if (first == '"' || first == '\'') {
								start++;
								end--;
							}
						}
						processAttributeValue(names, text, start, end, currentAttrKind);
						currentAttrKind = ATTR_NONE;
					}
					break;

				case DTDDoctypeSystemId:
					int sysStart = scanner.getTokenOffset();
					int sysEnd = scanner.getTokenEnd();
					if (sysEnd - sysStart >= 2) {
						char first = text.charAt(sysStart);
						if (first == '"' || first == '\'') {
							sysStart++;
							sysEnd--;
						}
					}
					addFilePath(names, text, sysStart, sysEnd);
					break;

				case DTDDoctypePublicId:
					int pubStart = scanner.getTokenOffset();
					int pubEnd = scanner.getTokenEnd();
					if (pubEnd - pubStart >= 2) {
						char first = text.charAt(pubStart);
						if (first == '"' || first == '\'') {
							pubStart++;
							pubEnd--;
						}
					}
					if (pubStart < pubEnd) {
						names.add(decodeXmlEntities(text, pubStart, pubEnd));
					}
					break;

				default:
					break;
			}
			token = scanner.scan();
		}
		return names;
	}

	private static int classifyAttribute(CharSequence text, int start, int end, String elementName,
			List<IGrammarReferenceContributor> contributors) {
		// Find the local name (after the last ':')
		int colonPos = -1;
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == ':') {
				colonPos = i;
			}
		}
		int localStart = colonPos >= 0 ? colonPos + 1 : start;
		boolean hasPrefix = colonPos >= 0;

		// xmlns / xmlns:prefix — namespace declaration used for catalog URI resolution
		if (!hasPrefix && regionEquals(text, start, end, "xmlns")) {
			return ATTR_XMLNS;
		}
		if (hasPrefix && regionEquals(text, start, colonPos, "xmlns")) {
			return ATTR_XMLNS;
		}

		if (regionEquals(text, localStart, end, "schemaLocation")) {
			if (hasPrefix) {
				return ATTR_SCHEMA_LOCATION_XSI;
			}
			return isXsdIncludeOrImport(elementName) ? ATTR_SCHEMA_LOCATION_PLAIN : ATTR_NONE;
		}
		if (regionEquals(text, localStart, end, "noNamespaceSchemaLocation")) {
			return ATTR_NO_NS_SCHEMA_LOCATION;
		}
		if (regionEquals(text, localStart, end, "href")) {
			return ATTR_HREF;
		}
		if (regionEquals(text, localStart, end, "location")) {
			// wsdl:import location="types.wsdl" — WSDL-namespace import file reference
			return "import".equals(elementName) ? ATTR_HREF : ATTR_NONE;
		}
		if (regionEquals(text, localStart, end, "type")) {
			return ATTR_TYPE;
		}
		if (regionEquals(text, localStart, end, "ref")) {
			return ATTR_REF;
		}
		if (regionEquals(text, localStart, end, "base")) {
			return ATTR_BASE;
		}
		if (regionEquals(text, localStart, end, "name")) {
			return ATTR_NAME;
		}
		if (regionEquals(text, localStart, end, "substitutionGroup")) {
			return ATTR_SUBSTITUTION_GROUP;
		}
		// Try registered contributors (String allocated only on built-in miss)
		if (!contributors.isEmpty()) {
			String attrLocal = text.subSequence(localStart, end).toString();
			for (IGrammarReferenceContributor c : contributors) {
				IGrammarReferenceContributor.ExtractionKind kind = c.classify(elementName, attrLocal, hasPrefix);
				if (kind != null) {
					return kind == IGrammarReferenceContributor.ExtractionKind.PATH
							? ATTR_CONTRIBUTOR_PATH : ATTR_CONTRIBUTOR_PAIRS;
				}
			}
		}
		return ATTR_NONE;
	}

	private static void processAttributeValue(Set<String> names, CharSequence text,
			int start, int end, int attrKind) {
		if (start >= end) {
			return;
		}
		switch (attrKind) {
			case ATTR_SCHEMA_LOCATION_XSI:
			case ATTR_CONTRIBUTOR_PAIRS:
				collectSchemaLocationPaths(names, text, start, end);
				break;
			case ATTR_NO_NS_SCHEMA_LOCATION:
			case ATTR_SCHEMA_LOCATION_PLAIN:
			case ATTR_HREF:
			case ATTR_CONTRIBUTOR_PATH:
				addFilePath(names, text, start, end);
				break;
			case ATTR_XMLNS:
				// Namespace URI — added as-is (not split on '/') for catalog identifier lookup.
				if (start < end) {
					names.add(text.subSequence(start, end).toString());
				}
				break;
			case ATTR_TYPE:
			case ATTR_REF:
			case ATTR_BASE:
			case ATTR_NAME:
			case ATTR_SUBSTITUTION_GROUP:
				addSymbolName(names, text, start, end);
				break;
			default:
				break;
		}
	}

	private static void collectSchemaLocationPaths(Set<String> names, CharSequence text,
			int start, int end) {
		int pairIndex = 0;
		int tokenStart = -1;
		for (int i = start; i <= end; i++) {
			boolean isWs = i == end || Character.isWhitespace(text.charAt(i));
			if (!isWs && tokenStart < 0) {
				tokenStart = i;
			} else if (isWs && tokenStart >= 0) {
				if (pairIndex % 2 == 1) {
					// odd token: schema location path (e.g. "schema.xsd")
					addFilePath(names, text, tokenStart, i);
				} else {
					// even token: namespace URI (e.g. "http://foo") — used by catalog
					// URI resolution (LSPXMLCatalogResolver.resolveURI(namespaceURI))
					names.add(text.subSequence(tokenStart, i).toString());
				}
				pairIndex++;
				tokenStart = -1;
			}
		}
	}

	private static void addFilePath(Set<String> names, CharSequence text, int start, int end) {
		if (start >= end) {
			return;
		}
		String path = decodeXmlEntities(text, start, end);
		names.add(path);
		int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < path.length() - 1) {
			names.add(path.substring(lastSlash + 1));
		}
	}

	/**
	 * Decodes XML character references in {@code text[start, end)} and returns
	 * the decoded string.
	 *
	 * <p>Handles the five predefined named entities ({@code &amp;}, {@code &lt;},
	 * {@code &gt;}, {@code &quot;}, {@code &apos;}) and numeric decimal/hex
	 * references ({@code &#65;}, {@code &#x41;}). Unrecognized references are
	 * kept verbatim.
	 *
	 * <p>Fast path: if no {@code &} is present the substring is returned directly
	 * with no extra allocation.
	 *
	 * <p>Source offsets and lengths are <em>not</em> affected — callers that need
	 * to build text edits must continue to use the raw source span.
	 */
	static String decodeXmlEntities(CharSequence text, int start, int end) {
		// Fast path: look for '&'; most paths have none
		int ampPos = -1;
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == '&') {
				ampPos = i;
				break;
			}
		}
		if (ampPos < 0) {
			return text.subSequence(start, end).toString();
		}
		StringBuilder sb = new StringBuilder(end - start);
		int i = start;
		while (i < end) {
			char c = text.charAt(i);
			if (c == '&') {
				// Search for ';' within a reasonable window (longest std ref is ~10 chars)
				int semi = -1;
				for (int j = i + 1; j < end && j < i + 20; j++) {
					if (text.charAt(j) == ';') {
						semi = j;
						break;
					}
				}
				if (semi > i + 1) {
					String decoded = decodeEntityRef(text, i + 1, semi);
					if (decoded != null) {
						sb.append(decoded);
						i = semi + 1;
						continue;
					}
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}

	private static String decodeEntityRef(CharSequence text, int start, int end) {
		int len = end - start;
		if (len == 0) {
			return null;
		}
		if (text.charAt(start) == '#') {
			// Numeric character reference: &#65; or &#x41;
			if (len < 2) {
				return null;
			}
			try {
				int codePoint;
				char second = text.charAt(start + 1);
				if (second == 'x' || second == 'X') {
					codePoint = Integer.parseInt(text.subSequence(start + 2, end).toString(), 16);
				} else {
					codePoint = Integer.parseInt(text.subSequence(start + 1, end).toString(), 10);
				}
				return String.valueOf((char) codePoint);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		// Named entities
		switch (text.subSequence(start, end).toString()) {
			case "amp":  return "&";
			case "lt":   return "<";
			case "gt":   return ">";
			case "quot": return "\"";
			case "apos": return "'";
			default:     return null;
		}
	}

	private static void addSymbolName(Set<String> names, CharSequence text, int start, int end) {
		if (start >= end) {
			return;
		}
		String value = text.subSequence(start, end).toString();
		int colon = value.indexOf(':');
		if (colon >= 0 && colon < value.length() - 1) {
			names.add(value.substring(colon + 1));
		}
		names.add(value);
	}

	private static void addElementName(Set<String> names, CharSequence text, int start, int end) {
		if (start >= end) {
			return;
		}
		String name = text.subSequence(start, end).toString();
		int colon = name.indexOf(':');
		if (colon >= 0 && colon < name.length() - 1) {
			names.add(name.substring(colon + 1));
		}
		names.add(name);
	}

	private static boolean isXsdIncludeOrImport(String localName) {
		return "include".equals(localName) || "import".equals(localName);
	}

	private static String extractLocalName(CharSequence text, int start, int end) {
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == ':') {
				return text.subSequence(i + 1, end).toString();
			}
		}
		return text.subSequence(start, end).toString();
	}

	private static void extractAndAddHref(Set<String> names, CharSequence text, int start, int end) {
		int idx = indexOf(text, start, end, "href");
		if (idx < 0) {
			return;
		}
		int pos = idx + 4; // past "href"
		while (pos < end && Character.isWhitespace(text.charAt(pos))) {
			pos++;
		}
		if (pos >= end || text.charAt(pos) != '=') {
			return;
		}
		pos++; // past '='
		while (pos < end && Character.isWhitespace(text.charAt(pos))) {
			pos++;
		}
		if (pos >= end) {
			return;
		}
		char quote = text.charAt(pos);
		if (quote != '"' && quote != '\'') {
			return;
		}
		int valStart = pos + 1;
		int valEnd = indexOf(text, valStart, end, quote);
		if (valEnd < 0) {
			return;
		}
		addFilePath(names, text, valStart, valEnd);
	}

	private static boolean regionEquals(CharSequence text, int start, int end, String target) {
		int len = end - start;
		if (len != target.length()) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (text.charAt(start + i) != target.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	private static int indexOf(CharSequence text, int start, int end, String target) {
		int targetLen = target.length();
		int limit = end - targetLen;
		for (int i = start; i <= limit; i++) {
			boolean match = true;
			for (int j = 0; j < targetLen; j++) {
				if (text.charAt(i + j) != target.charAt(j)) {
					match = false;
					break;
				}
			}
			if (match) {
				return i;
			}
		}
		return -1;
	}

	private static int indexOf(CharSequence text, int start, int end, char ch) {
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == ch) {
				return i;
			}
		}
		return -1;
	}
}
