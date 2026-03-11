/*******************************************************************************
* Copyright (c) 2018 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.uriresolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.utils.ExceptionUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class CacheResourcesManagerTest extends AbstractCacheBasedTest {

	private CacheResourcesManager cacheResourcesManager;

	private FileServer server;

	@BeforeEach
	public void setup() throws Exception {
		cacheResourcesManager = new CacheResourcesManager(testingCache());
		cacheResourcesManager.setUseCache(true);
	}

	@AfterEach
	public void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void testCanUseCache() {
		testCanUseCache(true);
		testCanUseCache(false);
	}

	private void testCanUseCache(boolean useCacheEnabled) {
		cacheResourcesManager.setUseCache(useCacheEnabled);
		assertEquals(useCacheEnabled, cacheResourcesManager.canUseCache("http://foo"));
		assertEquals(useCacheEnabled, cacheResourcesManager.canUseCache("ftp://foo"));
		assertEquals(useCacheEnabled, cacheResourcesManager.canUseCache("https://foo"));
		assertFalse(cacheResourcesManager.canUseCache("file:///foo"));
	}

	@Test
	public void testUnavailableCache() throws Exception {
		FileServer server = new FileServer();
		server.start();
		String uri = server.getUri("bad/url");
		try {
			cacheResourcesManager.getResource(uri);
			fail("cacheResourcesManager should be busy downloading the url");
		} catch (CacheResourceDownloadingException containsFuture) {
			try {
				containsFuture.getFuture().get(2, TimeUnit.SECONDS);
				fail("Download should have failed");
			} catch (ExecutionException failedDownload) {
				// Failed to download file, so exception is thrown
				if (!(failedDownload.getCause() instanceof CacheResourceDownloadedException)) {
					fail("Incorrect exception thrown during failed download");
				}
			}
			try {
				cacheResourcesManager.getResource(uri);
				fail("cacheResourcesManager should retrow CacheResourceDownloadedException");
			} catch (CacheResourceDownloadedException e) {
			}
		}
		TimeUnit.MILLISECONDS.sleep(200); // wait past the cache expiration date
		try {
			cacheResourcesManager.getResource(uri);
			fail("cacheResourcesManager should be busy re-downloading the url");
		} catch (CacheResourceDownloadingException ignored) {
		}
	}

	@Test
	public void testAvailableCache() throws Exception {
		FileServer server = new FileServer();
		server.start();
		String uri = server.getUri("/dtd/web-app_2_3.dtd");
		Path path = null;
		try {
			cacheResourcesManager.getResource(uri);
			fail("cacheResourcesManager should be busy downloading the url");
		} catch (CacheResourceDownloadingException containsFuture) {
			path = containsFuture.getFuture().get(2, TimeUnit.SECONDS);
		}
		assertNotNull(path);
		assertNotNull(cacheResourcesManager.getResource(uri));
		server.stop();
		TimeUnit.MILLISECONDS.sleep(200); // wait past the cache expiration date
		cacheResourcesManager.getResource(uri);
		// Manager should return cached content, even if server is offline
		assertNotNull(cacheResourcesManager.getResource(uri));
	}

	@Test
	public void testGetBadResourceName() throws Exception {
		String url = "http://localhost/foo/bar/`test.txt`";
		try {
			cacheResourcesManager.getResource(url);
			fail("Invalid url should fail to download");
		} catch (Exception e) {
			assertEquals(InvalidURIException.class, e.getClass());
			assertEquals(InvalidURIException.InvalidURIError.ILLEGAL_SYNTAX, ((InvalidURIException)e).getErrorCode());
		}
	}

	@Test
	public void testDirectoryTraversal() throws Exception {
		FileServer server = new FileServer();
		server.start();
		String uri = server.getUri("/dtd/web-app_2_3.dtd/../../xsd/choice.xsd");
		Path path = null;
		try {
			cacheResourcesManager.getResource(uri);
			fail("cacheResourcesManager should be busy downloading the url");
		} catch (CacheResourceDownloadingException containsFuture) {
			path = containsFuture.getFuture().get(2, TimeUnit.SECONDS);
		}
		assertEquals("choice.xsd",path.getFileName().toString());
		String choice = FilesUtils.readString(path);
		assertTrue(choice.contains("<xs:element name=\"person\">"), () -> {return "Unexpected file content:"+choice;});

		String invalidUri = server.getUri("/../../../xsd/choice.xsd");
		try {
			cacheResourcesManager.getResource(invalidUri);
			fail("Invalid url should fail to download");
		} catch (Exception e) {
			assertEquals(InvalidURIException.class, e.getClass());
			assertEquals(InvalidURIException.InvalidURIError.INVALID_PATH, ((InvalidURIException)e).getErrorCode());
		}
	}

	@Test
	public void testForbiddenRedirection() throws Exception {
		Handler redirectHandler = new AbstractHandler() {
			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request,
					HttpServletResponse response) throws IOException, ServletException {
				response.setHeader("Location", request.getParameter("redirect"));
			}

		};
		FileServer server = new FileServer(redirectHandler);
		server.start();
		String uri = server.getUri("/?redirect=file:///etc/password");
		try {
			cacheResourcesManager.getResource(uri);
			fail("cacheResourcesManager should be busy downloading the url");
		} catch (CacheResourceDownloadingException containsFuture) {
			try {
				containsFuture.getFuture().get(2, TimeUnit.SECONDS);
				fail("Download should have failed");
			} catch (ExecutionException failedDownload) {
				Throwable cause = failedDownload.getCause();
				assertEquals(CacheResourceDownloadedException.class, cause.getClass());
				Throwable rootCause = ExceptionUtils.getRootCause(cause);
				assertEquals(InvalidURIException.InvalidURIError.UNSUPPORTED_PROTOCOL, ((InvalidURIException)rootCause).getErrorCode());
				assertEquals("Unsupported 'file' protocol in 'file:/etc/password'", rootCause.getMessage());
			}
		}
	}

	private Cache<String, CacheResourceDownloadedException> testingCache() {
		return CacheBuilder.newBuilder().expireAfterWrite(100, TimeUnit.MILLISECONDS).maximumSize(1).build();
	}

}