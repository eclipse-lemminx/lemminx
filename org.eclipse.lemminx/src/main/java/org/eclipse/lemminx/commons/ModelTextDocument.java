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
package org.eclipse.lemminx.commons;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * A {@link TextDocument} which is associate to a model loaded in async.
 * 
 * @author Angelo ZERR
 *
 * @param <T> the model type (ex : DOM Document)
 * @param <S> the scoped settings type (ex : CompositeSettings)
 */
public class ModelTextDocument<T, S> extends TextDocument {

	private static final Logger LOGGER = Logger.getLogger(ModelTextDocument.class.getName());

	private static final int DISPOSED_VERSION = -9999;

	private final BiFunction<TextDocument, CancelChecker, T> parse;

	/**
	 * A parsed model with the modification stamp of the text document it was
	 * parsed from.
	 */
	private static final class ParsedModel<T> {

		private final T model;

		private final int modificationStamp;

		ParsedModel(T model, int modificationStamp) {
			this.model = model;
			this.modificationStamp = modificationStamp;
		}
	}

	/**
	 * Incremented each time the text or the version of the document changes, so
	 * that a model parsed from an older content is never returned as the current
	 * one, even when the parse completes after the change. It is a plain field
	 * (not initialized) because the super constructor already calls
	 * {@link #setText(String)}. Updates come from document change notifications
	 * which are serialized by {@link TextDocuments}, and a lost update could never
	 * bring the stamp back to an older value.
	 */
	private volatile int modificationStamp;

	private volatile ParsedModel<T> model;

	private S settings;

	public ModelTextDocument(TextDocumentItem document, BiFunction<TextDocument, CancelChecker, T> parse) {
		super(document);
		this.parse = parse;
	}

	public ModelTextDocument(String text, String uri, BiFunction<TextDocument, CancelChecker, T> parse) {
		super(text, uri);
		this.parse = parse;
	}

	/**
	 * Returns the existing parsed model synchronized with last version of the text
	 * document and null otherwise.
	 * 
	 * @return the existing parsed model synchronized with last version of the text
	 *         document and null otherwise.
	 */
	public T getExistingModel() {
		return getUpToDateModel();
	}

	/**
	 * Returns the parsed model synchronized with last version of the text document.
	 * 
	 * @return the parsed model synchronized with last version of the text document.
	 */
	public T getModel() {
		if (isDisposed()) {
			return null;
		}
		T current = getUpToDateModel();
		if (current == null) {
			return getSynchronizedModel();
		}
		return current;
	}

	/**
	 * Returns the cached model if it was parsed from the current content of the
	 * text document and null otherwise.
	 * 
	 * @return the cached model if it was parsed from the current content of the
	 *         text document and null otherwise.
	 */
	private T getUpToDateModel() {
		ParsedModel<T> current = model;
		if (current != null && current.modificationStamp == modificationStamp) {
			return current.model;
		}
		return null;
	}

	/**
	 * Return the existing parsed model synchronized with last version of the text
	 * document or parse the model.
	 * 
	 * @return the existing parsed model synchronized with last version of the text
	 *         document or parse the model.
	 */
	private synchronized T getSynchronizedModel() {
		if (isDisposed()) {
			return null;
		}
		T current = getUpToDateModel();
		if (current != null) {
			return current;
		}
		// Read the stamp before the text is read by the parser: if the text changes
		// while parsing, the stamp no longer matches and the model is parsed again.
		int stamp = modificationStamp;
		int version = super.getVersion();
		long start = System.currentTimeMillis();
		try {
			LOGGER.fine("Start parsing of model with version '" + version);
			// Stop of parse process can be done when completable future is canceled or when
			// version of document changes
			CancelChecker cancelChecker = new TextDocumentVersionChecker(this, version);
			// parse the model
			current = parse.apply(this, cancelChecker);
			model = new ParsedModel<>(current, stamp);
		} catch (CancellationException e) {
			LOGGER.fine("Stop parsing parsing of model with version '" + version + "' in "
					+ (System.currentTimeMillis() - start) + "ms");
			throw e;
		} finally {
			LOGGER.fine("End parse of model with version '" + version + "' in " + (System.currentTimeMillis() - start)
					+ "ms");
		}
		return current;
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		// text changed, cancel the completable future which load the model
		cancelModel();
	}

	@Override
	public void update(List<TextDocumentContentChangeEvent> changes) {
		super.update(changes);
		// In incremental mode the text is updated in place without calling
		// setText(String), mark the model as dirty
		cancelModel();
	}

	@Override
	public void setVersion(int version) {
		super.setVersion(version);
		// version changed, mark the model as dirty
		cancelModel();
	}

	/**
	 * Mark the model as dirty
	 */
	private void cancelModel() {
		modificationStamp++;
		model = null;
	}

	public S getSettings() {
		return settings;
	}

	public void setSettings(S settings) {
		this.settings = settings;
	}

	public void clearSettings() {
		this.settings = null;
	}

	/**
	 * Dispose this document, preventing any further re-parsing and releasing
	 * all retained memory (model and text content) for garbage collection.
	 */
	@Override
	public void dispose() {
		// Setting an invalid version cancels any in-progress parse via
		// TextDocumentVersionChecker, and triggers cancelModel() which
		// releases the cached DOM tree.
		setVersion(DISPOSED_VERSION);
		clearSettings();
		super.dispose();
	}

}