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
import java.util.List;

import org.eclipse.lemminx.utils.FilesUtils;

/**
 * Pairs a file's probabilistic {@link BloomFilter} with its confirmed
 * {@link GrammarReference} list, enabling both fast pre-filtering and
 * zero-I/O reference counting from a single indexing read.
 *
 * <p><b>Use case:</b> When {@code order.xml} is indexed, its entry stores:
 * <ul>
 *   <li>A {@link BloomFilter} containing all token names found in the file
 *       ({@code "schema.xsd"}, element names, type names, …). Used to answer
 *       "does {@code order.xml} possibly reference {@code schema.xsd}?" in O(1)
 *       during rename candidate lookup.</li>
 *   <li>A {@link GrammarReference} list with the confirmed grammar references:
 *       e.g. {@code [XML_MODEL_HREF@17 "schema.xsd"]}. Used to count references
 *       for the "N references" code lens on {@code schema.xsd} without re-reading
 *       {@code order.xml} — and to skip Bloom false positives during "Find References".</li>
 *   <li>A cached {@link Path} derived from {@link #fileUri()} at construction time,
 *       so that query methods in {@link WorkspaceIndex} never call
 *       {@code FilesUtils.getPathOrNull} per entry per query.</li>
 * </ul>
 *
 * <p>All three structures are populated once at indexing time and replaced atomically
 * whenever the file changes ({@link WorkspaceIndex#indexFile}). No separate cache
 * invalidation is needed.
 *
 * @see WorkspaceIndex
 * @see BloomFilter
 * @see GrammarReference
 */
public final class FileBloomEntry {

	private final String fileUri;
	private final Path path;
	private final BloomFilter filter;
	private final List<GrammarReference> refs;

	public FileBloomEntry(String fileUri, BloomFilter filter, List<GrammarReference> refs) {
		this.fileUri = fileUri;
		this.path = FilesUtils.getPathOrNull(fileUri);
		this.filter = filter;
		this.refs = refs;
	}

	public String fileUri() {
		return fileUri;
	}

	/**
	 * Cached {@link Path} derived from {@link #fileUri()} at construction time.
	 * May be {@code null} for non-{@code file://} URIs.
	 */
	public Path path() {
		return path;
	}

	public BloomFilter filter() {
		return filter;
	}

	/** Confirmed grammar references found during indexing. Never {@code null}. */
	public List<GrammarReference> refs() {
		return refs;
	}
}
