/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Validate a given model document with delay.
 * 
 * @author Angelo ZERR
 *
 * @param <T>
 */
public class ModelValidatorDelayer<T> {

	private static final long DEFAULT_VALIDATION_DELAY_MS = 100;

	private final ScheduledExecutorService executorService;

	private final Consumer<ModelTextDocument<T>> validator;

	private final Map<String, Future<?>> pendingValidationRequests;

	private final long validationDelayMs;

	public ModelValidatorDelayer(Consumer<ModelTextDocument<T>> validator) {
		this(Executors.newScheduledThreadPool(2), validator, DEFAULT_VALIDATION_DELAY_MS);
	}

	public ModelValidatorDelayer(ScheduledExecutorService executorService, Consumer<ModelTextDocument<T>> validator,
			long validationDelayMs) {
		this.executorService = executorService;
		this.validator = validator;
		this.pendingValidationRequests = new HashMap<>();
		this.validationDelayMs = validationDelayMs;
	}

	/**
	 * Validate the given model <code>document</code> identified by the given
	 * <code>uri</code> with a delay.
	 * 
	 * @param uri      the document URI.
	 * @param document the document model to validate.
	 */
	public void validateWithDelay(ModelTextDocument<T> document) {
		String uri = document.getUri();
		cleanPendingValidation(uri);
		int version = document.getVersion();
		Future<?> request = executorService.schedule(() -> {
			synchronized (pendingValidationRequests) {
				pendingValidationRequests.remove(uri);
			}
			if (version == document.getVersion()) {
				validator.accept(document);
			}
		}, validationDelayMs, TimeUnit.MILLISECONDS);
		synchronized (pendingValidationRequests) {
			pendingValidationRequests.put(uri, request);
		}
	}

	public void cleanPendingValidation(String uri) {
		synchronized (pendingValidationRequests) {
			Future<?> request = pendingValidationRequests.get(uri);
			if (request != null) {
				request.cancel(true);
				pendingValidationRequests.remove(uri);
			}
		}
	}
}
