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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.parser.Scanner;
import org.eclipse.lemminx.dom.parser.TokenType;
import org.eclipse.lemminx.dom.parser.XMLScanner;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;

/**
 * Locates grammar file references in XML content and returns them as
 * {@link GrammarReference} objects with pre-computed line/column positions.
 *
 * <p>Positions are computed once during {@link #locate(TextDocument)} from the
 * {@link TextDocument} passed in. After that, callers build LSP
 * {@link org.eclipse.lsp4j.Location}s and {@link org.eclipse.lsp4j.TextEdit}s
 * directly from the cached {@link GrammarReference#line()},
 * {@link GrammarReference#startChar()}, and {@link GrammarReference#endChar()}
 * — no file re-read required.
 *
 * <p>This is step 2 in the rename flow (after Bloom filter pre-filtering):
 * <ol>
 *   <li>{@link WorkspaceIndex#findReferencingFiles} — Bloom filter finds candidates (instant)</li>
 *   <li><b>GrammarReferenceLocator</b> — scans each candidate for exact positions</li>
 *   <li>{@link GrammarFileRenameSupport} — builds the WorkspaceEdit from the located refs</li>
 * </ol>
 *
 * <p>Unlike {@link XMLReferenceCollector} which collects names into a Set
 * for Bloom filter indexing, this class returns {@link GrammarReference}
 * records with line/column positions for building {@code TextEdit}s.
 *
 * <p><b>Use cases covered (Phase 1 — file-level references):</b>
 * <ul>
 *   <li>UC14: {@code <?xml-model href="..."?>} — rename grammar → update href</li>
 *   <li>UC15: {@code xsi:noNamespaceSchemaLocation="..."} — idem</li>
 *   <li>UC16: {@code xsi:schemaLocation="ns path"} — update the file path part</li>
 *   <li>UC17/18: {@code <!DOCTYPE ... SYSTEM "...">} — update system ID</li>
 *   <li>UC21: {@code href="..."} and {@code location="..."} on XInclude, XSLT import/include,
 *       WSDL import</li>
 *   <li>xs:include/xs:import {@code schemaLocation="..."}</li>
 *   <li>OASIS XML Catalog: {@code uri="..."} on {@code uri}, {@code public},
 *       {@code system}, {@code systemSuffix}, {@code uriSuffix} entries</li>
 * </ul>
 *
 * <p>Uses LemMinX's {@link XMLScanner} (fault-tolerant, no DOM).
 * All comparisons are char-by-char — Strings are only created for
 * values that are actually returned as {@link GrammarReference}s.
 *
 * @see GrammarReference
 * @see GrammarFileRenameSupport
 * @see WorkspaceIndex
 */
public class GrammarReferenceLocator {

	/**
	 * Scans XML content and returns every grammar reference with its exact
	 * line/column position.
	 *
	 * <p><b>Use case (indexing):</b> {@link WorkspaceIndex#indexFile} reads a
	 * bounded head of {@code order.xml} into a {@link TextDocument} and passes it
	 * here. The returned references — e.g.
	 * {@code [XML_MODEL_HREF line=0,col=17-27 "schema.xsd"]} — are cached in the
	 * {@link FileBloomEntry} and used for all subsequent code-lens counts, Find
	 * References, and rename edits without re-reading the file.
	 *
	 * @param doc the XML document to scan (may be a head-limited fragment).
	 * @return all located grammar references; empty if none found.
	 */
	public static List<GrammarReference> locate(TextDocument doc) {
		return locate(doc, Collections.emptyList());
	}

