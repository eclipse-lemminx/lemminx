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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Registry of datatype libraries available in RNC documents.
 *
 * <p>A datatype library maps a URI (declared via {@code datatypes prefix = "uri"})
 * to a set of named data types, each with documentation. This class is used by
 * both completion (to propose types like {@code xsd:integer}) and hover (to show
 * documentation when the cursor is on a type reference).</p>
 *
 * <p>Libraries are loaded from the bundled {@code rnc-datatype-libraries.json}
 * resource, which defines three libraries matching Jing's architecture:</p>
 * <ul>
 *   <li>Built-in (empty URI): {@code string}, {@code token}</li>
 *   <li>Compatibility ({@code http://relaxng.org/ns/compatibility/datatypes/1.0}):
 *       {@code ID}, {@code IDREF}, {@code IDREFS}</li>
 *   <li>XSD ({@code http://www.w3.org/2001/XMLSchema-datatypes}): all W3C XML
 *       Schema built-in datatypes</li>
 * </ul>
 */
public class RNCDatatypeLibrary {

	private static final Logger LOGGER = Logger.getLogger(RNCDatatypeLibrary.class.getName());

	private static final Map<String, RNCDatatypeLibrary> LIBRARIES = loadLibraries();

	private final String uri;
	private final String description;
	private final String defaultPrefix;
	private final Map<String, Entry> entries;

	private RNCDatatypeLibrary(String uri, String description, String defaultPrefix, Map<String, Entry> entries) {
		this.uri = uri;
		this.description = description;
		this.defaultPrefix = defaultPrefix;
		this.entries = entries;
	}

	/**
	 * Returns the library URI (e.g. {@code "http://www.w3.org/2001/XMLSchema-datatypes"}).
	 *
	 * @return the URI string, or empty string for the built-in library.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Returns a human-readable description of this library.
	 *
	 * @return the description (e.g. {@code "W3C XML Schema datatypes"}).
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the conventional prefix for this library, used to generate
	 * {@code datatypes} snippets (e.g. {@code "xsd"}, {@code "compat"}).
	 *
	 * @return the default prefix, or {@code null} for the built-in library.
	 */
	public String getDefaultPrefix() {
		return defaultPrefix;
	}

	/**
	 * Returns all datatype entries in this library.
	 *
	 * @return the entries (e.g. {@code string}, {@code integer}, {@code ID}).
	 */
	public Collection<Entry> getEntries() {
		return entries.values();
	}

	/**
	 * Returns the entry for the given datatype name, or {@code null} if not found.
	 *
	 * @param name the datatype name (e.g. {@code "integer"}, {@code "ID"}).
	 * @return the matching entry, or {@code null}.
	 */
	public Entry getEntry(String name) {
		return entries.get(name);
	}

	/**
	 * Returns the datatype library for the given URI, or {@code null} if unknown.
	 */
	public static RNCDatatypeLibrary get(String uri) {
		return LIBRARIES.get(uri);
	}

	/**
	 * Returns all registered datatype libraries.
	 */
	public static Collection<RNCDatatypeLibrary> getLibraries() {
		return LIBRARIES.values();
	}

	/**
	 * Resolves a datatype prefix to its library by looking up the
	 * {@code datatypes} declarations in the given RNC document.
	 *
	 * @return the library, or {@code null} if the prefix is not declared
	 *         or maps to an unknown library.
	 */
	public static RNCDatatypeLibrary resolve(RNCDocument document, String prefix) {
		for (RNCDatatypesDecl decl : document.getDatatypesDecls()) {
			if (prefix.equals(decl.getPrefix())) {
				return get(decl.getUri());
			}
		}
		return null;
	}

	private static Map<String, RNCDatatypeLibrary> loadLibraries() {
		Map<String, RNCDatatypeLibrary> libraries = new HashMap<>();
		try (InputStream is = RNCDatatypeLibrary.class.getResourceAsStream("rnc-datatype-libraries.json")) {
			if (is == null) {
				LOGGER.warning("rnc-datatype-libraries.json resource not found");
				return libraries;
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			JsonObject root = new Gson().fromJson(reader, JsonObject.class);
			for (Map.Entry<String, JsonElement> libEntry : root.entrySet()) {
				String libraryUri = libEntry.getKey();
				JsonObject libObj = libEntry.getValue().getAsJsonObject();
				String description = libObj.has("description") ? libObj.get("description").getAsString() : null;
				String defaultPrefix = libObj.has("defaultPrefix") ? libObj.get("defaultPrefix").getAsString() : null;
				Map<String, Entry> entries = new HashMap<>();
				for (Map.Entry<String, JsonElement> typeEntry : libObj.entrySet()) {
					String typeName = typeEntry.getKey();
					JsonElement val = typeEntry.getValue();
					if (!val.isJsonObject()) {
						continue;
					}
					JsonObject typeObj = val.getAsJsonObject();
					String markdown = typeObj.has("markdown") ? typeObj.get("markdown").getAsString() : null;
					String plaintext = typeObj.has("plaintext") ? typeObj.get("plaintext").getAsString() : null;
					if (markdown != null || plaintext != null) {
						entries.put(typeName, new Entry(typeName, markdown, plaintext));
					}
				}
				libraries.put(libraryUri, new RNCDatatypeLibrary(libraryUri, description, defaultPrefix, entries));
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error loading RNC datatype libraries", e);
		}
		return libraries;
	}

	/**
	 * A single datatype entry within a library, holding its name and documentation
	 * in both Markdown and plain text formats.
	 *
	 * <p>Use case: completion items display the entry name, and hover/resolve
	 * show the documentation via {@link #getDocumentation(boolean)}.</p>
	 */
	public static class Entry {

		private final String name;
		private final String markdown;
		private final String plaintext;

		/**
		 * Creates a new datatype entry.
		 *
		 * @param name      the datatype name (e.g. {@code "integer"}).
		 * @param markdown  the Markdown documentation, or {@code null}.
		 * @param plaintext the plain text documentation, or {@code null}.
		 */
		public Entry(String name, String markdown, String plaintext) {
			this.name = name;
			this.markdown = markdown;
			this.plaintext = plaintext;
		}

		/**
		 * Returns the datatype name.
		 *
		 * @return the name (e.g. {@code "integer"}, {@code "ID"}).
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the documentation as a {@link MarkupContent}, selecting Markdown
		 * or plain text based on client capabilities.
		 *
		 * @param supportsMarkdown {@code true} if the client supports Markdown.
		 * @return the documentation content, or {@code null} if none available.
		 */
		public MarkupContent getDocumentation(boolean supportsMarkdown) {
			if (supportsMarkdown && markdown != null) {
				return new MarkupContent(MarkupKind.MARKDOWN, markdown);
			}
			if (plaintext != null) {
				return new MarkupContent(MarkupKind.PLAINTEXT, plaintext);
			}
			if (markdown != null) {
				return new MarkupContent(MarkupKind.MARKDOWN, markdown);
			}
			return null;
		}
	}
}
