///*******************************************************************************
// * Copyright (c) 2016-2017 Red Hat Inc. and others.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * SPDX-License-Identifier: EPL-2.0
// *
// * Contributors:
// *     Red Hat, Inc. - initial API and implementation
// *******************************************************************************/
//package org.eclipse.lemminx.utils;
//
//import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;
//import static org.apache.commons.lang3.StringEscapeUtils.unescapeXml;
//
//import java.util.logging.Logger;
//
//import com.vladsch.flexmark.formatter.Formatter;
//import com.vladsch.flexmark.html.HtmlRenderer;
//import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
//import com.vladsch.flexmark.html2md.converter.HtmlConverterOptions;
//import com.vladsch.flexmark.parser.Parser;
//import com.vladsch.flexmark.util.data.MutableDataSet;
//
//
///**
// * Converts HTML content into Markdown equivalent.
// *
// * @author Fred Bricon
// */
//public class MarkdownConverter2 {
//
//	
//	final private static com.vladsch.flexmark.util.data.DataHolder OPTIONS = new MutableDataSet();
//	static final MutableDataSet FORMAT_OPTIONS = new MutableDataSet();
//	
//	private static final Logger LOGGER = Logger.getLogger(MarkdownConverter2.class.getName());
////	private static final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
//
//	private MarkdownConverter2(){
//	}
//
//	@SuppressWarnings("deprecation")
//	public static String convert(String remark, boolean useAtxHeading) {
//		
//		FORMAT_OPTIONS.set(Parser.EXTENSIONS, Parser.EXTENSIONS.get(OPTIONS));
//		
//	    Parser PARSER = Parser.builder(OPTIONS).build();
//	    Formatter RENDERER = Formatter.builder(FORMAT_OPTIONS).build();
//		
//	    
//	    String commonmark = RENDERER.render(remark);
//	    
////		var options = new MutableDataSet();
////		options.set(Parser.EXTENSIONS, Parser.EXTENSIONS.get(OPTIONS);
//
//		
//		
////		com.vladsch.flexmark.util.data.DataHolder options = new MutableDataSet();
////		options.toMutable().se(FlexmarkHtmlConverter.BR_AS_EXTRA_BLANK_LINES, false);
//		//options.set(HtmlRenderer.SOFT_BREAK, "");
//		//options.set(HtmlRenderer.HEADER_ID_REF_TEXT_TRIM_LEADING_SPACES, true);
//
//		if(!StringUtils.isTagOutsideOfBackticks(remark)) {
//			return unescapeXml(remark);
//		}
//		var converter = FlexmarkHtmlConverter.builder(options).build();
//		String converted = converter.convert(remark, -1);
//		return unescapeJava(converted);
//	}
//
//	// Backward compatible default
//	public static String convert(String remark) {
//		return convert(remark, false);
//	}
//	
//	
////	public static String convert(String html) {
////		//html = html.replaceAll("\r\n", System.lineSeparator());
////		//return converter.convert(html);
////		return unescapeJava(converter.convert(html));
////	}
////	private static String normalize(String s) {
////		return s ;//== null ? null : s.replaceAll("\s\s", " ").replaceAll("\s\n", "\n").trim();
////	}
//	
//}