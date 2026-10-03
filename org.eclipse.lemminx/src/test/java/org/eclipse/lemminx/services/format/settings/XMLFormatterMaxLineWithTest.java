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
package org.eclipse.lemminx.services.format.settings;

import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with max line width.
 *
 */
public class XMLFormatterMaxLineWithTest extends AbstractCacheBasedTest {

	@Test
	public void splitText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(6);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a>abcde fghi</a>";
		String expected = "<a>abcde" + //
				System.lineSeparator() + //
				"  fghi</a>";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitTextWithSpace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(6);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a> abcde fghi</a>";
		String expected = "<a>" + //
				System.lineSeparator() + //
				"  abcde" + //
				System.lineSeparator() + //
				"  fghi</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 0, 4, System.lineSeparator() + "  "),
				te(0, 9, 0, 10, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitMixedText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(5);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a><b /> efgh</a>";
		String expected = "<a><b />" + //
				System.lineSeparator() + //
				"  efgh</a>";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void noSplit() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a>abcde fghi</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void splitWithAttribute() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<aaaaaaaaa bb=\"tes t\" c=\"a\" d=\"a\" e=\"a\"> </aaaaaaaaa>";
		String expected = "<aaaaaaaaa" + System.lineSeparator() + //
				"    bb=\"tes t\" c=\"a\"" + System.lineSeparator() + //
				"    d=\"a\" e=\"a\"> </aaaaaaaaa>";
		assertFormat(content, expected, settings, //
				te(0, 10, 0, 11, System.lineSeparator() + "    "), //
				te(0, 27, 0, 28, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeMultiLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\"a\" c=\"a\" d=\"a\" e=\"a\"> </a>";
		String expected = "<a bb=\"a\"" + System.lineSeparator() + //
				"    c=\"a\"" + System.lineSeparator() + //
				"    d=\"a\"" + System.lineSeparator() + //
				"    e=\"a\"> </a>";
		assertFormat(content, expected, settings, //
				te(0, 9, 0, 10, System.lineSeparator() + "    "), //
				te(0, 15, 0, 16, System.lineSeparator() + "    "), //
				te(0, 21, 0, 22, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeWithChild() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\"a\" c=\"a\" d=\"a\" e=\"a\"> <b bb=\"a\" c=\"a\" d=\"a\" e=\"a\"> </b> </a>";
		String expected = "<a bb=\"a\"" + System.lineSeparator() + //
				"    c=\"a\"" + System.lineSeparator() + //
				"    d=\"a\"" + System.lineSeparator() + //
				"    e=\"a\">" + System.lineSeparator() + //
				"    <b" + System.lineSeparator() + //
				"        bb=\"a\"" + System.lineSeparator() + //
				"        c=\"a\"" + System.lineSeparator() + //
				"        d=\"a\"" + System.lineSeparator() + //
				"        e=\"a\"> </b>" + System.lineSeparator() + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 9, 0, 10, System.lineSeparator() + "    "), //
				te(0, 15, 0, 16, System.lineSeparator() + "    "), //
				te(0, 21, 0, 22, System.lineSeparator() + "    "),
				te(0, 28, 0, 29, System.lineSeparator() + "    "), //
				te(0, 31, 0, 32, System.lineSeparator() + "        "), //
				te(0, 38, 0, 39, System.lineSeparator() + "        "),
				te(0, 44, 0, 45, System.lineSeparator() + "        "), //
				te(0, 50, 0, 51, System.lineSeparator() + "        "), //
				te(0, 62, 0, 63, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeMixedDontSplit() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\"test\"> <b/> h </a>";
		String expected = "<a" + System.lineSeparator() + //
				"    bb=\"test\">" + System.lineSeparator() + //
				"    <b/> h </a>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, System.lineSeparator() + "    "), //
				te(0, 13, 0, 14, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeMixedSplit() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\"test\"> <b/> gh </a>";
		String expected = "<a" + System.lineSeparator() + //
				"    bb=\"test\">" + System.lineSeparator() + //
				"    <b/>" + System.lineSeparator() + //
				"    gh </a>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, System.lineSeparator() + "    "), //
				te(0, 13, 0, 14, System.lineSeparator() + "    "), //
				te(0, 18, 0, 19, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\"test\"> <b c=\"test\"> </b>  </a>";
		String expected = "<a" + System.lineSeparator() + //
				"    bb=\"test\">" + System.lineSeparator() + //
				"    <b" + System.lineSeparator() + //
				"        c=\"test\"> </b>" + System.lineSeparator() + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, System.lineSeparator() + "    "), //
				te(0, 13, 0, 14, System.lineSeparator() + "    "), //
				te(0, 16, 0, 17, System.lineSeparator() + "        "), //
				te(0, 31, 0, 33, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeKeepSameLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<aaaaaaaaa bb=\"t tt\" c=\"a\" d=\"a\" e=\"a\"> </aaaaaaaaa>";
		String expected = "<aaaaaaaaa bb=\"t tt\"" + System.lineSeparator() + //
				"    c=\"a\" d=\"a\"" + System.lineSeparator() + //
				"    e=\"a\"> </aaaaaaaaa>";
		assertFormat(content, expected, settings, //
				te(0, 20, 0, 21, System.lineSeparator() + "    "), //
				te(0, 32, 0, 33, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeInvalid() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=>    </a>";
		String expected = "<a bb=> </a>";
		assertFormat(content, expected, settings, //
				te(0, 7, 0, 11, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeInvalidSingleQuote() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb=\">    </a>";
		String expected = "<a" + System.lineSeparator() + //
				"    bb=\">    </a>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeInvalidSpace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb = >    </a>";
		String expected = "<a bb=> </a>";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 6, ""), //
				te(0, 7, 0, 8, ""), //
				te(0, 9, 0, 13, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeInvalidSpaceSingleQuote() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb = \" >    </a>";
		String expected = "<a" + System.lineSeparator() + //
				"    bb=\" >    </a>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, System.lineSeparator() + "    "), //
				te(0, 5, 0, 6, ""), //
				te(0, 7, 0, 8, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void splitWithAttributeInvalidSpaceQuoted() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(10);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a bb = \" \" >    </a>";
		String expected = "<a bb=\" \"> </a>";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 6, ""), //
				te(0, 7, 0, 8, ""), //
				te(0, 11, 0, 12, ""), //
				te(0, 13, 0, 17, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void longText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<foo>\r\n" + //
				"	<para>    \r\n" + //
				"		vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\r\n"
				+ //
				"	</para>\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"  <para>\r\n" + //
				"    vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\r\n"
				+ //
				"  </para>\r\n" + //
				"</foo>";
		assertFormat(content, expected, settings, //
				te(0, 5, 1, 1, "\r\n  "), //
				te(1, 7, 2, 2, "\r\n    "), //
				te(2, 102, 3, 1, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void complex() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<ip_log  version=\"1.0\">\r\n" + //
				"  <project id=\"org.apache.ant\" version=\"1.10.12\" status=\"done\">\r\n" + //
				"    <info>\r\n" + //
				"      <name>Apache            Ant (all-in-one) ffffffffffffffffff        fffffffffffffffffffffffff    ggggggggggggggg</name>\r\n"
				+ //
				"      <repository>scm:git:git.eclipse.org:/gitroot/orbit/recipes.git</repository>\r\n" + //
				"      <location>apache-parent/ant/org.apache.ant</location>\r\n" + //
				"    </info>\r\n" + //
				"    <contact>\r\n" + //
				"      <name>Sarika          \r\n" + //
				"        Sinha</name>\r\n" + //
				"      <email>sarika.\r\n" + //
				"        \r\n" + //
				"        \r\n" + //
				"        sinha@in.ibm.com</email>\r\n" + //
				"      <company>IBM</company>\r\n" + //
				"    </contact>\r\n" + //
				"  </project>\r\n" + //
				"</ip_log>";

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<ip_log version=\"1.0\">\r\n" + //
				"  <project id=\"org.apache.ant\" version=\"1.10.12\" status=\"done\">\r\n" + //
				"    <info>\r\n" + //
				"      <name>Apache Ant (all-in-one) ffffffffffffffffff fffffffffffffffffffffffff\r\n" + //
				"        ggggggggggggggg</name>\r\n" + //
				"      <repository>scm:git:git.eclipse.org:/gitroot/orbit/recipes.git</repository>\r\n" + //
				"      <location>apache-parent/ant/org.apache.ant</location>\r\n" + //
				"    </info>\r\n" + //
				"    <contact>\r\n" + //
				"      <name>Sarika Sinha</name>\r\n" + //
				"      <email>sarika. sinha@in.ibm.com</email>\r\n" + //
				"      <company>IBM</company>\r\n" + //
				"    </contact>\r\n" + //
				"  </project>\r\n" + //
				"</ip_log>";

		assertFormat(content, expected, settings, //
				te(1, 7, 1, 9, " "), //
				te(4, 18, 4, 30, " "), //
				te(4, 65, 4, 73, " "), //
				te(4, 98, 4, 102, "\r\n        "), //
				te(9, 18, 10, 8, " "), //
				te(11, 20, 14, 8, " "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse/lemminx/issues/594
	@Test
	public void mixedText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(130);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all new\r\n"
				+ //
				"  XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"  <emphasis>following</emphasis> this\r\n" + //
				"  <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n" + //
				"\r\n" + //
				"  Using\r\n" + //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		String expected = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all\r\n"
				+ //
				"  new XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>. Using\r\n"
				+ //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		assertFormat(content, expected, settings, //
				te(1, 127, 1, 128, "\r\n  "), //
				te(1, 131, 2, 2, " "), //
				te(3, 31, 6, 2, " "), //
				te(6, 37, 7, 2, " "), //
				te(7, 56, 9, 2, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void mixedTextDefaultLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all new\r\n"
				+ //
				"  XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"  <emphasis>following</emphasis> this\r\n" + //
				"  <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n" + //
				"\r\n" + //
				"  Using\r\n" + //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		String expected = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>\r\n"
				+ //
				"  XML <alt>Extensible Markup Language</alt></acronym> namespaces are used to distinguish between\r\n" + //
				"  different element sets. In the last few years, almost all new XML grammars have used their own\r\n" + //
				"  namespace. It is easy to create compound documents that contain elements from different XML\r\n" + //
				"  vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n"
				+ //
				"  Using namespaces in your documents is very easy. Consider this simple article marked up in DocBook\r\n" + //
				"  V4.5:</para>";
		XMLAssert.assertFormat(null, content, expected, settings, "test.xml", Boolean.FALSE, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse/lemminx/issues/594
	@Test
	public void mixedTextIsChild() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(130);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<parent>\r\n" + //
				"<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all new\r\n"
				+ //
				"  XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"  <emphasis>following</emphasis> this\r\n" + //
				"  <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n" + //
				"\r\n" + //
				"  Using\r\n" + //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>"
				+ //
				"</parent> <para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all\r\n"
				+ //
				"  new XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>. Using\r\n"
				+ //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		String expected = "<parent>\r\n" + //
				"  <para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible\r\n"
				+ //
				"    Markup Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years,\r\n"
				+ //
				"    almost all new XML grammars have used their own namespace. It is easy to create compound documents that contain elements from\r\n"
				+ //
				"    different XML vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n"
				+ //
				"    Using namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>\r\n"
				+ //
				"</parent>\r\n" + //
				"<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all\r\n"
				+ //
				"  new XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>. Using\r\n"
				+ //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		assertFormat(content, expected, settings, //
				te(0, 8, 1, 0, "\r\n  "), //
				te(1, 123, 1, 124, "\r\n    "), //
				te(1, 130, 2, 2, " "), //
				te(2, 116, 2, 117, "\r\n    "), //
				te(2, 131, 3, 2, " "),
				te(3, 112, 3, 113, "\r\n    "), //
				te(3, 126, 4, 2, " "), //
				te(4, 31, 7, 2, " "), //
				te(7, 37, 8, 2, " "), //
				te(8, 56, 10, 2, "\r\n    "),
				te(10, 7, 11, 2, " "), //
				te(11, 107, 11, 107, "\r\n"), //
				te(11, 116, 11, 117, "\r\n"));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse/lemminx/issues/594
	@Test
	public void mixedTextNoJoinContentLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(130);
		settings.getFormattingSettings().setJoinContentLines(false);
		String content = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all new\r\n"
				+ //
				"  XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"  <emphasis>following</emphasis> this\r\n" + //
				"  <emphasis>design</emphasis>/<emphasis>rule</emphasis>.\r\n" + //
				"\r\n" + //
				"  Using\r\n" + //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";
		String expected = "<para>All DocBook V5.0 elements are in the namespace <uri>http://docbook.org/ns/docbook</uri>. <acronym>XML <alt>Extensible Markup\r\n"
				+ //
				"  Language</alt></acronym> namespaces are used to distinguish between different element sets. In the last few years, almost all\r\n"
				+ //
				"  new XML grammars have used their own namespace. It is easy to create compound documents that contain elements from different XML\r\n"
				+ //
				"  vocabularies. DocBook V5.0 is <emphasis>following</emphasis> this <emphasis>design</emphasis>/<emphasis>rule</emphasis>. Using\r\n"
				+ //
				"  namespaces in your documents is very easy. Consider this simple article marked up in DocBook V4.5:</para>";

		assertFormat(content, expected, settings, //
				te(1, 127, 1, 128, "\r\n  "), //
				te(1, 131, 2, 2, " "), //
				te(3, 31, 6, 2, " "), //
				te(6, 37, 7, 2, " "), //
				te(7, 56, 9, 2, " "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: mixed content with child elements exceeding maxLineWidth.
	// The second <g> overflows and must move to a new line as a whole unit,
	// not be split across lines.
	@Test
	public void mixedContentElementOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<root>\r\n" + //
				"  <p>text <b>bold</b> <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></p>\r\n"
				+ //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <p>text <b>bold</b> <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g>\r\n" + //
				"    <g>kkkkkkkkk</g></p>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(1, 90, 1, 90, "\r\n    "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: same as above with LF line endings.
	@Test
	public void mixedContentElementOverflowLF() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<root>\n" + //
				"  <p>text <b>bold</b> <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></p>\n"
				+ //
				"</root>";
		String expected = "<root>\n" + //
				"  <p>text <b>bold</b> <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g>\n" + //
				"    <g>kkkkkkkkk</g></p>\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(1, 90, 1, 90, "\n    "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: all elements fit on one line — no change.
	@Test
	public void mixedContentElementFitsOnLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<root>\r\n" + //
				"  <p>text <b>bold</b> <i>italic</i></p>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: mixed content with no maxLineWidth — no change.
	@Test
	public void mixedContentElementNoMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<root>\r\n" + //
				"  <p>text <b>bold</b> <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></p>\r\n"
				+ //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1131
	// Use case: sibling elements with mixed content that fit within maxLineWidth.
	// No change needed.
	@Test
	public void mixedContentConsistentWrapping() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<update>\r\n" + //
				"  <if test=\"username != null\">username=#{username},</if>\r\n" + //
				"  <if test=\"password != null and password != ''\">password=#{password},</if>\r\n" + //
				"</update>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: element after overflow moves to new line.
	// <b> makes the line exceed maxLineWidth, so <c> moves to a new line.
	@Test
	public void mixedContentMultipleOverflows() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<p>text <a>aaaaaaaaaa</a><b>bbbbbbbbbb</b><c>cccccccccc</c></p>";
		String expected = "<p>text <a>aaaaaaaaaa</a>" + //
				System.lineSeparator() + //
				"  <b>bbbbbbbbbb</b>" + //
				System.lineSeparator() + //
				"  <c>cccccccccc</c></p>";
		assertFormat(content, expected, settings, //
				te(0, 25, 0, 25, System.lineSeparator() + "  "), //
				te(0, 42, 0, 42, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: deeply nested mixed content with overflow.
	@Test
	public void mixedContentNestedOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<root>\r\n" + //
				"  <parent>\r\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaaaaaaa</a><b>bbbbbbbbbbb</b></p>\r\n" + //
				"  </parent>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <parent>\r\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaaaaaaa</a>\r\n" + //
				"      <b>bbbbbbbbbbb</b></p>\r\n" + //
				"  </parent>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(2, 40, 2, 40, "\r\n      "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: LF variant of deeply nested mixed content with overflow.
	@Test
	public void mixedContentNestedOverflowLF() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<root>\n" + //
				"  <parent>\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaaaaaaa</a><b>bbbbbbbbbbb</b></p>\n" + //
				"  </parent>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <parent>\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaaaaaaa</a>\n" + //
				"      <b>bbbbbbbbbbb</b></p>\n" + //
				"  </parent>\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(2, 40, 2, 40, "\n      "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: three adjacent elements, each overflowing.
	// <b> full element (17 chars) overflows remaining width (5 chars) → moves.
	// <c> full element (17 chars) overflows remaining width → moves.
	// <d> (9 chars) fits after <c> on the new line → stays.
	@Test
	public void mixedContentThreeAdjacentOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<p>text <a>aaaaaaaaaa</a><b>bbbbbbbbbb</b><c>cccccccccc</c><d>dd</d></p>";
		String expected = "<p>text <a>aaaaaaaaaa</a>" + //
				System.lineSeparator() + //
				"  <b>bbbbbbbbbb</b>" + //
				System.lineSeparator() + //
				"  <c>cccccccccc</c><d>dd</d></p>";
		assertFormat(content, expected, settings, //
				te(0, 25, 0, 25, System.lineSeparator() + "  "), //
				te(0, 42, 0, 42, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: element directly after parent start tag should NOT move to new line.
	@Test
	public void mixedContentFirstChildNoMove() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<a><b>text</b> more</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1797
	// Use case: text between elements (like '/') should not cause element move.
	@Test
	public void mixedContentTextSeparatedElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(50);
		String content = "<p>prefix <em>aaa</em>/<em>bbb</em> suffix</p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// MyBatis mapper: 2nd <if> has 28-space indentation that should be normalized
	@Test
	public void mixedContentMyBatisMapper() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<mapper>\r\n" + //
				"  <update id=\"updateEmp\"> update emp <set>\r\n" + //
				"      <if test=\"username != null and username != ''\">username=#{username},</if>\r\n" + //
				"                            <if test=\"password != null and password != ''\">\r\n" + //
				"    password=#{password},\r\n" + //
				"                </if>\r\n" + //
				"      <if test=\"entryDate != null\">entry_date=#{entryDate},</if>\r\n" + //
				"      <if test=\"deptId != null\">\r\n" + //
				"    dept_id=#{deptId},</if> update_time=#{updateTime} </set> where id=#{id} </update>\r\n" + //
				"</mapper>";
		String expected = "<mapper>\r\n" + //
				"  <update id=\"updateEmp\"> update emp <set>\r\n" + //
				"      <if test=\"username != null and username != ''\">username=#{username},</if>\r\n" + //
				"      <if test=\"password != null and password != ''\">\r\n" + //
				"        password=#{password},\r\n" + //
				"      </if>\r\n" + //
				"      <if test=\"entryDate != null\">entry_date=#{entryDate},</if>\r\n" + //
				"      <if test=\"deptId != null\">\r\n" + //
				"        dept_id=#{deptId},</if> update_time=#{updateTime} </set> where id=#{id} </update>\r\n" + //
				"</mapper>";
		assertFormat(content, expected, settings, //
				te(2, 79, 3, 28, "\r\n      "), //
				te(3, 75, 4, 4, "\r\n        "), //
				te(4, 25, 5, 16, "\r\n      "), //
				te(7, 32, 8, 4, "\r\n        "));
		assertFormat(expected, expected, settings);
	}

	// Adjacent elements: start tag alone fits but full element overflows
	// <g>hhh...h</g><g>kkkkkkkkk</g> — 2nd <g> must move to new line
	@Test
	public void mixedContentAdjacentElementFullOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		String content = "<root><bbbbbb>c<g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></bbbbbb></root>";
		String expected = "<root>" + //
				System.lineSeparator() + //
				"  <bbbbbb>c<g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g>" + //
				System.lineSeparator() + //
				"    <g>kkkkkkkkk</g></bbbbbb>" + //
				System.lineSeparator() + //
				"</root>";
		XMLAssert.assertFormat(null, content, expected, settings, "test.xml", Boolean.FALSE,
				te(0, 6, 0, 6, System.lineSeparator() + "  "),
				te(0, 99, 0, 99, System.lineSeparator() + "    "),
				te(0, 124, 0, 124, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Adjacent elements that fit within maxLineWidth stay on the same line
	@Test
	public void mixedContentAdjacentElementNoOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		String content = "<root><bbbbbb>c<g>short</g><g>ok</g></bbbbbb></root>";
		String expected = "<root>" + //
				System.lineSeparator() + //
				"  <bbbbbb>c<g>short</g><g>ok</g></bbbbbb>" + //
				System.lineSeparator() + //
				"</root>";
		XMLAssert.assertFormat(null, content, expected, settings, "test.xml", Boolean.FALSE,
				te(0, 6, 0, 6, System.lineSeparator() + "  "),
				te(0, 45, 0, 45, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Edge case tests: same content with different maxLineWidth values
	// Content: <p>text <a>aaa</a><b>bbb</b></p> (32 chars total)
	// After </a>: 18 chars used, <b>bbb</b> = 10 chars
	// Threshold: maxLineWidth=28 → fits (10 remaining), 27 → wraps (9 remaining)
	@Test
	public void mixedContentAdjacentEdgeFits() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(28);
		String content = "<p>text <a>aaa</a><b>bbb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void mixedContentAdjacentEdgeOverflows() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(27);
		String content = "<p>text <a>aaa</a><b>bbb</b></p>";
		String expected = "<p>text <a>aaa</a>" + //
				System.lineSeparator() + //
				"  <b>bbb</b></p>";
		assertFormat(content, expected, settings, //
				te(0, 18, 0, 18, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Edge case: maxLineWidth exactly equals line length → no wrap
	@Test
	public void mixedContentAdjacentEdgeExactFit() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(32);
		String content = "<p>text <a>aaa</a><b>bbb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Edge case: maxLineWidth=0 (disabled) → no wrap regardless of length
	@Test
	public void mixedContentAdjacentMaxLineWidthDisabled() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<p>text <a>aaa</a><b>bbb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Edge case: previous sibling is text (not element) → no adjacent element move
	// Text "x" separates </a> from <b>, so <b> is not adjacent to <a>.
	@Test
	public void mixedContentAdjacentWithTextSeparator() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<p>text <a>aaa</a>x<b>bbb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Edge case: self-closing element as previous sibling
	// <a/> before <b> triggers adjacent check, same as </a> before <b>.
	@Test
	public void mixedContentAdjacentSelfClosing() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<p>text <a/><b>bbbbbbbbb</b></p>";
		String expected = "<p>text <a />" + //
				System.lineSeparator() + //
				"  <b>bbbbbbbbb</b></p>";
		assertFormat(content, expected, settings, //
				te(0, 10, 0, 10, " "), //
				te(0, 12, 0, 12, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Edge case: inline spacing preserved — "</b> <i>" stays on one line
	// even when overflow, because there's no line break in the whitespace.
	@Test
	public void mixedContentInlineSpacingPreserved() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<p>text <b>bold</b> <i>italic</i></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/851
	@Test
	public void commentFormattingLineBreakingEarly() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100); // set to default
		String content = "<!--\r\n" + //
				"/*******************************************************************************\r\n" + //
				" * Lorem ipsum dolor sit amet, consectetur\r\n" + //
				" * © Copyright adipiscing elit. In eget magna ornare,\r\n" + //
				" *\r\n" + //
				" * pharetra sapienvitae, iaculis purus. Sed et dignissim lacus.\r\n" + //
				" * Morbi condimentum nisi eget sem laoreet placerat.\r\n" + //
				" * Pellentesque diam elit, vehicula et auctor a, euismod a enim.\r\n" + //
				" *******************************************************************************/\r\n" + //
				"-->\r\n" + //
				"</foo>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void commentFormattingLineBreakingEarlyAtMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100); // set to default
		String content = "<!--\r\n" + //
				"/*******************************************************************************\r\n" + //
				" * Lorem ipsum dolor sit amet, consectetur © Copyright adipiscing elit. In eget magna ornare, tester\r\n"
				+ // line width = 100
				" *\r\n" + //
				" * pharetra sapienvitae, iaculis purus. Sed et dignissim lacus. Morbi condimentum nisi eget sem laoreet placerat.\r\n"
				+ //
				" * Pellentesque diam elit, vehicula et auctor a, euismod a enim.\r\n" + //
				" *******************************************************************************/\r\n" + //
				"-->\r\n" + //
				"</foo>";
		String expected = "<!--\r\n" + //
				"/*******************************************************************************\r\n" + //
				" * Lorem ipsum dolor sit amet, consectetur © Copyright adipiscing elit. In eget magna ornare, tester\r\n"
				+ //
				" *\r\n" + //
				" * pharetra sapienvitae, iaculis purus. Sed et dignissim lacus. Morbi condimentum nisi eget sem\r\n" + //
				"laoreet placerat.\r\n"
				+ //
				" * Pellentesque diam elit, vehicula et auctor a, euismod a enim.\r\n" + //
				" *******************************************************************************/\r\n" + //
				"-->\r\n" + //
				"</foo>";
		assertFormat(content, expected, settings, //
				te(4, 95, 4, 96, "\r\n"));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse/lemminx/issues/1439
	@Test
	public void firstLineNoBreakAtMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(160);
		String content = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"></project>";
		String expected = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ System.lineSeparator() + //
				"  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"></project>";
		assertFormat(content, expected, settings, //
				te(0, 104, 0, 105, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse/lemminx/issues/1439
	@Test
	public void firstLineNoBreakAtMaxLineWidthDefaultWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100); // default max line width
		String content = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"></project>";
		String expected = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\""
				+ System.lineSeparator() + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ System.lineSeparator() + //
				"  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"></project>";
		assertFormat(content, expected, settings, //
				te(0, 50, 0, 51, System.lineSeparator() + "  "), //
				te(0, 104, 0, 105, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1131
	@Test
	public void noZigzagForNormalizeSpaceElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(60);
		String content = "<root>" + System.lineSeparator() + //
				"    <set>" + System.lineSeparator() + //
				"        <if test=\"username != null\">username=#{username},</if>" + System.lineSeparator() + //
				"        <if test=\"password != null\">password=#{password},</if>" + System.lineSeparator() + //
				"    </set>" + System.lineSeparator() + //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1131
	@Test
	public void noZigzagSingleWordText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<a attr=\"value\">text</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1010
	@Test
	public void noZigzagHexBinaryData() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		String content = "<root>" + System.lineSeparator() + //
				"  <atr>6f1087c8105312e302e30820408a00005001588306312e3021000000a5049f1c611a4d02156501ff</atr>" + System.lineSeparator() + //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1010
	@Test
	public void noZigzagHexBinaryDataNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(100);
		String content = "<root>" + System.lineSeparator() + //
				"  <record>" + System.lineSeparator() + //
				"    <atr>6f1087c8105312e302e30820408a00005001588306312e3021000000a5049f1c611a4d02156501ff</atr>" + System.lineSeparator() + //
				"  </record>" + System.lineSeparator() + //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1131
	@Test
	public void multiWordTextStillWraps() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a attr=\"value\">word1 word2</a>";
		String expected = "<a attr=\"value\">word1" + System.lineSeparator() + //
				"    word2</a>";
		assertFormat(content, expected, settings, //
				te(0, 21, 0, 22, System.lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	// --- Additional edge case tests for mixed content + maxLineWidth ---

	// Adjacent self-closing elements: <a/><b/> where both are self-closing
	// and full element width causes overflow
	@Test
	public void mixedContentAdjacentSelfClosingOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<p>text <a /><b /><c /><d /><e /><f /></p>";
		String expected = "<p>text <a /><b /><c /><d />" + //
				System.lineSeparator() + //
				"  <e /><f /></p>";
		assertFormat(content, expected, settings, //
				te(0, 28, 0, 28, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Mixed content with comment node: comment counts toward line width
	// but the space after is inline (no line break), so element stays
	@Test
	public void mixedContentCommentBeforeElementNoOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<p>text <!-- a long comment here --> <b>content</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Mixed content with empty child elements: <b></b> still has width
	@Test
	public void mixedContentEmptyChildElementOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<p>text <a>aaaaaaaaaa</a><b></b></p>";
		// <b></b> = 7 chars, after </a> available=5, 5-7<0 → wrap
		String expected = "<p>text <a>aaaaaaaaaa</a>" + //
				System.lineSeparator() + //
				"  <b></b></p>";
		assertFormat(content, expected, settings, //
				te(0, 25, 0, 25, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Text before element: prevSibling of <b> is a text node, not element,
	// so adjacent element overflow doesn't trigger. The text space stays inline.
	@Test
	public void mixedContentTextBeforeElementNoAdjacentOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<p>word1 word2 word3 word4 <b>bbbbbbbbb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Mixed content with tabSize=4 affects indent width and available width
	@Test
	public void mixedContentOverflowTabSize4() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<root>\r\n" + //
				"    <p>text <a>aaaa</a><b>bbbbbbbbbbb</b></p>\r\n" + //
				"</root>";
		// After </a>: available = 30-8(indent)-12(text <a>aaaa</a>) = 10
		// <b>bbbbbbbbbbb</b> = 18 chars, 10-18<0 → wrap
		String expected = "<root>\r\n" + //
				"    <p>text <a>aaaa</a>\r\n" + //
				"        <b>bbbbbbbbbbb</b></p>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(1, 23, 1, 23, "\r\n        "));
		assertFormat(expected, expected, settings);
	}

	// First child is very long text exceeding maxLineWidth:
	// text wraps to a new line, then <b> wraps as adjacent element overflow
	@Test
	public void mixedContentFirstChildExceedsMaxWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<p>aaaaaaaaaaaaaaaaaaaaaaaaa<b>bb</b></p>";
		// Text overflows maxLineWidth, gets moved to new line by DOMTextFormatter,
		// then <b> wraps because it's adjacent to a text node that caused overflow
		String expected = "<p>" + //
				System.lineSeparator() + //
				"  aaaaaaaaaaaaaaaaaaaaaaaaa" + //
				System.lineSeparator() + //
				"  <b>bb</b></p>";
		XMLAssert.assertFormat(null, content, expected, settings, "test.xml", Boolean.FALSE,
				te(0, 3, 0, 3, System.lineSeparator() + "  "),
				te(0, 28, 0, 28, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// @formatter:off/on with maxLineWidth: ensures availableLineWidth tracking
	// survives across formatter-off regions so subsequent content wraps correctly.
	@Test
	public void formatterOffOnWithMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<root>\r\n" + //
				"  <!-- @formatter:off -->\r\n" + //
				"  <pre>       preserved       spacing</pre>\r\n" + //
				"  <!-- @formatter:on -->\r\n" + //
				"  <p>text <a>aaaaaaaaaaaaaaaa</a><b>bbbbbbb</b></p>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <!-- @formatter:off -->\r\n" + //
				"  <pre>       preserved       spacing</pre>\r\n" + //
				"  <!-- @formatter:on -->\r\n" + //
				"  <p>text <a>aaaaaaaaaaaaaaaa</a>\r\n" + //
				"    <b>bbbbbbb</b></p>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(4, 33, 4, 33, "\r\n    "));
		assertFormat(expected, expected, settings);
	}

	// Multiple sibling elements — some fit, some don't (alternating)
	// Tests that availableLineWidth is correctly reset after each wrap
	@Test
	public void mixedContentAlternatingFitAndOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(25);
		String content = "<p>t <a>a</a><b>bbbbbbbb</b><c>c</c><d>dddddddd</d></p>";
		// After </a>: "p>t <a>a</a>" = 12 chars used, available=13
		// <b>bbbbbbbb</b> = 15 chars, 13-15<0 → wrap
		// After wrap+<b>: available=25-2(indent)-15=8
		// <c>c</c> = 7 chars, 8-7=1 → fits
		// <d>dddddddd</d> = 15 chars, 1-15<0 → wrap
		String expected = "<p>t <a>a</a>" + //
				System.lineSeparator() + //
				"  <b>bbbbbbbb</b><c>c</c>" + //
				System.lineSeparator() + //
				"  <d>dddddddd</d></p>";
		assertFormat(content, expected, settings, //
				te(0, 13, 0, 13, System.lineSeparator() + "  "), //
				te(0, 36, 0, 36, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Mixed content with whitespace-separated elements that have a line break
	// in the original content — whitespace with line break triggers different path
	@Test
	public void mixedContentWhitespaceLineBreakOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		String content = "<p>text <a>aaaaaaaaaaaa</a>\r\n  <b>bbbbbbbbbbb</b></p>";
		// After </a>: available < 0 → <b> stays on new line with proper indent
		String expected = "<p>text <a>aaaaaaaaaaaa</a>\r\n" + //
				"  <b>bbbbbbbbbbb</b></p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Deeply nested (3 levels) mixed content with overflow
	@Test
	public void mixedContentDeeplyNestedOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<root>\r\n" + //
				"  <div>\r\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaa</a><b>bbbbbbb</b></p>\r\n" + //
				"  </div>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <div>\r\n" + //
				"    <p>text <a>aaaaaaaaaaaaaaaa</a>\r\n" + //
				"      <b>bbbbbbb</b></p>\r\n" + //
				"  </div>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(2, 35, 2, 35, "\r\n      "));
		assertFormat(expected, expected, settings);
	}

	// Mixed content wrapping at exact maxLineWidth boundary (boundary - 1)
	// Verifies off-by-one correctness
	@Test
	public void mixedContentBoundaryMinusOne() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		// <p>aa <b>bb</b></p> = 18 chars
		// At maxLineWidth=17, <b>bb</b>=10 chars, after "aa "=7, 17-7=10, 10-10=0 → fits (not < 0)
		settings.getFormattingSettings().setMaxLineWidth(17);
		String content = "<p>aa <b>bb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void mixedContentBoundaryMinusTwo() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		// At maxLineWidth=16, inline space before <b> has no line break,
		// so MixedContent overflow doesn't trigger — content stays on one line
		settings.getFormattingSettings().setMaxLineWidth(16);
		String content = "<p>aa <b>bb</b></p>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Mixed content with text+element: after first element wraps,
	// "second" text is between elements via text node, and <b>'s
	// prevSibling is text, not element — stays on same line.
	@Test
	public void mixedContentRepeatedTextElementOverflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<p>first <a>aaaaaaaaaaa</a> second <b>bbbbbbbbb</b></p>";
		String expected = "<p>first <a>aaaaaaaaaaa</a>" + //
				System.lineSeparator() + //
				"  second <b>bbbbbbbbb</b></p>";
		assertFormat(content, expected, settings, //
				te(0, 27, 0, 28, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Mixed content: elements separated by whitespace with CRLF line endings
	// where whitespace contains line break — tests replaceLeftSpacesWithIndentation
	// with preserved line breaks behavior.
	@Test
	public void mixedContentPreservedNewlinesBetweenElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		settings.getFormattingSettings().setPreservedNewlines(2);
		String content = "<p>text <a>aaa</a>\r\n\r\n\r\n  <b>bbb</b></p>";
		// 3 blank lines reduced to preserved=2 (so 2+1=3 line separators)
		String expected = "<p>text <a>aaa</a>\r\n\r\n\r\n  <b>bbb</b></p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// NormalizeSpace: single long word that exceeds maxLineWidth
	// followed by end tag on the same line — no zigzag effect
	@Test
	public void noZigzagSingleWordExceedsWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		String content = "<root>\r\n" + //
				"  <a>verylongwordthatexceedsmaxlinewidth</a>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// NormalizeSpace: two words where second causes overflow → wrap
	@Test
	public void normalizeSpaceTextWrapping() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(20);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<root>\r\n" + //
				"  <item>short longerthanfifteen</item>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <item>short\r\n" + //
				"    longerthanfifteen</item>\r\n" + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(1, 13, 1, 14, "\r\n    "));
		assertFormat(expected, expected, settings);
	}

	// Comment wrapping: long comment content wraps at maxLineWidth
	@Test
	public void commentWrapsAtMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setJoinCommentLines(true);
		String content = "<!-- short word1 word2 word3 word4 word5 -->";
		String expected = "<!-- short word1 word2 word3" + System.lineSeparator() + //
				"word4 word5 -->";
		assertFormat(content, expected, settings, //
				te(0, 28, 0, 29, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// @formatter:off/on in mixed content: formatting off region preserves content,
	// but elements after @formatter:on resume wrapping — line is already overflowed.
	@Test
	public void formatterOffOnInsideMixedContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<p>text <!-- @formatter:off --> <b>   bold   </b> <!-- @formatter:on --> <i>italic</i></p>";
		String expected = "<p>text <!-- @formatter:off --> <b>   bold   </b> <!-- @formatter:on -->" + //
				System.lineSeparator() + //
				"  <i>italic</i></p>";
		assertFormat(content, expected, settings, //
				te(0, 72, 0, 73, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Mixed content idempotency: format → re-format produces same result.
	// <i> and <a> stay inline because the space before them has no line break.
	// <em> wraps because it's adjacent to </a> and the full element overflows.
	@Test
	public void mixedContentIdempotencyComplex() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(50);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<doc><p>first word <b>bold</b> middle <i>italic</i> <a>link</a><em>emphasis</em></p></doc>";
		String expected = "<doc>" + System.lineSeparator() + //
				"  <p>first word <b>bold</b> middle <i>italic</i> <a>link</a>" + System.lineSeparator() + //
				"    <em>emphasis</em></p>" + System.lineSeparator() + //
				"</doc>";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 5, System.lineSeparator() + "  "), //
				te(0, 63, 0, 63, System.lineSeparator() + "    "), //
				te(0, 84, 0, 84, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Mixed content with collapse empty element setting:
	// <b></b> collapsed to <b /> AND moved to new line because full element overflows.
	@Test
	public void mixedContentCollapseEmptyElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setEmptyElement(XMLFormattingOptions.EmptyElements.collapse);
		String content = "<p>text <a>aaaaaaaaaa</a><b></b></p>";
		String expected = "<p>text <a>aaaaaaaaaa</a>" + //
				System.lineSeparator() + //
				"  <b /></p>";
		assertFormat(content, expected, settings, //
				te(0, 25, 0, 25, System.lineSeparator() + "  "), //
				te(0, 27, 0, 32, " />"));
		assertFormat(expected, expected, settings);
	}

	// CDATA wrapping: CDATA content wraps at maxLineWidth.
	// <root> stays on same line as CDATA (no IgnoreSpace indentation for CDATA children).
	@Test
	public void cdataWrapsAtMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setJoinCDATALines(true);
		String content = "<root><![CDATA[word1 word2 word3 word4 word5]]></root>";
		String expected = "<root><![CDATA[word1 word2 word3 word4" + //
				System.lineSeparator() + //
				"  word5]]></root>";
		assertFormat(content, expected, settings, //
				te(0, 38, 0, 39, System.lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Attribute splitting + text wrapping combined:
	// Element with single attribute stays on one line (no split for single attr),
	// then text wraps at maxLineWidth.
	@Test
	public void attributeSplitAndTextWrap() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(2);
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<root><item attr=\"value\">word1 word2 word3</item></root>";
		String expected = "<root>" + System.lineSeparator() + //
				"  <item attr=\"value\">word1" + System.lineSeparator() + //
				"    word2 word3</item>" + System.lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(0, 6, 0, 6, System.lineSeparator() + "  "), //
				te(0, 30, 0, 31, System.lineSeparator() + "    "), //
				te(0, 49, 0, 49, System.lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits)
			throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, "test.xml", Boolean.FALSE, expectedEdits);
	}
}