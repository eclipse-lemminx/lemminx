/**
 * Copyright (c) 2020 Red Hat Inc. and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.participants.ContentModelCodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

public class ContentModelCodeActionTest {

	private ContentModelCodeActionParticipant cmCodeActionParticipant;

	class MockCodeActionRequest implements ICodeActionRequest {

		private final Diagnostic diagnostic;

		public MockCodeActionRequest(Diagnostic diagnostic) {
			this.diagnostic = diagnostic;
		}

		@Override
		public DOMDocument getDocument() {
			return null;
		}

		@Override
		public SharedSettings getSharedSettings() {
			return null;
		}

		@Override
		public <T> T getComponent(Class clazz) {
			return null;
		}

		@Override
		public Diagnostic getDiagnostic() {
			return diagnostic;
		}

		@Override
		public Range getRange() {
			return null;
		}

		@Override
		public XMLElementGenerator getXMLGenerator() {
			return null;
		}
		
	}

	@Test
	public void codeActionParticipantRobustAgainstNull() {
		cmCodeActionParticipant = new ContentModelCodeActionParticipant();
		cmCodeActionParticipant.doCodeAction(new MockCodeActionRequest(null), null, () -> {
		});
		cmCodeActionParticipant.doCodeAction(new MockCodeActionRequest(new Diagnostic()), null, () -> {
		});
	}

}