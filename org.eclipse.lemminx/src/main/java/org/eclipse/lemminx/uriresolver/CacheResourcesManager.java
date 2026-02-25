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
package org.eclipse.lemminx.uriresolver;

import static org.eclipse.lemminx.utils.ExceptionUtils.getRootCause;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.uriresolver.CacheResourceDownloadingException.CacheResourceDownloadingError;
import org.eclipse.lemminx.uriresolver.InvalidURIException.InvalidURIError;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.platform.Platform;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

/**
 * Cache resources manager.
 *
 */
public class CacheResourcesManager {

	private static final String USER_AGENT_KEY = "User-Agent";
	private static final String USER_AGENT_VALUE = "LemMinX/" + Platform.getVersion().getVersionNumber() + " ("
			+ Platform.getOS().getName() + " " + Platform.getOS().getVersion() + ")";

	private final Cache<String, CacheResourceDownloadedException> unavailableURICache;

	private final Cache<String, Boolean> forceDownloadExternalResources;

	private static final String CACHE_PATH = "cache";
	private static final Logger LOGGER = Logger.getLogger(CacheResourcesManager.class.getName());

	private static final Path TEMP_DOWNLOAD_DIR;

	static {
		Path tempDownloadDir = null;
		try {
			tempDownloadDir = Files.createTempDirectory("lemminx-temp");
		} catch (Exception e) {
		}
		TEMP_DOWNLOAD_DIR = tempDownloadDir;
	}

	private final Map<String, CompletableFuture<Path>> resourcesLoading;
	private boolean useCache;

	private boolean downloadExternalResources;

	private final Set<String> protocolsForCache;

	class ResourceInfo {

		String resourceURI;

		CompletableFuture<Path> future;

	}

	/**
	 * Classpath resource to deploy into the lemminx cache
	 */
	public static class ResourceToDeploy {

		private final Path resourceCachePath;
		private final String resourceFromClasspath;

		/**
		 * @param resourceURI           - used to compute the path to deploy the
		 *                              resource to in the lemminx cache. Generally this
		 *                              is the URL to the resource. Ex.
		 *                              https://www.w3.org/2007/schema-for-xslt20.xsd
		 * @param resourceFromClasspath - the classpath location of the resource to
		 *                              deploy to the lemminx cache
		 */
		public ResourceToDeploy(String resourceURI, String resourceFromClasspath) {
			this(URI.create(resourceURI), resourceFromClasspath);
		}

		/**
		 * @param resourceURI           - used to compute the path to deploy the
		 *                              resource to in the lemminx cache. Generally this
		 *                              is the URL to the resource. Ex.
		 *                              https://www.w3.org/2007/schema-for-xslt20.xsd
		 * @param resourceFromClasspath - the classpath location of the resource to
		 *                              deploy to the lemminx cache
		 */
		public ResourceToDeploy(URI resourceURI, String resourceFromClasspath) {
			this.resourceCachePath = Paths.get(CACHE_PATH, resourceURI.getScheme(), resourceURI.getHost(),
					resourceURI.getPath());
			this.resourceFromClasspath = resourceFromClasspath.startsWith("/") ? resourceFromClasspath
					: "/" + resourceFromClasspath;
		}

		/**
		 * @return The computed path in the lemmix cache that the resource will be
		 *         stored at
		 */
		public Path getDeployedPath() throws IOException {
			return FilesUtils.getDeployedPath(resourceCachePath);
		}

		/**
		 * @return The path to the resource on the classpath
		 */
		public String getResourceFromClasspath() {
			return resourceFromClasspath;
		}
	}

