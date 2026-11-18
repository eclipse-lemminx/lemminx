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
package org.eclipse.lemminx.services.extensions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.extensions.index.GrammarReference;

/**
 * Extension point that allows XML plugins to contribute a custom file scanner
 * for non-XML grammar formats to the workspace index.
 *
 * <p>The default index scanner understands XML-based formats (XSD, RNG, DTD,
 * WSDL…). Implement this interface to handle grammar files whose syntax is
 * <em>not</em> XML — for example, RELAX NG Compact Syntax (RNC) — so that
 * their {@code include} and {@code external} directives are captured by the
 * workspace Bloom index.
 *
 * <p>Register your implementation in {@link IXMLExtension#start} via
 * {@link XMLExtensionsRegistry#registerIndexFileScannerContributor}.
 *
 * <p>When the workspace index needs to scan a file, it asks each registered
 * contributor in order whether it {@link #canScan} the file. The first
 * contributor that accepts it performs the scan; the XML scanner is used as
 * the fallback for all other files.
 *
 * @see XMLExtensionsRegistry#registerIndexFileScannerContributor
 */
public interface IIndexFileScannerContributor {

	/**
	 * Returns {@code true} if this contributor can scan the given file.
	 *
	 * <p>Implementations typically check the file extension. The check must be
	 * cheap (no I/O).
	 *
	 * @param path the absolute path of the file to be indexed.
	 * @return {@code true} to claim the file; {@code false} to pass.
	 */
	boolean canScan(Path path);

	/**
	 * Scans the file and populates {@code refs} and {@code names}.
	 *
	 * <p>The scan must be streaming: never load the whole file into memory.
	 * Stop reading after {@code maxChars} characters and return {@code true}
	 * to signal that the file is only partially indexed.
	 *
	 * @param path     the absolute path of the file to scan.
	 * @param maxChars maximum characters to consume;
	 *                 {@link Integer#MAX_VALUE} means no limit.
	 * @param refs     output — located grammar references with line/column positions.
	 * @param names    output — token names for the Bloom filter.
	 * @return {@code true} if scanning stopped early due to {@code maxChars}.
	 * @throws IOException if the file cannot be opened or read.
	 */
	boolean scan(Path path, int maxChars, List<GrammarReference> refs, Set<String> names) throws IOException;

	/**
	 * Scans the given in-memory content and populates {@code refs} and {@code names}.
	 *
	 * <p>Called when the file is open in the editor with unsaved changes, so that
	 * reference positions stay accurate even before the file is saved and the
	 * workspace index is re-indexed. The default implementation falls back to
	 * {@link #scan(Path, int, List, Set)}, ignoring the in-memory content.
	 *
	 * @param content in-memory file content (may differ from disk).
	 * @param path    absolute path of the file (used for resolving relative references).
	 * @param refs    output — located grammar references with line/column positions.
	 * @param names   output — token names for the Bloom filter.
	 * @throws IOException if the fallback disk scan fails.
	 */
	default void scan(String content, Path path, List<GrammarReference> refs, Set<String> names) throws IOException {
		scan(path, Integer.MAX_VALUE, refs, names);
	}

	/**
	 * Returns the in-memory text content of the given URI if the file is currently
	 * open in the editor and tracked by this contributor, or {@code null} if the
	 * file is not open or this contributor does not track open documents.
	 *
	 * <p>The default implementation returns {@code null}. Override to supply live
	 * content from the contributor's own text-document service so that reference
	 * positions remain accurate for files with unsaved edits.
	 *
	 * @param uri the document URI to look up.
	 * @return live content, or {@code null} if unavailable.
	 */
	default String getOpenDocumentContent(String uri) {
		return null;
	}
}
