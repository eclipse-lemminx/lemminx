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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures.FutureCancelChecker;

/**
 * The cache of {@link TextDocument} linked to a model.
 *
 * @author Angelo ZERR
 *
 * @param <T> the model type (ex : DOM Document)
 */
public class ModelTextDocuments<T> extends TextDocuments<ModelTextDocument<T>> {

	private final BiFunction<TextDocument, CancelChecker, T> parse;
	private final ExecutorService executorService;

	public ModelTextDocuments(BiFunction<TextDocument, CancelChecker, T> parse) {
		this(parse, null);
	}

	public ModelTextDocuments(BiFunction<TextDocument, CancelChecker, T> parse, ExecutorService executorService) {
		this.parse = parse;
		this.executorService = executorService;
	}

	@Override
	public ModelTextDocument<T> createDocument(TextDocumentItem document) {
		ModelTextDocument<T> doc = new ModelTextDocument<T>(document, parse);
		doc.setIncremental(isIncremental());
		return doc;
	}

	/**
	 * Returns the model of the given text document Uri and null otherwise.
	 *
	 * @param uri the text document uri.
	 *
	 * @return the model of the given text document Uri and null otherwise.
	 */
	public T getExistingModel(TextDocumentIdentifier documentIdentifier) {
		return getExistingModel(documentIdentifier.getUri());
	}

	/**
	 * Returns the model of the given text document Uri and null otherwise.
	 *
	 * @param uri the text document uri.
	 *
	 * @return the model of the given text document Uri and null otherwise.
	 */
	public T getExistingModel(String uri) {
		ModelTextDocument<T> document = get(uri);
		if (document != null) {
			return document.getExistingModel();
		}
		return null;
	}

	/**
	 * Returns the model of the given text document Uri and null otherwise.
	 *
	 * @param uri the text document uri.
	 *
	 * @return the model of the given text document Uri and null otherwise.
	 */
	public T getModel(TextDocumentIdentifier documentIdentifier) {
		return getModel(documentIdentifier.getUri());
	}

	/**
	 * Returns the model of the given text document Uri and null otherwise.
	 *
	 * @param uri the text document uri.
	 *
	 * @return the model of the given text document Uri and null otherwise.
	 */
	public T getModel(String uri) {
		ModelTextDocument<T> document = get(uri);
		if (document != null) {
			return document.getModel();
		}
		return null;
	}

	/**
	 * Get or parse the model and apply the code function which expects the model.
	 *
	 * @param <R>
	 * @param documentIdentifier the document indentifier.
	 * @param code               a bi function that accepts the parsedmodel and
	 *                           {@link CancelChecker} and returns the to be
	 *                           computed value
	 * @return the DOM Document for a given uri in a future and then apply the given
	 *         function.
	 */
	public <R> CompletableFuture<R> computeModelAsync(TextDocumentIdentifier documentIdentifier,
			BiFunction<T, CancelChecker, R> code) {
		if (executorService != null) {
			return computeAsyncWithExecutor(cancelChecker -> {
				// Get or parse the model.
				T model = getModel(documentIdentifier);
				if (model == null) {
					return null;
				}
				// Execute the code with the model
				return code.apply(model, cancelChecker);
			});
		}
		return CompletableFutures.computeAsync(cancelChecker -> {
			// Get or parse the model.
			T model = getModel(documentIdentifier);
			if (model == null) {
				return null;
			}
			// Apply the function code by using the parsed model.
			return code.apply(model, cancelChecker);
		});
	}

	/**
	 * Get or parse the model and apply the code function which expects the model.
	 *
	 * @param <R>
	 * @param documentIdentifier the document identifier.
	 * @param code               a bi function that accepts the parsed model and
	 *                           {@link CancelChecker} and returns as future the to
	 *                           be computed value
	 * @return the model for a given uri in a future and then apply the given
	 *         function.
	 */
	public <R> CompletableFuture<R> computeModelAsyncCompose(TextDocumentIdentifier documentIdentifier,
			BiFunction<T, CancelChecker, CompletableFuture<R>> code) {
		if (executorService != null) {
			return computeAsyncComposeWithExecutor(cancelChecker -> {
				// Get or parse the model.
				T model = getModel(documentIdentifier);
				if (model == null) {
					return CompletableFuture.completedFuture(null);
				}
				// Execute the code with the model
				return code.apply(model, cancelChecker);
			});
		}
		return computeAsyncCompose(cancelChecker -> {
			// Get or parse the model.
			T model = getModel(documentIdentifier);
			if (model == null) {
				return null;
			}
			cancelChecker.checkCanceled();
			// Apply the function code by using the parsed model.{
			return code.apply(model, cancelChecker);
		});
	}

	private <R> CompletableFuture<R> computeAsyncWithExecutor(Function<CancelChecker, R> code) {
		CompletableFuture<CancelChecker> start = new CompletableFuture<>();
		CompletableFuture<R> result = start.thenApplyAsync(code, executorService);
		start.complete(new FutureCancelChecker(result));
		return result;
	}

	private <R> CompletableFuture<R> computeAsyncComposeWithExecutor(Function<CancelChecker, CompletableFuture<R>> code) {
		CompletableFuture<CancelChecker> start = new CompletableFuture<>();
		CompletableFuture<R> result = start.thenComposeAsync(code, executorService);
		start.complete(new FutureCancelChecker(result));
		return result;
	}

	private static <R> CompletableFuture<R> computeAsyncCompose(Function<CancelChecker, CompletableFuture<R>> code) {
		CompletableFuture<CancelChecker> start = new CompletableFuture<>();
		CompletableFuture<R> result = start.thenComposeAsync(code);
		start.complete(new FutureCancelChecker(result));
		return result;
	}
}