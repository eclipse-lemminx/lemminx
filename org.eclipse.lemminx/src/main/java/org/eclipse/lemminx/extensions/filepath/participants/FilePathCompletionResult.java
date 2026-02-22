/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.filepath.participants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lemminx.utils.StringUtils;

/**
 * File path completion result information.
 */
public class FilePathCompletionResult {

	private static final Predicate<Character> isStartValidCharForSimplePath = (c) -> c != '/' && c != '\\';

	private final int startOffset;

	private final int endOffset;

	private final Path baseDir;

	public FilePathCompletionResult(int startOffset, int endOffset, Path baseDir) {
		super();
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.baseDir = baseDir;
	}

	/**
	 * Returns the start offset of the last path declared (ex : <a
	 * href="path/to/|file.xml").
	 * 
	 * @return the start offset of the last path declared.
	 */
	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * Returns the end offset of the last path declared (ex : <a
	 * href="path/to/file.xml|").
	 * 
	 * @return the end offset of the last path declared.
	 */
	public int getEndOffset() {
		return endOffset;
	}

	/**
	 * Returns the resolved base directory of the declared path.
	 * 
	 * <p>
	 * Ex : <a href="path/to/file.xml" />
	 * </p>
	 * 
	 * <p>
	 * the method will return 'file://C:/path/to'
	 * 
	 * </p>
	 * 
	 * @return the resolved base directory of the declared path.
	 */
	public Path getBaseDir() {
		return baseDir;
	}

	/**
	 * Create the file path completion result.
	 * 
	 * @param content          the xml content.
	 * @param fileUri          the file Uri.
	 * @param startNodeOffset  the start node offset where file path is declared.
	 * @param endNodeOffset    the end node offset where file path is declared.
	 * @param completionOffset the completion offset.
	 * @param separator        the separator used to declare multiple files and null
	 *                         otherwise.
	 * @return the file path completion result.
	 */
	public static FilePathCompletionResult create(CharSequence content, String fileUri, int startNodeOffset,
			int endNodeOffset, int completionOffset, Character separator) {
		boolean isMultiFilePath = separator != null;
		Predicate<Character> isStartValidChar = isStartValidCharForSimplePath;
		int endPathOffset = endNodeOffset;
		if (isMultiFilePath) {
			// multiple file path (ex : <a hrefs="path/to/file1.xml;path/to/file2.xml" />
			isStartValidChar = c -> c != separator && isStartValidCharForSimplePath.test(c);
 			endPathOffset = StringUtils.findEndWord(content, completionOffset -1, endNodeOffset, c -> c != separator);
			if (endPathOffset == -1) {
				endPathOffset = endNodeOffset;
			}
		}
		int startPathOffset = StringUtils.findStartWord(content, completionOffset, startNodeOffset, isStartValidChar);
		int startBaseDirOffset = startNodeOffset;
		if (isMultiFilePath && !isStartValidChar.test(content.charAt(startPathOffset - 1))) {
			// multiple file path (ex : <a hrefs="path/to/file1.xml;path/to/file2.xml" />
			int tmp = StringUtils.findStartWord(content, completionOffset, startNodeOffset, c -> c != separator);
			if (tmp != -1) {
				startBaseDirOffset = tmp;
			}
		}

		Path baseDir = getBaseDir(content, fileUri, startBaseDirOffset, startPathOffset);
		if (baseDir == null || !Files.exists(baseDir)) {
			baseDir = null;
		}
		return new FilePathCompletionResult(startPathOffset, endPathOffset, baseDir);
	}

	private static Path getBaseDir(CharSequence content, String fileUri, int start, int end) {
		if (end > start) {
			// ex : <a href="path/to/file.xml" />
			String basePath = content.subSequence(start, end).toString();
			if (!hasPathBeginning(basePath)) {
				// Try to returns the absolute path
				// Ex basePath=
				// - path/to/file.xml
				// - C://path/to/file.xml
				try {
					Path baseDir = FilesUtils.getPath(basePath);
					if (Files.exists(baseDir)) {
						// returns C://path/to
						return baseDir;
					}
				} catch (Exception e) {

				}
			}
			// Resolve the relative path by using the parent folder of the document file
			// Uri.:
			// Ex basePath=
			// - path/to/file.xml
			// - ./path/to/file.xml
			// - ../path/to/file.xml
			try {
				// returns C://path/to
				return FilesUtils.getPath(fileUri).getParent().resolve(basePath);
			} catch (Exception e) {
				return null;
			}
		}
		return FilesUtils.getPath(fileUri).getParent();
	}

	private static boolean hasPathBeginning(String currentText) {
		if (currentText.startsWith("/")
				|| currentText.startsWith("./")
				|| currentText.startsWith("../")
				|| currentText.startsWith("..\\")
				|| currentText.startsWith(".\\")) {
			return true;
		}
		return isAbsoluteWindowsPath(currentText);
	}

	private static boolean isAbsoluteWindowsPath(String currentText) {
		if (currentText.length() < 3) {
			return false;
		}
		if (!Character.isLetter(currentText.charAt(0))) {
			return false;
		}
		return currentText.charAt(1) == ':' && (currentText.charAt(2) == '\\' || currentText.charAt(2) == '/');
	}

}