	/**
	 * Scans XML content and returns every grammar reference, including those
	 * discovered by extension-provided {@link IGrammarReferenceContributor}s.
	 *
	 * @param doc          the XML document to scan.
	 * @param contributors extension-provided contributors for non-standard grammar
	 *                     reference attributes.
	 * @return all located grammar references; empty if none found.
	 */
	public static List<GrammarReference> locate(TextDocument doc,
			List<IGrammarReferenceContributor> contributors) {
		CharSequence text = doc.getTextSequence();
		List<GrammarReference> refs = new ArrayList<>();
		Scanner scanner = XMLScanner.createScanner(text, 0);

		int currentAttrKind = ATTR_NONE;
		IGrammarReferenceContributor currentContributor = null;
		boolean inXmlModelPI = false;
		String currentElement = null;

		TokenType token = scanner.scan();
		while (token != TokenType.EOS) {
			switch (token) {
				case PIName:
					inXmlModelPI = regionEquals(text, scanner.getTokenOffset(), scanner.getTokenEnd(), "xml-model");
					break;

				case PIContent:
					if (inXmlModelPI) {
						locateHrefInPI(refs, doc, text, scanner.getTokenOffset(), scanner.getTokenEnd());
						inXmlModelPI = false;
					}
					break;

				case StartTag:
					currentElement = extractLocalName(text, scanner.getTokenOffset(), scanner.getTokenEnd());
					break;

				case AttributeName: {
					currentContributor = null;
					int attrStart = scanner.getTokenOffset();
					int attrEnd = scanner.getTokenEnd();
					currentAttrKind = classifyAttribute(text, attrStart, attrEnd, currentElement);
					if (currentAttrKind == ATTR_NONE && !contributors.isEmpty()) {
						int colonPos = -1;
						for (int i = attrStart; i < attrEnd; i++) {
							if (text.charAt(i) == ':') {
								colonPos = i;
							}
						}
						int localStart = colonPos >= 0 ? colonPos + 1 : attrStart;
						boolean hasPrefix = colonPos >= 0;
						String attrLocal = text.subSequence(localStart, attrEnd).toString();
						for (IGrammarReferenceContributor c : contributors) {
							IGrammarReferenceContributor.ExtractionKind kind =
									c.classify(currentElement, attrLocal, hasPrefix);
							if (kind != null) {
								currentContributor = c;
								currentAttrKind = kind == IGrammarReferenceContributor.ExtractionKind.PATH
										? ATTR_CONTRIBUTOR_PATH : ATTR_CONTRIBUTOR_PAIRS;
								break;
							}
						}
					}
					break;
				}

				case AttributeValue:
					if (currentAttrKind != ATTR_NONE) {
						int start = scanner.getTokenOffset();
						int end = scanner.getTokenEnd();
						if (end - start >= 2) {
							char first = text.charAt(start);
							if (first == '"' || first == '\'') {
								start++;
								end--;
							}
						}
						locateAttributeValue(refs, doc, text, start, end, currentAttrKind, currentContributor);
						currentAttrKind = ATTR_NONE;
						currentContributor = null;
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
					if (sysStart < sysEnd) {
						GrammarReference sysRef = makeRef(doc, text, sysStart, sysEnd,
								GrammarReference.Kind.DOCTYPE_SYSTEM);
						if (sysRef != null) {
							refs.add(sysRef);
						}
					}
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
						GrammarReference pubRef = makeRef(doc, text, pubStart, pubEnd,
								GrammarReference.Kind.DOCTYPE_PUBLIC);
						if (pubRef != null) {
							refs.add(pubRef);
						}
					}
					break;

				default:
					break;
			}
			token = scanner.scan();
		}
		return refs;
	}

	/**
	 * Convenience wrapper: wraps {@code text} in a {@link TextDocument} and
	 * delegates to {@link #locate(TextDocument)}.
	 *
	 * @param text raw XML content.
	 * @return all located grammar references; empty if none found.
	 */
	public static List<GrammarReference> locate(CharSequence text) {
		return locate(text, Collections.emptyList());
	}

	/**
	 * Convenience wrapper: wraps {@code text} in a {@link TextDocument} and
	 * delegates to {@link #locate(TextDocument, List)}.
	 *
	 * @param text         raw XML content.
	 * @param contributors extension-provided contributors.
	 * @return all located grammar references; empty if none found.
	 */
	public static List<GrammarReference> locate(CharSequence text,
			List<IGrammarReferenceContributor> contributors) {
		return locate(new TextDocument(text instanceof String ? (String) text : text.toString(), ""),
				contributors);
	}

