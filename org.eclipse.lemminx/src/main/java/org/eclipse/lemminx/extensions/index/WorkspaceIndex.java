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
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;
import org.eclipse.lemminx.settings.XMLWorkspaceIndexSettings;

/**
 * Workspace-level index that maintains one {@link FileBloomEntry} per XML/grammar
 * file, enabling ultra-fast cross-file reference lookup with zero repeated file I/O.
 *
 * <p><b>Purpose:</b> Answers "which files reference grammar X?" and "how many?"
 * entirely from memory, powering:
 * <ul>
 *   <li>UC19 — "Find References" on {@code types.xsd}: lists every XML file that
 *       references it, with exact {@code Location}s.</li>
 *   <li>UC14–UC18 — Rename grammar file: finds all impacted files and computes
 *       a {@code WorkspaceEdit} to update their references.</li>
 *   <li>Code lens — "N references" badge on {@code schema.xsd}: counts references
 *       instantly without reading any file.</li>
 * </ul>
 *
 * <p><b>One-read-per-file principle:</b> Each file is read exactly once, at indexing
 * time (triggered by startup scan or {@code didChangeWatchedFiles}). The read is
 * bounded to a head region (64 KB for XML data files, up to
 * {@code grammarHeadLimit} for grammar files) because grammar references always
 * appear at the top of a file. The result is stored in a {@link FileBloomEntry}
 * containing two complementary structures:
 * <ol>
 *   <li>A {@link BloomFilter} with all token names — O(1) probabilistic lookup,
 *       used as a fast pre-filter to find rename/reference candidates.</li>
 *   <li>A confirmed {@link GrammarReference} list — exact line/column positions of every
 *       grammar reference in the head, used for zero-I/O code lens counts and
 *       for building LSP Locations/TextEdits without re-reading files.</li>
 * </ol>
 * After indexing, files are never re-read for code lens computation. When a file
 * changes, {@link #indexFile} atomically replaces its entry, so the cache is always
 * up-to-date without any explicit invalidation step.
 *
 * <p><b>Memory:</b>
 * <ul>
 *   <li>BloomFilter: ~12 KB per 10,000 files (1.2 bytes per file)</li>
 *   <li>GrammarReference list: ~100–200 bytes per file (1–3 refs × ~80 bytes each)</li>
 *   <li>Total overhead for 10,000 files: approximately 1–2 MB</li>
 *   <li>Keeping {@code grammarHeadLimit} ≤ 512 KB prevents the transient read buffer
 *       from becoming a G1GC humongous object on workspaces with many large XSD files.</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> Uses {@link java.util.concurrent.ConcurrentHashMap}
 * for the entry map and a monotonic sequence number per path to handle concurrent
 * re-indexing of the same file without locks.
 *
 * @see FileBloomEntry
 * @see BloomFilter
 * @see GrammarReference
 * @see XMLReferenceCollector
 * @see GrammarFileRenameSupport
 */
public class WorkspaceIndex {

	/**
	 * Listener notified as files are indexed during a {@link #scanDirectory} call.
	 */
	@FunctionalInterface
	public interface IndexProgressListener {
		/**
		 * Called each time a file has been indexed.
		 *
		 * @param indexed the number of files indexed so far.
		 * @param total   the total number of XML-family files to index.
		 * @param file    the file just indexed.
		 */
		void onProgress(int indexed, int total, Path file);
	}

	private static final Set<String> XML_EXTENSIONS = Set.of(
			".xml", ".xsd", ".xsl", ".xslt", ".dtd", ".rng", ".rnc", ".wsdl");

	private static final Set<String> GRAMMAR_EXTENSIONS = Set.of(
			".xsd", ".xsl", ".xslt", ".dtd", ".rng", ".rnc", ".wsdl");

	private static final int XML_HEAD_LIMIT = 64 * 1024;

