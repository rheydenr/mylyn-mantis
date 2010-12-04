/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.itsolut.mantis.core.model.MantisProject;

/**
 * Wizard for creating new Mantis tickets through a rich editor..
 * 
 * @author Steffen Pingel
 */
public class NewMantisTaskWizard extends NewTaskWizard implements INewWizard {

	public NewMantisTaskWizard(TaskRepository taskRepository,
			ITaskMapping taskSelection) {
		super(taskRepository, taskSelection);
		
		this.taskRepository = taskRepository;

		
		setWindowTitle("New Repository Task");
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		
		setNeedsProgressMonitor(true);
	}

	private TaskRepository taskRepository;

	private MantisProjectPage newTaskPage;


	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		newTaskPage = new MantisProjectPage(this.taskRepository);
		addPage(newTaskPage);
	}
	

	@Override
	protected ITaskMapping getInitializationData() {
		final MantisProject project = newTaskPage.getSelectedProject();
		return new TaskMapping() {
			@Override
			public String getProduct() {
				return project.getName();
			}
		};
	}

}