	/**
	 * Finds grammar references in {@code text} whose filename portion equals {@code fileName}.
	 *
	 * <p><b>Use case:</b> Bloom-filter pre-filtering for rename: quickly find candidate
	 * references in {@code order.xml} when renaming {@code schema.xsd}. A reference
	 * {@code href="../schemas/schema.xsd"} matches because its last path segment is
	 * {@code "schema.xsd"}. Callers that need path accuracy should use
	 * {@link #locateReferencesTo(CharSequence, Path, Path)} instead.
	 *
	 * @param text     XML content to scan.
	 * @param fileName bare filename to match (e.g. {@code "schema.xsd"}).
	 * @return references whose last path segment equals {@code fileName}; empty if none.
	 */
	public static List<GrammarReference> locateReferencesTo(CharSequence text, String fileName) {
		return locateReferencesTo(text, fileName, Collections.emptyList());
	}

	public static List<GrammarReference> locateReferencesTo(CharSequence text, String fileName,
			List<IGrammarReferenceContributor> contributors) {
		return locateReferencesTo(locate(text, contributors), fileName);
	}

	static List<GrammarReference> locateReferencesTo(List<GrammarReference> all, String fileName) {
		List<GrammarReference> matches = new ArrayList<>();
		for (GrammarReference ref : all) {
			if (matchesFileName(ref.value(), fileName)) {
				matches.add(ref);
			}
		}
		return matches;
	}

	/**
	 * Finds grammar references in {@code text} that actually resolve to {@code targetPath}
	 * when the containing file is {@code referencingFile}.
	 *
	 * <p>This is the path-accurate variant used by Find References and the code lens
	 * participant. Without path resolution, two XSD files named {@code foo.xsd} in
	 * different directories would each claim references that belong to the other.
	 *
	 * <p><b>Use case:</b> Workspace has {@code schemas/order.xsd} and {@code lib/order.xsd}.
	 * {@code data.xml} contains {@code href="schemas/order.xsd"}. Opening {@code lib/order.xsd}
	 * must show 0 references; opening {@code schemas/order.xsd} must show 1 reference.
	 * This method resolves {@code "schemas/order.xsd"} relative to {@code data.xml}'s
	 * directory and compares with the target path.
	 *
	 * @param text           XML content to scan.
	 * @param targetPath     absolute path of the grammar file being queried.
	 * @param referencingFile absolute path of the file being scanned.
	 * @return references that resolve to {@code targetPath}; empty if none.
	 */
	public static List<GrammarReference> locateReferencesTo(CharSequence text,
			Path targetPath, Path referencingFile) {
		return locateReferencesTo(text, targetPath, referencingFile, Collections.emptyList());
	}

	public static List<GrammarReference> locateReferencesTo(CharSequence text,
			Path targetPath, Path referencingFile, List<IGrammarReferenceContributor> contributors) {
		return locateReferencesTo(locate(text, contributors), targetPath, referencingFile);
	}

	/**
	 * Finds grammar references in {@code doc} that actually resolve to {@code targetPath}
	 * when the containing file is {@code referencingFile}.
	 *
	 * <p><b>Use case — rename with open document:</b> When a file is open in the editor
	 * and the user has made edits, cached line/column positions may no longer match the
	 * live DOM. {@link GrammarFileRenameSupport} passes the live document content here
	 * to recompute accurate positions before building the rename {@code TextEdit}s.
	 *
	 * @param doc             live document content.
	 * @param targetPath      absolute path of the grammar file being renamed.
	 * @param referencingFile absolute path of the file being scanned.
	 * @param contributors    extension-provided contributors.
	 * @return references that resolve to {@code targetPath}; empty if none.
	 */
	public static List<GrammarReference> locateReferencesTo(TextDocument doc,
			Path targetPath, Path referencingFile, List<IGrammarReferenceContributor> contributors) {
		return locateReferencesTo(locate(doc, contributors), targetPath, referencingFile);
	}

