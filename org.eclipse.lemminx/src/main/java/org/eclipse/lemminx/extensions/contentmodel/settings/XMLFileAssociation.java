/**
 *  Copyright (c) 2018 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.settings;

import java.net.URI;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.settings.PathPatternMatcher;
import org.eclipse.lemminx.uriresolver.IExternalGrammarLocationProvider;
import org.eclipse.lemminx.utils.DOMUtils;

/**
 * XML file association between a XML file pattern (glob) and an XML Schema file
 * (systemId).
 **/
public class XMLFileAssociation extends PathPatternMatcher {

	private static final Logger LOGGER = Logger.getLogger(XMLFileAssociation.class.getName());

	private transient Map<String, String> externalSchemaLocation;
	private String systemId;

	private transient List<String> workspaceFolderUris;
	private transient boolean patternVariablesChecked;
	private transient boolean hasPatternVariables;
	private transient boolean systemIdVariablesChecked;
	private transient boolean hasSystemIdVariables;

	private transient volatile ResolvedPatternCache resolvedCache;

	private static final class ResolvedPatternCache {
		final String pattern;
		final PathMatcher matcher;

		ResolvedPatternCache(String pattern, PathMatcher matcher) {
			this.pattern = pattern;
			this.matcher = matcher;
		}
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
		this.externalSchemaLocation = null;
		this.systemIdVariablesChecked = false;
	}

	@Override
	public PathPatternMatcher setPattern(String pattern) {
		this.patternVariablesChecked = false;
		this.resolvedCache = null;
		return super.setPattern(pattern);
	}

	/**
	 * Sets the workspace folder URIs used for resolving variables like
	 * {@code ${workspaceFolder}}.
	 *
	 * @param workspaceFolderUris the workspace folder URIs.
	 */
	public void setWorkspaceFolderUris(List<String> workspaceFolderUris) {
		this.workspaceFolderUris = workspaceFolderUris;
		this.resolvedCache = null;
	}

	/**
	 * Returns true if the pattern contains variable references.
	 *
	 * @return true if the pattern contains {@code ${...}} variable references.
	 */
	public boolean hasPatternVariables() {
		if (!patternVariablesChecked) {
			hasPatternVariables = VariableSupportUtils.hasVariables(getPattern());
			patternVariablesChecked = true;
		}
		return hasPatternVariables;
	}

	/**
	 * Returns true if the systemId contains variable references.
	 *
	 * @return true if the systemId contains {@code ${...}} variable references.
	 */
	public boolean hasSystemIdVariables() {
		if (!systemIdVariablesChecked) {
			hasSystemIdVariables = VariableSupportUtils.hasVariables(systemId);
			systemIdVariablesChecked = true;
		}
		return hasSystemIdVariables;
	}

	@Override
	public boolean matches(URI uri) {
		if (!hasPatternVariables()) {
			return super.matches(uri);
		}
		return matchesWithVariables(uri);
	}

	private boolean matchesWithVariables(URI documentUri) {
		String resolvedPattern = FileAssociationVariableSupport.resolve(
				getPattern(), documentUri, workspaceFolderUris);

		ResolvedPatternCache cache = resolvedCache;
		if (cache != null && resolvedPattern.equals(cache.pattern)) {
			try {
				return cache.matcher.matches(Paths.get(documentUri));
			} catch (Exception e) {
				return false;
			}
		}

		if (resolvedPattern.isEmpty()) {
			resolvedCache = null;
			return false;
		}
		PathMatcher matcher;
		try {
			matcher = createGlobMatcher(resolvedPattern);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Invalid glob pattern after variable resolution: " + resolvedPattern, e);
			resolvedCache = null;
			return false;
		}
		resolvedCache = new ResolvedPatternCache(resolvedPattern, matcher);
		try {
			return matcher.matches(Paths.get(documentUri));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the systemId with variables resolved for the given document URI.
	 *
	 * @param documentUri the document URI.
	 * @return the resolved systemId.
	 */
	public String getResolvedSystemId(URI documentUri) {
		if (!hasSystemIdVariables()) {
			return systemId;
		}
		return FileAssociationVariableSupport.resolve(systemId, documentUri, workspaceFolderUris);
	}

	public Map<String, String> getExternalSchemaLocation() {
		return getExternalSchemaLocation(null);
	}

	/**
	 * Returns the external schema location map with variables resolved for the
	 * given document URI.
	 *
	 * @param documentUri the document URI, or null if no variable resolution is
	 *                    needed.
	 * @return the schema location map.
	 */
	public Map<String, String> getExternalSchemaLocation(URI documentUri) {
		String resolvedSystemId = documentUri != null ? getResolvedSystemId(documentUri) : systemId;
		if (!hasSystemIdVariables() && externalSchemaLocation != null) {
			return externalSchemaLocation;
		}
		Map<String, String> schemaLocation = new HashMap<>();
		if (DOMUtils.isXSD(resolvedSystemId)) {
			schemaLocation.put(IExternalGrammarLocationProvider.NO_NAMESPACE_SCHEMA_LOCATION, resolvedSystemId);
		} else if (DOMUtils.isRelaxNGUri(resolvedSystemId)) {
			schemaLocation.put(IExternalGrammarLocationProvider.RELAXNG, resolvedSystemId);
		} else {
			schemaLocation.put(IExternalGrammarLocationProvider.DOCTYPE, resolvedSystemId);
		}
		if (!hasSystemIdVariables()) {
			this.externalSchemaLocation = schemaLocation;
		}
		return schemaLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getPattern() == null) ? 0 : getPattern().hashCode());
		result = prime * result + ((systemId == null) ? 0 : systemId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XMLFileAssociation other = (XMLFileAssociation) obj;
		String thisPattern = getPattern();
		String otherPattern = other.getPattern();
		if (thisPattern == null) {
			if (otherPattern != null) {
				return false;
			}
		} else if (!thisPattern.equals(otherPattern)) {
			return false;
		}
		if (systemId == null) {
			if (other.systemId != null) {
				return false;
			}
		} else if (!systemId.equals(other.systemId)) {
			return false;
		}
		return true;
	}
}
