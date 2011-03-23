/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.itsolut.mantis.core.util.HtmlFormatter;

/**
 * @author Robert Munteanu
 */
public class HtmlFormatterTest {

	private static final String OUTPUT_BR = "first<br/>second<br/>third";

	private static final String INPUT_BR = "first\nsecond\nthird";

	private static final String INPUT_PRE = "<pre>first\nsecond\nthird</pre>";

	private static final String OUTPUT_PRE = INPUT_PRE;

	private static final String INPUT_MIXED = "first\nsecond\n<pre>pre\nformatted</pre>third\nfourth";

	private static final String OUTPUT_MIXED = "first<br/>second<br/><pre>pre\nformatted</pre>third<br/>fourth";

	@Test
	public void linesHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_BR), is(OUTPUT_BR));
	}

	@Test
	public void preBlockDoesNotHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_PRE), is(OUTPUT_PRE));
	}

	@Test
	public void linesBeforeAndAfterPreBlockHaveBrAppended() {

		assertThat(HtmlFormatter.convertToDisplayHtml(INPUT_MIXED), is(OUTPUT_MIXED));
	}
	
	@Test
	public void linesWithBrAreConvertedToNewlines() {
		
		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_BR), is(INPUT_BR));
	}
	
	@Test
	public void linesWithPreAreNotConvertedToNewlines() {

		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_PRE), is(INPUT_PRE));
	}
	@Test
	public void linesWithBrBeforeAndAfterPreAreConvertedToNewlines() {

		assertThat(HtmlFormatter.convertFromDisplayHtml(OUTPUT_MIXED), is(INPUT_MIXED));
	}
}
