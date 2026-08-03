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
package org.eclipse.lemminx.dom.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * XML scanner tests.
 *
 */
public class XMLScannerTest {
  public Scanner scanner;
  //public TokenType token;

  @Test
  public void testSingleElement() {
    scanner = XMLScanner.createScanner("<hello></hello>");
    
    //<hello>
    assertOffsetAndToken(0, TokenType.StartTagOpen);
    assertOffsetAndToken(1, TokenType.StartTag, "hello");
    assertOffsetAndToken(6, TokenType.StartTagClose);
    //</hello>
    assertOffsetAndToken(7, TokenType.EndTagOpen);
    assertOffsetAndToken(9, TokenType.EndTag, "hello");
    assertOffsetAndToken(14, TokenType.EndTagClose);
  }

  @Test
  public void testNestedElement() {
    scanner = XMLScanner.createScanner("<hello><a></a></hello>");
    
    //<hello>
    assertOffsetAndToken(0, TokenType.StartTagOpen);
    assertOffsetAndToken(1, TokenType.StartTag, "hello");
    assertOffsetAndToken(6, TokenType.StartTagClose);
    //<a>
    assertOffsetAndToken(7, TokenType.StartTagOpen);
    assertOffsetAndToken(8, TokenType.StartTag, "a");
    assertOffsetAndToken(9, TokenType.StartTagClose);
    //</a>
    assertOffsetAndToken(10, TokenType.EndTagOpen);
    assertOffsetAndToken(12, TokenType.EndTag, "a");
    assertOffsetAndToken(13, TokenType.EndTagClose);
    //</hello>
    assertOffsetAndToken(14, TokenType.EndTagOpen);
    assertOffsetAndToken(16, TokenType.EndTag, "hello");
    assertOffsetAndToken(21, TokenType.EndTagClose);
  }

  @Test
  public void testMinusElement() {
    scanner = XMLScanner.createScanner("<hello-word></hello-word>");
    
    //<hello-word>
    assertOffsetAndToken(0, TokenType.StartTagOpen);
    assertOffsetAndToken(1, TokenType.StartTag, "hello-word");
    assertOffsetAndToken(11, TokenType.StartTagClose);
    //</hello-word>
    assertOffsetAndToken(12, TokenType.EndTagOpen);
    assertOffsetAndToken(14, TokenType.EndTag, "hello-word");
    assertOffsetAndToken(24, TokenType.EndTagClose);
  }
  
  @Test
  public void testElementWithAttribute() {
    scanner = XMLScanner.createScanner("<hello key=\"value\"></hello>");
    
    //<hello>
    assertOffsetAndToken(0, TokenType.StartTagOpen);
    assertOffsetAndToken(1, TokenType.StartTag, "hello");
    //key="value"
    assertOffsetAndToken(6, TokenType.Whitespace);
    assertOffsetAndToken(7, TokenType.AttributeName, "key");
    assertOffsetAndToken(10, TokenType.DelimiterAssign, "=");
    assertOffsetAndToken(11, TokenType.AttributeValue, "\"value\"");
    //end of <hello>
    assertOffsetAndToken(18, TokenType.StartTagClose);  
    //</hello>
    assertOffsetAndToken(19, TokenType.EndTagOpen);
    assertOffsetAndToken(21, TokenType.EndTag, "hello");
    assertOffsetAndToken(26, TokenType.EndTagClose);
  }

  @Test
  public void testStartTagNotClosed() {
    scanner = XMLScanner.createScanner("<hello");
    
    //<hello>
    assertOffsetAndToken(0, TokenType.StartTagOpen);
    assertOffsetAndToken(1, TokenType.StartTag, "hello");
   
    
  }


	@Test
	public void testName0() {
		scanner = XMLScanner.createScanner("<abc");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
	}


