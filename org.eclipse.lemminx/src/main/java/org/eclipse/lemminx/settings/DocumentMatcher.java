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
package org.eclipse.lemminx.settings;

import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.XMLModel;

/**
 * Extends {@link PathPatternMatcher} with document-level matching criteria.
 * While {@link PathPatternMatcher} matches only on the file URI (glob),
 * {@code DocumentMatcher} can also match on the document's namespace URI,
 * root element name, DOCTYPE (public/system ID), or resolved grammar URI.
 *
 * <p>
 * All criteria are optional and combined with AND. Within each criterion,
 * any matching entry is sufficient (OR). The inherited {@link #getPattern()}
 * field from {@link PathPatternMatcher} provides file path glob matching.
 * </p>
 *
 * <p>
 * This class serves as the base for all per-document scoped settings:
 * </p>
 * <ul>
 * <li>{@code xml.colors} — color expressions per document type</li>
 * <li>{@code xml.symbols.filters} — symbol filters per document type</li>
 * <li>{@code xml.references} — reference expressions per document type</li>
 * <li>{@code xml.filePathSupport} — file path completions per document type</li>
 * <li>{@code xml.format.profiles} — format overrides per document type</li>
 * </ul>
 *
 * <p>
 * Existing callers that use {@link #matches(String)} (file URI only) continue
 * to work unchanged. The new {@link #matches(DOMDocument)} method checks all
 * criteria including the file URI pattern.
 * </p>
 *
 * <h3>Use case: match DocBook documents by namespace</h3>
 *
 * <pre>
 * {
 *   "pattern": "**&#47;*.xml",
 *   "namespaceURI": ["http://docbook.org/ns/docbook*"]
 * }
 * </pre>
 *
 * <h3>Use case: match MyBatis mapper by DOCTYPE</h3>
 *
 * <pre>
 * {
 *   "publicId": ["-//mybatis.org//DTD Mapper 3.0//EN"]
 * }
 * </pre>
 *
 * <h3>Use case: match documents by xml-model processing instruction</h3>
 *
 * <pre>
 * {
 *   "grammarURI": ["http://docbook.org/xml/5.0/rng/docbook.rng*"]
 * }
 * </pre>
 *
 * @see PathPatternMatcher
 */
public class DocumentMatcher extends PathPatternMatcher {

	private List<String> namespaceURI;
	private List<String> rootElement;
	private List<String> publicId;
	private List<String> systemId;
	private List<String> grammarURI;

