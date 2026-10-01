/* SPDX-License-Identifier: EPL-2.0 */

package org.eclipse.lemminx;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CompletionItem;
import org.junit.jupiter.api.Test;

public class XMLTextDocumentServiceTest {

	@Test
	public void resolveCompletionItemWithoutDataReturnsOriginalItem() throws Exception {
		XMLTextDocumentService service = new XMLTextDocumentService(new XMLLanguageServer());
		CompletionItem item = new CompletionItem("test");

		CompletionItem resolved = service.resolveCompletionItem(item).get();

		assertSame(item, resolved);
	}

	@Test
	public void resolveCompletionItemWithoutUriReturnsOriginalItem() throws Exception {
		XMLTextDocumentService service = new XMLTextDocumentService(new XMLLanguageServer());
		CompletionItem item = new CompletionItem("test");

		Map<String, Object> data = new HashMap<>();
		data.put("foo", "bar");
		item.setData(data);

		CompletionItem resolved = service.resolveCompletionItem(item).get();

		assertSame(item, resolved);
	}

	@Test
	public void resolveCodeActionWithoutDataReturnsOriginalItem() throws Exception {
		XMLTextDocumentService service = new XMLTextDocumentService(new XMLLanguageServer());
		CodeAction item = new CodeAction("test");

		CodeAction resolved = service.resolveCodeAction(item).get();

		assertSame(item, resolved);
	}

	@Test
	public void resolveCodeActionWithoutUriReturnsOriginalItem() throws Exception {
		XMLTextDocumentService service = new XMLTextDocumentService(new XMLLanguageServer());
		CodeAction item = new CodeAction("test");

		Map<String, Object> data = new HashMap<>();
		data.put("foo", "bar");
		item.setData(data);

		CodeAction resolved = service.resolveCodeAction(item).get();

		assertSame(item, resolved);
	}
}