	public CacheResourcesManager() {
		this(CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.SECONDS).build());
	}

	CacheResourcesManager(Cache<String, CacheResourceDownloadedException> cache) {
		resourcesLoading = new HashMap<>();
		protocolsForCache = new HashSet<>();
		unavailableURICache = cache;
		forceDownloadExternalResources = CacheBuilder.newBuilder().maximumSize(100)
				.expireAfterWrite(30, TimeUnit.SECONDS).build();
		addDefaultProtocolsForCache();
		setDownloadExternalResources(true);
	}

	public Path getResource(final String resourceURI) throws IOException {
		Path resourceCachePath = getResourceCachePath(resourceURI);
		if (Files.exists(resourceCachePath)) {
			return resourceCachePath;
		}

		if (!isDownloadExternalResources() && !isForceDownloadExternalResource(resourceURI)) {
			throw new CacheResourceDownloadingException(resourceURI, resourceCachePath,
					CacheResourceDownloadingError.DOWNLOAD_DISABLED, null, null);
		}

		if (!FilesUtils.isIncludedInDeployedPath(resourceCachePath)) {
			throw new CacheResourceDownloadingException(resourceURI, resourceCachePath,
					CacheResourceDownloadingError.RESOURCE_NOT_IN_DEPLOYED_PATH, null, null);
		}

		CacheResourceDownloadedException cacheException = unavailableURICache.getIfPresent(resourceURI);
		if (cacheException != null) {
			// There were an error while downloading DTD, XSD schema, to avoid trying to
			// download it on each key stroke,
			// throw again the last cached exception
			throw cacheException;
		}

		CompletableFuture<Path> f = null;
		synchronized (resourcesLoading) {
			if (resourcesLoading.containsKey(resourceURI)) {
				CompletableFuture<Path> future = resourcesLoading.get(resourceURI);
				throw new CacheResourceDownloadingException(resourceURI, resourceCachePath,
						CacheResourceDownloadingError.RESOURCE_LOADING, future, null);
			}
			f = downloadResource(resourceURI, resourceCachePath);
			resourcesLoading.put(resourceURI, f);
		}

		if (f.getNow(null) == null) {
			throw new CacheResourceDownloadingException(resourceURI, resourceCachePath,
					CacheResourceDownloadingError.RESOURCE_LOADING, f, null);
		}

		return resourceCachePath;
	}

	private CompletableFuture<Path> downloadResource(final String resourceURI, Path resourceCachePath) {
		return CompletableFuture.supplyAsync(() -> {
			long start = System.currentTimeMillis();
			URLConnection conn = null;
			try {
				String actualURI = resourceURI;
				URL url = new URL(actualURI);
				String originalProtocol = url.getProtocol();
				if (!protocolsForCache.contains(formatProtocol(originalProtocol))) {
					throw new InvalidURIException(resourceURI, InvalidURIException.InvalidURIError.UNSUPPORTED_PROTOCOL,
							originalProtocol);
				}
				boolean isOriginalRequestSecure = isSecure(originalProtocol);
				LOGGER.info("Downloading " + resourceURI + " to " + resourceCachePath + "...");
				conn = url.openConnection();
				conn.setRequestProperty(USER_AGENT_KEY, USER_AGENT_VALUE);
				/* XXX: This should really be implemented using HttpClient or similar */
				int allowedRedirects = 5;
				while (conn.getHeaderField("Location") != null && allowedRedirects > 0) //$NON-NLS-1$
				{
					allowedRedirects--;
					url = new URL(actualURI = conn.getHeaderField("Location")); //$NON-NLS-1$
					String protocol = url.getProtocol();
					if (!protocolsForCache.contains(formatProtocol(protocol))) {
						throw new InvalidURIException(url.toString(),
								InvalidURIException.InvalidURIError.UNSUPPORTED_PROTOCOL, protocol);
					}
					if (isOriginalRequestSecure && !isSecure(protocol)) {
						throw new InvalidURIException(resourceURI,
								InvalidURIException.InvalidURIError.INSECURE_REDIRECTION, url.toString());
					}
					conn = url.openConnection();
					conn.setRequestProperty(USER_AGENT_KEY, USER_AGENT_VALUE);
				}

				// Download resource in a temporary file
				Path path = Files.createTempFile(TEMP_DOWNLOAD_DIR, resourceCachePath.getFileName().toString(), ".lemminx");
				try (ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
						FileOutputStream fos = new FileOutputStream(path.toFile())) {
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				}

				// Move the temporary file in the lemminx cache folder.
				Path dir = resourceCachePath.getParent();
				if (!Files.exists(dir)) {
					Files.createDirectories(dir);
				}
				Files.move(path, resourceCachePath);
				long elapsed = System.currentTimeMillis() - start;
				LOGGER.info("Downloaded " + resourceURI + " to " + resourceCachePath + " in " + elapsed + "ms");
			} catch (Exception e) {
				// Do nothing
				Throwable rootCause = getRootCause(e);
				String error = "[" + rootCause.getClass().getTypeName() + "] " + rootCause.getMessage();
				LOGGER.log(Level.SEVERE,
						"Error while downloading " + resourceURI + " to " + resourceCachePath + " : " + error);
				String httpResponseCode = getHttpResponseCode(conn);
				if (httpResponseCode != null) {
					error = error + " with code: " + httpResponseCode;
				}
				CacheResourceDownloadedException cacheException = new CacheResourceDownloadedException(resourceURI,
						resourceCachePath, error, e);
				unavailableURICache.put(resourceURI, cacheException);
				throw cacheException;
			} finally {
				synchronized (resourcesLoading) {
					resourcesLoading.remove(resourceURI);
				}
				if (conn != null && conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				}
			}
			return resourceCachePath;
		});
	}

	/**
	 * Returns the http response code from a url connection, null if code could not
	 * be retrived
	 *
	 * @param conn the URL connection
	 * @return the http response code from a url connection, null if code could not
	 *         be retrived
	 */
	private static String getHttpResponseCode(URLConnection conn) {
		if (conn != null && conn instanceof HttpURLConnection) {
			try {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				return (String.valueOf(httpConn.getResponseCode())) + " " + httpConn.getResponseMessage();
			} catch (IOException e) {
				// connection refused and no code could be retrived, do nothing
			}
		}
		return null;
	}

	private boolean isSecure(String protocol) {
		// really dumb way to check for secure protocol
		return "https".equals(protocol);
	}

	public static Path getResourceCachePath(String resourceURI) throws IOException {
		URI uri = null;
		try {
			uri = URI.create(resourceURI);
		} catch (Exception e) {
			throw new InvalidURIException(resourceURI, InvalidURIError.ILLEGAL_SYNTAX, e);
		}
		return getResourceCachePath(uri);
	}

	public static Path getResourceCachePath(URI uri) throws IOException {
		// Eliminate all path traversals
		URI normalizedUri = uri.normalize();

		// If there's any /../ left, we bail, as that looks like a malicious URI.
		if (normalizedUri.getPath().contains("/../")) {
			throw new InvalidURIException(uri.toString(), InvalidURIError.INVALID_PATH);
		}
		Path resourceCachePath = normalizedUri.getPort() > 0
				? Paths.get(CACHE_PATH, normalizedUri.getScheme(), normalizedUri.getHost(),
						String.valueOf(normalizedUri.getPort()), normalizedUri.getPath())
				: Paths.get(CACHE_PATH, normalizedUri.getScheme(), normalizedUri.getHost(), normalizedUri.getPath());
		return FilesUtils.getDeployedPath(resourceCachePath);
	}

	/**
	 * Try to get the cached {@link ResourceToDeploy#resourceCachePath} in cache
	 * file system and if it is not found, create the file with the given content of
	 * {@link ResourceToDeploy#resourceFromClasspath} stored in classpath.
	 *
	 * @param resource the resource to deploy if needed.
	 *
	 * @return the cached {@link ResourceToDeploy#resourceCachePath} in cache file
	 *         system.
	 * @throws IOException
	 */
	public static Path getResourceCachePath(ResourceToDeploy resource) throws IOException {
		Path outFile = resource.getDeployedPath();
		if (!outFile.toFile().exists()) {
			try (InputStream in = CacheResourcesManager.class
					.getResourceAsStream(resource.getResourceFromClasspath())) {
				FilesUtils.saveToFile(in, outFile);
			}
		}
		return outFile;
	}

	/**
	 * Returns <code>true</code> if cache is enabled and url comes from "http(s)" or
	 * "ftp" and <code>false</code> otherwise.
	 *
	 * @param url
	 * @return <code>true</code> if cache is enabled and url comes from "http(s)" or
	 *         "ftp" and <code>false</code> otherwise.
	 */
	public boolean canUseCache(String url) {
		return isUseCache() && isUseCacheFor(url);
	}

	/**
	 * Set <code>true</code> if cache must be used, <code>false</code> otherwise.
	 *
	 * @param useCache <code>true</code> if cache must be used, <code>false</code>
	 *                 otherwise.
	 */
	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	/**
	 * Returns <code>true</code> if cache must be used, <code>false</code>
	 * otherwise.
	 *
	 * @return <code>true</code> if cache must be used, <code>false</code>
	 *         otherwise.
	 */
	public boolean isUseCache() {
		return useCache;
	}

	/**
	 * Returns true if the external resources can be downloaded and false otherwise.
	 *
	 * @return true if the external resources can be downloaded and false otherwise.
	 */
	public boolean isDownloadExternalResources() {
		return downloadExternalResources;
	}

	/**
	 * Set true if the external resources can be downloaded and false otherwise.
	 *
	 * @param downloadExternalResources the external resources
	 */
	public void setDownloadExternalResources(boolean downloadExternalResources) {
		this.downloadExternalResources = downloadExternalResources;
	}

	/**
	 * Remove the cache directory (.lemminx/cache) if it exists.
	 *
	 * @throws IOException if the delete of directory (.lemminx/cache) cannot be
	 *                     done.
	 */
	public void evictCache() throws IOException {
		// Get the cache directory path
		Path cachePath = FilesUtils.getDeployedPath(Paths.get(CACHE_PATH));
		if (Files.exists(cachePath)) {
			// Remove the cache directory
			MoreFiles.deleteDirectoryContents(cachePath, RecursiveDeleteOption.ALLOW_INSECURE);
		}
	}

	/**
	 * Add protocol for using cache when url will start with the given protocol.
	 *
	 * @param protocol the protocol to add.
	 */
	public void addProtocolForCache(String protocol) {
		protocolsForCache.add(formatProtocol(protocol));
	}

	/**
	 * Remove protocol to avoid using cache when url will start with the given
	 * protocol.
	 *
	 * @param protocol the protocol to remove.
	 */
	public void removeProtocolForCache(String protocol) {
		protocolsForCache.remove(formatProtocol(protocol));
	}

	/**
	 * Add ':' separator if the given protocol doesn't contain it.
	 *
	 * @param protocol the protocol to format.
	 *
	 * @return the protocol concat with ':'.
	 */
	private static String formatProtocol(String protocol) {
		if (!protocol.endsWith(":")) {
			return protocol + ":";
		}
		return protocol;
	}

	/**
	 * Returns true if the cache must be used for the given url and false otherwise.
	 *
	 * @param url the url.
	 *
	 * @return true if the cache must be used for the given url and false otherwise.
	 */
	private boolean isUseCacheFor(String url) {
		if (StringUtils.isEmpty(url)) {
			return false;
		}
		for (String protocol : protocolsForCache) {
			if (url.startsWith(protocol)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add http, https, ftp protocol to use cache.
	 */
	private void addDefaultProtocolsForCache() {
		addProtocolForCache("http");
		addProtocolForCache("https");
		addProtocolForCache("ftp");
	}

	/**
	 * Force the given <code>url</code> to download.
	 *
	 * @param url the url to download.
	 */
	public void forceDownloadExternalResource(String url) {
		forceDownloadExternalResources.put(url, Boolean.TRUE);
	}

	/**
	 * Returns true if the given <code>url</code> can be downloaded and false
	 * otherwise.
	 *
	 * @param url the url to download.
	 * @return true if the given <code>url</code> can be downloaded and false
	 *         otherwise.
	 */
	private boolean isForceDownloadExternalResource(String url) {
		return forceDownloadExternalResources.getIfPresent(url) != null;
	}

	public void waitForDownload(String resourceUri) {
		CompletableFuture<Path> downloadedFile = resourcesLoading.get(resourceUri);
		if (downloadedFile != null) {
			try {
				downloadedFile.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
			}
		}
	}
}
