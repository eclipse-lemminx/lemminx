/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.embeddedLanguages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.XMLAssert.SettingsSaveContext;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.embeddedLanguages.participants.IEmbeddedLanguageTokenizer;
import org.eclipse.lemminx.extensions.embeddedLanguages.participants.IEmbeddedLanguageTokenizer.EmbeddedToken;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguageExpression;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguages;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguagesSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants;
import org.eclipse.lsp4j.SemanticTokens;
import org.junit.jupiter.api.Test;

/**
 * Tests for the embedded language semantic tokens participant using a mock
 * tokenizer.
 */
public class EmbeddedLanguageSemanticTokensTest {

	@Test
	public void embeddedTokensInTextNode() {
		String xml = "<root><script>var x = 1;</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x = 1;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")),
				new EmbeddedToken(4, 5, Arrays.asList("source.js", "variable.other.js")),
				new EmbeddedToken(8, 9, Arrays.asList("source.js", "constant.numeric.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		// 3 tokens * 5 ints each = 15
		assertEquals(15, data.size());

		// Token 1: "var" at offset 14 in document (after "<root><script>")
		// keyword.var.js → KEYWORD (4)
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(3));
		assertEquals(3, data.get(2)); // length

		// Token 2: "x" → variable.other.js → VARIABLE (1)
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_VARIABLE, data.get(8));
		assertEquals(1, data.get(7)); // length

		// Token 3: "1" → constant.numeric.js → NUMBER (6)
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_NUMBER, data.get(13));
		assertEquals(1, data.get(12)); // length
	}

	@Test
	public void embeddedTokensInCDATANode() {
		String xml = "<root><script><![CDATA[var x = 1;]]></script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x = 1;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		assertEquals(5, data.size());
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(3));
		assertEquals(3, data.get(2)); // length of "var"
	}

	@Test
	public void noMatchWhenXPathDoesNotMatch() {
		String xml = "<root><other>var x = 1;</other></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x = 1;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		assertTrue(result.getData().isEmpty());
	}

	@Test
	public void noMatchWhenDocumentPatternDoesNotMatch() {
		String xml = "<root><script>var x = 1;</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/other-file.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x = 1;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		assertTrue(result.getData().isEmpty());
	}

	@Test
	public void multipleExpressionsWithDifferentLanguages() {
		String xml = "<root>" //
				+ "<script>var x;</script>" //
				+ "<style>color: red;</style>" //
				+ "</root>";
		XMLEmbeddedLanguagesSettings settings = new XMLEmbeddedLanguagesSettings();
		List<XMLEmbeddedLanguages> embeddedLanguagesList = new ArrayList<>();
		XMLEmbeddedLanguages entry = new XMLEmbeddedLanguages();
		entry.setPattern("**/*.xml");
		XMLEmbeddedLanguageExpression jsExpr = new XMLEmbeddedLanguageExpression();
		jsExpr.setXPath("script/text()");
		jsExpr.setLanguageId("javascript");
		XMLEmbeddedLanguageExpression cssExpr = new XMLEmbeddedLanguageExpression();
		cssExpr.setXPath("style/text()");
		cssExpr.setLanguageId("css");
		entry.setExpressions(Arrays.asList(jsExpr, cssExpr));
		embeddedLanguagesList.add(entry);
		settings.setEmbeddedLanguages(embeddedLanguagesList);

		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")));
		tokenizer.addTokens("css", "color: red;",
				new EmbeddedToken(0, 5, Arrays.asList("source.css", "support.type.property-name.css")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		// 2 tokens * 5 ints = 10
		assertEquals(10, data.size());

		// Token 1: "var" → KEYWORD
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(3));
		// Token 2: "color" → CLASS (support.type maps to CLASS)
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_CLASS, data.get(8));
	}

	@Test
	public void multilineEmbeddedContent() {
		String xml = "<root><script>var x = 1;\nvar y = 2;</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "var x = 1;\nvar y = 2;",
				new EmbeddedToken(0, 3, Arrays.asList("source.js", "keyword.var.js")),
				new EmbeddedToken(11, 14, Arrays.asList("source.js", "keyword.var.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		assertEquals(10, data.size());
		// Both tokens are keywords
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(3));
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(8));
	}

	@Test
	public void noTokensWhenTokenizerReturnsEmpty() {
		String xml = "<root><script>unknown content</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		// No tokens added for "unknown content"

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		assertTrue(result.getData().isEmpty());
	}

	@Test
	public void nestedXPathExpression() {
		String xml = "<mapper><select>SELECT * FROM users</select><insert>INSERT INTO users</insert></mapper>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "mapper/select/text()", "sql");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("sql", "SELECT * FROM users",
				new EmbeddedToken(0, 6, Arrays.asList("source.sql", "keyword.other.DML.sql")));
		// insert/text() should NOT be matched (only select/text())
		tokenizer.addTokens("sql", "INSERT INTO users",
				new EmbeddedToken(0, 6, Arrays.asList("source.sql", "keyword.other.DML.sql")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		// Only 1 token from select/text(), not from insert/text()
		assertEquals(5, data.size());
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_KEYWORD, data.get(3));
		assertEquals(6, data.get(2)); // "SELECT" length
	}

	@Test
	public void scopeMapperStringRegexpBeforeString() {
		String xml = "<root><script>/pattern/</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "/pattern/",
				new EmbeddedToken(0, 9, Arrays.asList("source.js", "string.regexp.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		assertEquals(5, data.size());
		// string.regexp should map to REGEXP, not STRING
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_REGEXP, data.get(3));
	}

	@Test
	public void scopeMapperKeywordOperatorBeforeKeyword() {
		String xml = "<root><script>a + b</script></root>";
		XMLEmbeddedLanguagesSettings settings = createSettings("**/*.xml", "script/text()", "javascript");
		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.addTokens("javascript", "a + b",
				new EmbeddedToken(2, 3, Arrays.asList("source.js", "keyword.operator.arithmetic.js")));

		SemanticTokens result = findSemanticTokens(xml, "file:///test/test.xml", settings, tokenizer);

		assertNotNull(result);
		List<Integer> data = result.getData();
		assertEquals(5, data.size());
		// keyword.operator should map to OPERATOR, not KEYWORD
		assertEquals(ServerCapabilitiesConstants.TOKEN_TYPE_OPERATOR, data.get(3));
	}

	// ------------------- Helper methods -------------------

	private static SemanticTokens findSemanticTokens(String xml, String fileURI,
			XMLEmbeddedLanguagesSettings settings, MockTokenizer tokenizer) {
		XMLLanguageService service = new XMLLanguageService();
		service.doSave(new SettingsSaveContext(settings));

		XMLEmbeddedLanguagesPlugin plugin = findEmbeddedLanguagesPlugin(service);
		assertNotNull(plugin, "XMLEmbeddedLanguagesPlugin should be registered");
		plugin.setTokenizer(tokenizer);

		TextDocument textDocument = new TextDocument(xml, fileURI);
		DOMDocument doc = DOMParser.getInstance().parse(textDocument, service.getResolverExtensionManager());
		service.setDocumentProvider(uri -> doc);

		return service.findSemanticTokens(doc, () -> {
		});
	}

	private static XMLEmbeddedLanguagesPlugin findEmbeddedLanguagesPlugin(XMLLanguageService service) {
		for (IXMLExtension ext : service.getExtensions()) {
			if (ext instanceof XMLEmbeddedLanguagesPlugin) {
				return (XMLEmbeddedLanguagesPlugin) ext;
			}
		}
		return null;
	}

	private static XMLEmbeddedLanguagesSettings createSettings(String pattern, String xpath, String languageId) {
		XMLEmbeddedLanguagesSettings settings = new XMLEmbeddedLanguagesSettings();
		List<XMLEmbeddedLanguages> embeddedLanguagesList = new ArrayList<>();
		XMLEmbeddedLanguages entry = new XMLEmbeddedLanguages();
		entry.setPattern(pattern);
		XMLEmbeddedLanguageExpression expression = new XMLEmbeddedLanguageExpression();
		expression.setXPath(xpath);
		expression.setLanguageId(languageId);
		entry.setExpressions(Arrays.asList(expression));
		embeddedLanguagesList.add(entry);
		settings.setEmbeddedLanguages(embeddedLanguagesList);
		return settings;
	}

	/**
	 * Mock tokenizer that returns pre-configured tokens for known text/language
	 * pairs.
	 */
	private static class MockTokenizer implements IEmbeddedLanguageTokenizer {

		private final List<MockEntry> entries = new ArrayList<>();

		public void addTokens(String languageId, String text, EmbeddedToken... tokens) {
			entries.add(new MockEntry(languageId, text, Arrays.asList(tokens)));
		}

		@Override
		public void registerGrammarPath(String languageId, String grammarPath) {
		}

		@Override
		public List<EmbeddedToken> tokenize(String text, String languageId) {
			for (MockEntry entry : entries) {
				if (entry.languageId.equals(languageId) && entry.text.equals(text)) {
					return entry.tokens;
				}
			}
			return Collections.emptyList();
		}

		private static class MockEntry {
			final String languageId;
			final String text;
			final List<EmbeddedToken> tokens;

			MockEntry(String languageId, String text, List<EmbeddedToken> tokens) {
				this.languageId = languageId;
				this.text = text;
				this.tokens = tokens;
			}
		}
	}
}
