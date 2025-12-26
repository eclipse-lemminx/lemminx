/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.commons;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Watches the parent process PID and invokes exit if it is no longer available.
 * This implementation waits for periods of inactivity to start querying the
 * PIDs.
 */
public final class ParentProcessWatcher implements Runnable, Function<MessageConsumer, MessageConsumer> {

	private static final Logger LOGGER = Logger.getLogger(ParentProcessWatcher.class.getName());

	/**
	 * Exit code returned when XML Language Server is forced to exit.
	 */
	private static final int FORCED_EXIT_CODE = 1;

	private static final long INACTIVITY_DELAY_SECS = 30 * 1000;
	private static final int POLL_DELAY_SECS = 10;
	private volatile long lastActivityTime;
	private final ProcessLanguageServer server;
	private ScheduledFuture<?> task;
	private ScheduledExecutorService service;

	public interface ProcessLanguageServer extends LanguageServer {

		long getParentProcessId();

		void exit(int exitCode);
	}

	public ParentProcessWatcher(ProcessLanguageServer server) {
		this.server = server;
		service = Executors.newScheduledThreadPool(1);
		task = service.scheduleWithFixedDelay(this, POLL_DELAY_SECS, POLL_DELAY_SECS, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		if (!parentProcessStillRunning()) {
			LOGGER.warning("Parent process stopped running, forcing server exit");
			task.cancel(true);
			server.exit(FORCED_EXIT_CODE);
		}
	}

	/**
	 * Checks whether the parent process is still running. If not, then we assume it
	 * has crashed, and we have to terminate the Java Language Server.
	 *
	 * @return true if the parent process is still running
	 */
	private boolean parentProcessStillRunning() {
		// Wait until parent process id is available
		final long pid = server.getParentProcessId();
		if (pid == 0 || lastActivityTime > (System.currentTimeMillis() - INACTIVITY_DELAY_SECS)) {
			return true;
		}
		Optional<ProcessHandle> optionalHandle = ProcessHandle.of(pid);
		return !optionalHandle.isEmpty() && optionalHandle.get().isAlive();
	}

	@Override
	public MessageConsumer apply(final MessageConsumer consumer) {
		//inject our own consumer to refresh the timestamp
		return message -> {
			lastActivityTime=System.currentTimeMillis();
			consumer.consume(message);
		};
	}
}