	static List<GrammarReference> locateReferencesTo(List<GrammarReference> all,
			Path targetPath, Path referencingFile) {
		String fileName = targetPath.getFileName().toString();
		List<GrammarReference> candidates = locateReferencesTo(all, fileName);
		if (candidates.isEmpty()) {
			return candidates;
		}
		Path dir = referencingFile != null ? referencingFile.getParent() : null;
		if (dir == null) {
			return candidates;
		}
		Path normalizedTarget = targetPath.normalize();
		List<GrammarReference> resolved = new ArrayList<>();
		for (GrammarReference ref : candidates) {
			String value = ref.value();
			int hash = value.lastIndexOf('#');
			if (hash >= 0) {
				value = value.substring(0, hash);
			}
			if (value.isEmpty()) {
				continue;
			}
			try {
				if (dir.resolve(value).normalize().equals(normalizedTarget)) {
					resolved.add(ref);
				}
			} catch (Exception e) {
				// Path resolution failed (e.g. URI scheme, Windows drive mismatch):
				// include conservatively to avoid false negatives.
				resolved.add(ref);
			}
		}
		return resolved;
	}

	/**
	 * Finds grammar references in {@code text} whose value exactly equals {@code identifier}.
	 *
	 * <p>Used by catalog code lenses to verify that a candidate file actually references a
	 * grammar via a catalog key (systemId, publicId, or URI name) rather than by filename.
	 * Unlike {@link #locateReferencesTo(CharSequence, String)}, this method does not strip
	 * path segments — the full value must match {@code identifier} exactly.
	 *
	 * @param text       XML content to scan.
	 * @param identifier catalog key to match (e.g. {@code "http://example.org/schema"}).
	 * @return references whose value equals {@code identifier}; empty if none.
	 */
	public static List<GrammarReference> locateReferencesWithValue(CharSequence text, String identifier) {
		return locateReferencesWithValue(text, identifier, Collections.emptyList());
	}

	public static List<GrammarReference> locateReferencesWithValue(CharSequence text, String identifier,
			List<IGrammarReferenceContributor> contributors) {
		return locateReferencesWithValue(locate(text, contributors), identifier);
	}

	public static List<GrammarReference> locateReferencesWithValue(List<GrammarReference> all, String identifier) {
		List<GrammarReference> matches = new ArrayList<>();
		for (GrammarReference ref : all) {
			if (identifier.equals(ref.value())) {
				matches.add(ref);
			}
		}
		return matches;
	}

	private static boolean matchesFileName(String refValue, String fileName) {
		if (refValue.equals(fileName)) {
			return true;
		}
		if (refValue.endsWith("/" + fileName) || refValue.endsWith("\\" + fileName)) {
			return true;
		}
		int lastSlash = Math.max(refValue.lastIndexOf('/'), refValue.lastIndexOf('\\'));
		if (lastSlash >= 0) {
			String refFileName = refValue.substring(lastSlash + 1);
			return refFileName.equals(fileName);
		}
		return false;
	}

	/**
	 * Builds a {@link GrammarReference} from a character range in the document, converting
	 * the start offset to a line/column position via {@link TextDocument#positionAt}.
	 * Returns {@code null} if the offset is out of range (and skips the ref silently).
	 */
	private static GrammarReference makeRef(TextDocument doc, CharSequence text, int start, int end,
			GrammarReference.Kind kind) {
		if (start >= end) {
			return null;
		}
		try {
			org.eclipse.lsp4j.Position pos = doc.positionAt(start);
			// Decode XML character references in the value (e.g. &amp; → &) so path
			// comparisons work correctly. Source offsets/lengths are kept raw so that
			// TextEdit ranges always point to the actual characters in the source.
			String value = XMLReferenceCollector.decodeXmlEntities(text, start, end);
			int col = pos.getCharacter();
			return new GrammarReference(start, pos.getLine(), col, col + (end - start), value, kind);
		} catch (BadLocationException e) {
			return null;
		}
	}

	// --- Attribute classification (reuses the same logic as XMLReferenceCollector) ---

