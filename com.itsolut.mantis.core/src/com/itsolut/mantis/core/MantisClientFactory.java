/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.core;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisClientFactory {

    private static MantisClientFactory DEFAULT = new MantisClientFactory();

    private TaskRepositoryLocationFactory taskRepositoryLocationFactory;

    public void setTaskRepositoryLocationFactory(TaskRepositoryLocationFactory taskRepositoryLocationFactory) {

        this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;

    }

    public TaskRepositoryLocationFactory getTaskRepositoryLocationFactory() {

        Assert.isNotNull(taskRepositoryLocationFactory);

        return taskRepositoryLocationFactory;
    }

    public static MantisClientFactory getDefault() {

        return DEFAULT;
    }

    public IMantisClient createClient(AbstractWebLocation webLocation) throws MantisException {

        return new MantisClient(webLocation);

    }
}
