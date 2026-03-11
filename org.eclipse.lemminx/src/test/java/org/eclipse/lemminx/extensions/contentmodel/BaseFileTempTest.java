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
package org.eclipse.lemminx.extensions.contentmodel;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.eclipse.lemminx.AbstractCacheBasedTest;

public abstract class BaseFileTempTest extends AbstractCacheBasedTest {

	protected static void createFile(String fileName, String contents) throws IOException {
		URI fileURI = new File(fileName).toURI();
		createFile(fileURI, contents);
	}

	protected static void createFile(URI fileURI, String contents) throws IOException {
		Path path = Paths.get(fileURI);
		Files.write(path, contents.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	protected static void updateFile(String fileName, String contents) throws IOException {
		URI fileURI = new File(fileName).toURI();
		updateFile(fileURI, contents);
	}
	protected static void updateFile(URI fileURI, String contents) throws IOException {
		createFile(fileURI, contents);
		// Set the last modified time 2 seconds into the future to ensure
		// Files.getLastModifiedTime detects the change (filesystem timestamp
		// resolution on macOS/Linux is 1 second).
		Path path = Paths.get(fileURI);
		Files.setLastModifiedTime(path, FileTime.from(Instant.now().plusSeconds(2)));
	}

	protected Path getTempDirPath() {
		return testWorkDirectory;
	}
}