	private static final int ATTR_NONE = 0;
	private static final int ATTR_SCHEMA_LOCATION_XSI = 1;
	private static final int ATTR_NO_NS_SCHEMA_LOCATION = 2;
	private static final int ATTR_SCHEMA_LOCATION_PLAIN = 3;
	private static final int ATTR_HREF = 4;
	private static final int ATTR_CONTRIBUTOR_PATH = 5;
	private static final int ATTR_CONTRIBUTOR_PAIRS = 6;
	private static final int ATTR_XMLNS = 7;

	private static int classifyAttribute(CharSequence text, int start, int end, String elementName) {
		int colonPos = -1;
		for (int i = start; i < end; i++) {
			if (text.charAt(i) == ':') {
				colonPos = i;
			}
		}
		int localStart = colonPos >= 0 ? colonPos + 1 : start;
		boolean hasPrefix = colonPos >= 0;

		// xmlns / xmlns:prefix — namespace declaration
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
		return ATTR_NONE;
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

	private static void locateAttributeValue(List<GrammarReference> refs, TextDocument doc,
			CharSequence text, int start, int end, int attrKind,
			IGrammarReferenceContributor contributor) {
		if (start >= end) {
			return;
		}
		switch (attrKind) {
			case ATTR_SCHEMA_LOCATION_XSI:
				locateSchemaLocationPaths(refs, doc, text, start, end);
				break;
			case ATTR_NO_NS_SCHEMA_LOCATION: {
				GrammarReference ref = makeRef(doc, text, start, end,
						GrammarReference.Kind.NO_NS_SCHEMA_LOCATION);
				if (ref != null) {
					refs.add(ref);
				}
				break;
			}
			case ATTR_SCHEMA_LOCATION_PLAIN: {
				GrammarReference ref = makeRef(doc, text, start, end,
						GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION);
				if (ref != null) {
					refs.add(ref);
				}
				break;
			}
			case ATTR_HREF: {
				GrammarReference ref = makeRef(doc, text, start, end, GrammarReference.Kind.HREF);
				if (ref != null) {
					refs.add(ref);
				}
				break;
			}
			case ATTR_XMLNS: {
				if (start < end) {
					GrammarReference ref = makeRef(doc, text, start, end,
							GrammarReference.Kind.NAMESPACE_DECL);
					if (ref != null) {
						refs.add(ref);
					}
				}
				break;
			}
			case ATTR_CONTRIBUTOR_PATH: {
				GrammarReference ref = makeRef(doc, text, start, end, GrammarReference.Kind.HREF);
				if (ref != null) {
					refs.add(ref);
				}
				break;
			}
			case ATTR_CONTRIBUTOR_PAIRS:
				locateSchemaLocationPaths(refs, doc, text, start, end);
				break;
			default:
				break;
		}
	}

	private static void locateSchemaLocationPaths(List<GrammarReference> refs, TextDocument doc,
			CharSequence text, int start, int end) {
		int pairIndex = 0;
		int tokenStart = -1;
		for (int i = start; i <= end; i++) {
			boolean isWs = i == end || Character.isWhitespace(text.charAt(i));
			if (!isWs && tokenStart < 0) {
				tokenStart = i;
			} else if (isWs && tokenStart >= 0) {
				GrammarReference.Kind kind = pairIndex % 2 == 1
						? GrammarReference.Kind.SCHEMA_LOCATION   // odd: schema path — renameable
						: GrammarReference.Kind.NAMESPACE_DECL;   // even: namespace URI
				GrammarReference ref = makeRef(doc, text, tokenStart, i, kind);
				if (ref != null) {
					refs.add(ref);
				}
				pairIndex++;
				tokenStart = -1;
			}
		}
	}

	private static void locateHrefInPI(List<GrammarReference> refs, TextDocument doc,
			CharSequence text, int start, int end) {
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
		GrammarReference ref = makeRef(doc, text, valStart, valEnd, GrammarReference.Kind.XML_MODEL_HREF);
		if (ref != null) {
			refs.add(ref);
		}
	}

	// --- Char-by-char utilities (no String allocation) ---

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
