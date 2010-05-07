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
 * Copyright (c) 2007, 2008 - 2007 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     David Carver - STAR - fixed issue with background synchronization of repository.
 *     David Carver - STAR - Migrated to Mylyn 3.0
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.eclipse.mylyn.tasks.ui.TasksUi;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Dave Carver - STAR - Standards for Technology in Automotive Retail
 * @author Chris Hane
 */
@SuppressWarnings("restriction")
public class MantisRepositoryConnector extends AbstractRepositoryConnector {

    private final static String CLIENT_LABEL = "Mantis (supports connector 0.0.5 or 1.1.0a4 or greater only)";
    public static final String TASK_KEY_SUPPORTS_SUBTASKS = "SupportsSubtasks";

    private MantisClientManager clientManager;

    private final MantisTaskDataHandler offlineTaskHandler = new MantisTaskDataHandler(this);

    private final MantisAttachmentHandler attachmentHandler = new MantisAttachmentHandler(this);

    public MantisRepositoryConnector() {

        MantisCorePlugin.getDefault().setConnector(this);
    }

    @Override
    public boolean canCreateNewTask(TaskRepository repository) {

        return true;
    }

    @Override
    public boolean canCreateTaskFromKey(TaskRepository repository) {

        return true;
    }

    @Override
    public String getLabel() {

        return CLIENT_LABEL;
    }

    @Override
    public String getConnectorKind() {

        return MantisCorePlugin.REPOSITORY_KIND;
    }

    @Override
    public String getRepositoryUrlFromTaskUrl(String url) {

        // There is no way of knowing the proper URL for the repository
        // so we return at least a common prefix which should be good
        // enough for TaskRepositoryManager#getConnectorForRepositoryTaskUrl
        if (url == null) {
            return null;
        }
        int index = url.lastIndexOf(IMantisClient.URL_SHOW_BUG);
        return index == -1 ? null : url.substring(0, index);
    }

    @Override
    public String getTaskIdFromTaskUrl(String url) {

        if (url == null) {
            return null;
        }
        int index = url.lastIndexOf(IMantisClient.URL_SHOW_BUG);
        return index == -1 ? null : url.substring(index + IMantisClient.URL_SHOW_BUG.length());
    }

    @Override
    public String getTaskUrl(String repositoryUrl, String taskId) {

        return MantisUtils.getRepositoryBaseUrl(repositoryUrl) + IMantisClient.URL_SHOW_BUG + taskId.toString();
    }

    @Override
    public AbstractTaskAttachmentHandler getTaskAttachmentHandler() {

        return this.attachmentHandler;
    }

    @Override
    public AbstractTaskDataHandler getTaskDataHandler() {

        return offlineTaskHandler;
    }

