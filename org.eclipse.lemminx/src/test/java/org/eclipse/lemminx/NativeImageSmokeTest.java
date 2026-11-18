/*******************************************************************************
* Copyright (c) 2026 Aleksandar Kurtakov and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Aleksandar Kurtakov - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * End-to-end smoke test of a LemMinX binary (e.g. the GraalVM native image)
 * over stdio. Only runs when <code>lemminx.binary</code> points to the binary:
 *
 * <pre>
 * ./mvnw surefire:test -pl org.eclipse.lemminx -Dtest=NativeImageSmokeTest -Dlemminx.binary=target/lemminx-...
 * </pre>
 */
@EnabledIfSystemProperty(named = "lemminx.binary", matches = ".+")
public class NativeImageSmokeTest {

	@Test
	public void smokeTest() throws Exception {
		Process process = new ProcessBuilder(System.getProperty("lemminx.binary"))
				.redirectError(ProcessBuilder.Redirect.INHERIT).start();
		try {
			CompletableFuture<PublishDiagnosticsParams> diagnostics = new CompletableFuture<>();
			MockXMLLanguageClient client = new MockXMLLanguageClient() {
				@Override
				public void publishDiagnostics(PublishDiagnosticsParams params) {
					if (params.getDiagnostics().stream()
							.anyMatch(d -> d.getCode() != null && "ETagRequired".equals(d.getCode().get()))) {
						diagnostics.complete(params);
					}
				}
			};
			Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, process.getInputStream(),
					process.getOutputStream());
			launcher.startListening();
			LanguageServer server = launcher.getRemoteProxy();

			server.initialize(new InitializeParams()).get(60, SECONDS);
			server.initialized(new InitializedParams());
			// the XML parser must report the unclosed <child> element
			server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
					new TextDocumentItem("file:///test.xml", "xml", 1, "<root>\n  <child>\n</root>\n")));
			diagnostics.get(60, SECONDS);
			server.shutdown().get(60, SECONDS);
			server.exit();
			assertTrue(process.waitFor(60, SECONDS), "Server did not exit");
			assertEquals(0, process.exitValue(), "Server exit code");
		} finally {
			process.destroyForcibly();
		}
	}
}
