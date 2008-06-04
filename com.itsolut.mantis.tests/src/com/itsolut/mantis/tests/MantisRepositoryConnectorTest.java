/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryManager;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUi;

import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;

public class MantisRepositoryConnectorTest extends TestCase {

	private static final String REPOSITORY_ROOT = "http://mylyn-mantis.sourceforge.net/MantisTest/";

	private MantisRepositoryConnector connector;

	private TaskRepositoryManager manager;

	private String taskId = "12";

	private String expectedUrl = REPOSITORY_ROOT + "view.php?id=" + taskId;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		manager = TasksUiPlugin.getRepositoryManager();
		manager.clearRepositories(TasksUiPlugin.getDefault().getRepositoriesFilePath());

		connector = (MantisRepositoryConnector) manager.getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
		TasksUiPlugin.getSynchronizationScheduler().synchronize(manager.getDefaultRepository(MantisCorePlugin.REPOSITORY_KIND));

	}

	public void testGetUrl10x() {

		assertEquals("Wrong url for Mantis 1.0.x", expectedUrl, connector.getTaskUrl(
				REPOSITORY_ROOT + "mc/mantisconnect.php", taskId));
	}

	public void testGetUrl11x() {

		assertEquals("Wrong url for Mantis 1.1.x", expectedUrl, connector.getTaskUrl(
				REPOSITORY_ROOT + "api/soap/mantisconnect.php", taskId));
	}

}