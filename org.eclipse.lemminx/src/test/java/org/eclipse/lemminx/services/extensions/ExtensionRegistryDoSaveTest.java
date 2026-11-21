/**
 *  Copyright (c) 2020 Yatao Li.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Yatao Li <yatao.li@live.com> - initial API and implementation
 */
package org.eclipse.lemminx.services.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Predicate;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert.SettingsSaveContext;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.InitializeParams;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Ensures that {@code XMLExtensionsRegistry.doSave()} correctly captures the
 * initial LS configuration, and the configuration is correctly forwarded to
 * {@code IXMLExtension.doSave()} for registered extensions.
 */
public class ExtensionRegistryDoSaveTest extends AbstractCacheBasedTest {

	class RegistryTestExtension implements IXMLExtension {

		private ISaveContext context = null;

		@Override
		public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		}

		@Override
		public void stop(XMLExtensionsRegistry registry) {
		}

		@Override
		public void doSave(ISaveContext context) {
			this.context = context;
		}

		public ISaveContext getContext() {
			return context;
		}
	}

	private static class SaveFileContext implements ISaveContext {
		@Override
		public DOMDocument getDocument(String uri) {
			return null;
		}

		@Override
		public void refreshDiagnostics(Predicate<DOMDocument> filter) {
		}

		@Override
		public SaveContextType getType() {
			return SaveContextType.DOCUMENT;
		}

		@Override
		public String getUri() {
			return null;
		}

		@Override
		public Object getSettings() {
			return null;
		}
	}

	@Test
	public void initialConfiguration() {
		XMLExtensionsRegistry registry = new XMLExtensionsRegistry();
		ISaveContext settingsSaveContext = new SettingsSaveContext(new JsonObject());
		// initial config passed in during server startup.
		registry.doSave(settingsSaveContext);
		// then another event comes in from the client
		registry.doSave(new SaveFileContext());
		// extension registration is triggered
		registry.initializeIfNeeded();
		RegistryTestExtension extension = new RegistryTestExtension();
		registry.registerExtension(extension);
		// examine that the extension properly receives the initial config
		assertEquals(settingsSaveContext, extension.getContext());
	}
}