	private final Map<String, FileBloomEntry> entries = new ConcurrentHashMap<>();
	private final Set<String> partiallyIndexed = ConcurrentHashMap.newKeySet();
	// Monotonically increasing counter: incremented before each file read so that
	// concurrent reads for the same path can be ordered. The per-path sequence map
	// records the highest sequence number that has been published (or removed) for
	// each URI, preventing stale reads from overwriting newer results.
	private final AtomicLong globalSeq = new AtomicLong(0);
	private final Map<String, Long> fileSeq = new ConcurrentHashMap<>();
	private volatile List<IGrammarReferenceContributor> contributors = Collections.emptyList();
	private volatile int grammarHeadLimit = XMLWorkspaceIndexSettings.DEFAULT_GRAMMAR_HEAD_LIMIT;
	private volatile boolean scanCompleted = false;
	private volatile CompletableFuture<Void> scanCompletedFuture = new CompletableFuture<>();

	public void setContributors(List<IGrammarReferenceContributor> contributors) {
		this.contributors = contributors != null ? contributors : Collections.emptyList();
	}

	public List<IGrammarReferenceContributor> getContributors() {
		return contributors;
	}

	/**
	 * Sets the maximum number of characters read from grammar files during indexing.
	 * Grammar files larger than this limit are marked as partially indexed and always
	 * returned as candidates (conservative correctness, no false negatives).
	 *
	 * @param grammarHeadLimit maximum characters to read; must be positive.
	 */
	public void setGrammarHeadLimit(int grammarHeadLimit) {
		this.grammarHeadLimit = grammarHeadLimit > 0 ? grammarHeadLimit
				: XMLWorkspaceIndexSettings.DEFAULT_GRAMMAR_HEAD_LIMIT;
	}

	/**
	 * Builds or refreshes the Bloom filter entry for a single file.
	 *
	 * <p><b>Use case:</b> The file watcher fires a {@code Created} or {@code Changed}
	 * event for {@code order.xml}. The plugin calls {@code indexFile(path)} so that
	 * subsequent {@link #findReferencingFiles("schema.xsd")} calls include this file
	 * if it references {@code schema.xsd}.
	 *
	 * <p>XML data files are truncated at 64 KB (grammar declarations always appear at
	 * the top). Grammar files are truncated at {@link #grammarHeadLimit} characters
	 * (default 512 KB, configurable via {@code xml.workspaceIndex.grammarHeadLimit}).
	 * Files larger than their respective limit are added to {@code partiallyIndexed}
	 * and always returned as candidates, avoiding false negatives while bounding the
	 * transient heap allocation per file.
	 *
	 * @param path the file to index.
	 * @return {@code true} if the file contains at least one grammar reference
	 *         (the index was meaningfully updated); {@code false} if the file has
	 *         no grammar references and no code lens refresh is necessary.
	 * @throws IOException if the file cannot be read.
	 */
	public boolean indexFile(Path path) throws IOException {
		String uri = path.toUri().toString();
		// Acquire a monotonic stamp BEFORE reading the file. If a newer read or a
		// removal completes while this read is in progress, our stamp will be lower
		// than theirs, and the publish step below will silently discard our result.
		long mySeq = globalSeq.incrementAndGet();

		int headLimit = isGrammarFile(path) ? grammarHeadLimit : XML_HEAD_LIMIT;
		String text = readHead(path, headLimit);
		boolean partial = Files.size(path) > headLimit;
		List<IGrammarReferenceContributor> currentContributors = this.contributors;
		// Build a TextDocument so GrammarReferenceLocator can produce line/column positions
		// directly — no file re-read needed for code lens, Find References, or rename.
		TextDocument headDoc = new TextDocument(text, uri);
		List<GrammarReference> refs = GrammarReferenceLocator.locate(headDoc, currentContributors);
		Set<String> names = XMLReferenceCollector.collect(text, currentContributors);
		BloomFilter filter = BloomFilter.create(names);
		FileBloomEntry newEntry = new FileBloomEntry(uri, filter, Collections.unmodifiableList(refs));
		boolean isPartial = partial;

		// Publish only if our stamp is strictly newer than whatever was last
		// published (or removed) for this path. The compute() call is atomic
		// per-key, so no two threads can publish for the same path simultaneously.
		boolean[] published = { false };
		fileSeq.compute(uri, (k, current) -> {
			if (current == null || current < mySeq) {
				entries.put(k, newEntry);
				if (isPartial) {
					partiallyIndexed.add(k);
				} else {
					partiallyIndexed.remove(k);
				}
				published[0] = true;
				return mySeq;
			}
			return current; // a newer result or removal already registered — discard
		});

		// Return true only when actual grammar file references are present (not just
		// element names). XMLReferenceCollector also collects element names for symbol
		// cross-file refs, so names.isEmpty() is not a reliable grammar-ref indicator.
		return published[0] && !refs.isEmpty();
	}

