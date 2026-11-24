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
package org.eclipse.lemminx.extensions.contentmodel.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FileAssociationVariableSupport}.
 */
public class FileAssociationVariableSupportTest {

	// --- hasVariables ---

	@Test
	public void hasVariablesTrue() {
		assertTrue(VariableSupportUtils.hasVariables("${workspaceFolder}/data/*.xml"));
		assertTrue(VariableSupportUtils.hasVariables("${fileDirname}/schema.xsd"));
		assertTrue(VariableSupportUtils.hasVariables("${env:HOME}/file.xml"));
	}

	@Test
	public void hasVariablesFalse() {
		assertFalse(VariableSupportUtils.hasVariables("*.xml"));
		assertFalse(VariableSupportUtils.hasVariables("/home/user/file.xml"));
		assertFalse(VariableSupportUtils.hasVariables(null));
		assertFalse(VariableSupportUtils.hasVariables(""));
	}

	// --- File-level variables ---

	@Test
	public void resolveFileDirname() {
		URI docUri = getTestFileUri("project", "data", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${fileDirname}/*.xml", docUri, null);
		Path expected = getTestFilePath("project", "data");
		assertEquals(normalizePath(expected) + "/*.xml", result);
	}

	@Test
	public void resolveFileBasename() {
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${fileBasename}", docUri, null);
		assertEquals("test.xml", result);
	}

	@Test
	public void resolveFileBasenameNoExtension() {
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${fileBasenameNoExtension}", docUri, null);
		assertEquals("test", result);
	}

	@Test
	public void resolveFileExtname() {
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${fileExtname}", docUri, null);
		assertEquals(".xml", result);
	}

	@Test
	public void resolveFile() {
		URI docUri = getTestFileUri("project", "data", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${file}", docUri, null);
		Path expected = getTestFilePath("project", "data", "test.xml");
		assertEquals(normalizePath(expected), result);
	}

	// --- Workspace-level variables ---

	@Test
	public void resolveWorkspaceFolder() {
		URI docUri = getTestFileUri("project", "src", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolder}", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project")), result);
	}

	@Test
	public void resolveWorkspaceFolderBasename() {
		URI docUri = getTestFileUri("project", "src", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolderBasename}", docUri, wsFolders);
		assertEquals("project", result);
	}

	@Test
	public void resolveRelativeFile() {
		URI docUri = getTestFileUri("project", "src", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${relativeFile}", docUri, wsFolders);
		assertEquals("src/test.xml", result);
	}

	@Test
	public void resolveRelativeFileDirname() {
		URI docUri = getTestFileUri("project", "src", "main", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${relativeFileDirname}", docUri, wsFolders);
		assertEquals("src/main", result);
	}

	@Test
	public void resolveFileWorkspaceFolder() {
		URI docUri = getTestFileUri("project", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${fileWorkspaceFolder}", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project")), result);
	}

	// --- Multi-root workspace ---

	@Test
	public void resolveWorkspaceFolderMultiRoot() {
		URI docUri = getTestFileUri("project-b", "src", "test.xml");
		List<String> wsFolders = Arrays.asList(
				getTestFilePath("project-a").toUri().toString(),
				getTestFilePath("project-b").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolder}", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project-b")), result);
	}

	@Test
	public void resolveWorkspaceFolderFallsBackToFirst() {
		URI docUri = getTestFileUri("other-project", "test.xml");
		List<String> wsFolders = Arrays.asList(
				getTestFilePath("project-a").toUri().toString(),
				getTestFilePath("project-b").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolder}", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project-a")), result);
	}

	@Test
	public void resolveWorkspaceFolderDeepestMatch() {
		URI docUri = getTestFileUri("project", "sub", "deep", "test.xml");
		List<String> wsFolders = Arrays.asList(
				getTestFilePath("project").toUri().toString(),
				getTestFilePath("project", "sub").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolder}", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project", "sub")), result);
	}

	// --- Non-file URI ---

	@Test
	public void resolveEnvVariableWithNonFileUri() throws Exception {
		String pathValue = System.getenv("PATH");
		if (pathValue == null) {
			return;
		}
		URI nonFileUri = new java.net.URI("untitled:Untitled-1");
		String result = FileAssociationVariableSupport.resolve(
				"${env:PATH}/schema.xsd", nonFileUri, null);
		assertEquals(pathValue + "/schema.xsd", result);
	}

	@Test
	public void resolveFileVariableUnchangedWithNonFileUri() throws Exception {
		URI nonFileUri = new java.net.URI("untitled:Untitled-1");
		String result = FileAssociationVariableSupport.resolve(
				"${fileDirname}/*.xml", nonFileUri, null);
		assertEquals("${fileDirname}/*.xml", result);
	}

	// --- No variables ---

	@Test
	public void resolveNoVariables() {
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"**/*.xml", docUri, null);
		assertEquals("**/*.xml", result);
	}

	// --- Multiple variables ---

	@Test
	public void resolveMultipleVariables() {
		URI docUri = getTestFileUri("project", "src", "test.xml");
		List<String> wsFolders = Collections.singletonList(
				getTestFilePath("project").toUri().toString());
		String result = FileAssociationVariableSupport.resolve(
				"${workspaceFolder}/${fileBasenameNoExtension}.xsd", docUri, wsFolders);
		assertEquals(normalizePath(getTestFilePath("project")) + "/test.xsd", result);
	}

	// --- Environment variables ---

	@Test
	public void resolveEnvVariable() {
		String pathValue = System.getenv("PATH");
		if (pathValue == null) {
			return;
		}
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${env:PATH}", docUri, null);
		assertEquals(pathValue, result);
	}

	@Test
	public void resolveUnknownEnvVariable() {
		URI docUri = getTestFileUri("project", "test.xml");
		String result = FileAssociationVariableSupport.resolve(
				"${env:UNLIKELY_VAR_NAME_12345}", docUri, null);
		assertEquals("${env:UNLIKELY_VAR_NAME_12345}", result);
	}

	// --- File without extension ---

	@Test
	public void resolveFileWithoutExtension() {
		URI docUri = getTestFileUri("project", "Makefile");
		String result = FileAssociationVariableSupport.resolve(
				"${fileBasenameNoExtension}", docUri, null);
		assertEquals("Makefile", result);
	}

	@Test
	public void resolveFileExtnameWithoutExtension() {
		URI docUri = getTestFileUri("project", "Makefile");
		String result = FileAssociationVariableSupport.resolve(
				"${fileExtname}", docUri, null);
		assertEquals("", result);
	}

	// --- Helpers ---

	private static String normalizePath(Path path) {
		return path.toString().replace('\\', '/');
	}

	private static Path getTestFilePath(String... parts) {
		Path base = Paths.get(System.getProperty("java.io.tmpdir"), "lemminx-test");
		for (String part : parts) {
			base = base.resolve(part);
		}
		return base;
	}

	private static URI getTestFileUri(String... parts) {
		return getTestFilePath(parts).toUri();
	}
}
