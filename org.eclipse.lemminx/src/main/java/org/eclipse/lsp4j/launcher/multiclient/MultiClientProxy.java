/**
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.lsp4j.launcher.multiclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Generic proxy that broadcasts LSP notifications to multiple connected
 * clients. For request methods (that return CompletableFuture), only the first
 * client is called.
 *
 * This class is generic and can be used with any LSP client interface.
 *
 * @param <T> the client interface type
 */
public class MultiClientProxy<T> implements InvocationHandler {

	private static final Logger LOGGER = Logger.getLogger(MultiClientProxy.class.getName());

	private final List<T> clients = new CopyOnWriteArrayList<>();
	private final Class<T> clientInterface;

	private MultiClientProxy(Class<T> clientInterface) {
		this.clientInterface = clientInterface;
	}

	/**
	 * Creates a proxy instance that broadcasts to multiple clients.
	 *
	 * @param <T>             the client interface type
	 * @param clientInterface the client interface class
	 * @return a proxy instance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> clientInterface) {
		MultiClientProxy<T> handler = new MultiClientProxy<>(clientInterface);
		return (T) Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class<?>[] { clientInterface },
				handler);
	}

	/**
	 * Adds a client to receive broadcasts.
	 */
	public void addClient(T client) {
		clients.add(client);
		System.err.println("[MultiClientProxy] Client added to " + clientInterface.getSimpleName() + ". Total clients: "
				+ clients.size());
	}

	/**
	 * Removes a client from broadcasts.
	 */
	public void removeClient(T client) {
		clients.remove(client);
		System.err.println("[MultiClientProxy] Client removed from " + clientInterface.getSimpleName()
				+ ". Total clients: " + clients.size());
	}

	/**
	 * Returns true if there are any connected clients.
	 */
	public boolean hasClients() {
		return !clients.isEmpty();
	}

	/**
	 * Returns the number of connected clients.
	 */
	public int getClientCount() {
		return clients.size();
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		Class<?> returnType = method.getReturnType();

		// For notification methods (void or CompletableFuture<Void>), broadcast to all
		// clients
		if (returnType == void.class || isVoidFuture(method)) {
			broadcastNotification(method, args);
			return returnType == void.class ? null : CompletableFuture.completedFuture(null);
		}

		// For request methods that expect a response, call only the first client
		if (!clients.isEmpty()) {
			return method.invoke(clients.get(0), args);
		}

		// No clients connected
		if (returnType == CompletableFuture.class) {
			return CompletableFuture.completedFuture(null);
		}
		return null;
	}

	private void broadcastNotification(Method method, Object[] args) {
		// Broadcast to all clients
		// Note: LSP4J proxies are async, so method.invoke() won't throw on disconnect
		// Cleanup happens via the launcher.startListening() Future callback
		for (T client : clients) {
			try {
				method.invoke(client, args);
			} catch (Throwable e) {
				// Should rarely happen since LSP4J is async, but log just in case
				System.err.println("[MultiClientProxy] Unexpected error broadcasting " + method.getName() +
					": " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Check if the exception indicates a connection error (client disconnected).
	 */
	private boolean isConnectionError(Exception e) {
		Throwable cause = e;
		while (cause != null) {
			String message = cause.getMessage();
			if (message != null) {
				// Check for various connection error patterns
				if (message.contains("connection was aborted") || message.contains("Connection reset")
						|| message.contains("Broken pipe") || message.contains("Stream closed")
						|| message.contains("An established connection was aborted")) {
					return true;
				}
			}
			cause = cause.getCause();
		}
		return false;
	}

	private boolean isVoidFuture(Method method) {
		if (method.getReturnType() != CompletableFuture.class) {
			return false;
		}
		// Check if it's CompletableFuture<Void> by method name convention
		// LSP notification methods typically start with these prefixes
		String name = method.getName();
		return name.startsWith("telemetry") || name.startsWith("publish") || name.startsWith("show")
				|| name.startsWith("log");
	}
}