	/**
	 * Removes a file's Bloom entry from the index.
	 *
	 * <p><b>Use case:</b> The user deletes {@code order.xml} from the workspace.
	 * The file watcher fires a {@code Deleted} event; the plugin calls
	 * {@code removeFile(path)} so the file is no longer returned by
	 * {@link #findReferencingFiles}. The deletion is always processed even when
	 * the index is otherwise disabled (to prevent stale entries).
	 *
	 * @param path the file to remove.
	 */
	public void removeFile(Path path) {
		String uri = path.toUri().toString();
		long mySeq = globalSeq.incrementAndGet();
		// Remove only if our deletion stamp is newer than the last published entry.
		// If a later indexFile has already published (e.g. file was recreated after
		// deletion and immediately re-indexed), leave that newer entry in place.
		fileSeq.compute(uri, (k, current) -> {
			if (current == null || current < mySeq) {
				entries.remove(k);
				partiallyIndexed.remove(k);
				return mySeq;
			}
			return current;
		});
	}

	/**
	 * Recursively indexes all XML-family files under {@code root}.
	 *
	 * <p><b>Use case:</b> Called once at startup to populate the index for the
	 * entire workspace root (e.g. {@code /home/user/my-project}).
	 *
	 * @param root the directory to scan.
	 * @throws IOException if the directory cannot be walked.
	 */
	public void scanDirectory(Path root) throws IOException {
		scanDirectory(root, null, null);
	}

	/**
	 * Recursively indexes all XML-family files under {@code root}, reporting progress.
	 *
	 * <p><b>Use case:</b> Called by {@link WorkspaceIndexPlugin} during the async
	 * workspace scan triggered by {@code initialized}. The {@code listener} drives
	 * the LSP progress notification (e.g. "Indexed 42 / 200 files — 21%").
	 *
	 * @param root     the directory to scan.
	 * @param listener optional progress listener; {@code null} for no progress.
	 * @throws IOException if the directory cannot be walked.
	 */
	public void scanDirectory(Path root, IndexProgressListener listener) throws IOException {
		scanDirectory(root, listener, null);
	}

