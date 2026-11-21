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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.progress.ProgressMonitor;
import org.eclipse.lemminx.commons.progress.ProgressSupport;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.IReferenceParticipant;
import org.eclipse.lemminx.services.extensions.IWatchedFilesContext;
import org.eclipse.lemminx.services.extensions.IWorkspaceServiceParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lemminx.extensions.index.GrammarFileRenameSupport;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex.IndexProgressListener;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexCodeLensParticipant;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexReferenceParticipant;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.settings.AllXMLSettings;
import org.eclipse.lemminx.settings.InitializationOptionsSettings;
import org.eclipse.lemminx.settings.XMLGeneralClientSettings;
import org.eclipse.lemminx.settings.XMLWorkspaceIndexSettings;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileRename;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * LSP extension that owns the lifecycle of the workspace Bloom index.
 *
 * <p><b>Lifecycle:</b>
 * <pre>
 *   initialize  → start()                 — register participants, read initial settings
 *   initialized → initialized()           — trigger async workspace scan (if enabled)
 *   didSave / settings change → doSave()  — re-index open file or react to setting change
 *   file watcher → didChangeWatchedFiles()— incremental index update (Created/Changed/Deleted)
 *   willRenameFiles               — compute WorkspaceEdit for grammar references
 *   shutdown → stop()                     — unregister participants, clear index
 * </pre>
 *
 * <p><b>Enabled state:</b> The index is <em>disabled by default</em>
 * ({@code enabled = false}). It becomes active only when the client sends
 * {@code "xml.workspaceIndex.enabled": true} in {@code initializationOptions} or via a
 * subsequent {@code didChangeConfiguration}. This avoids the startup scan cost
 * for users who have not opted in.
 *
 * <p><b>One-read-per-file principle:</b> Each file is read at most once per
 * indexing event. The resulting {@link FileBloomEntry} stores both the
 * probabilistic {@link BloomFilter} (for O(1) candidate lookup) and the confirmed
 * {@link GrammarReference} list (for zero-I/O code lens counts). Code lens
 * refresh and Find References never re-read files from disk.
 *
 * <p>Loaded via ServiceLoader from
 * {@code META-INF/services/org.eclipse.lemminx.services.extensions.IXMLExtension}.
 *
 * @see WorkspaceIndex
 * @see WorkspaceIndexReferenceParticipant
 * @see WorkspaceIndexCodeLensParticipant
 * @see XMLWorkspaceIndexSettings
 */
public class WorkspaceIndexPlugin implements IXMLExtension, IWorkspaceServiceParticipant {

	private static final Logger LOGGER = Logger.getLogger(WorkspaceIndexPlugin.class.getName());

	private final WorkspaceIndex workspaceIndex;
	private final IReferenceParticipant referenceParticipant;
	private final IReferenceParticipant catalogReferenceParticipant;
	private final ICodeLensParticipant codeLensParticipant;

	private volatile boolean enabled = false;
	private volatile boolean clientInitialized;
	private XMLExtensionsRegistry registry;
	private List<WorkspaceFolder> workspaceFolders;
	private String rootUri;
	/** Catalog file paths last seen in settings; used to diff on settings change. */
	private volatile String[] currentCatalogPaths = new String[0];
	/** Directory names to skip during workspace scan; kept in sync with settings. */
	private volatile Set<String> currentExcludeDirectories =
			new HashSet<>(XMLWorkspaceIndexSettings.DEFAULT_EXCLUDE_DIRECTORIES);

	public WorkspaceIndexPlugin() {
		workspaceIndex = new WorkspaceIndex();
		referenceParticipant = new WorkspaceIndexReferenceParticipant(workspaceIndex,
				() -> this.registry != null ? this.registry.getDocumentProvider() : null);
		catalogReferenceParticipant = new WorkspaceIndexCatalogReferenceParticipant(workspaceIndex,
				() -> this.registry != null ? this.registry.getDocumentProvider() : null);
		codeLensParticipant = new WorkspaceIndexCodeLensParticipant(workspaceIndex);
	}

