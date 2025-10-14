/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.te;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.uriresolver.URIResolverExtension;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.MarkupKind;
import org.junit.jupiter.api.Test;

/**
 * XML completion tests based on XML Schema.
 *
 */
public class XMLSchemaCompletionExtensionsTest extends BaseFileTempTest {

	@Test
	public void completionInRoot() throws BadLocationException {
		// completion on <|
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, //
				c("modelVersion", te(3, 1, 3, 2, "<modelVersion></modelVersion>"), "<modelVersion"), //
				c("parent", "<parent></parent>", "<parent"));

		// completion on <| >
		xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|   >" + //
				"</project>";
		testCompletionWithCatalogFor(xml, //
				c("modelVersion", te(3, 1, 3, 6, "<modelVersion></modelVersion>"), "<modelVersion"), //
				c("parent", "<parent></parent>", "<parent"));

		// completion on |
		xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, //
				c("modelVersion", te(3, 1, 3, 1, "<modelVersion></modelVersion>"), "modelVersion"), //
				c("parent", "<parent></parent>", "parent"));

		// completion on mod
		xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	mod|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, //
				c("modelVersion", te(3, 1, 3, 4, "<modelVersion></modelVersion>"), "modelVersion"), //
				c("parent", "<parent></parent>", "parent"));
	}

	@Test
	public void completionInRootWithCloseBracket() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|  >" + // here last '<' is replaced with <modelVersion></modelVersion>
				"</project>";
		testCompletionWithCatalogFor(xml,
				c("modelVersion", te(3, 1, 3, 5, "<modelVersion></modelVersion>"), "<modelVersion"), //
				c("parent", "<parent></parent>", "<parent"));
	}

	@Test
	public void completionInRootWithCloseBracketItemDefaults() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|  >" + // here last '<' is replaced with <modelVersion></modelVersion>
				"</project>";
		testCompletionWithCatalogFor(xml,
				c("modelVersion", te(3, 1, 3, 5, "<modelVersion></modelVersion>"), "<modelVersion"), //
				c("parent", "<parent></parent>", "<parent"));
	}

	@Test
	public void completionInChildElement() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<parent><|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, c("groupId", "<groupId></groupId>", "<groupId"), //
				c("artifactId", "<artifactId></artifactId>", "<artifactId"), //
				c("version", "<version></version>", "<version"));
	}

	@Test
	public void completionInChildElementNoOpenBracket() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<parent>|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, c("groupId", "<groupId></groupId>", "groupId"), //
				c("artifactId", "<artifactId></artifactId>", "artifactId"), //
				c("version", "<version></version>", "version"));
	}

	@Test
	public void completionInRootWithAndWithoutPrefixes() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"       xmlns:camel=\"http://camel.apache.org/schema/spring\"\r\n" + //
				"       xmlns:cxf=\"http://camel.apache.org/schema/cxf\"\r\n" + //
				"       xsi:schemaLocation=\"\r\n" + //
				"         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/cxf http://camel.apache.org/schema/cxf/camel-cxf.xsd\">\r\n" + //
				"\r\n" + //
				"  <| ";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>", "<bean"),
				c("camel:camelContext", "<camel:camelContext></camel:camelContext>", "<camel:camelContext"));
	}

	@Test
	public void completionInRootWithAndWithoutPrefixesNoOpenBracket() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"       xmlns:camel=\"http://camel.apache.org/schema/spring\"\r\n" + //
				"       xmlns:cxf=\"http://camel.apache.org/schema/cxf\"\r\n" + //
				"       xsi:schemaLocation=\"\r\n" + //
				"         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/cxf http://camel.apache.org/schema/cxf/camel-cxf.xsd\">\r\n" + //
				"\r\n" + //
				"  | ";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>", "bean"),
				c("camel:camelContext", "<camel:camelContext></camel:camelContext>", "camel:camelContext"));
	}

	@Test
	public void completionInRootWithOnlyPrefix() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"       xmlns:camel=\"http://camel.apache.org/schema/spring\"\r\n" + //
				"       xmlns:cxf=\"http://camel.apache.org/schema/cxf\"\r\n" + //
				"       xsi:schemaLocation=\"\r\n" + //
				"         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/cxf http://camel.apache.org/schema/cxf/camel-cxf.xsd\">\r\n" + //
				"\r\n" + //
				"  <camel:| ";
		testCompletionWithCatalogFor(xml, c("camel:camelContext", "<camel:camelContext></camel:camelContext>"));
	}

	@Test
	public void completionInChildElementWithPrefixes() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"       xmlns:camel=\"http://camel.apache.org/schema/spring\"\r\n" + //
				"       xmlns:cxf=\"http://camel.apache.org/schema/cxf\"\r\n" + //
				"       xsi:schemaLocation=\"\r\n" + //
				"         http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd\r\n"
				+ //
				"         http://camel.apache.org/schema/cxf http://camel.apache.org/schema/cxf/camel-cxf.xsd\">\r\n" + //
				"\r\n" + //
				"  <camel:camelContext><| ";
		testCompletionWithCatalogFor(xml, c("camel:route", "<camel:route></camel:route>"),
				c("camel:template", "<camel:template />"));
	}

	@Test
	public void completionInChildElementWithElementWhichDeclareNS() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<root>\r\n" + //
				"  <camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + "\r\n" + //
				"    <|";
		testCompletionWithCatalogFor(xml, c("route", "<route></route>"), c("template", "<template />"));
	}

	@Test
	public void noNamespaceSchemaLocationCompletion() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<Configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/Format.xsd\">\r\n"
				+ //
				"  <ViewDefinitions>\r\n" + //
				"    <View><|";
		// Completion only with Name
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/Format.xml", 4 + 2 /* CDATA and Comments */,
				c("Name", "<Name></Name>"), c("End with '</Configuration>'", "/Configuration>"),
				c("End with '</ViewDefinitions>'", "/ViewDefinitions>"), c("End with '</View>'", "/View>"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<Configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/Format.xsd\">\r\n"
				+ //
				"  <ViewDefinitions>\r\n" + //
				"    <View><Name /><|";
		// Completion only with Name
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/Format.xml", 5 + 2 /* CDATA and Comments */,
				c("OutOfBand", "<OutOfBand>false</OutOfBand>"),
				c("ViewSelectedBy", "<ViewSelectedBy></ViewSelectedBy>"),
				c("End with '</Configuration>'", "/Configuration>"),
				c("End with '</ViewDefinitions>'", "/ViewDefinitions>"), c("End with '</View>'", "/View>"));
	}

	@Test
	public void schemaLocationWithXSDFileSystemCompletion() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:schemaLocation=\"http://invoice xsd/invoice-ns.xsd \">\r\n" + //
				"  <|";
		// Completion only for date
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/invoice.xml", 2 + 2 /* CDATA and Comments */,
				c("date", "<date></date>"), c("End with '</invoice>'", "</invoice>"));

		// Completion only for number
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:schemaLocation=\"http://invoice xsd/invoice-ns.xsd \">\r\n" + //
				"  <date></date>|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/invoice.xml", 2 + 2 /* CDATA and Comments */,
				c("number", "<number></number>"), c("End with '</invoice>'", "</invoice>"));
	}

	@Test
	public void completionOnAttributeName() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean |/>";
		testCompletionWithCatalogFor(xml, c("abstract", "abstract=\"false\""), c("autowire", "autowire=\"default\""),
				c("class", "class=\"\""));
	}

	@Test
	public void completionOnAttributeNameResolve() throws BadLocationException {
		String xml = "<dress\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\"\r\n" + //
				"	s| />";
		XMLAssert.testCompletionItemResolveFor(xml, null, "src/test/resources/dress.xml", 5, //
				c("size", te(3, 1, 3, 2, "size=\"\""), "size", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("xmlns", te(3, 1, 3, 2, "xmlns=\"\""), "xmlns"), //
				c("xsi:nil", te(3, 1, 3, 2, "xsi:nil=\"true\""), "xsi:nil"), //
				c("xsi:type", te(3, 1, 3, 2, "xsi:type=\"\""), "xsi:type"), //
				c("xsi:schemaLocation", te(3, 1, 3, 2, "xsi:schemaLocation=\"\""), "xsi:schemaLocation"));
		XMLAssert.testCompletionItemUnresolvedFor(xml, null, "src/test/resources/dress.xml", 5, //
				c("size", te(3, 1, 3, 2, "size=\"\""), "size"), //
				c("xmlns", te(3, 1, 3, 2, "xmlns=\"\""), "xmlns"), //
				c("xsi:nil", te(3, 1, 3, 2, "xsi:nil=\"true\""), "xsi:nil"), //
				c("xsi:type", te(3, 1, 3, 2, "xsi:type=\"\""), "xsi:type"), //
				c("xsi:schemaLocation", te(3, 1, 3, 2, "xsi:schemaLocation=\"\""), "xsi:schemaLocation"));
	}

	@Test
	public void completionOnAttributeValue() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean autowire=\"|\"/>";
		testCompletionWithCatalogFor(xml, c("byName", "byName"), c("byType", "byType"),
				c("constructor", "constructor"));
	}

	@Test
	public void completionOnAttributeValue2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:schemaLocation=\"http://invoice xsd/invoice.xsd \">\r\n" + //
				"  <payments>\r\n" + //
				"    <payment method=\"|\"/>\r\n" + //
				"  </payments>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/invoice.xml", null, c("credit", "credit"),
				c("debit", "debit"), c("cash", "cash"));
	}

	@Test
	public void completionOnAttributeValueWithUnionAndEnumeration() throws BadLocationException {
		String xml = "<dress\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\"\r\n" + //
				"	size=\"|\" />";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/dress.xml", 4, //
				c("small", te(3, 7, 3, 7, "small"), "small", //
						"Small documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("medium", te(3, 7, 3, 7, "medium"), "medium", //
						"Medium documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("large", te(3, 7, 3, 7, "large"), "large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("x-large", te(3, 7, 3, 7, "x-large"), "x-large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT));
	}

	@Test
	public void completionOnAttributeValueWithUnionAndEnumerationResolve() throws BadLocationException {
		String xml = "<dress\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\"\r\n" + //
				"	size=\"|\" />";
		XMLAssert.testCompletionItemResolveFor(xml, null, "src/test/resources/dress.xml", 4, //
				c("small", te(3, 7, 3, 7, "small"), "small", //
						"Small documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("medium", te(3, 7, 3, 7, "medium"), "medium", //
						"Medium documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("large", te(3, 7, 3, 7, "large"), "large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("x-large", te(3, 7, 3, 7, "x-large"), "x-large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT));
		XMLAssert.testCompletionItemUnresolvedFor(xml, null, "src/test/resources/dress.xml", 4, //
				c("small", te(3, 7, 3, 7, "small"), "small"), //
				c("medium", te(3, 7, 3, 7, "medium"), "medium"), //
				c("large", te(3, 7, 3, 7, "large"), "large"), //
				c("x-large", te(3, 7, 3, 7, "x-large"), "x-large"));
	}

	@Test
	public void completionOnTextWithUnionAndEnumeration() throws BadLocationException {
		String xml = "<dresssize\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\" >\r\n" + //
				"	| " + //
				"</dresssize>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/dressSize.xml", 4 /*
																						 * start/end region + CDATA and
																						 * Comments
																						 */ + 4, //
				c("small", te(2, 52, 3, 2, "small"), //
						"\r\n" + //
								"	small", //
						"Small documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("medium", te(2, 52, 3, 2, "medium"), //
						"\r\n" + //
								"	medium", //
						"Medium documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("large", te(2, 52, 3, 2, "large"), //
						"\r\n" + //
								"	large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT), //
				c("x-large", te(2, 52, 3, 2, "x-large"), //
						"\r\n" + //
								"	x-large", //
						"Size Type documentation" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: dressSize.xsd", //
						MarkupKind.PLAINTEXT));
	}

	@Test
	public void completionOnTextWithEnumeration() throws BadLocationException {
		String xml = "<team xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"team_namespace\" xsi:schemaLocation=\"team_namespace xsd/team.xsd\">\r\n"
				+ //
				"	<member>\r\n" + //
				"		<skills>\r\n" + //
				"			|\r\n" + //
				"		</skills>";
		// Completion on skills Text node
		// - without snippet
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/team.xml", null,
				c("skill", "<skill>Java</skill>", r(3, 3, 3, 3), "skill"));
		// - with snippet
		testCompletionSnippetSupportFor(xml, "src/test/resources/team.xml", null,
				c("skill", "<skill>${1|Java,Node,XML|}$2</skill>$0", r(3, 3, 3, 3), "skill"));

		xml = "<team xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"team_namespace\" xsi:schemaLocation=\"team_namespace xsd/team.xsd\">\r\n"
				+ //
				"	<member>\r\n" + //
				"		<skills>\r\n" + //
				"			<skill>|</skill>\r\n" + //
				"		</skills>";
		// Completion on skill Text node
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/team.xml", null, //
				c("Java", "Java", r(3, 10, 3, 10), "Java"), //
				c("Node", "Node", r(3, 10, 3, 10), "Node"), //
				c("XML", "XML", r(3, 10, 3, 10), "XML"));

		xml = "<team xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"team_namespace\" xsi:schemaLocation=\"team_namespace xsd/team.xsd\">\r\n"
				+ //
				"	<member>\r\n" + //
				"		<skills>\r\n" + //
				"			<skill> |</skill>\r\n" + //
				"		</skills>";
		// Completion on skill Text node
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/team.xml", null, //
				c("Java", "Java", r(3, 10, 3, 11), " Java"), //
				c("Node", "Node", r(3, 10, 3, 11), " Node"), //
				c("XML", "XML", r(3, 10, 3, 11), " XML"));
	}

	@Test
	public void schemaLocationWithElementAndAttributeCompletion() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://simpleAttribute\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:schemaLocation=\"http://simpleAttribute xsd/simpleAttribute.xsd \">\r\n" + //
				"  <pro|</invoice>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/simpleAttribute.xml", null,
				c("product", "<product description=\"\" />"));
	}

	@Test
	public void completionWithoutStartBracket() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	|";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	|" + "</beans>";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean />|" + "</beans>";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean />|";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean ></bean>|" + "</beans>";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean ></bean>|";
		testCompletionWithCatalogFor(xml, c("bean", "<bean></bean>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean>|</bean>";
		testCompletionWithCatalogFor(xml, c("constructor-arg", "<constructor-arg></constructor-arg>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean>\r\n   |      \r\n</bean>";
		testCompletionWithCatalogFor(xml, c("constructor-arg", "<constructor-arg></constructor-arg>"));

	}

	@Test
	public void completionWithXMLSchemaContentChanged() throws Exception {
		// This https://github.com/eclipse/lemminx/issues/194 for the test scenario
		String xsdPath = getTempDirPath().toString() + "/resources.xsd";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<resources | xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
				+ "xsi:noNamespaceSchemaLocation=\"" + xsdPath + "\">\r\n" + //
				"    <resource name=\"res00\" >\r\n" + //
				"        <property name=\"propA\" value=\"...\" />\r\n" + //
				"        <property name=\"propB\" value=\"...\" />\r\n" + //
				"    </resource>\r\n" + //
				"    <resource name=\"\" >\r\n" + //
				"        <property name=\"\" value=\"...\" />\r\n" + //
				"        <property name=\"\" value=\"...\" />\r\n" + //
				"    </resource> \r\n" + "</resources>";

		// Schema defines variant attribute -> completion for @variant
		String schema = "<?xml version=\"1.0\"?>\r\n" + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ "\r\n" + "    <xs:complexType name=\"property\">\r\n"
				+ "        <xs:attribute name=\"name\" type=\"xs:string\" />\r\n"
				+ "        <xs:attribute name=\"value\" type=\"xs:string\" />\r\n" + "    </xs:complexType>\r\n"
				+ "\r\n" + "    <xs:complexType name=\"resource\">\r\n" + "        <xs:sequence>\r\n"
				+ "            <xs:element name=\"property\" type=\"property\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\r\n"
				+ "        </xs:sequence>\r\n"
				+ "        <xs:attribute name=\"name\" type=\"xs:string\" use=\"required\" />\r\n"
				+ "    </xs:complexType>\r\n" + "\r\n" + "    <xs:element name=\"resources\">\r\n"
				+ "        <xs:complexType>\r\n" + "            <xs:sequence>\r\n"
				+ "                <xs:element name=\"resource\" type=\"resource\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\r\n"
				+ "            </xs:sequence>\r\n"
				+ "            <xs:attribute name=\"variant\" type=\"xs:string\" use=\"required\"/>\r\n"
				+ "        </xs:complexType>\r\n" + "    </xs:element>\r\n" + "</xs:schema>";

		createFile(xsdPath, schema);
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null, "target/resources.xml", 5, false,
				c("variant", "variant=\"\""));

		// Update resources.xsd, Schema doesn't define variant attribute -> no
		// completion
		schema = "<?xml version=\"1.0\"?>\r\n" + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ "\r\n" + "    <xs:complexType name=\"property\">\r\n"
				+ "        <xs:attribute name=\"name\" type=\"xs:string\" />\r\n"
				+ "        <xs:attribute name=\"value\" type=\"xs:string\" />\r\n" + "    </xs:complexType>\r\n"
				+ "\r\n" + "    <xs:complexType name=\"resource\">\r\n" + "        <xs:sequence>\r\n"
				+ "            <xs:element name=\"property\" type=\"property\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\r\n"
				+ "        </xs:sequence>\r\n"
				+ "        <xs:attribute name=\"name\" type=\"xs:string\" use=\"required\" />\r\n"
				+ "    </xs:complexType>\r\n" + "\r\n" + "    <xs:element name=\"resources\">\r\n"
				+ "        <xs:complexType>\r\n" + "            <xs:sequence>\r\n"
				+ "                <xs:element name=\"resource\" type=\"resource\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\r\n"
				+ "            </xs:sequence>\r\n"
				// + " <xs:attribute name=\"variant\" type=\"xs:string\" use=\"required\"/>\r\n"
				+ "        </xs:complexType>\r\n" + "    </xs:element>\r\n" + "</xs:schema>";
		updateFile(xsdPath, schema);
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null, "target/resources.xml", 4, false);

	}

	/**
	 * @see https://github.com/eclipse/lemminx/issues/214
	 *
	 * @throws BadLocationException
	 * @throws MalformedURIException
	 */
	@Test
	public void issue214() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Uri=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n"
				+ //
				"    <edmx:Include Namespace=\"Org.OData.Core.V1\" Alias=\"Core\">\r\n" + //
				"      <Annotation Term=\"Core.DefaultNamespace\" />      \r\n" + //
				"    </edmx:Include>\r\n" + //
				" |";
		testCompletionWithCatalogFor(xml, c("Annotation", "<Annotation Term=\"\"></Annotation>"), //
				c("edmx:Include", "<edmx:Include Namespace=\"\"></edmx:Include>"), //
				c("edmx:IncludeAnnotations", "<edmx:IncludeAnnotations TermNamespace=\"\" />"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">   \r\n"
				+ "  <edmx:DataServices>\r\n" + //
				"    <Schema Namespace=\"ODataDemo\">\r\n" + //
				" |";
		testCompletionWithCatalogFor(xml, c("Action", "<Action Name=\"\"></Action>"), //
				c("Annotation", "<Annotation Term=\"\"></Annotation>"), //
				c("Annotations", "<Annotations Target=\"\"></Annotations>"), //
				c("ComplexType", "<ComplexType Name=\"\"></ComplexType>"));
	}

	@Test
	public void issue214WithMarkdown() throws BadLocationException, MalformedURIException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Uri=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n"
				+ //
				"    <edmx:Include Namespace=\"Org.OData.Core.V1\" Alias=\"Core\">\r\n" + //
				"      <Annotation Term=\"Core.DefaultNamespace\" />      \r\n" + //
				"    </edmx:Include>\r\n" + //
				" |";
		testCompletionMarkdownSupportFor(xml,
				c("Annotation", te(6, 1, 6, 1, "<Annotation Term=\"\"></Annotation>"), "Annotation", null, null), //
				c("edmx:Include", te(6, 1, 6, 1, "<edmx:Include Namespace=\"\"></edmx:Include>"), "edmx:Include", null,
						null), //
				c("edmx:IncludeAnnotations", "<edmx:IncludeAnnotations TermNamespace=\"\" />"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">   \r\n"
				+ "  <edmx:DataServices>\r\n" + //
				"    <Schema Namespace=\"ODataDemo\">\r\n" + //
				" |";
		testCompletionMarkdownSupportFor(xml, c("Action", "<Action Name=\"\"></Action>"), //
				c("Annotation", "<Annotation Term=\"\"></Annotation>"), //
				c("Annotations", "<Annotations Target=\"\"></Annotations>"), //
				c("ComplexType", "<ComplexType Name=\"\"></ComplexType>"));
	}

	/**
	 * @see https://github.com/eclipse/lemminx/issues/311
	 *
	 * @throws BadLocationException
	 */
	@Test
	public void issue311() throws BadLocationException {
		// with xmlns:edm="http://docs.oasis-open.org/odata/ns/edm"
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns:edm=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ "  | \r\n" //
				+ "</edmx:Edmx>";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog.xml", "test.xsd",
				4 /*
					 * edmx:DataServices, <edmx:DataServices, #region, #endregion AND NOT
					 * edm:Annotation
					 */ + 2 /* CDATA and Comments */, c("edmx:DataServices", "<edmx:DataServices></edmx:DataServices>"), //
				c("edmx:Reference", "<edmx:Reference Uri=\"\"></edmx:Reference>"));

		// with xmlns="http://docs.oasis-open.org/odata/ns/edm"
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ "  | \r\n" //
				+ "</edmx:Edmx>";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog.xml", "test.xsd",
				4 /*
					 * edmx:DataServices, <edmx:DataServices, #region, #endregion AND NOT
					 * edm:Annotation
					 */ + 2 /* CDATA and Comments */, c("edmx:DataServices", "<edmx:DataServices></edmx:DataServices>"), //
				c("edmx:Reference", "<edmx:Reference Uri=\"\"></edmx:Reference>"));
	}

	@Test
	public void xsiCompletionTestAllItems() throws BadLocationException {
		String xml = "<project\r\n" + //
				"    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    |>\r\n" + //
				"</project>";

		XMLAssert.testCompletionFor(xml, 4, //
				c("xsi:nil", "xsi:nil=\"true\""), //
				c("xsi:type", "xsi:type=\"\""), //
				c("xsi:noNamespaceSchemaLocation", "xsi:noNamespaceSchemaLocation=\"\""), //
				c("xsi:schemaLocation", "xsi:schemaLocation=\"\""));
	}

	@Test
	public void xsiCompletionNonRootElement() throws BadLocationException {
		String xml = "<project\r\n" + //
				"    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <modelVersion xs|></modelVersion>\r\n" + "</project>";

		XMLAssert.testCompletionFor(xml, 2, //
				c("xsi:nil", "xsi:nil=\"true\""), //
				c("xsi:type", "xsi:type=\"\""));
	}

	@Test
	public void xsiCompletionNonRootElement2() throws BadLocationException {
		String xml = "<project\r\n" + "    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <modelVersion xsi:nil=\"\" |></modelVersion>\r\n" + "</project>";

		XMLAssert.testCompletionFor(xml, 1, //
				c("xsi:type", "xsi:type=\"\""));
	}

	@Test
	public void xsiCompletionNotUsingXSIName() throws BadLocationException {
		String xml = "<project\r\n" + //
				"    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xmlns:XXY=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    XXY:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" |>\r\n"
				+ //
				"  <modelVersion></modelVersion>\r\n" + "</project>";

		testCompletionWithCatalogFor(xml, 4, //
				c("child.project.url.inherit.append.path", "child.project.url.inherit.append.path=\"\""), //
				c("XXY:nil", "XXY:nil=\"true\""), //
				c("XXY:type", "XXY:type=\"\""),
				c("XXY:noNamespaceSchemaLocation", "XXY:noNamespaceSchemaLocation=\"\""));
	}

	@Test
	public void xmlnsXSICompletion() throws BadLocationException {
		String xml = "<project\r\n" + //
				"    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xsi:|>\r\n" + //
				"  <modelVersion></modelVersion>\r\n" + //
				"</project>";

		XMLAssert.testCompletionFor(xml, 1, //
				c("xmlns:xsi", "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
	}

	@Test
	public void xmlnsXSIValueCompletion() throws BadLocationException {
		String xml = "<project\r\n" + "    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + "    xmlns:xsi=|>\r\n"
				+ "  <modelVersion></modelVersion>\r\n" + "</project>";

		XMLAssert.testCompletionFor(xml, 1,
				c("http://www.w3.org/2001/XMLSchema-instance", "\"http://www.w3.org/2001/XMLSchema-instance\""));
	}

	@Test
	public void xsiCompletionSchemaLocationExists() throws BadLocationException {
		String xml = "<project\r\n" + //
				"    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\" |>\r\n"
				+ "  <modelVersion></modelVersion>\r\n" + //
				"</project>";

		XMLAssert.testCompletionFor(xml, 4, //
				c("xsi:nil", "xsi:nil=\"true\""), //
				c("xsi:type", "xsi:type=\"\""), //
				c("xsi:noNamespaceSchemaLocation", "xsi:noNamespaceSchemaLocation=\"\""));
	}

	@Test
	public void xsiCompletionNoNamespaceSchemaLocationExists() throws BadLocationException {
		String xml = "<project\r\n" + "    xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n"
				+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ "    xsi:noNamespaceSchemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" |>\r\n"
				+ // <- completion
				"  <modelVersion></modelVersion>\r\n" + "</project>";

		XMLAssert.testCompletionFor(xml, 3, c("xsi:nil", "xsi:nil=\"true\""), c("xsi:type", "xsi:type=\"\""),
				c("xsi:schemaLocation", "xsi:schemaLocation=\"\""));
	}

	@Test
	public void choice() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				" <|";
		// Completion only member or employee
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/choice.xml", null, c("member", "<member></member>"),
				c("employee", "<employee></employee>"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				" <employee /> | ";
		// Completion only member or employee
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/choice.xml", null, c("member", "<member></member>"),
				c("employee", "<employee></employee>"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				" <employee />\r\n" + //
				" <employee /> <| " + //
				"</person>";
		// Completion only member or employee
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/choice.xml", 2 + 2 /* CDATA and Comments */,
				c("employee", "<employee></employee>"));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				" <employee />\r\n" + //
				" <employee />\r\n" + //
				" <employee /> <| " + //
				"</person>";
		// maxOccurs = 3, completion should be empty
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/choice.xml", 2 /* CDATA and Comments */);
	}

	@Test
	public void sequence() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null, c("e1", "<e1></e1>"),
				c("optional0", "<optional0></optional0>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1> | ";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null, c("e2", "<e2></e2>"),
				c("optional1", "<optional1></optional1>"), c("optional11", "<optional11></optional11>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1>|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null, c("e2", "<e2></e2>"),
				c("optional1", "<optional1></optional1>"), c("optional11", "<optional11></optional11>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1><e2></e2>|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null, c("e3", "<e3></e3>"),
				c("optional2", "<optional2></optional2>"), c("optional22", "<optional22></optional22>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1><e2></e2><e3 />|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null,
				c("optional3", "<optional3></optional3>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1><e2></e2><e3 /><optional3></optional3>|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", null,
				c("optional3", "<optional3></optional3>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/sequence.xsd\">\r\n" + //
				"	<e1></e1><e2></e2><e3 /><optional3></optional3><optional3></optional3>|";
		// optional3 is not return by completion since optional3 has a max=2 occurences
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("End with '</data>'", "</data>"));
	}

	@Test
	public void xsAnyStrict() throws IOException, BadLocationException {
		Path dir = getTempDirPath().resolve("target/xsd/");
		if (!Files.isDirectory(dir)) {
			Files.createDirectories(dir);
		}
		Files.deleteIfExists(Paths.get(dir.toString(), "any.xsd"));
		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		// Test completion with xs:any processContents="strict"
		String schema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	targetNamespace=\"http://ui\">\r\n" + //
				"	<xs:element name=\"textbox\"></xs:element>\r\n" + //
				"	<xs:element name=\"page\">\r\n" + //
				"		<xs:complexType>\r\n" + //
				"			<xs:sequence>				\r\n" + //
				"				<xs:element name=\"title\" />\r\n" + //
				"				<xs:any processContents=\"strict\" />\r\n" + // <-- xs:any processContents="strict"
				"			</xs:sequence>			\r\n" + //
				"		</xs:complexType>\r\n" + //
				"	</xs:element>\r\n" + //
				"</xs:schema>";
		Files.write(getTempDirPath().resolve("target/xsd/any.xsd"), schema.getBytes());

		String xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4 + 1, true,
				c("title", "<title></title>"));

		// xs:any completion with strict -> only XML Schema global element declaration
		// available in completion
		xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	<title></title>\r\n" + //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4 + 2, true,
				c("ui:page", "<ui:page></ui:page>"), c("ui:textbox", "<ui:textbox></ui:textbox>"));

		// no completion
		xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	<title></title>\r\n" + //
				"	<ui:textbox></ui:textbox>\r\n" + //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4, true);
	}

	@Test
	public void xsAnyLax() throws IOException, BadLocationException {
		Path dir = getTempDirPath().resolve("target/xsd/");
		if (!Files.isDirectory(dir)) {
			Files.createDirectories(dir);
		}
		Files.deleteIfExists(Paths.get(dir.toString(), "any.xsd"));
		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		// Test completion with xs:any processContents="lax" (or processContents="skip")
		String schema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"	targetNamespace=\"http://ui\">\r\n" + //
				"	<xs:element name=\"textbox\"></xs:element>\r\n" + //
				"	<xs:element name=\"page\">\r\n" + //
				"		<xs:complexType>\r\n" + //
				"			<xs:sequence>				\r\n" + //
				"				<xs:element name=\"title\" />\r\n" + //
				"				<xs:any processContents=\"lax\" />\r\n" + // <-- xs:any processContents="lax"
				"			</xs:sequence>			\r\n" + //
				"		</xs:complexType>\r\n" + //
				"	</xs:element>\r\n" + //
				"</xs:schema>";

		Files.write(getTempDirPath().resolve("target/xsd/any.xsd"), schema.getBytes());

		String xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4 + 1, true,
				c("title", "<title></title>"));

		// xs:any completion with strict -> all XML Schema element declaration
		// available in completion + tags completion
		xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	<title></title>\r\n" + //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4 + 4, true,
				c("title", "<title></title>"), c("a", "<a/>"), c("ui:page", "<ui:page></ui:page>"),
				c("ui:textbox", "<ui:textbox></ui:textbox>"));

		// no completion
		xml = "<ui:page xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ui=\"http://ui\" xsi:schemaLocation=\"http://ui xsd/any.xsd\" >\r\n"
				+ //
				"	<title></title>\r\n" + //
				"	<ui:textbox></ui:textbox>\r\n" + //
				"	|	\r\n" + //
				"	<a/>" + //
				"</ui:page>";
		XMLAssert.testCompletionFor(xmlLanguageService, xml, null, null,
				getTempDirPath().resolve("target/any.xml").toUri().toString(), 4, true);
	}

	@Test
	public void xsAnySkip() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\r\n"
				+ //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + //
				"	<modelVersion>4.0.0</modelVersion>\r\n" + //
				"\r\n" + //
				"	<groupId>org.test</groupId>\r\n" + //
				"	<artifactId>test</artifactId>\r\n" + //
				"	<version>0.0.1-SNAPSHOT</version>\r\n" + //
				"	<packaging>pom</packaging>\r\n" + //
				"	\r\n" + //
				"	<build>\r\n" + //
				"		<plugins>\r\n" + //
				"			<plugin>\r\n" + //
				"				<groupId>org.apache.maven.plugins</groupId>\r\n" + //
				"				<artifactId>maven-dependency-plugin</artifactId>\r\n" + //
				"				<version>3.1.1</version>\r\n" + //
				"				<executions>\r\n" + //
				"					<execution>\r\n" + //
				"						<goals><goal>list</goal></goals>\r\n" + //
				"						<configuration>\r\n" + //
				"							<|>\r\n" + // <-- completion is triggered here (configuration has xs:any
														// processContents="skip"), it must return only project element.
				"						</configuration>\r\n" + //
				"					</execution>\r\n" + //
				"				</executions>\r\n" + //
				"			</plugin>\r\n" + //
				"		</plugins>\r\n" + //
				"	</build>\r\n" + //
				"</project>";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog.xml", null,
				3 /* project, comment and cdata */, c("project", te(20, 7, 20, 9, "<project></project>"), "<project"));
	}

	@Test
	public void xsAnyDuplicate() throws IOException, BadLocationException {
		String xml = "<Page loaded=\"pageLoaded\" class=\"page\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/tns.xsd\" >\r\n"
				+ //
				"\r\n" + //
				" | ";
		// testCompletionFor checks the duplicate label
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/tns.xml", null,
				c("Page", te(2, 1, 2, 1, "<Page></Page>"), "Page", null, null),
				c("AbsoluteLayout", te(2, 1, 2, 1, "<AbsoluteLayout></AbsoluteLayout>"), "AbsoluteLayout", null, null),
				c("DockLayout", te(2, 1, 2, 1, "<DockLayout></DockLayout>"), "DockLayout", null, null));
	}

	@Test
	public void substitutionGroup() throws BadLocationException {
		String xml = "<fleet xmlns=\"http://example/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://example/ xsd/substitutionGroup.xsd\">\r\n"
				+ "	   | ";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/substitutionGroup.xml", null,
				c("truck", "<truck />"), //
				c("automobile", "<automobile />"));
	}

	@Test
	public void substitutionGroup2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<BookStore\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/bookstore.xsd\">\r\n" + //
				"    <Name>ABC Store</Name>\r\n" + //
				"\r\n" + //
				"    <Publication>\r\n" + //
				"    </Publication>\r\n" + //
				"\r\n" + //
				"    <Book>\r\n" + //
				"      |\r\n" + // <-- completion here
				"    </Book>\r\n" + //
				"    \r\n" + //
				"</BookStore>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/bookstore.xml", null,
				c("Title", "<Title></Title>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<BookStore\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/bookstore.xsd\">\r\n" + //
				"    <Name>ABC Store</Name>\r\n" + //
				"\r\n" + //
				"    <Publication>\r\n" + //
				"    </Publication>\r\n" + //
				"\r\n" + //
				"    <Book>\r\n" + //
				"      <Title></Title>\r\n" + //
				"      |\r\n" + // <-- completion here
				"    </Book>\r\n" + //
				"    \r\n" + //
				"</BookStore>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/bookstore.xml", null,
				c("Author", "<Author></Author>"), //
				c("Date", "<Date></Date>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<BookStore\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/bookstore.xsd\">\r\n" + //
				"    <Name>ABC Store</Name>\r\n" + //
				"\r\n" + //
				"    <Publication>\r\n" + //
				"    </Publication>\r\n" + //
				"\r\n" + //
				"    <Book>\r\n" + //
				"      <Title></Title>\r\n" + //
				"      <Author></Author>\r\n" + //
				"      <Date></Date>\r\n" + //
				"      |\r\n" + // <-- completion here
				"    </Book>\r\n" + //
				"    \r\n" + //
				"</BookStore>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/bookstore.xml", null,
				c("ISBN", "<ISBN></ISBN>"));
	}

	@Test
	public void tag() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 4 + 2 /* CDATA and Comments */,
				c("tag", "<tag></tag>"), c("End with '</root>'", "</root>"), c("#region", "<!-- #region -->"),
				c("#endregion", "<!-- #endregion-->"), c("<![CDATA[", "<![CDATA[ ]]>"), //
				c("<!--", "<!-- -->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	|\r\n" + //
				"</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 3 + 2 /* CDATA and Comments */,
				c("tag", "<tag></tag>"), c("#region", "<!-- #region -->"), c("#endregion", "<!-- #endregion-->"),
				c("<![CDATA[", "<![CDATA[ ]]>"), c("<!--", "<!-- -->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 2 + 2 /* CDATA and Comments */,
				c("tag", "<tag></tag>"), c("End with '</root>'", "</root>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<|\r\n" + //
				"</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("tag", "<tag></tag>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 2 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "</root>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />|\r\n" + "</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag /><|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 2 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "/root>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag /><|\r\n" + "</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 4 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "</root>"),
				c("#region", "<!-- #region -->"), c("#endregion", "<!-- #endregion-->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"|r\n" + "</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 3 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("#region", "<!-- #region -->"),
				c("#endregion", "<!-- #endregion-->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"<|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 2 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "/root>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"<|\r\n" + "</root>";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"	<optional />\r\n" + //
				"|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 4 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "</root>"),
				c("#region", "<!-- #region -->"), c("#endregion", "<!-- #endregion-->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"	<optional />\r\n" + //
				"<|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 2 + 2 /* CDATA and Comments */,
				c("optional", "<optional></optional>"), c("End with '</root>'", "/root>"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"	<optional />\r\n" + //
				"	<optional />\r\n" + //
				"|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 3 + 2 /* CDATA and Comments */,
				c("End with '</root>'", "</root>"), c("#region", "<!-- #region -->"),
				c("#endregion", "<!-- #endregion-->"));

		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">\r\n" + //
				"	<tag />\r\n" + //
				"	<optional />\r\n" + //
				"	<optional />\r\n" + //
				"<|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/sequence.xml", 1 + 2 /* CDATA and Comments */,
				c("End with '</root>'", "/root>"));

	}

	@Test
	public void generateOnlyStartElementOnText() throws BadLocationException {
		// </employee> already exists, completion must generate only <employee>

		// completion on empty text
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 2, //
				c("member", te(2, 0, 2, 0, "<member$0>"), Arrays.asList(te(2, 2, 2, 10, "member")), "member"), //
				c("employee", te(2, 0, 2, 0, "<employee$0>"), Collections.emptyList(), "employee")); // <--
																										// here
																										// only
																										// start
																										// employee
																										// is
		// generated

		// completion on text
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"em|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 2, //
				c("member", te(2, 0, 2, 2, "<member$0>"), Arrays.asList(te(2, 4, 2, 12, "member")), "member"), //
				c("employee", te(2, 0, 2, 2, "<employee$0>"), Collections.emptyList(), "employee")); // <--
																										// here
																										// only
		// start
		// employee is
		// generated

		// completion on text inside element
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee>|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 3, //
				c("person", te(2, 10, 2, 10, "<person>$1</person>$0"), Collections.emptyList(), "person"),
				c("member", te(2, 10, 2, 10, "<member>$1</member>$0"), Collections.emptyList(), "member"), //
				c("employee", te(2, 10, 2, 10, "<employee>$1</employee>$0"), Collections.emptyList(), "employee"));

		// completion on text inside element with text content
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee> |</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 3, //
				c("person", te(2, 11, 2, 11, "<person>$1</person>$0"), Collections.emptyList(), "person"),
				c("member", te(2, 11, 2, 11, "<member>$1</member>$0"), Collections.emptyList(), "member"), //
				c("employee", te(2, 11, 2, 11, "<employee>$1</employee>$0"), Collections.emptyList(), "employee"));

		// completion on text inside element with text content
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee>| </employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 3, //
				c("person", te(2, 10, 2, 10, "<person>$1</person>$0"), Collections.emptyList(), "person"),
				c("member", te(2, 10, 2, 10, "<member>$1</member>$0"), Collections.emptyList(), "member"), //
				c("employee", te(2, 10, 2, 10, "<employee>$1</employee>$0"), Collections.emptyList(), "employee"));

		// completion on text inside element with text content
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee> | </employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 3, //
				c("person", te(2, 11, 2, 11, "<person>$1</person>$0"), Collections.emptyList(), "person"),
				c("member", te(2, 11, 2, 11, "<member>$1</member>$0"), Collections.emptyList(), "member"), //
				c("employee", te(2, 11, 2, 11, "<employee>$1</employee>$0"), Collections.emptyList(), "employee"));

		// completion on text inside element with text content
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee> | </employee></employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 3, //
				c("person", te(2, 11, 2, 11, "<person>$1</person>$0"), Collections.emptyList(), "person"),
				c("member", te(2, 11, 2, 11, "<member>$1</member>$0"), Collections.emptyList(), "member"), //
				c("employee", te(2, 11, 2, 11, "<employee>$1</employee>$0"), Collections.emptyList(), "employee"));

		// completion on text inside element with text content
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee></employee>\r\n" + //
				"|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", null, //
				c("member", te(3, 0, 3, 0, "<member$0>"), Arrays.asList(te(3, 2, 3, 10, "member")), "member"), //
				c("employee", te(3, 0, 3, 0, "<employee$0>"), Collections.emptyList(), "employee"));
	}

	@Test
	public void completionWithUnqualifiedElementFormDefault() throws BadLocationException {
		String xml = "<f:foo xmlns:f=\"http://foo\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"\r\n" + //
				"		http://foo xsd/foo-unqualified.xsd\">\r\n" + //
				"	<bar>\r\n" + //
				"		|";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/foo-unqualified.xml", //
				null, //
				c("item", te(5, 2, 5, 2, "<item></item>"), "item", null, null));
	}

	@Test
	public void completionWithUnqualifiedElementFormDefaultWithCatalog() throws BadLocationException {
		String xml = "<f:foo xmlns:f=\"http://foo\">\r\n" + //
				"	<bar>\r\n" + //
				"		|";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog.xml", //
				"src/test/resources/foo-unqualified.xml", //
				null, //
				c("item", te(2, 2, 2, 2, "<item></item>"), "item", null, null));
	}

	@Test
	public void generateOnlyStartElementOnElement() throws BadLocationException {
		// </employee> already exists, completion must generate only <employee>

		// completion on start tag
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 2, //
				c("member", te(2, 1, 2, 1, "member$0>"), Arrays.asList(te(2, 3, 2, 11, "member")), "<member"), //
				c("employee", te(2, 1, 2, 1, "employee$0>"), Collections.emptyList(), "<employee")); // <-- here only
																										// start
																										// employee is
		// generated

		// completion on start tag element
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<em|</employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 2, //
				c("member", te(2, 1, 2, 3, "member$0>"), Arrays.asList(te(2, 5, 2, 13, "member")), "<member"), //
				c("employee", te(2, 1, 2, 3, "employee$0>"), Collections.emptyList(), "<employee")); // <-- here only
																										// start
																										// employee is
		// generated

		// completion inside tag element
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/choice.xsd\">\r\n"
				+ //
				"<employee|></employee>";
		testCompletionSnippetSupportFor(xml, "src/test/resources/choice.xml", 2, //
				c("member", te(2, 1, 2, 9, "member$0"), Arrays.asList(te(2, 12, 2, 20, "member")), "<member"), //
				c("employee", te(2, 1, 2, 9, "employee$0"), Collections.emptyList(), "<employee")); // <-- here only
																									// start employee is
		// generated
	}

	@Test
	public void documentationAsPlainText() throws BadLocationException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|" + //
				"</project>";
		testCompletionWithCatalogFor(xml, c("groupId", te(3, 1, 3, 2, "<groupId></groupId>"), "<groupId", "3.0.0+" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"A universally unique identifier for a project. It is normal to use " + //
				"a fully-qualified package name to distinguish it from other projects with a similar name " + //
				"(eg. org.apache.maven)." + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Source: maven-4.0.0.xsd", MarkupKind.PLAINTEXT));
	}

	@Test
	public void documentationAsMarkdown() throws BadLocationException, MalformedURIException {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<|" + //
				"</project>";

		String mavenFileURI = getXMLSchemaFileURI("maven-4.0.0.xsd");
		testCompletionMarkdownSupportFor(xml, c("groupId", te(3, 1, 3, 2, "<groupId></groupId>"), "<groupId", "3.0.0+" + //
				"\n\n" + 
				"A universally unique identifier for a project. It is normal to use " + //
				"a fully-qualified package name to distinguish it from other projects with a similar name " + //
				"(eg. `org.apache.maven`)." + //
				"\n\n" + 
				"Source: [maven-4.0.0.xsd](" + mavenFileURI + ")", MarkupKind.MARKDOWN));
	}

	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true).replace("///",
				"/");
	}

	private static void testCompletionWithCatalogFor(String xml, CompletionItem... expectedItems)
			throws BadLocationException {
		testCompletionWithCatalogFor(xml, null, expectedItems);
	}

	private static void testCompletionWithCatalogFor(String xml, Integer expectedCount, CompletionItem... expectedItems)
			throws BadLocationException {
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog.xml", null, expectedCount, expectedItems);
	}

	private void testCompletionMarkdownSupportFor(String xml, CompletionItem... expectedItems)
			throws BadLocationException {
		
		
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		CompletionItemCapabilities completionItem = new CompletionItemCapabilities(false);
		completionItem.setDocumentationFormat(Arrays.asList(MarkupKind.MARKDOWN));
		completionCapabilities.setCompletionItem(completionItem);

		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);
		XMLAssert.testCompletionFor(new XMLLanguageService(), xml, "src/test/resources/catalogs/catalog.xml", null,
				null, null, sharedSettings, expectedItems);
	}

	private void testCompletionSnippetSupportFor(String xml, String fileURI, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		CompletionItemCapabilities completionItem = new CompletionItemCapabilities(true);
		completionItem.setDocumentationFormat(Arrays.asList(MarkupKind.MARKDOWN));
		completionCapabilities.setCompletionItem(completionItem);

		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);
		XMLAssert.testCompletionFor(new XMLLanguageService(), xml, null, null, fileURI, null, sharedSettings,
				expectedItems);
	}

	@Test
	public void customProtocolNamespaceSchemaLocationCompletionWhenCachingOn() throws BadLocationException {

		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

			@Override
			public URLStreamHandler createURLStreamHandler(String protocol) {
				if ("custom".equals(protocol)) {
					return new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							return XMLSchemaCompletionExtensionsTest.class.getResource(u.getPath()).openConnection();
						}
					};
				}
				return null;
			}
		});

		Consumer<XMLLanguageService> config = service -> {
			ContentModelManager contentModelManager = service.getComponent(ContentModelManager.class);

			contentModelManager.setUseCache(true);

			service.getResolverExtensionManager().registerResolver(new URIResolverExtension() {

				@Override
				public String resolve(String baseLocation, String publicId, String systemId) {
					if ("test://schema/format".equals(publicId)) {
						return "custom://test/xsd/Format.xsd";
					}
					return null;
				}

			});
		};

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<Configuration xmlns=\"test://schema/format\">\r\n" + //
				"  <ViewDefinitions>\r\n" + //
				"    <View><|";
		// Completion only with Name
		XMLAssert.testCompletionFor(new XMLLanguageService(), xml, (String) null, config,
				"src/test/resources/Format.xml", 4 + 2 /* CDATA and Comments */, true, c("Name", "<Name></Name>"),
				c("End with '</Configuration>'", "/Configuration>"),
				c("End with '</ViewDefinitions>'", "/ViewDefinitions>"), c("End with '</View>'", "/View>"));
	}

	@Test
	public void completionWithCatalogAndXSDInclude() throws BadLocationException {
		String xml = "<document xmlns=\"http://foobar.com/test\">\r\n" + //
				"	|\r\n" + //
				"</document>";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/include/catalog-include.xml", //
				"src/test/resources/test.xml", //
				null, //
				c("page", te(1, 1, 1, 1, "<page></page>"), "page", null, null));
	}

	@Test
	public void completionWithPrefixAttributeName() throws BadLocationException {
		String xml = "<sample xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xmlns:test=\"test-attrs\" xsi:noNamespaceSchemaLocation=\"xsd/attr-prefix/sample.xsd\"\r\n" + //
				"	| />"; // <-- completion with test:label
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/include/catalog-include.xml", //
				"src/test/resources/test.xml", //
				null, //
				c("test:label", te(2, 1, 2, 1, "test:label=\"\""), "test:label", null, null));
	}

	@Test
	public void completionWithCatalogAndWebApp() throws BadLocationException {
		String xml = "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\r\n"
				+ //
				"    version=\"2.4\">\r\n" + //
				"    |\r\n" + //
				"</web-app>";
		XMLAssert.testCompletionFor(xml, "src/test/resources/catalogs/catalog-web-app.xml", //
				"web.xml", //
				31, //
				c("servlet", te(4, 4, 4, 4, "<servlet></servlet>"), "servlet", null, null));
	}

	@Test
	public void completionWithAttrEnum() throws BadLocationException {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"      xsi:noNamespaceSchemaLocation=\"xsd/attr-enum.xsd\"\r\n" + //
				"      attribute=\"|\" />";
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/attr-enum.xml", 2, //
				c("value1", te(2, 17, 2, 17, "value1"), "value1", //
						"Documentation for attribute value1" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: attr-enum.xsd",
						MarkupKind.PLAINTEXT), //
				c("value2", te(2, 17, 2, 17, "value2"), "value2", //
						"Documentation for attribute" + //
								System.lineSeparator() + //
								System.lineSeparator() + //
								"Source: attr-enum.xsd",
						MarkupKind.PLAINTEXT));
	}
}