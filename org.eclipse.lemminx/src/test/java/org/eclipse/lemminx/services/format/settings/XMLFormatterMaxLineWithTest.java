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

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits)
			throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, "test.xml", Boolean.FALSE, expectedEdits);
	}
}