	/**
	 * Called once by the extension registry when the LSP {@code initialize} request
	 * is received. Registers the index participants and reads the initial
	 * {@code xml.workspaceIndex.enabled} setting from {@code initializationOptions}.
	 *
	 * <p><b>Use case:</b> VS Code starts with the LemMinX server and sends
	 * {@code initialize} with {@code "xml.workspaceIndex.enabled": true}.
	 * This method registers the reference and code-lens participants so they
	 * are active from the first request, and stores the workspace folders for
	 * the upcoming scan.
	 */
	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		this.registry = registry;
		registry.registerComponent(workspaceIndex);
		registry.registerReferenceParticipant(referenceParticipant);
		registry.registerReferenceParticipant(catalogReferenceParticipant);
		registry.registerCodeLensParticipant(codeLensParticipant);
		registry.registerWorkspaceServiceParticipant(this);

		// Read initial settings and workspace root from initialize params
		if (params != null) {
			workspaceFolders = params.getWorkspaceFolders();
			rootUri = params.getRootUri();

			Object initOptions = InitializationOptionsSettings.getSettings(params);
			Object xmlSettings = AllXMLSettings.getAllXMLSettings(initOptions);
			XMLGeneralClientSettings clientSettings = XMLGeneralClientSettings.getGeneralXMLSettings(xmlSettings);
			if (clientSettings != null && clientSettings.getWorkspaceIndex() != null) {
				XMLWorkspaceIndexSettings idxSettings = clientSettings.getWorkspaceIndex();
				enabled = idxSettings.isEnabled();
				List<String> excludeDirs = idxSettings.getExcludeDirectories();
				if (excludeDirs != null) {
					currentExcludeDirectories = new HashSet<>(excludeDirs);
				}
				workspaceIndex.setGrammarHeadLimit(idxSettings.getGrammarHeadLimit());
			}
			ContentModelSettings cmSettings = ContentModelSettings.getContentModelXMLSettings(xmlSettings);
			if (cmSettings != null) {
				currentCatalogPaths = cmSettings.getCatalogs();
			}
		}
	}

	/**
	 * Called when the client sends the {@code initialized} notification, signalling
	 * that it is ready to receive requests. Triggers the async workspace scan.
	 *
	 * <p><b>Use case:</b> After VS Code finishes loading LemMinX, it sends
	 * {@code initialized}. The plugin sets {@code clientInitialized = true} and,
	 * if the index is enabled, spawns a background thread that walks the workspace
	 * folders and builds Bloom filters. Once the scan completes, it sends
	 * {@code workspace/codeLens/refresh} so grammar files show up-to-date counts.
	 */
	@Override
	public void initialized(InitializedParams params, XMLExtensionsRegistry registry) {
		clientInitialized = true;
		if (!enabled) {
			return;
		}
		workspaceIndex.setContributors(registry.getGrammarReferenceContributors());
		triggerAsyncScan();
	}

	/**
	 * Reacts to document saves and workspace settings changes.
	 *
	 * <p><b>Use case — document saved:</b> The user edits {@code order.xml},
	 * adds {@code xsi:noNamespaceSchemaLocation="schema.xsd"}, then saves.
	 * The file is re-indexed immediately so subsequent
	 * {@link WorkspaceIndex#findReferencingFiles} calls include it.
	 * {@link org.eclipse.lemminx.services.extensions.ICodeLensRefresher#requestCodeLensRefresh()} is called only when
	 * {@link WorkspaceIndex#indexFile} returns {@code true} — i.e., the file
	 * actually contains grammar references. A plain XML with no declarations
	 * does not trigger an unnecessary client round-trip.
	 *
	 * <p><b>Use case — disable:</b> The user sets
	 * {@code "xml.workspaceIndex.enabled": false} in VS Code settings.
	 * The index is cleared immediately so no stale candidates are served.
	 *
	 * <p><b>Use case — re-enable:</b> The user toggles back to {@code true}.
	 * If the client is already initialized ({@code initialized} was already received),
	 * a fresh async scan is triggered and code lenses are refreshed. This avoids
	 * showing zero references after re-enabling.
	 *
	 * <p><b>Use case — catalog settings change:</b> The user adds or removes a catalog
	 * path in {@code xml.catalogs}. Added catalogs are indexed immediately so grammar
	 * files they reference appear in Find References / code lens. Removed catalogs are
	 * evicted from the index. A code lens refresh is requested when any catalog changes
	 * — reference counts on grammar files may have changed.
	 */
	@Override
	public void doSave(ISaveContext context) {
		if (context.getType() == ISaveContext.SaveContextType.DOCUMENT) {
			if (enabled) {
				String uri = context.getUri();
				// Only re-index files that are open in the editor (the user just saved them).
				// For closed files the workspace/didChangeWatchedFiles participant already
				// re-indexes them via didChangeWatchedFiles. Processing them here too would
				// cause each file to be read twice, triggering OOM on workspaces with many
				// large XSD files (the doSave loop in XMLWorkspaceService fires for every
				// file-change event regardless of whether the file is open).
				if (context.getDocument(uri) == null) {
					return;
				}
				try {
					Path path = FilesUtils.getPath(uri);
					// indexFile returns true only when grammar references were found;
					// no refresh is requested for plain XML files with no declarations.
					if (path != null && workspaceIndex.indexFile(path)) {
						context.requestCodeLensRefresh();
					}
				} catch (IOException | IllegalArgumentException e) {
					LOGGER.fine("WorkspaceIndex: failed to re-index on save " + uri + ": " + e.getMessage());
				}
			}
			return;
		}
		if (context.getType() != ISaveContext.SaveContextType.SETTINGS) {
			return;
		}
		// Note: context.getSettings() is already the xml-part of the settings
		// (AllXMLSettings extraction happens before SaveContext is created in XMLTextDocumentService).

		// Resolve the new catalog paths before any state change so updateCatalogPaths
		// can diff them against the still-current currentCatalogPaths.
		ContentModelSettings cmSettings = ContentModelSettings.getContentModelXMLSettings(context.getSettings());
		String[] newCatalogPaths = cmSettings != null ? cmSettings.getCatalogs() : currentCatalogPaths;

		boolean wasEnabled = enabled;
		XMLGeneralClientSettings clientSettings = XMLGeneralClientSettings.getGeneralXMLSettings(context.getSettings());
		if (clientSettings != null) {
			XMLWorkspaceIndexSettings newSettings = clientSettings.getWorkspaceIndex();
			if (newSettings != null) {
				enabled = newSettings.isEnabled();
				if (!enabled) {
					workspaceIndex.clear();
				}
				List<String> excludeDirs = newSettings.getExcludeDirectories();
				currentExcludeDirectories = excludeDirs != null
						? new HashSet<>(excludeDirs)
						: new HashSet<>(XMLWorkspaceIndexSettings.DEFAULT_EXCLUDE_DIRECTORIES);
				workspaceIndex.setGrammarHeadLimit(newSettings.getGrammarHeadLimit());
			}
		}

		if (!wasEnabled && enabled && clientInitialized) {
			// Re-enabled: apply catalog path changes then trigger a full fresh scan
			// that covers workspace folders and the updated catalog paths.
			currentCatalogPaths = newCatalogPaths;
			workspaceIndex.setContributors(registry.getGrammarReferenceContributors());
			triggerAsyncScan();
			return;
		}

		// Incremental catalog update when already enabled (or just disabled).
		// Even when disabled, evict removed catalogs so stale entries don't accumulate.
		boolean catalogsChanged = updateCatalogPaths(newCatalogPaths);
		if (catalogsChanged) {
			context.requestCodeLensRefresh();
		}
	}

	/**
	 * Diffs old vs new catalog paths: evicts removed entries from the index and
	 * indexes newly added files. Returns {@code true} when any change occurred
	 * (so the caller can decide whether to refresh code lenses).
	 */
	private boolean updateCatalogPaths(String[] newPaths) {
		Set<String> oldSet = new HashSet<>(Arrays.asList(currentCatalogPaths));
		Set<String> newSet = new HashSet<>(Arrays.asList(newPaths));
		if (oldSet.equals(newSet)) {
			return false;
		}
		// Evict catalogs that were removed from settings — but only when they lie
		// outside every workspace root. If the file is still under a workspace folder
		// it was already indexed by the directory scan and must stay in the index.
		for (String removed : oldSet) {
			if (!newSet.contains(removed)) {
				try {
					Path path = FilesUtils.getPath(removed);
					if (path != null && !isUnderWorkspaceRoot(path)) {
						workspaceIndex.removeFile(path);
					}
				} catch (IllegalArgumentException e) {
					LOGGER.fine("WorkspaceIndex: failed to evict catalog " + removed + ": " + e.getMessage());
				}
			}
		}
		// Index catalogs that were added to settings (only when enabled)
		if (enabled) {
			for (String added : newSet) {
				if (!oldSet.contains(added)) {
					try {
						Path path = FilesUtils.getPath(added);
						if (path != null) {
							workspaceIndex.indexFile(path);
						}
					} catch (IOException | IllegalArgumentException e) {
						LOGGER.fine("WorkspaceIndex: failed to index catalog " + added + ": " + e.getMessage());
					}
				}
			}
		}
		currentCatalogPaths = newPaths;
		return true;
	}

	/**
	 * Updates the tracked folder list and triggers an incremental scan for newly
	 * added folders.
	 *
	 * <p><b>Use case:</b> The user adds a second root folder
	 * ({@code /home/user/lib}) to the VS Code workspace via "Add Folder to Workspace".
	 * VS Code sends {@code workspace/didChangeWorkspaceFolders} with {@code added=[lib]}.
	 * This method appends {@code lib} to the tracked folders and launches an async
	 * scan covering only the added folder so the existing index is not rebuilt.
	 * Deleted folders have their files evicted by the subsequent file-watcher events.
	 */
	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		if (params.getEvent() == null) {
			return;
		}
		List<WorkspaceFolder> removed = params.getEvent().getRemoved();
		if (removed != null) {
			List<WorkspaceFolder> current = this.workspaceFolders;
			if (current != null) {
				List<WorkspaceFolder> updated = new ArrayList<>(current);
				updated.removeAll(removed);
				this.workspaceFolders = updated;
			}
		}
		List<WorkspaceFolder> added = params.getEvent().getAdded();
		if (added != null && !added.isEmpty() && enabled && clientInitialized) {
			ProgressSupport progressSupport = registry.getProgressSupport();
			List<WorkspaceFolder> current = this.workspaceFolders;
			List<WorkspaceFolder> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
			updated.addAll(added);
			this.workspaceFolders = updated;
			Set<String> excludedDirs = this.currentExcludeDirectories;
			CompletableFuture.runAsync(() -> {
				scanFolders(added, progressSupport, excludedDirs);
				workspaceIndex.markScanCompleted();
				registry.refreshCodeLenses(WorkspaceIndexReferenceParticipant::isGrammarFile);
			});
		}
	}

	/**
	 * Keeps the index in sync with file system changes reported by the client.
	 *
	 * <p><b>Use case — file copied / created:</b> The user copies or creates an XML file
	 * {@code shipping.xml} that contains {@code xsi:noNamespaceSchemaLocation="schema.xsd"}.
	 * The file watcher fires a {@code Created} event; the plugin re-indexes the file so
	 * it appears in future {@link WorkspaceIndex#findReferencingFiles} calls.
	 * {@link org.eclipse.lemminx.services.extensions.ICodeLensRefresher#requestCodeLensRefresh()} is called only when
	 * {@link WorkspaceIndex#indexFile} returns {@code true} — i.e., the file contains
	 * at least one grammar reference. Creating a plain XML file with no declarations
	 * does not trigger an unnecessary client round-trip.
	 * The registry sends exactly one {@code workspace/codeLens/refresh} after all
	 * participants finish, regardless of how many files triggered a refresh request
	 * (same N-to-1 pattern as {@link org.eclipse.lemminx.services.extensions.ICodeLensRefresher#requestCodeLensRefresh()}).
	 *
	 * <p><b>Use case — file deleted:</b> The user deletes {@code old.xml}. The plugin
	 * removes its Bloom entry even when the index is disabled, so stale entries
	 * do not accumulate.
	 */
	@Override
	public void didChangeWatchedFiles(List<FileEvent> changes, IWatchedFilesContext context) {
		for (FileEvent change : changes) {
			String uri = change.getUri();
			try {
				Path path = FilesUtils.getPath(uri);
				if (path == null) {
					continue;
				}
				if (change.getType() == FileChangeType.Deleted) {
					workspaceIndex.removeFile(path);
					// A deleted file may have been the last referencing file for a catalog entry,
					// so both code lenses and catalog "unused" diagnostics need refreshing.
					context.requestCodeLensRefresh();
					context.requestDiagnosticsRefresh();
				} else if (enabled && workspaceIndex.indexFile(path)) {
					// A newly indexed file may reference a catalog entry that was previously
					// unused, so both code lenses and catalog "unused" diagnostics need refreshing.
					context.requestCodeLensRefresh();
					context.requestDiagnosticsRefresh();
				}
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.fine("WorkspaceIndex: failed to update entry for " + uri + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Computes text edits to keep grammar references consistent when files are renamed.
	 *
	 * <p><b>Use case:</b> The user renames {@code schemas/order.xsd} to
	 * {@code schemas/order-v2.xsd} via the VS Code file explorer. Before the rename
	 * is applied, VS Code sends {@code workspace/willRenameFiles}. This method
	 * delegates to {@link GrammarFileRenameSupport} for each renamed file and merges
	 * the resulting {@code TextEdit}s into a single {@link WorkspaceEdit}.
	 * When multiple grammar files are renamed in one operation, all edits are
	 * accumulated into a single map (each XML file appears at most once per key).
	 *
	 * @return the merged edit, or {@code null} if none of the renamed files are
	 *         referenced by any indexed XML file.
	 */
	@Override
	public WorkspaceEdit willRenameFiles(RenameFilesParams params, CancelChecker cancelChecker) {
		Map<String, List<TextEdit>> allChanges = new HashMap<>();
		for (FileRename rename : params.getFiles()) {
			cancelChecker.checkCanceled();
			// Only grammar files (XSD, DTD, RNG, RNC) can be referenced by other XML
			// files as grammars. Renaming an XML instance file or any other file type
			// cannot affect grammar references, so skip immediately to avoid an O(N)
			// Bloom filter scan plus disk reads for partially-indexed large files.
			if (!WorkspaceIndexReferenceParticipant.isGrammarFile(rename.getOldUri())) {
				continue;
			}
			Path oldPath = FilesUtils.getPathOrNull(rename.getOldUri());
			Path newPath = FilesUtils.getPathOrNull(rename.getNewUri());
			if (oldPath == null || newPath == null) {
				continue;
			}
			IXMLDocumentProvider docProvider = registry.getDocumentProvider();
			Function<String, String> openDocContent = docProvider != null
					? uri -> openDocumentContent(docProvider, uri)
					: null;
			WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(workspaceIndex, oldPath, newPath,
					openDocContent);
			if (edit != null && edit.getChanges() != null) {
				edit.getChanges().forEach((uri, edits) ->
					allChanges.computeIfAbsent(uri, k -> new ArrayList<>()).addAll(edits)
				);
			}
		}
		if (allChanges.isEmpty()) {
			return null;
		}
		WorkspaceEdit result = new WorkspaceEdit();
		result.setChanges(allChanges);
		return result;
	}


	/**
	 * Unregisters all participants and clears the index on server shutdown.
	 *
	 * <p><b>Use case:</b> VS Code sends {@code shutdown} then {@code exit}.
	 * The extension registry calls {@code stop} on each extension. This method
	 * removes the reference and code-lens participants so no dangling callbacks
	 * remain, and clears the Bloom index to release memory before the JVM exits.
	 */
	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.unregisterReferenceParticipant(referenceParticipant);
		registry.unregisterReferenceParticipant(catalogReferenceParticipant);
		registry.unregisterCodeLensParticipant(codeLensParticipant);
		registry.unregisterWorkspaceServiceParticipant(this);
		workspaceIndex.clear();
	}

	/**
	 * Spawns a background thread that scans all workspace folders plus any catalog
	 * files listed in {@code xml.catalogs}, then signals completion.
	 *
	 * <p><b>Use case:</b> Called from {@link #initialized} and from {@link #doSave}
	 * when the index is re-enabled after being disabled. The scan walks every
	 * XML-family file under the workspace root (skipping {@link #currentExcludeDirectories}),
	 * reads a bounded head from each file, and builds a {@link FileBloomEntry}.
	 * When the scan finishes, {@link WorkspaceIndex#markScanCompleted()} unblocks any
	 * participant that was waiting on the scan future, and
	 * {@link XMLExtensionsRegistry#refreshCodeLenses} triggers a
	 * {@code workspace/codeLens/refresh} so grammar files show up-to-date counts.
	 */
	private void triggerAsyncScan() {
		List<WorkspaceFolder> folders = this.workspaceFolders;
		String root = this.rootUri;
		String[] catalogPaths = this.currentCatalogPaths;
		Set<String> excludedDirs = this.currentExcludeDirectories;
		ProgressSupport progressSupport = registry.getProgressSupport();
		CompletableFuture.runAsync(() -> {
			if (folders != null && !folders.isEmpty()) {
				scanFolders(folders, progressSupport, excludedDirs);
			} else if (root != null) {
				scanSingle(root, progressSupport, excludedDirs);
			}
			// Catalog files from xml.catalogs settings may live outside the workspace
			// folders (absolute paths); index them separately after the workspace scan.
			indexCatalogFiles(catalogPaths);
			workspaceIndex.markScanCompleted();
			registry.refreshCodeLenses(WorkspaceIndexReferenceParticipant::isGrammarFile);
		});
	}

	/**
	 * Indexes each catalog file listed in {@code xml.catalogs} settings.
	 * Catalog files reference grammar files via OASIS {@code uri} attributes;
	 * they may reside outside the workspace and would be missed by the directory scan.
	 */
	private void indexCatalogFiles(String[] catalogPaths) {
		for (String catalogUri : catalogPaths) {
			try {
				Path path = FilesUtils.getPath(catalogUri);
				if (path != null) {
					workspaceIndex.indexFile(path);
				}
			} catch (IOException | IllegalArgumentException e) {
				LOGGER.fine("WorkspaceIndex: failed to index catalog " + catalogUri + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Returns {@code true} when {@code path} is a descendant of at least one
	 * workspace folder or the root URI. Used to avoid evicting catalog files that
	 * are already covered by the workspace directory scan.
	 */
	private boolean isUnderWorkspaceRoot(Path path) {
		Path normalized = path.normalize();
		List<WorkspaceFolder> folders = this.workspaceFolders;
		if (folders != null) {
			for (WorkspaceFolder folder : folders) {
				Path folderPath = FilesUtils.getPathOrNull(folder.getUri());
				if (folderPath != null && normalized.startsWith(folderPath.normalize())) {
					return true;
				}
			}
		}
		String root = this.rootUri;
		if (root != null) {
			Path rootPath = FilesUtils.getPathOrNull(root);
			if (rootPath != null && normalized.startsWith(rootPath.normalize())) {
				return true;
			}
		}
		return false;
	}

	// --- scan helpers (moved from XMLLanguageServer) ---

	private void scanFolders(List<WorkspaceFolder> folders, ProgressSupport progressSupport,
			Set<String> excludedDirs) {
		long start = System.currentTimeMillis();
		ProgressMonitor monitor = progressSupport != null ? progressSupport.createProgressMonitor() : null;
		if (monitor != null) {
			monitor.begin("Indexing XML workspace", null, null, false);
		}
		for (WorkspaceFolder folder : folders) {
			scanUri(folder.getUri(), monitor, excludedDirs);
		}
		logAndEnd(monitor, start);
	}

	private void scanSingle(String uri, ProgressSupport progressSupport, Set<String> excludedDirs) {
		long start = System.currentTimeMillis();
		ProgressMonitor monitor = progressSupport != null ? progressSupport.createProgressMonitor() : null;
		if (monitor != null) {
			monitor.begin("Indexing XML workspace", null, null, false);
		}
		scanUri(uri, monitor, excludedDirs);
		logAndEnd(monitor, start);
	}

	private void scanUri(String uri, ProgressMonitor monitor, Set<String> excludedDirs) {
		try {
			Path path = FilesUtils.getPath(uri);
			IndexProgressListener listener = monitor != null ? buildProgressListener(monitor) : null;
			workspaceIndex.scanDirectory(path, listener, excludedDirs);
		} catch (IOException | IllegalArgumentException e) {
			LOGGER.warning("WorkspaceIndex: failed to scan " + uri + ": " + e.getMessage());
		}
	}

	private void logAndEnd(ProgressMonitor monitor, long start) {
		int total = workspaceIndex.size();
		long elapsed = System.currentTimeMillis() - start;
		LOGGER.info("WorkspaceIndex: indexed " + total + " file" + (total != 1 ? "s" : "") + " in " + elapsed + " ms");
		if (monitor != null) {
			monitor.end("Workspace index ready (" + total + " file" + (total != 1 ? "s" : "") + " indexed)");
		}
	}

	private static IndexProgressListener buildProgressListener(ProgressMonitor monitor) {
		return (indexed, total, file) -> {
			if (total == 0) {
				// Discovery phase: indexed = number of XML files found so far.
				// Null percentage → indeterminate spinner; message updates every 100 files.
				monitor.report("Collecting XML files... (" + indexed + " found)", null, false);
				return;
			}
			// Indexing phase: 0% → 100%
			int step = Math.max(1, total / 20);
			if (indexed % step == 0 || indexed == total) {
				int percentage = (indexed * 100) / total;
				monitor.report("Indexed " + indexed + " / " + total + " files", percentage, false);
			}
		};
	}

	/**
	 * Returns the in-memory content of the given URI if it is currently open in the
	 * editor, or {@code null} if the file is not open.
	 *
	 * <p>Direct URI lookup is tried first. If it fails (LSP clients differ in URI
	 * format — VS Code sends lowercase drive letters, LSP4E uppercase, and
	 * {@link java.nio.file.Path#toUri()} also produces uppercase on Windows), a
	 * path-based fallback iterates all open documents and compares via
	 * {@link java.nio.file.Path#equals}, which is case-insensitive on Windows.
	 *
	 * @param docProvider the document provider.
	 * @param uri         URI of the candidate file (from the workspace index).
	 * @return the live document text, or {@code null} if the file is not open.
	 */
	static String openDocumentContent(IXMLDocumentProvider docProvider, String uri) {
		DOMDocument d = docProvider.getDocument(uri);
		if (d != null) {
			return d.getText();
		}
		// Path-based fallback: handles URI format mismatches across LSP clients.
		// Path.equals() is case-insensitive on Windows, so drive-letter case differences
		// (file:///C:/... vs file:///c:/...) are handled transparently.
		Path path = FilesUtils.getPathOrNull(uri);
		if (path != null) {
			for (DOMDocument doc : docProvider.getAllDocuments()) {
				if (path.equals(FilesUtils.getPathOrNull(doc.getDocumentURI()))) {
					return doc.getText();
				}
			}
		}
		return null;
	}
}
