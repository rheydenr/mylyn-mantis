/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class AllMantisTests {

	public static Test suite() {
		//		UrlConnectionUtil.initCommonsLoggingSettings();

		TestSuite suite = new TestSuite("Test for com.itsolut.mantis.tests");
		//$JUnit-BEGIN$
		suite.addTestSuite(MantisAxisClientTest.class);
		suite.addTestSuite(MantisTaskDataHandlerTest.class);
		suite.addTestSuite(MantisUtilsTest.class);
		suite.addTestSuite(MantisRepositoryConnectorTest.class);
		suite.addTestSuite(MantisRelationshipTest.class);
		//$JUnit-END$
		return suite;
	}

}