	/**
	 * Recursively indexes all XML-family files under {@code root}, reporting progress
	 * and skipping the specified directories.
	 *
	 * <p><b>Single-pass approach:</b> collects all XML file paths in one directory
	 * traversal, then indexes them. This avoids the two-pass overhead of an earlier
	 * count-then-index approach while still allowing accurate progress percentages.
	 *
	 * <p><b>Directory exclusions:</b> any directory whose simple name exactly matches
	 * an entry in {@code excludedDirs} is skipped entirely (along with all descendants).
	 * Typical exclusions: {@code .git}, {@code node_modules}, {@code target}.
	 *
	 * @param root        the directory to scan.
	 * @param listener    optional progress listener; {@code null} for no progress.
	 * @param excludedDirs directory names to skip; {@code null} means skip nothing.
	 * @throws IOException if the directory cannot be walked.
	 */
	public void scanDirectory(Path root, IndexProgressListener listener,
			Set<String> excludedDirs) throws IOException {
		// Single pass: collect all XML file paths while skipping excluded dirs.
		// Report discovery progress every 100 files so the user sees immediate
		// feedback while walkFileTree runs (which can take several seconds on
		// large workspaces before per-file indexing starts). total=0 signals
		// "discovery in progress" to the listener.
		List<Path> xmlFiles = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				if (!dir.equals(root) && excludedDirs != null) {
					Path name = dir.getFileName();
					if (name != null && excludedDirs.contains(name.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (isXmlFile(file)) {
					xmlFiles.add(file);
					if (listener != null && xmlFiles.size() % 100 == 0) {
						listener.onProgress(xmlFiles.size(), 0, file);
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				return FileVisitResult.CONTINUE;
			}
		});

		int total = xmlFiles.size();
		for (int i = 0; i < total; i++) {
			Path file = xmlFiles.get(i);
			try {
				indexFile(file);
			} catch (IOException e) {
				// skip unreadable files
			}
			if (listener != null) {
				listener.onProgress(i + 1, total, file);
			}
		}
	}

	/**
	 * Returns URIs of all indexed files whose Bloom filter may contain {@code name}.
	 *
	 * <p><b>Use case:</b> Before renaming {@code schema.xsd}, the rename support
	 * calls {@code findReferencingFiles("schema.xsd")} to get the candidate set
	 * (e.g. {@code [order.xml, invoice.xml]}). Each candidate is then opened and
	 * scanned with {@link GrammarReferenceLocator} for exact position confirmation.
	 *
	 * <p>Files larger than 64 KB (partially indexed) are always included as
	 * candidates regardless of their Bloom filter result, to avoid false negatives
	 * caused by references beyond the head limit. Use {@link #findReferencingFiles(String, boolean)}
	 * with {@code false} when this conservative behaviour is not needed (e.g. for catalog
	 * identifier lookups where references always appear in the first 64 KB).
	 *
	 * @param name a grammar filename, symbol name, or any string to look up.
	 * @return list of file URIs that may reference {@code name} (may include false positives).
	 */
	public List<String> findReferencingFiles(String name) {
		return findReferencingFiles(name, true);
	}

	/**
	 * Returns URIs of indexed files whose Bloom filter may contain {@code name}.
	 *
	 * @param name                    a grammar filename, catalog identifier, or any string to look up.
	 * @param includePartiallyIndexed when {@code true}, files larger than 64 KB are always
	 *                                included regardless of their Bloom filter result (conservative
	 *                                correctness for rename). When {@code false}, only Bloom filter
	 *                                hits are returned — safe for queries where the target always
	 *                                appears in the first 64 KB (e.g. namespace URI declarations,
	 *                                catalog systemId/publicId references).
	 * @return list of file URIs that may reference {@code name} (may include false positives).
	 */
	public List<String> findReferencingFiles(String name, boolean includePartiallyIndexed) {
		List<String> result = new ArrayList<>();
		for (FileBloomEntry entry : entries.values()) {
			if (entry.filter().mightContain(name)
					|| (includePartiallyIndexed && partiallyIndexed.contains(entry.fileUri()))) {
				result.add(entry.fileUri());
			}
		}
		return result;
	}

	/**
	 * Counts grammar references that resolve to {@code targetPath}, using only
	 * cached data — zero file I/O.
	 *
	 * <p><b>Use case:</b> The user opens {@code types.xsd}. The code lens provider
	 * calls this method to show "3 references" without reading any file.
	 * {@code order.xml} contributes 1 (its {@code xml-model} href), and
	 * {@code invoice.xml} contributes 2 (both {@code xml-model} and
	 * {@code xsi:noNamespaceSchemaLocation} point to {@code types.xsd}).
	 *
	 * <p>Each {@link GrammarReference} in each indexed file is resolved relative
	 * to that file's directory. Only those whose resolved path equals
	 * {@code targetPath} are counted. A single file may contribute more than one
	 * count if it references the grammar multiple times.
	 *
	 * @param targetPath      absolute path of the grammar file.
	 * @param excludeGrammars when {@code true}, grammar files (XSD, DTD…) are
	 *                        excluded from the count — useful for catalog lenses
	 *                        that count only XML instance documents.
	 * @return total number of matching reference occurrences across all indexed files.
	 */
	public int countReferencesTo(Path targetPath, boolean excludeGrammars) {
		if (targetPath == null) {
			return 0;
		}
		int count = 0;
		for (FileBloomEntry entry : entries.values()) {
			if (excludeGrammars && entry.path() != null && isGrammarFile(entry.path())) {
				continue;
			}
			count += matchingRefs(entry, targetPath).size();
		}
		return count;
	}

	/**
	 * Returns, for each indexed file whose cached refs contain at least one entry
	 * with {@link GrammarReference#value()} equal to {@code value}, the list of
	 * matching references. Zero file I/O.
	 *
	 * <p><b>Use case:</b> The user clicks the "2 references" lens on the catalog entry
	 * {@code <uri name="http://example.com/ns" uri="schema.xsd"/>} in
	 * {@code catalog.xml}. The catalog reference participant calls
	 * {@code getReferencesWithValue("http://example.com/ns", true)} to retrieve only
	 * the XML instance files that contain that exact namespace URI. Locations are
	 * built directly from cached line/column positions — no files are read.
	 *
	 * <p>Unlike {@link #getReferencesTo(Path)}, which resolves ref values as file
	 * paths, this method matches by exact string equality — suitable for namespace
	 * URIs, systemIds, and publicIds that are not paths.
	 *
	 * @param value           the identifier to match against {@link GrammarReference#value()}.
	 * @param excludeGrammars when {@code true}, grammar files (XSD, DTD…) are excluded.
	 * @return map of fileUri → matching refs; empty if none found.
	 */
	public Map<String, List<GrammarReference>> getReferencesWithValue(String value, boolean excludeGrammars) {
		if (value == null) {
			return Collections.emptyMap();
		}
		Map<String, List<GrammarReference>> result = new LinkedHashMap<>();
		for (FileBloomEntry entry : entries.values()) {
			if (excludeGrammars && entry.path() != null && isGrammarFile(entry.path())) {
				continue;
			}
			List<GrammarReference> matching = new ArrayList<>();
			for (GrammarReference ref : entry.refs()) {
				if (value.equals(ref.value())) {
					matching.add(ref);
				}
			}
			if (!matching.isEmpty()) {
				result.put(entry.fileUri(), matching);
			}
		}
		return result;
	}

	/**
	 * Returns, for each indexed file that has confirmed references to
	 * {@code targetPath}, the list of matching {@link GrammarReference} objects.
	 * Zero file I/O — all data comes from the in-memory index.
	 *
	 * <p><b>Use case:</b> The user invokes "Find All References" on {@code schema.xsd}.
	 * {@link WorkspaceIndexReferenceParticipant} calls
	 * {@code getReferencesTo(schemaXsdPath)} to get a map such as:
	 * <pre>
	 *   "file:///project/order.xml"   → [XML_MODEL_HREF line=0,col=17-27 "schema.xsd"]
	 *   "file:///project/invoice.xml" → [NO_NS_SCHEMA_LOCATION line=2,col=43-53 "schema.xsd"]
	 * </pre>
	 * No files are opened — each {@link GrammarReference} in the map already carries
	 * pre-computed line/column positions from indexing time.
	 *
	 * <p>Each {@link GrammarReference#value()} is resolved relative to the
	 * containing file's directory and compared to {@code targetPath}.
	 * Values that cannot be parsed as a path (e.g. absolute URIs) are silently
	 * skipped.
	 *
	 * @param targetPath absolute path of the grammar file.
	 * @return map of fileUri → matching refs; empty if none found.
	 */
	public Map<String, List<GrammarReference>> getReferencesTo(Path targetPath) {
		if (targetPath == null) {
			return Collections.emptyMap();
		}
		Map<String, List<GrammarReference>> result = new LinkedHashMap<>();
		for (FileBloomEntry entry : entries.values()) {
			List<GrammarReference> matching = matchingRefs(entry, targetPath);
			if (!matching.isEmpty()) {
				result.put(entry.fileUri(), matching);
			}
		}
		return result;
	}

	/**
	 * Counts indexed files (excluding grammar files when requested) that contain at
	 * least one {@link GrammarReference} whose {@link GrammarReference#value()} equals
	 * {@code value}. Zero file I/O.
	 *
	 * <p><b>Use case:</b> A catalog entry
	 * {@code <system systemId="http://example.org/dtd" uri="book.dtd"/>} has
	 * {@code systemId} as the catalog identifier. The catalog code lens calls
	 * {@code countFilesWithRefValue("http://example.org/dtd", true)} to show
	 * "2 references" — the number of XML instance files that contain a reference
	 * with that exact value (e.g. a {@code DOCTYPE} declaration using that system ID).
	 *
	 * <p>Each file is counted at most once, even if it contains multiple refs with
	 * the same value.
	 *
	 * @param value           the identifier to match against {@link GrammarReference#value()}.
	 * @param excludeGrammars when {@code true}, grammar files (XSD, DTD…) are excluded.
	 * @return number of matching files.
	 */
	public int countFilesWithRefValue(String value, boolean excludeGrammars) {
		if (value == null) {
			return 0;
		}
		int count = 0;
		for (FileBloomEntry entry : entries.values()) {
			if (excludeGrammars && entry.path() != null && isGrammarFile(entry.path())) {
				continue;
			}
			for (GrammarReference ref : entry.refs()) {
				if (value.equals(ref.value())) {
					count++;
					break; // count the file once
				}
			}
		}
		return count;
	}

	/**
	 * Returns the number of files currently indexed.
	 *
	 * <p><b>Use case:</b> Logged after a workspace scan completes —
	 * "WorkspaceIndex: indexed 342 files in 1 230 ms".
	 */
	public int size() {
		return entries.size();
	}

	/**
	 * Returns {@code true} when the initial workspace scan has completed and
	 * all indexed files are available for reference lookup.
	 *
	 * <p><b>Use case:</b> The catalog diagnostics participant waits for the scan
	 * to complete before reporting unused entries, to avoid false positives when
	 * the index is still being populated.
	 */
	public boolean isScanCompleted() {
		return scanCompleted;
	}

	/**
	 * Marks the initial workspace scan as completed.
	 *
	 * <p>Called by {@link WorkspaceIndexPlugin} once the async scan finishes.
	 * After this point, {@link #isScanCompleted()} returns {@code true} and
	 * diagnostics participants may report unused catalog entries.
	 */
	public void markScanCompleted() {
		scanCompleted = true;
		scanCompletedFuture.complete(null);
	}

	/**
	 * Returns a future that completes when the initial workspace scan finishes.
	 * Participants that depend on the index (e.g. reference search) can chain on
	 * this future so they naturally wait without blocking a thread.
	 */
	public CompletableFuture<Void> getScanCompletedFuture() {
		return scanCompletedFuture;
	}

	/**
	 * Drops all Bloom filter entries, effectively disabling reference lookup.
	 *
	 * <p><b>Use case:</b> The user sets {@code xml.workspaceIndex.enabled = false}
	 * in their VS Code settings. {@link WorkspaceIndexPlugin#doSave} calls
	 * {@code clear()} so no stale candidates are returned until the index is
	 * re-enabled and re-scanned.
	 */
	public void clear() {
		fileSeq.clear();
		entries.clear();
		partiallyIndexed.clear();
		scanCompleted = false;
		// Complete the old future so any in-flight waiters are unblocked (they will
		// see an empty index), then replace it for the next scan cycle.
		scanCompletedFuture.complete(null);
		scanCompletedFuture = new CompletableFuture<>();
	}

	/**
	 * Returns the subset of {@code entry.refs()} whose resolved path equals {@code targetPath}.
	 * Zero file I/O — all resolution is done via {@link Path#resolve(String)}.
	 *
	 * <p>Each {@link GrammarReference#value()} is resolved relative to the containing
	 * file's directory. References that cannot be parsed as a path (e.g. absolute URIs)
	 * are silently skipped. When no refs match, returns {@link Collections#emptyList()}
	 * to avoid allocating a list in the common (non-matching) case.
	 *
	 * <p>Shared by {@link #countReferencesTo} and {@link #getReferencesTo} to avoid
	 * duplicating the path-resolution loop.
	 */
	private static List<GrammarReference> matchingRefs(FileBloomEntry entry, Path targetPath) {
		Path entryPath = entry.path();
		if (entryPath == null) {
			return Collections.emptyList();
		}
		Path parent = entryPath.getParent();
		if (parent == null) {
			return Collections.emptyList();
		}
		List<GrammarReference> result = null;
		for (GrammarReference ref : entry.refs()) {
			try {
				if (parent.resolve(ref.value()).normalize().equals(targetPath)) {
					if (result == null) {
						result = new ArrayList<>();
					}
					result.add(ref);
				}
			} catch (Exception e) {
				// ref.value() is a URL or otherwise unresolvable as a path
			}
		}
		return result != null ? result : Collections.emptyList();
	}

	private static boolean isXmlFile(Path path) {
		return XML_EXTENSIONS.contains(getExtension(path));
	}

	private static boolean isGrammarFile(Path path) {
		return GRAMMAR_EXTENSIONS.contains(getExtension(path));
	}

	private static String getExtension(Path path) {
		String name = path.getFileName().toString().toLowerCase();
		int dot = name.lastIndexOf('.');
		if (dot < 0) {
			return "";
		}
		return name.substring(dot);
	}

	/**
	 * Reads at most {@code maxChars} characters from {@code path} and returns them as a String.
	 * If the file is shorter than {@code maxChars}, the entire file is returned.
	 * The stream is closed after reading {@code maxChars} characters — the rest of the file
	 * is never touched.
	 *
	 * <p><b>Why bounded?</b> Grammar references ({@code xml-model}, {@code DOCTYPE},
	 * {@code xsi:schemaLocation}, {@code xs:include}) always appear near the top of a file.
	 * Callers pass either:
	 * <ul>
	 *   <li>{@link #XML_HEAD_LIMIT} (64 KB) for XML data files — sufficient for any grammar
	 *       declaration at the document head.</li>
	 *   <li>{@link #grammarHeadLimit} (default 512 KB, configurable via
	 *       {@code xml.workspaceIndex.grammarHeadLimit}) for grammar files. The larger limit
	 *       covers cross-schema {@code xs:include} chains that may appear anywhere in an XSD.
	 *       Keeping it at or below the G1GC humongous-object threshold (~512 KB for a 1 MB
	 *       region) prevents the transient {@code char[]} allocation from bypassing young-gen
	 *       collection, avoiding OOM in workspaces with many large XSD files.</li>
	 * </ul>
	 *
	 * @param path     file to read.
	 * @param maxChars maximum number of characters to read.
	 * @return at most {@code maxChars} characters of the file content, never {@code null}.
	 * @throws IOException if the file cannot be opened or read.
	 */
	static String readHead(Path path, int maxChars) throws IOException {
		char[] buf = new char[maxChars];
		int read;
		try (Reader reader = Files.newBufferedReader(path)) {
			read = 0;
			int n;
			while (read < maxChars && (n = reader.read(buf, read, maxChars - read)) != -1) {
				read += n;
			}
		}
		return new String(buf, 0, read);
	}
}
