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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Registry of well-known XML namespace URIs for RNC completion.
 *
 * <p>Loads namespace entries from {@code rnc-namespaces.json} at class
 * initialization time. Each entry has a URI and a human-readable description.</p>
 *
 * <p>Use case: when the cursor follows {@code =} in a {@code namespace}
 * declaration (e.g. {@code namespace ns = |}), the completion engine calls
 * {@link #getNamespaces()} to offer well-known namespace URIs.</p>
 */
public class RNCNamespaceLibrary {

	private static final Logger LOGGER = Logger.getLogger(RNCNamespaceLibrary.class.getName());

	/**
	 * A well-known namespace URI with its description.
	 */
	public static class Entry {

		private final String uri;
		private final String description;

		Entry(String uri, String description) {
			this.uri = uri;
			this.description = description;
		}

		/**
		 * Returns the namespace URI.
		 *
		 * @return the namespace URI.
		 */
		public String getUri() {
			return uri;
		}

		/**
		 * Returns the human-readable description.
		 *
		 * @return the description.
		 */
		public String getDescription() {
			return description;
		}
	}

	private static final List<Entry> NAMESPACES;

	static {
		NAMESPACES = loadNamespaces();
	}

	/**
	 * Returns all registered namespace entries.
	 *
	 * @return an unmodifiable list of namespace entries.
	 */
	public static List<Entry> getNamespaces() {
		return NAMESPACES;
	}

	private static List<Entry> loadNamespaces() {
		try (InputStream is = RNCNamespaceLibrary.class.getResourceAsStream("rnc-namespaces.json")) {
			if (is == null) {
				LOGGER.warning("rnc-namespaces.json not found");
				return Collections.emptyList();
			}
			Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
			JsonArray array = new Gson().fromJson(reader, JsonArray.class);
			List<Entry> result = new ArrayList<>();
			for (JsonElement el : array) {
				JsonObject obj = el.getAsJsonObject();
				String uri = obj.get("uri").getAsString();
				String description = obj.has("description") ? obj.get("description").getAsString() : null;
				result.add(new Entry(uri, description));
			}
			return Collections.unmodifiableList(result);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error loading rnc-namespaces.json", e);
			return Collections.emptyList();
		}
	}

	private RNCNamespaceLibrary() {
	}
}
