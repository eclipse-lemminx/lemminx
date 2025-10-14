/**
 *  Copyright (c) 2018 Angelo ZERR and Liferay Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *  Seiphon Wang <seiphon.wang@liferay.com>
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.uriresolver.CacheResourceDownloadingException;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.FileServer;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

public class DTDHoverExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void testTagHover() throws BadLocationException, MalformedURIException {
		String dtdURI = getDTDFileURI("liferay-service-builder_7_2_0.dtd");
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE service-builder PUBLIC \"-//Liferay//DTD Service Builder 7.2.0//EN\" \"http://www.liferay.com/dtd/liferay-service-builder_7_2_0.dtd\">"
				+ //
				"<service-builder dependency-injector=\"ds\" package-path=\"testSB\"></servi|ce-builder>";
		assertHover(xml, "The service-builder element is the root of the deployment descriptor for" + //
				" a Service Builder descriptor that is used to generate services available to" + //
				" portlets. The Service Builder saves the developer time by generating Spring" + //
				" utilities, SOAP utilities, and Hibernate persistence classes to ease the" + //
				" development of services." + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [liferay-service-builder_7_2_0.dtd](" + dtdURI + ")",
				r(1, 206, 1, 221));
	}

	@Test
	public void testAttributeNameHover() throws BadLocationException, MalformedURIException {
		String dtdURI = getDTDFileURI("liferay-service-builder_7_2_0.dtd");
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<!DOCTYPE service-builder PUBLIC \"-//Liferay//DTD Service Builder 7.2.0//EN\" \"http://www.liferay.com/dtd/liferay-service-builder_7_2_0.dtd\">"
				+ //
				"<service-builder dependency-injector=\"ds\" pa|ckage-path=\"testSB\"></service-builder>";
		assertHover(xml, "The package-path value specifies the package of the generated code." + //

				System.lineSeparator() + //
				System.lineSeparator() + "Source: [liferay-service-builder_7_2_0.dtd](" + dtdURI + ")",
				r(1, 182, 1, 194));
	}

	@Test
	public void webXML() throws Exception {
		FileServer server = new FileServer();
		try {
			server.start();
			// Delete the cached DTD file by waiting 1 sec
			String httpDTDUri = server.getUri("/dtd/web-app_2_3.dtd");
			Path cachedFilePath = CacheResourcesManager.getResourceCachePath(httpDTDUri);
			Files.deleteIfExists(cachedFilePath);

			CacheResourcesManager cacheResourcesManager = new CacheResourcesManager();
			try {
				cacheResourcesManager.getResource(httpDTDUri);
				fail("Expected file to be downloading");
			} catch (CacheResourceDownloadingException containsFuture) {
				// Wait for download to finish
				containsFuture.getFuture().get(30, TimeUnit.SECONDS);
			}
			assertTrue(Files.exists(cachedFilePath),
					"'" + cachedFilePath + "' file should be downloaded in the cache.");

			// Process hover with the DTD (http dtd)
			String dtdFileCacheURI = cachedFilePath.toUri().toString();//.replace("file:///", "file:/");
			String xml = "<!DOCTYPE web-app PUBLIC\n" + //
					" \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\n" + //
					" \"" + httpDTDUri + "\" >\n" + //
					"\n" + //
					"<web-a|pp>\n" + //
					"  <display-name>Servlet 2.3 Web Application</display-name>\n" + //
					"</web-app>";
			assertHoverWithCache(xml,
					"The web-app element is the root of the deployment descriptor for a web application." + //
							System.lineSeparator() + //
							System.lineSeparator() + "Source: [web-app_2_3.dtd](" + dtdFileCacheURI + ")",
					r(4, 1, 4, 8));
		} finally {
			server.stop();
		}
	}

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		XMLAssert.assertHover(new XMLLanguageService(), value, "src/test/resources/catalogs/catalog-liferay.xml", null,
				expectedHoverLabel, expectedHoverRange);
	}

	private static void assertHoverWithCache(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLAssert.assertHover(new XMLLanguageService(), value, null, null, expectedHoverLabel, expectedHoverRange,
				settings);
	}

	private static String getDTDFileURI(String dtdURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("dtd/" + dtdURI, "src/test/resources/test.xml", true).replace("///",
				"/");
	}
}
