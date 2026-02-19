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
 */
public class ModelTextDocument<T> extends TextDocument {

	private static final Logger LOGGER = Logger.getLogger(ModelTextDocument.class.getName());

	private final BiFunction<TextDocument, CancelChecker, T> parse;

	private T model;

	private final ModelUpdater<T> modelUpdater;

	private boolean incrementalModel;

	public ModelTextDocument(TextDocumentItem document, BiFunction<TextDocument, CancelChecker, T> parse,
			ModelUpdater<T> updateModel) {
		super(document);
		this.parse = parse;
		this.modelUpdater = updateModel;
	}

	public ModelTextDocument(String text, String uri, BiFunction<TextDocument, CancelChecker, T> parse) {
		super(text, uri);
		this.parse = parse;
		this.modelUpdater = null;
	}

	/**
	 * Returns the existing parsed model synchronized with last version of the text
	 * document and null otherwise.
	 * 
	 * @return the existing parsed model synchronized with last version of the text
	 *         document and null otherwise.
	 */
	public T getExistingModel() {
		return model;
	}

	/**
	 * Returns the parsed model synchronized with last version of the text document.
	 * 
	 * @return the parsed model synchronized with last version of the text document.
	 */
	public T getModel() {
		if (model == null) {
			return getSynchronizedModel();
		}
		return model;
	}

	/**
	 * Return the existing parsed model synchronized with last version of the text
	 * document or parse the model.
	 * 
	 * @return the existing parsed model synchronized with last version of the text
	 *         document or parse the model.
	 */
	private synchronized T getSynchronizedModel() {
		if (model != null) {
			return model;
		}
		int version = super.getVersion();
		long start = System.currentTimeMillis();
		try {
			LOGGER.fine("Start parsing of model with version '" + version);
			// Stop of parse process can be done when completable future is canceled or when
			// version of document changes
			CancelChecker cancelChecker = isIncrementalModel() ? ModelTextDocuments.NO_CANCELLABLE
					: new TextDocumentVersionChecker(this, version);
			// parse the model
			model = parse.apply(this, cancelChecker);
		} catch (CancellationException e) {
			LOGGER.fine("Stop parsing parsing of model with version '" + version + "' in "
					+ (System.currentTimeMillis() - start) + "ms");
			throw e;
		} finally {
			LOGGER.fine("End parse of model with version '" + version + "' in " + (System.currentTimeMillis() - start)
					+ "ms");
		}
		return model;
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		// text changed, cancel the completable future which load the model
		if (!isIncrementalModel()) {
			cancelModel();
		}
	}

	@Override
	public void setVersion(int version) {
		super.setVersion(version);
		// version changed, mark the model as dirty
		if (!isIncrementalModel()) {
			cancelModel();
		}
	}

	@Override
	public List<TextDocumentChange> update(List<TextDocumentContentChangeEvent> changes) {
		String oldText = super.getText();
		List<TextDocumentChange> result = super.update(changes);
		if (isIncrementalModel() && model != null && !result.isEmpty()) {
			updateModel(model, oldText, result);
		}
		return result;
	}

	private void updateModel(T model, String oldText, List<TextDocumentChange> changes) {
		modelUpdater.updateModel(model, changes, oldText);
	}

	/**
	 * Mark the model as dirty
	 */
	private void cancelModel() {
		model = null;
	}

	public boolean isIncrementalModel() {
		return incrementalModel;
	}

	public void setIncrementalModel(boolean incrementalModel) {
		this.incrementalModel = incrementalModel;
	}

}