	/**
	 * Returns {@code true} if this matcher matches the given document. Checks
	 * the file URI pattern (inherited from {@link PathPatternMatcher}) and all
	 * document-level criteria. All non-null criteria must match (AND).
	 *
	 * <p>
	 * Use case: a matcher with both {@code pattern="**&#47;docs/**&#47;*.xml"} and
	 * {@code namespaceURI=["http://docbook.org/ns/docbook"]} matches only DocBook
	 * files inside a {@code docs/} folder.
	 * </p>
	 *
	 * @param document the XML document to check.
	 * @return {@code true} if all specified criteria match.
	 */
	public boolean matches(DOMDocument document) {
		if (!hasAnyCriteria()) {
			return false;
		}
		// Check file URI pattern (from PathPatternMatcher)
		if (getPattern() != null && !getPattern().isEmpty()) {
			if (!super.matches(document.getDocumentURI())) {
				return false;
			}
		}
		// Check namespace URI
		if (namespaceURI != null && !namespaceURI.isEmpty()) {
			DOMElement root = document.getDocumentElement();
			String docNs = root != null ? root.getNamespaceURI() : null;
			if (!GlobMatcher.matchAny(namespaceURI, docNs)) {
				return false;
			}
		}
		// Check root element name
		if (rootElement != null && !rootElement.isEmpty()) {
			DOMElement root = document.getDocumentElement();
			String rootName = root != null ? root.getLocalName() : null;
			if (!GlobMatcher.matchAny(rootElement, rootName)) {
				return false;
			}
		}
		// Check DOCTYPE public ID
		if (publicId != null && !publicId.isEmpty()) {
			DOMDocumentType doctype = document.getDoctype();
			String docPublicId = doctype != null ? doctype.getPublicIdWithoutQuotes() : null;
			if (!GlobMatcher.matchAny(publicId, docPublicId)) {
				return false;
			}
		}
		// Check DOCTYPE system ID
		if (systemId != null && !systemId.isEmpty()) {
			DOMDocumentType doctype = document.getDoctype();
			String docSystemId = doctype != null ? doctype.getSystemIdWithoutQuotes() : null;
			if (!GlobMatcher.matchAny(systemId, docSystemId)) {
				return false;
			}
		}
		// Check grammar URI (DOCTYPE systemId, xml-model href, file associations, catalog)
		if (grammarURI != null && !grammarURI.isEmpty()) {
			if (!matchesGrammarURI(document)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns {@code true} if at least one matching criterion is specified.
	 * A matcher with no criteria never matches — this prevents a bare
	 * {@code {}} from matching every document.
	 *
	 * @return {@code true} if any criterion is non-null and non-empty.
	 */
	private boolean hasAnyCriteria() {
		return (getPattern() != null && !getPattern().isEmpty())
				|| (namespaceURI != null && !namespaceURI.isEmpty())
				|| (rootElement != null && !rootElement.isEmpty())
				|| (publicId != null && !publicId.isEmpty())
				|| (systemId != null && !systemId.isEmpty())
				|| (grammarURI != null && !grammarURI.isEmpty());
	}

	/**
	 * Collects all grammar URIs from the document (DOCTYPE systemId, xml-model
	 * href, file associations, catalog) and checks if any matches the
	 * {@code grammarURI} patterns.
	 *
	 * <p>
	 * Use case: a document using {@code <?xml-model href="docbook.rng"?>} can
	 * be matched with {@code grammarURI: ["*docbook*"]}.
	 * </p>
	 */
	private boolean matchesGrammarURI(DOMDocument document) {
		// Check DOCTYPE systemId
		DOMDocumentType doctype = document.getDoctype();
		if (doctype != null) {
			String sysId = doctype.getSystemIdWithoutQuotes();
			if (sysId != null && GlobMatcher.matchAny(grammarURI, sysId)) {
				return true;
			}
		}
		// Check xml-model hrefs
		List<XMLModel> xmlModels = document.getXMLModels();
		if (xmlModels != null) {
			for (XMLModel model : xmlModels) {
				String href = model.getHref();
				if (href != null && GlobMatcher.matchAny(grammarURI, href)) {
					return true;
				}
			}
		}
		// Check external grammar from file associations
		Map<String, String> externalGrammar = document.getExternalGrammarLocation();
		if (externalGrammar != null) {
			for (String uri : externalGrammar.values()) {
				if (uri != null && GlobMatcher.matchAny(grammarURI, uri)) {
					return true;
				}
			}
		}
		// Check external grammar from namespace URI (catalog)
		String nsGrammar = document.getExternalGrammarFromNamespaceURI();
		if (nsGrammar != null && GlobMatcher.matchAny(grammarURI, nsGrammar)) {
			return true;
		}
		return false;
	}

	// --- Getters / Setters ---

	/**
	 * Returns the namespace URI glob patterns.
	 *
	 * <p>
	 * Use case: {@code ["http://docbook.org/ns/docbook*"]} matches DocBook 5.x
	 * documents regardless of the minor version in the namespace.
	 * </p>
	 *
	 * @return the namespace URI patterns, or {@code null} if not specified.
	 */
	public List<String> getNamespaceURI() {
		return namespaceURI;
	}

	/**
	 * Sets the namespace URI glob patterns.
	 *
	 * @param namespaceURI the namespace URI patterns.
	 */
	public void setNamespaceURI(List<String> namespaceURI) {
		this.namespaceURI = namespaceURI;
	}

	/**
	 * Returns the root element name glob patterns.
	 *
	 * <p>
	 * Use case: {@code ["mapper"]} matches documents whose root element is
	 * {@code <mapper>}. {@code ["project"]} matches Maven POM files
	 * ({@code <project>}) — combine with {@code namespaceURI} or
	 * {@code pattern} to disambiguate from Ant's {@code <project>}.
	 * </p>
	 *
	 * @return the root element name patterns, or {@code null} if not specified.
	 */
	public List<String> getRootElement() {
		return rootElement;
	}

	/**
	 * Sets the root element name glob patterns.
	 *
	 * @param rootElement the root element name patterns.
	 */
	public void setRootElement(List<String> rootElement) {
		this.rootElement = rootElement;
	}

	/**
	 * Returns the DOCTYPE public ID glob patterns.
	 *
	 * <p>
	 * Use case: {@code ["-//mybatis.org//DTD Mapper*"]} matches any version
	 * of the MyBatis mapper DTD.
	 * </p>
	 *
	 * @return the public ID patterns, or {@code null} if not specified.
	 */
	public List<String> getPublicId() {
		return publicId;
	}

	/**
	 * Sets the DOCTYPE public ID glob patterns.
	 *
	 * @param publicId the public ID patterns.
	 */
	public void setPublicId(List<String> publicId) {
		this.publicId = publicId;
	}

	/**
	 * Returns the DOCTYPE system ID glob patterns.
	 *
	 * <p>
	 * Use case: {@code ["http://mybatis.org/dtd/*"]} matches all MyBatis DTDs.
	 * </p>
	 *
	 * @return the system ID patterns, or {@code null} if not specified.
	 */
	public List<String> getSystemId() {
		return systemId;
	}

	/**
	 * Sets the DOCTYPE system ID glob patterns.
	 *
	 * @param systemId the system ID patterns.
	 */
	public void setSystemId(List<String> systemId) {
		this.systemId = systemId;
	}

	/**
	 * Returns the grammar URI glob patterns. Checked against all grammar
	 * sources: DOCTYPE systemId, {@code <?xml-model?>} href, file associations,
	 * and XML catalog.
	 *
	 * <p>
	 * Use case: {@code ["*docbook.rng*"]} matches documents using a DocBook
	 * RelaxNG schema via any binding mechanism.
	 * </p>
	 *
	 * @return the grammar URI patterns, or {@code null} if not specified.
	 */
	public List<String> getGrammarURI() {
		return grammarURI;
	}

	/**
	 * Sets the grammar URI glob patterns.
	 *
	 * @param grammarURI the grammar URI patterns.
	 */
	public void setGrammarURI(List<String> grammarURI) {
		this.grammarURI = grammarURI;
	}
}
