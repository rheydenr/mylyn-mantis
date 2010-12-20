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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisTicket;

/**
 * Defines the requirements for classes that provide remote access to Mantis repositories.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public interface IMantisClient {

    public static final String CHARSET = "UTF-8";

    public static final String TIME_ZONE = "UTC";

    public static final String TICKET_ATTACHMENT_URL = "file_download.php?type=bug&file_id=";

    public static final String URL_SHOW_BUG = "view.php?id=";

    public static final String SEARCH_LIMIT = "searchLimit";
    
    public static final String PROJECT_NAME = "projectName";
    
    public static final String FILTER_NAME = "filterName";

    /**
     * Gets ticket with <code>id</code> from repository.
     * 
     * @param id
     *            the id of the ticket to get
     * @return the ticket
     * @throws MantisException
     *             thrown in case of a connection error
     */
    MantisTicket getTicket(int id, IProgressMonitor monitor) throws MantisException;

    /**
     * Queries tickets from repository. All found tickets are added to <code>result</code>.
     * 
     * @param query
     *            the search criteria
     * @param result
     *            the list of found tickets
     * @throws MantisException
     *             thrown in case of a connection error
     */
    void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException;

    /**
     * Validates the repository connection.
     * 
     * @throws MantisException
     *             thrown in case of a connection error
     */
    void validate(IProgressMonitor monitor) throws MantisException;

    /**
     * Updates cached repository details: milestones, versions etc.
     * 
     * @throws MantisException
     *             thrown in case of a connection error
     */
    void updateAttributes(IProgressMonitor monitor) throws MantisException;

	/**
	 * Updates cached repository details linked to the ticket with the specified id
	 * 
	 * @param monitor
	 * @param ticketId
	 * @throws MantisException 
	 */
	void updateAttributesForTask(IProgressMonitor monitor, Integer ticketId) throws MantisException;
    
    byte[] getAttachmentData(int id, IProgressMonitor monitor) throws MantisException;

    void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException;

    /**
     * @param monitor
     * @return the id of the created ticket
     */
    int createTicket(MantisTicket ticket, IProgressMonitor monitor) throws MantisException;

    void updateTicket(MantisTicket ticket, String comment, int timeTracking, IProgressMonitor monitor) throws MantisException;

    MantisCache getCache(IProgressMonitor progressMonitor) throws MantisException;

    /**
     * Determines if a task is completed or not.
     * 
     * <p>
     * The level of the status of the ticket is compared against the
     * <tt>bug_resolved_status_threshold</tt> of the Mantis installation.
     * </p>
     * 
     * <p>
     * This method may require remote calls if either mantis ticket stati or the threshold have not
     * been retrieved.
     * </p>
     * 
     * @param taskData
     *            the task data to read the status from
     * @param progressMonitor
     * @return true if the task is completed, false otherwise
     * @throws MantisException
     *             Failure in retrieving the required remote data
     */
    boolean isCompleted(TaskData taskData, IProgressMonitor progressMonitor) throws MantisException;

    /**
     * Returns the cache data
     * 
     * <p>This method should be used only for persistence purposes.</p>
     * 
     * @return the cache data
     */
    MantisCacheData getCacheData();

    /**
     * Sets the cache data.
     * 
     * <p>This method should only be used for setting a previously persisted cache data.</p>
     * @param cacheData
     */
    void setCacheData(MantisCacheData cacheData);
    
    boolean isTimeTrackingEnabled(IProgressMonitor monitor) throws MantisException;
    
    boolean isDueDateEnabled(IProgressMonitor monitor) throws MantisException;
    
}