    @Override
    public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
            ISynchronizationSession event, IProgressMonitor monitor) {

        final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
        monitor.beginTask("Querying repository", IProgressMonitor.UNKNOWN);

        IMantisClient client;
        try {
            client = getClientManager().getRepository(repository);
            client.search(MantisUtils.getMantisSearch(query), tickets, monitor);
            for (MantisTicket ticket : tickets) {
                TaskData taskData = offlineTaskHandler.createTaskDataFromTicket(client, repository, ticket, monitor);
                taskData.setPartial(true); // IMantisClient.search returns partial data
                resultCollector.accept(taskData);
            }
        } catch (Throwable e) {
            // MantisCorePlugin.log(e);
            return MantisCorePlugin.toStatus(e);
        }

        return Status.OK_STATUS;

    }

    protected void updateAttributes(TaskRepository repository, IProgressMonitor monitor) throws CoreException, MantisException {

        IMantisClient client = getClientManager().getRepository(repository);
        client.updateAttributes(monitor);
    }

    public synchronized MantisClientManager getClientManager() {

        File cacheFile = MantisCorePlugin.getDefault().getRepositoryAttributeCachePath().toFile();
        if (clientManager == null)
            clientManager = new MantisClientManager(cacheFile);
        return clientManager;
    }

    public static String getTicketDescription(MantisTicket ticket) {

        return ticket.getValue(Key.SUMMARY);
    }

    public static String getTicketDescription(TaskData taskData) {

        return taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.DESCRIPTION.toString()).toString();
    }

    public void stop() {

        if (clientManager != null)
            clientManager.persistCache();

    }

    public static String getDisplayUsername(TaskRepository repository) {

        if (repository.getCredentials(AuthenticationType.REPOSITORY) == null) {
            return IMantisClient.DEFAULT_USERNAME;
        }
        return repository.getUserName();
    }

    @Override
    public String getTaskIdPrefix() {

        return "#";
    }

    public static int getTicketId(String taskId) throws CoreException {

        try {
            return Integer.parseInt(taskId);
        } catch (NumberFormatException e) {
            throw new CoreException(MantisCorePlugin.errorStatus("Invalid ticket id: " + taskId + ".", e));
        }
    }

    // For the repositories, perform the queries to get the latest information
    // about the
    // tasks. This allows the connector to get a limited list of items instead
    // of every
    // item in the repository. Next check to see if the tasks have changed since
    // the
    // last synchronization. If so, add their ids to a List.
    /**
     * Gets the changed tasks for a given query
     * 
     * <p>For the <tt>repository</tt>, run the <tt>query</tt> to get the latest information about the
     * tasks. This allows the connector to get a limited list of items instead of every item in the
     * repository. Next check to see if the tasks have changed since the last synchronization. If
     * so, add their ids to a List.</p>
     * 
     * @param monitor
     * 
     * @return the ids of the changed tasks, or an empty list
     */
    private List<Integer> getChangedTasksByQuery(IRepositoryQuery query, TaskRepository repository, Date since,
            IProgressMonitor monitor) {

        if (MantisCorePlugin.DEBUG)
            MantisCorePlugin.debug("Looking for tasks changed in query " + query + " since " + since + " .", null);
        
        final List<MantisTicket> tickets = new ArrayList<MantisTicket>();
        List<Integer> changedTickets = new ArrayList<Integer>();

        IMantisClient client;
        try {
            client = getClientManager().getRepository(repository);
            client.search(MantisUtils.getMantisSearch(query), tickets, monitor);

            for (MantisTicket ticket : tickets) {
                if (ticket.getLastChanged() != null) {
                    if (ticket.getLastChanged().compareTo(since) > 0) {
//                        MantisCorePlugin.debug("Ticket with id " + ticket.getId() + " and lastChanged " + ticket.getLastChanged() + " marked as changed.", null);
                        changedTickets.add(Integer.valueOf(ticket.getId()));
                    }
                }
            }
        } catch (Throwable e) {
            MantisCorePlugin.error("Failed getting new tasks for query " + query.getSummary() + " . Message : "
                    + e.getMessage() + " .", e);
            return Collections.emptyList();
        }
        
        if ( MantisCorePlugin.DEBUG)
            MantisCorePlugin.debug("Found " + changedTickets.size() + " changed tickets.", null);
        
        return changedTickets;
    }

    @Override
    public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {

        try {
            updateAttributes(repository, monitor);
        } catch (MantisException e) {
            throw new CoreException(MantisCorePlugin.ioErrorRepositoryStatus(repository, "Could not update attributes", e));
        }

    }

    @Override
    public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor)
            throws CoreException {

        return offlineTaskHandler.getTaskData(repository, taskId, monitor);
    }

    // Based off of Trac Implementation.
    @Override
    public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {

        // always take into account the modification date since it
        // is returned by the search query
        TaskMapper mapper = getTaskMapper(taskData);

        Date repositoryDate = mapper.getModificationDate();
        Date taskModDate = task.getModificationDate();

        if (repositoryDate != null && repositoryDate.equals(taskModDate)) {
            return false;
        }
        return true;
    }

    @Override
    public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {

        TaskMapper scheme = getTaskMapper(taskData);
        scheme.applyTo(task);

        boolean completed = false;

        try {
            IMantisClient client = getClientManager().getRepository(taskData.getAttributeMapper().getTaskRepository());
            completed = client.isCompleted(taskData, new NullProgressMonitor());
        } catch (MantisException e) {
            MantisCorePlugin.error(e);
        }

        Date completionDate = completed ? scheme.getModificationDate() : null;

        task.setCompletionDate(completionDate);

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager()
                .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);

        task.setUrl(connector.getTaskUrl(repository.getRepositoryUrl(), taskData.getTaskId()));

        boolean supportsSubtasks = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey()) != null;

        task.setAttribute(TASK_KEY_SUPPORTS_SUBTASKS, Boolean.toString(supportsSubtasks));

    }

    public TaskMapper getTaskMapper(final TaskData taskData) {

        return new MantisTaskMapper(taskData);
    }

    @Override
    public ITaskMapping getTaskMapping(TaskData taskData) {

        return getTaskMapper(taskData);
    }

    @Override
    public void preSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {

        if (!event.isFullSynchronization()) {
            return;
        }

        // No Tasks, don't contact the repository
        if (event.getTasks().isEmpty()) {
            return;
        }

        TaskRepository repository = event.getTaskRepository();

        if (repository.getSynchronizationTimeStamp() == null || repository.getSynchronizationTimeStamp().length() == 0) {
            for (ITask task : event.getTasks())
                event.markStale(task);
            return;
        }

        Date since = new Date(0);
        try {
            if (repository.getSynchronizationTimeStamp().length() > 0)
                since = MantisUtils.parseDate(Long.valueOf(repository.getSynchronizationTimeStamp()));
        } catch (NumberFormatException e) {
             MantisCorePlugin.warn("Failed parsing repository synchronisationTimestamp " + repository.getSynchronizationTimeStamp() + " .", e);
        }


        // Run the queries to get the list of tasks currently meeting the query
        // criteria. The result returned are only the ids that have changed.
        // Next checkt to see if any of these ids matches the ones in the
        // task list. If so, then set it to stale.
        // 
        // The prior implementation retireved each id individually, and
        // checked it's date, this caused unnecessary SOAP traffic during
        // synchronization.
        event.setNeedsPerformQueries(false);
        List<IRepositoryQuery> queries = getMantisQueriesFor(repository);

        monitor.beginTask("Retrieving queries for repository", queries.size());

        for (IRepositoryQuery query : queries) {

            List<Integer> taskIds = this.getChangedTasksByQuery(query, repository, since, monitor);

            MantisCorePlugin.debug("Found " + taskIds.size() + " changed task ids.", null);

            for (Integer taskId : taskIds) {
                for (ITask task : event.getTasks()) {
                    if (getTicketId(task.getTaskId()) == taskId.intValue()) {
                        event.setNeedsPerformQueries(true);
                        event.markStale(task);
                    }
                }
            }

            monitor.worked(1);
        }
    }

    private List<IRepositoryQuery> getMantisQueriesFor(TaskRepository taskRespository) {

        List<IRepositoryQuery> queries = new ArrayList<IRepositoryQuery>();

        for (IRepositoryQuery query : TasksUiInternal.getTaskList().getQueries()) {

            boolean isMantisQuery = MantisCorePlugin.REPOSITORY_KIND.equals(query.getConnectorKind());
            boolean belongsToThisRepository = query.getRepositoryUrl().equals(taskRespository.getUrl());

            if (isMantisQuery && belongsToThisRepository) {
                queries.add(query);
            }
        }

        return queries;
    }

    @Override
    public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {

        try {
            monitor.beginTask("", 1);
            if (event.isFullSynchronization()) {
                Date date = this.getSynchronizationTimestamp(event);
                
                MantisCorePlugin.info("Synchronisation timestamp from event is " + date + " .");
                if (date != null) {
                    event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(date) + "");
                } else {
                    date = new Date();
                    event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(date) + "");
                }
            }
        } catch (Exception ex) {
            MantisCorePlugin.toStatus(ex);
            Date date = new Date();
            event.getTaskRepository().setSynchronizationTimeStamp(MantisUtils.toMantisTime(date) + "");
        } finally {
            monitor.done();
        }
    }

    private Date getSynchronizationTimestamp(ISynchronizationSession event) {

        Date mostRecent = new Date(0);
        Date mostRecentTimeStamp = null;
        if (event.getTaskRepository().getSynchronizationTimeStamp() == null) {
            mostRecentTimeStamp = mostRecent;
        } else {
            mostRecentTimeStamp = MantisUtils.parseDate(Long.parseLong(event.getTaskRepository()
                    .getSynchronizationTimeStamp()));
        }
        for (ITask task : event.getChangedTasks()) {
            Date taskModifiedDate = task.getModificationDate();
            if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
                mostRecent = taskModifiedDate;
                mostRecentTimeStamp = task.getModificationDate();
            }
        }
        return mostRecentTimeStamp;
    }

    @Override
    public Collection<TaskRelation> getTaskRelations(TaskData taskData) {

        if (!MantisRepositoryConfiguration.isDownloadSubTasks(taskData.getAttributeMapper().getTaskRepository()))
            return null;

        TaskAttribute parentTasksAttribute = taskData.getRoot().getAttribute(
                MantisAttributeMapper.Attribute.PARENT_OF.getKey());

        TaskAttribute childTasksAttribute = taskData.getRoot().getAttribute(
                MantisAttributeMapper.Attribute.CHILD_OF.getKey());

        if (parentTasksAttribute == null && childTasksAttribute == null)
            return null;

        List<TaskRelation> relations = new ArrayList<TaskRelation>();

        if (parentTasksAttribute != null)
            for (String taskId : parentTasksAttribute.getValues())
                relations.add(TaskRelation.subtask(taskId));

        if (childTasksAttribute != null)
            for (String taskId : childTasksAttribute.getValues())
                relations.add(TaskRelation.parentTask(taskId));

        return relations;

    }

    @Override
    public boolean hasRepositoryDueDate(TaskRepository taskRepository, ITask task, TaskData taskData) {
        
        try {
            return getClientManager().getRepository(taskRepository).isDueDateEnabled(new NullProgressMonitor());
        } catch (MantisException e) {
            MantisCorePlugin.error(e);
            return false;
        }
    }
}