	@Test
	public void testName1() {
		scanner = XMLScanner.createScanner("<input");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "input");
	}


	@Test
	public void testName2() {
		scanner = XMLScanner.createScanner("< abc");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.Whitespace);
		assertOffsetAndToken(2, TokenType.StartTag, "abc");
	}


	@Test
	public void testName3() {
		scanner = XMLScanner.createScanner("< abc>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.Whitespace);
		assertOffsetAndToken(2, TokenType.StartTag, "abc");
		assertOffsetAndToken(5, TokenType.StartTagClose);
	}


	@Test
	public void testName4() {
		scanner = XMLScanner.createScanner("i <len;");
		assertOffsetAndToken(0, TokenType.Content);
		assertOffsetAndToken(2, TokenType.StartTagOpen);
		assertOffsetAndToken(3, TokenType.StartTag, "len");
	}

	@Test
	public void testName4a() {
		scanner = XMLScanner.createScanner("i <len a");
		assertOffsetAndToken(0, TokenType.Content);
		assertOffsetAndToken(2, TokenType.StartTagOpen);
		assertOffsetAndToken(3, TokenType.StartTag, "len");
		assertOffsetAndToken(6, TokenType.Whitespace);
		assertOffsetAndToken(7, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.EOS);
	}


	@Test
	public void testName5() {
		scanner = XMLScanner.createScanner("<");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
	}

	@Test
	public void testName115() {
		scanner = XMLScanner.createScanner("<a</a>");
		TokenType t = null;
		while(t != TokenType.EOS) {
			t = scanner.scan();
			
		}
	}


	@Test
	public void testName6() {
		scanner = XMLScanner.createScanner("</a");
		assertOffsetAndToken(0, TokenType.EndTagOpen);
		assertOffsetAndToken(2, TokenType.EndTag, "a");
	}


	@Test
	public void testName7() {
		scanner = XMLScanner.createScanner("<abc>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.StartTagClose);
	}


	@Test
	public void testName8() {
		scanner = XMLScanner.createScanner("<abc >");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.StartTagClose);
	}


	@Test
	public void testName9() {
		scanner = XMLScanner.createScanner("<foo:bar>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "foo:bar");
		assertOffsetAndToken(8, TokenType.StartTagClose);
	}


	@Test
	public void testName10() {
		scanner = XMLScanner.createScanner("</abc>");
		assertOffsetAndToken(0, TokenType.EndTagOpen);
		assertOffsetAndToken(2, TokenType.EndTag, "abc");
		assertOffsetAndToken(5, TokenType.EndTagClose);
	}


	@Test
	public void testName11() {
		scanner = XMLScanner.createScanner("</abc  >");
		assertOffsetAndToken(0, TokenType.EndTagOpen);
		assertOffsetAndToken(2, TokenType.EndTag, "abc");
		assertOffsetAndToken(5, TokenType.Whitespace);
		assertOffsetAndToken(7, TokenType.EndTagClose);
	}


	@Test
	public void testName12() {
		scanner = XMLScanner.createScanner("<abc />");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.StartTagSelfClose);
	}


	@Test
	public void testName13() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\">var i= 10;</script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.StartTagClose);
		assertOffsetAndToken(31, TokenType.Content);
		assertOffsetAndToken(41, TokenType.EndTagOpen);
		assertOffsetAndToken(43, TokenType.EndTag, "script");
		assertOffsetAndToken(49, TokenType.EndTagClose);
	}


	@Test
	public void testName14() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.StartTagClose);
	}


	@Test
	public void testName15() {
		scanner = XMLScanner.createScanner("var i= 10;");
		assertOffsetAndToken(0, TokenType.Content);
	}


	@Test
	public void testName16() {
		scanner = XMLScanner.createScanner("</script>");
		assertOffsetAndToken(0, TokenType.EndTagOpen);
		assertOffsetAndToken(2, TokenType.EndTag, "script");
		assertOffsetAndToken(8, TokenType.EndTagClose);
	}


	@Test
	public void testName17() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\">var i= 10;");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.StartTagClose);
		assertOffsetAndToken(31, TokenType.Content);
	}


	@Test
	public void testName18() {
		scanner = XMLScanner.createScanner("</script>");
		assertOffsetAndToken(0, TokenType.EndTagOpen);
		assertOffsetAndToken(2, TokenType.EndTag, "script");
		assertOffsetAndToken(8, TokenType.EndTagClose);
	}


	@Test
	public void testName19() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.StartTagClose);
	}


	@Test
	public void testName20() {
		scanner = XMLScanner.createScanner("var i= 10;</script>");
		assertOffsetAndToken(0, TokenType.Content);
		assertOffsetAndToken(10, TokenType.EndTagOpen);
		assertOffsetAndToken(12, TokenType.EndTag, "script");
		assertOffsetAndToken(18, TokenType.EndTagClose);
	}


	@Test
	public void testName21() {
		scanner = XMLScanner.createScanner("<script type=\"text/plain\">a\n<a</script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(25, TokenType.StartTagClose);
    assertOffsetAndToken(26, TokenType.Content);
    assertOffsetAndToken(28, TokenType.StartTagOpen);
    assertOffsetAndToken(29, TokenType.StartTag);
	}


	@Test
	public void testName22() {
		scanner = XMLScanner.createScanner("<script>a</script><script>b</script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.Content);
		assertOffsetAndToken(9, TokenType.EndTagOpen);
		assertOffsetAndToken(11, TokenType.EndTag, "script");
		assertOffsetAndToken(17, TokenType.EndTagClose);
		assertOffsetAndToken(18, TokenType.StartTagOpen);
		assertOffsetAndToken(19, TokenType.StartTag, "script");
		assertOffsetAndToken(25, TokenType.StartTagClose);
		assertOffsetAndToken(26, TokenType.Content);
		assertOffsetAndToken(27, TokenType.EndTagOpen);
		assertOffsetAndToken(29, TokenType.EndTag, "script");
		assertOffsetAndToken(35, TokenType.EndTagClose);
	}


	@Test
	public void testName23() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\"></script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.StartTagClose);
		assertOffsetAndToken(31, TokenType.EndTagOpen);
		assertOffsetAndToken(33, TokenType.EndTag, "script");
		assertOffsetAndToken(39, TokenType.EndTagClose);
	}


	@Test
	public void testName24() {
		scanner = XMLScanner.createScanner("<script>var i= 10;</script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.Content);
		assertOffsetAndToken(18, TokenType.EndTagOpen);
		assertOffsetAndToken(20, TokenType.EndTag, "script");
		assertOffsetAndToken(26, TokenType.EndTagClose);
	}


	@Test
	public void testName25() {
		scanner = XMLScanner.createScanner("<script type=\"text/javascript\" src=\"main.js\"></script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.Whitespace);
		assertOffsetAndToken(8, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.DelimiterAssign);
		assertOffsetAndToken(13, TokenType.AttributeValue);
		assertOffsetAndToken(30, TokenType.Whitespace);
		assertOffsetAndToken(31, TokenType.AttributeName);
		assertOffsetAndToken(34, TokenType.DelimiterAssign);
		assertOffsetAndToken(35, TokenType.AttributeValue);
		assertOffsetAndToken(44, TokenType.StartTagClose);
		assertOffsetAndToken(45, TokenType.EndTagOpen);
		assertOffsetAndToken(47, TokenType.EndTag, "script");
		assertOffsetAndToken(53, TokenType.EndTagClose);
	}


	@Test
	public void testName26() {
		scanner = XMLScanner.createScanner("<script><!-- alert(\"<script></script>\"); --></script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
    assertOffsetAndToken(8, TokenType.StartCommentTag);
    assertOffsetAndToken(12, TokenType.Comment);
    assertOffsetAndToken(41, TokenType.EndCommentTag);
		assertOffsetAndToken(44, TokenType.EndTagOpen);
		assertOffsetAndToken(46, TokenType.EndTag, "script");
		assertOffsetAndToken(52, TokenType.EndTagClose);
	}


	@Test
	public void testName27() {
		scanner = XMLScanner.createScanner("<script><!-- alert(\"<script></script>\"); </script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.StartCommentTag);
    assertOffsetAndToken(12, TokenType.Comment);
    
	}


	@Test
	public void testName28() {
		scanner = XMLScanner.createScanner("<script><!-- alert(\"</script>\"); </script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.StartCommentTag);
    assertOffsetAndToken(12, TokenType.Comment);
    assertOffsetAndToken(42, TokenType.EOS);
	}


	@Test
	public void testName29() {
		scanner = XMLScanner.createScanner("<script> alert(\"<script></script>\"); </script>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.Content);
		assertOffsetAndToken(16, TokenType.StartTagOpen);
		assertOffsetAndToken(17, TokenType.StartTag, "script");
		assertOffsetAndToken(23, TokenType.StartTagClose);
    assertOffsetAndToken(24, TokenType.EndTagOpen);
		assertOffsetAndToken(26, TokenType.EndTag, "script");
    assertOffsetAndToken(32, TokenType.EndTagClose);
    assertOffsetAndToken(33, TokenType.Content);
		assertOffsetAndToken(37, TokenType.EndTagOpen);
		assertOffsetAndToken(39, TokenType.EndTag, "script");
		assertOffsetAndToken(45, TokenType.EndTagClose);
	}


	@Test
	public void testName30() {
		scanner = XMLScanner.createScanner("<abc foo=\"bar\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
		assertOffsetAndToken(14, TokenType.StartTagClose);
	}


	@Test
	public void testName31() {
		scanner = XMLScanner.createScanner("<abc foo=\"bar\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
		assertOffsetAndToken(14, TokenType.StartTagClose);
	}

	@Test
	public void testAttributeSingleQuote() {
		scanner = XMLScanner.createScanner("<abc foo=\'bar\'>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
		assertOffsetAndToken(14, TokenType.StartTagClose);
	}


	@Test
	public void testName32() {
		scanner = XMLScanner.createScanner("<abc foo=\"\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
		assertOffsetAndToken(11, TokenType.StartTagClose);
	}


	@Test
	public void testName33() {
		scanner = XMLScanner.createScanner("<abc foo=\"bar\" bar=\"foo\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
		assertOffsetAndToken(14, TokenType.Whitespace);
		assertOffsetAndToken(15, TokenType.AttributeName);
		assertOffsetAndToken(18, TokenType.DelimiterAssign);
		assertOffsetAndToken(19, TokenType.AttributeValue);
		assertOffsetAndToken(24, TokenType.StartTagClose);
	}


	@Test
	public void testName34() {
		scanner = XMLScanner.createScanner("<abc foo=bar bar=help-me>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.Whitespace);
		assertOffsetAndToken(13, TokenType.AttributeName);
		assertOffsetAndToken(16, TokenType.DelimiterAssign);
		assertOffsetAndToken(17, TokenType.AttributeName);
		assertOffsetAndToken(24, TokenType.StartTagClose);
	}


	@Test
	public void testName35() {
		scanner = XMLScanner.createScanner("<abc foo=bar/>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.StartTagSelfClose);
	}


	@Test
	public void testName36() {
		scanner = XMLScanner.createScanner("<abc foo=  \"bar\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(11, TokenType.AttributeValue);
		assertOffsetAndToken(16, TokenType.StartTagClose);
	}


	@Test
	public void testName37() {
		scanner = XMLScanner.createScanner("<abc foo = \"bar\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.Whitespace);
		assertOffsetAndToken(9, TokenType.DelimiterAssign);
		assertOffsetAndToken(10, TokenType.Whitespace);
		assertOffsetAndToken(11, TokenType.AttributeValue);
		assertOffsetAndToken(16, TokenType.StartTagClose);
	}


	@Test
	public void testName38() {
		scanner = XMLScanner.createScanner("<abc foo>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.StartTagClose);
	}


	@Test
	public void testName39() {
		scanner = XMLScanner.createScanner("<abc foo bar>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.Whitespace);
		assertOffsetAndToken(9, TokenType.AttributeName);
		assertOffsetAndToken(12, TokenType.StartTagClose);
	}


	@Test
	public void testName40() {
		scanner = XMLScanner.createScanner("<abc foo!@#=\"bar\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(11, TokenType.DelimiterAssign);
		assertOffsetAndToken(12, TokenType.AttributeValue);
		assertOffsetAndToken(17, TokenType.StartTagClose);
	}


	@Test
	public void testName41() {
		scanner = XMLScanner.createScanner("<abc #myinput (click)=\"bar\" [value]=\"someProperty\" *ngIf=\"someCondition\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(13, TokenType.Whitespace);
		assertOffsetAndToken(14, TokenType.AttributeName);
		assertOffsetAndToken(21, TokenType.DelimiterAssign);
		assertOffsetAndToken(22, TokenType.AttributeValue);
		assertOffsetAndToken(27, TokenType.Whitespace);
		assertOffsetAndToken(28, TokenType.AttributeName);
		assertOffsetAndToken(35, TokenType.DelimiterAssign);
		assertOffsetAndToken(36, TokenType.AttributeValue);
		assertOffsetAndToken(50, TokenType.Whitespace);
		assertOffsetAndToken(51, TokenType.AttributeName);
		assertOffsetAndToken(56, TokenType.DelimiterAssign);
		assertOffsetAndToken(57, TokenType.AttributeValue);
		assertOffsetAndToken(72, TokenType.StartTagClose);
	}


	@Test
	public void testName42() {
		scanner = XMLScanner.createScanner("<abc foo=\">");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "abc");
		assertOffsetAndToken(4, TokenType.Whitespace);
		assertOffsetAndToken(5, TokenType.AttributeName);
		assertOffsetAndToken(8, TokenType.DelimiterAssign);
		assertOffsetAndToken(9, TokenType.AttributeValue);
	}


	@Test
	public void testName43() {
		scanner = XMLScanner.createScanner("<!--a-->");
		assertOffsetAndToken(0, TokenType.StartCommentTag);
		assertOffsetAndToken(4, TokenType.Comment);
		assertOffsetAndToken(5, TokenType.EndCommentTag);
	}


	@Test
	public void testName44() {
		scanner = XMLScanner.createScanner("<!--a>foo bar</a -->");
		assertOffsetAndToken(0, TokenType.StartCommentTag);
		assertOffsetAndToken(4, TokenType.Comment);
		assertOffsetAndToken(17, TokenType.EndCommentTag);
	}


	@Test
	public void testName45() {
		scanner = XMLScanner.createScanner("<!--a>\nfoo \nbar</a -->");
		assertOffsetAndToken(0, TokenType.StartCommentTag);
		assertOffsetAndToken(4, TokenType.Comment);
		assertOffsetAndToken(19, TokenType.EndCommentTag);
	}

	@Test
	public void testName49() {
		scanner = XMLScanner.createScanner("");
		assertOffsetAndToken(0, TokenType.EOS);
	}


	@Test
	public void testName50() {
		scanner = XMLScanner.createScanner("<!---");
		assertOffsetAndToken(0, TokenType.StartCommentTag);
		assertOffsetAndToken(4, TokenType.Comment);
	}


	@Test
	public void testName51() {
		scanner = XMLScanner.createScanner("<style>color:red");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "style");
		assertOffsetAndToken(6, TokenType.StartTagClose);
		assertOffsetAndToken(7, TokenType.Content);
	}


	@Test
	public void testName52() {
		scanner = XMLScanner.createScanner("<script>alert(\"!!\")");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag, "script");
		assertOffsetAndToken(7, TokenType.StartTagClose);
		assertOffsetAndToken(8, TokenType.Content);
	}

	@Test
	public void testPrologNormal() {
		scanner = XMLScanner.createScanner("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		assertOffsetAndToken(0, TokenType.StartPrologOrPI);
		assertOffsetAndToken(2, TokenType.PrologName, "xml");
		assertOffsetAndToken(5, TokenType.Whitespace);
		assertOffsetAndToken(6, TokenType.AttributeName);
		assertOffsetAndToken(13, TokenType.DelimiterAssign);
		assertOffsetAndToken(14, TokenType.AttributeValue);
		assertOffsetAndToken(19, TokenType.Whitespace);
		assertOffsetAndToken(20, TokenType.AttributeName);
		assertOffsetAndToken(28, TokenType.DelimiterAssign);
		assertOffsetAndToken(29, TokenType.AttributeValue);
		assertOffsetAndToken(36, TokenType.PrologEnd);
	}
	@Test
	public void testPrologInsideElement() {
		scanner = XMLScanner.createScanner("<a><?xml version=\"1.0\" encoding=\"UTF-8\"?></a>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.StartTagClose);
		assertOffsetAndToken(0 + 3, TokenType.StartPrologOrPI);
		assertOffsetAndToken(2 + 3, TokenType.PrologName, "xml");
		assertOffsetAndToken(5 + 3, TokenType.Whitespace);
		assertOffsetAndToken(6 + 3, TokenType.AttributeName);
		assertOffsetAndToken(13 + 3, TokenType.DelimiterAssign);
		assertOffsetAndToken(14 + 3, TokenType.AttributeValue);
		assertOffsetAndToken(19 + 3, TokenType.Whitespace);
		assertOffsetAndToken(20 + 3, TokenType.AttributeName);
		assertOffsetAndToken(28 + 3, TokenType.DelimiterAssign);
		assertOffsetAndToken(29 + 3, TokenType.AttributeValue);
		assertOffsetAndToken(36 + 3, TokenType.PrologEnd);
		assertOffsetAndToken(41, TokenType.EndTagOpen);
		assertOffsetAndToken(43, TokenType.EndTag);
		assertOffsetAndToken(44, TokenType.EndTagClose);
	}

	@Test
	public void testPINormal() {
		scanner = XMLScanner.createScanner("<?m2e execute onConfiguration?>");
		assertOffsetAndToken(0, TokenType.StartPrologOrPI);
		assertOffsetAndToken(2, TokenType.PIName, "m2e");
		assertOffsetAndToken(5, TokenType.Whitespace);
		assertOffsetAndToken(6, TokenType.PIContent);
		assertOffsetAndToken(29, TokenType.PIEnd);
	}

	@Test
	public void testPINormalInsideElement() {
		scanner = XMLScanner.createScanner("<a><?m2e execute onConfiguration?></a>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.StartTagClose);
		assertOffsetAndToken(3, TokenType.StartPrologOrPI);
		assertOffsetAndToken(5, TokenType.PIName, "m2e");
		assertOffsetAndToken(8, TokenType.Whitespace);
		assertOffsetAndToken(9, TokenType.PIContent);
		assertOffsetAndToken(32, TokenType.PIEnd);
		assertOffsetAndToken(34, TokenType.EndTagOpen);
		assertOffsetAndToken(36, TokenType.EndTag);
		assertOffsetAndToken(37, TokenType.EndTagClose);
	}

	@Test
	public void testPIWithBracketsInContent() {
		scanner = XMLScanner.createScanner("<a><?form <varGrp/>?></a>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.StartTagClose);
		assertOffsetAndToken(3, TokenType.StartPrologOrPI);
		assertOffsetAndToken(5, TokenType.PIName, "form");
		assertOffsetAndToken(9, TokenType.Whitespace);
		assertOffsetAndToken(10, TokenType.PIContent);
		assertOffsetAndToken(19, TokenType.PIEnd);
		assertOffsetAndToken(21, TokenType.EndTagOpen);
		assertOffsetAndToken(23, TokenType.EndTag);
		assertOffsetAndToken(24, TokenType.EndTagClose);
	}

	@Test
	public void testMissingClosingBracket() {
		scanner = XMLScanner.createScanner("<a</a>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.EndTagOpen);
		assertOffsetAndToken(4, TokenType.EndTag);
		assertOffsetAndToken(5, TokenType.EndTagClose);
	}

	@Test
	public void testMissingClosingBracket2() {
		scanner = XMLScanner.createScanner("<a></a<b></b>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.StartTagClose);
		assertOffsetAndToken(3, TokenType.EndTagOpen);
		assertOffsetAndToken(5, TokenType.EndTag);
		assertOffsetAndToken(6, TokenType.StartTagOpen);
		assertOffsetAndToken(7, TokenType.StartTag);
		assertOffsetAndToken(8, TokenType.StartTagClose);
		assertOffsetAndToken(9, TokenType.EndTagOpen);
		assertOffsetAndToken(11, TokenType.EndTag);
		assertOffsetAndToken(12, TokenType.EndTagClose);
	}

	@Test
	public void testCDATAWithBracketsInText() {
		scanner = XMLScanner.createScanner("<a><![CDATA[<>]]></a>");
		assertOffsetAndToken(0, TokenType.StartTagOpen);
		assertOffsetAndToken(1, TokenType.StartTag);
		assertOffsetAndToken(2, TokenType.StartTagClose);
		assertOffsetAndToken(3, TokenType.CDATATagOpen);
		assertOffsetAndToken(12, TokenType.CDATAContent);
		assertOffsetAndToken(14, TokenType.CDATATagClose);
		assertOffsetAndToken(17, TokenType.EndTagOpen);
		assertOffsetAndToken(19, TokenType.EndTag);
		assertOffsetAndToken(20, TokenType.EndTagClose);
	}

  //----------Tools-------------------------------------------------------


  public void assertOffsetAndToken(int tokenOffset, TokenType tokenType) {
	TokenType token = scanner.scan();
    assertEquals(tokenOffset, scanner.getTokenOffset());
    assertEquals(tokenType, token);
  }

  public void assertOffsetAndToken(int tokenOffset, TokenType tokenType, String tokenText) {
	TokenType token = scanner.scan();
    assertEquals(tokenOffset, scanner.getTokenOffset());
	assertEquals(tokenType, token);
    assertEquals(tokenText, scanner.getTokenText());
  }
}
