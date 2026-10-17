/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.utils;

import static java.io.File.separator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.utils.platform.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FilesUtilsTest
 */
public class FilesUtilsTest extends AbstractCacheBasedTest {

	@Test
	public void testFilesCachePathPreference() throws Exception {
		System.clearProperty(FilesUtils.LEMMINX_WORKDIR_KEY);
		String newBasePathString = System.getProperty("user.home");
		String newSubPathString = Paths.get("New", "Sub", "Path").toString();
		Path newSubPath = Paths.get(newSubPathString);
		String previousCachePathSetting = FilesUtils.getCachePathSetting();
		try {
			FilesUtils.setCachePathSetting(newBasePathString);
			Path finalPath = FilesUtils.getDeployedPath(newSubPath);
			assertEquals(Paths.get(newBasePathString, newSubPathString).toString(), finalPath.toString());
		} finally {
			// cachePathSetting is static: leaving it set would leak into every other test of the
			// JVM (tests run in random order).
			FilesUtils.setCachePathSetting(previousCachePathSetting);
		}
	}

	@Test
	public void normalizePathTest() {
		assertEquals(Paths.get(System.getProperty("user.home"), "Test", "Folder").toString(), FilesUtils.normalizePath("~/Test/Folder"));
		assertEquals(Paths.get(separator + "Test", "~", "Folder").toString(), FilesUtils.normalizePath("/Test/~/Folder"));
		assertEquals(Paths.get("~", "Test", "Folder").toString(), FilesUtils.normalizePath("./~/Test/Folder"));
		assertEquals(Paths.get(separator +  "Folder").toString(), FilesUtils.normalizePath("/Test/../Folder"));
		assertEquals(Paths.get(separator + "Users", "Nikolas").toString(), FilesUtils.normalizePath("\\Users\\Nikolas\\"));
	}

	@Test
	public void getFilePathSlashTest() {
		assertEquals("/", FilesUtils.getFilePathSlash("src/a/b/c"));
		assertEquals("\\", FilesUtils.getFilePathSlash("src\\a\\b\\c"));
		assertEquals("/", FilesUtils.getFilePathSlash("src"));
		assertEquals("/", FilesUtils.getFilePathSlash(""));
	}

	@Test
	public void cleanPathForWindows() {
		assertEquals("C:\\Users\\Home\\Documents", FilesUtils.convertToWindowsPath("\\C:\\Users\\Home\\Documents"));
		assertEquals("C:\\Users\\Home\\Documents\\", FilesUtils.convertToWindowsPath("\\C:\\Users\\Home\\Documents\\"));
		assertEquals("C:\\Users\\Home\\Documents\\", FilesUtils.convertToWindowsPath("/C:/Users/Home/Documents/"));
		assertEquals("C:\\Users\\Home\\Documents\\", FilesUtils.convertToWindowsPath("C:/Users/Home/Documents/"));
	}

	@Test
	public void saveFileAndRemoveExecutableFlag(@TempDir Path temporaryDirectory) throws IOException {
		Path tempFilePath = temporaryDirectory.resolve("file.xml");
		FilesUtils.saveToFile("<content />", tempFilePath);
		File tempFile = tempFilePath.toFile();
		assertTrue(tempFile.exists());
		assertTrue(tempFile.isFile());
		assertTrue(!tempFile.canExecute() || Platform.isWindows);
	}

	// ==========================================
	// getRelativeURI tests
	// ==========================================

	@Test
	public void getRelativeURISameDirectory(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("tag.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("tag.xml").toUri().toString();
		assertEquals("tag.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURISubDirectory(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("xsd/tag.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("tag.xml").toUri().toString();
		assertEquals("xsd/tag.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURIParentDirectory(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("tag.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("sub/tag.xml").toUri().toString();
		assertEquals("../tag.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURIWithSpaces(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("my schema.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("tag.xml").toUri().toString();
		assertEquals("my%20schema.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURIWithHash(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("schema#v1.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("tag.xml").toUri().toString();
		assertEquals("schema%23v1.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURIRegularFilename(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("foo.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("foo.xml").toUri().toString();
		assertEquals("foo.xsd", FilesUtils.getRelativeURI(target, base));
	}

	@Test
	public void getRelativeURIFallbackOnInvalidURI() {
		assertEquals("not-a-uri", FilesUtils.getRelativeURI("not-a-uri", "also-not-a-uri"));
	}

	@Test
	public void getRelativeURIWithPlusSign(@TempDir Path temporaryDirectory) {
		String target = temporaryDirectory.resolve("a+b.xsd").toUri().toString();
		String base = temporaryDirectory.resolve("tag.xml").toUri().toString();
		assertEquals("a+b.xsd", FilesUtils.getRelativeURI(target, base));
	}
}
