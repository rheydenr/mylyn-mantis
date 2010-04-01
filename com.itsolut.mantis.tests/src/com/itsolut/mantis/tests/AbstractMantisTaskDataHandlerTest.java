/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;

import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;

public abstract class AbstractMantisTaskDataHandlerTest extends AbstractIntegrationTest {

	public void testUnableToCloneNullTask() {
		
		AbstractTaskDataHandler taskDataHandler = newTaskDataHandler();
		
		assertFalse(taskDataHandler.canInitializeSubTaskData(repositoryAccessor.getRepository(), null));
	}

	private AbstractTaskDataHandler newTaskDataHandler() {
		MantisRepositoryConnector connector = new MantisRepositoryConnector();
		AbstractTaskDataHandler taskDataHandler = connector.getTaskDataHandler();
		return taskDataHandler;
	}
	
	public void testUnableToCloneTaskWithoutProperKey() {
		
		assertFalse(newTaskDataHandler().canInitializeSubTaskData(repositoryAccessor.getRepository(), newDummyTask()));
	}

	private ITask newDummyTask() {
		
		return new TaskTask(MantisCorePlugin.REPOSITORY_KIND, repositoryAccessor.getLocation().getUrl(), "1");
	}
	
	public void testAbleToCloneTaskWithProperKey() {
		
		ITask task = newDummyTask();
		task.setAttribute(MantisRepositoryConnector.TASK_KEY_SUPPORTS_SUBTASKS, Boolean.TRUE.toString());
		assertTrue(newTaskDataHandler().canInitializeSubTaskData(repositoryAccessor.getRepository(), task));
	}

}