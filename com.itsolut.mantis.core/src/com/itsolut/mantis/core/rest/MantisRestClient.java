package com.itsolut.mantis.core.rest;

import static com.itsolut.mantis.core.ConfigurationKey.BUG_ASSIGNED_STATUS;
import static com.itsolut.mantis.core.ConfigurationKey.BUG_SUBMIT_STATUS;
import static com.itsolut.mantis.core.ConfigurationKey.DEVELOPER_THRESHOLD;
import static com.itsolut.mantis.core.ConfigurationKey.DUE_DATE_UPDATE_THRESOLD;
import static com.itsolut.mantis.core.ConfigurationKey.DUE_DATE_VIEW_THRESOLD;
import static com.itsolut.mantis.core.ConfigurationKey.ENABLE_PROFILES;
import static com.itsolut.mantis.core.ConfigurationKey.REPORTER_THRESHOLD;
import static com.itsolut.mantis.core.ConfigurationKey.RESOLVED_STATUS_THRESHOLD;
import static com.itsolut.mantis.core.ConfigurationKey.TIME_TRACKING_ENABLED;
import static com.itsolut.mantis.core.DefaultConstantValues.Attribute.ETA_ENABLED;
import static com.itsolut.mantis.core.DefaultConstantValues.Attribute.PROJECTION_ENABLED;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;

import com.itsolut.mantis.core.DefaultConstantValues;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisCacheData;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.RepositoryValidationResult;
import com.itsolut.mantis.core.TaskRelationshipChange;
import com.itsolut.mantis.core.TraceLocation;
import com.itsolut.mantis.core.Tracer;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisIssueHistory;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.model.MantisTicketComment;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisViewState;
import com.itsolut.mantis.core.soap.MantisConverter;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueHeaderData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.TagData;

public class MantisRestClient implements IMantisClient {

	private final MantisCache cache;
	private AbstractWebLocation location;
	private final Object sync = new Object();
	private final MantisRestAPIClient restClient;
	private final Tracer tracer;
	private final NumberFormat formatter = new DecimalFormat("#.#");

	public MantisRestClient(AbstractWebLocation webLocation, Tracer tracer) {
		this.tracer = tracer;
		cache = new MantisCache();
		location = webLocation;
		restClient = new MantisRestAPIClient(webLocation);
	}

	public MantisTicket getTicket(int ticketId, IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		IssueData issueData = restClient.getIssueData(ticketId, monitor);

		registerAdditionalReporters(issueData);

		MantisTicket ticket = MantisConverter.convert(issueData, this, monitor);

		Policy.advance(monitor, 1);

		return ticket;
	}

	private void registerAdditionalReporters(IssueData issueData) {

		int projectId = issueData.getProject().getId().intValue();

		cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueData.getReporter()));

		if (issueData.getHandler() != null)
			cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueData.getHandler()));

		if (issueData.getNotes() != null)
			for (IssueNoteData note : issueData.getNotes())
				cache.registerAdditionalReporter(projectId, MantisConverter.convert(note.getReporter()));

		if (issueData.getMonitors() != null)
			for (AccountData issueMonitor : issueData.getMonitors())
				cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueMonitor));

	}

	public byte[] getAttachmentData(int id, IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		return restClient.getIssueAttachment(id, monitor);
	}

	public void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		final byte[] encoded = cache.getRepositoryVersion().hasCorrectBase64Encoding() ? data
				: Base64.encode(data).getBytes();

		restClient.addIssueAttachment(id, name, encoded, monitor);
	}

	public void deleteAttachment(int attachmentId, IProgressMonitor progressMonitor) throws MantisException {

		restClient.deleteIssueAttachment(attachmentId, progressMonitor);
	}

	public void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException {

		monitor.beginTask("", IProgressMonitor.UNKNOWN);
		try {
			refreshIfNeeded(monitor, location.getUrl());

			String projectName = query.getProjectName();
			String filterName = query.getFilterName();

			int projectId = cache.getProjectId(projectName);
			int filterId = cache.getProjectFilterId(projectId, filterName);

			IssueHeaderData[] issueHeaders;

			if (filterId == MantisCache.BUILT_IN_PROJECT_TASKS_FILTER_ID)
				issueHeaders = restClient.getIssueHeaders(projectId, query.getLimit(), monitor);
			else
				issueHeaders = restClient.getIssueHeaders(projectId, filterId, query.getLimit(), monitor);

			for (IssueHeaderData issueHeader : issueHeaders)
				result.add(MantisConverter.convert(issueHeader, cache, projectName));
		} finally {
			monitor.done();
		}
	}

	public void updateAttributes(IProgressMonitor monitor) throws MantisException {

		refresh(monitor, location.getUrl());
	}

	public void updateAttributesForTask(IProgressMonitor monitor, Integer ticketId) throws MantisException {

		IssueData issueData = restClient.getIssueData(ticketId, monitor);

		refreshForProject(monitor, location.getUrl(), issueData.getProject().getId().intValue());
	}

	public void updateTicket(MantisTicket ticket, MantisTicketComment note, List<TaskRelationshipChange> changes,
			IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		IssueData issue = MantisConverter.convert(ticket, this, getAuthToken(), monitor);
		issue.setId(BigInteger.valueOf(ticket.getId()));

		updateRelationsIfApplicable(ticket, changes, monitor);

		addCommentIfApplicable(issue, note);

		restClient.updateIssue(issue, monitor);
	}

	private String getAuthToken() {
		return restClient.getAuthToken();
	}

	private void updateRelationsIfApplicable(MantisTicket ticket, List<TaskRelationshipChange> relationshipChanges,
			IProgressMonitor monitor) throws MantisException {

		if (!cache.getRepositoryVersion().isHasProperTaskRelations())
			return;

		for (TaskRelationshipChange relationshipChange : relationshipChanges) {

			switch (relationshipChange.getDirection()) {

			case Removed:
				restClient.deleteRelationship(ticket.getId(), relationshipChange.getRelationship().getId(), monitor);
				break;
			case Added:
				restClient.addRelationship(ticket.getId(),
						MantisConverter.convert(relationshipChange.getRelationship()), monitor);
				break;
			}

		}
	}

	private void addCommentIfApplicable(IssueData issue, MantisTicketComment note) throws MantisException {

		if (!note.hasContent())
			return;

		Assert.isLegal(issue.getNotes() == null || issue.getNotes().length == 0, "Issue should not have had notes");

		issue.setNotes(new IssueNoteData[] { createIssue(note) });
	}

	public void addIssueComment(int issueId, MantisTicketComment note, IProgressMonitor monitor)
			throws MantisException {

		restClient.addNote(issueId, createIssue(note), monitor);
	}

	private IssueNoteData createIssue(MantisTicketComment note) throws MantisException {

		IssueNoteData ind = new IssueNoteData();

		ind.setReporter(MantisConverter.convert(getAuthToken(), cache));
		ind.setTime_tracking(BigInteger.valueOf(note.getTimeTracking()));
		ind.setText(note.getComment());
		return ind;
	}

	public int createTicket(MantisTicket ticket, IProgressMonitor monitor,
			List<TaskRelationshipChange> relationshipChanges) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		IssueData issueData = MantisConverter.convert(ticket, this, getAuthToken(), monitor);

		int issueId = restClient.addIssue(issueData, monitor);

		ticket.setId(issueId);

		updateRelationsIfApplicable(ticket, relationshipChanges, monitor);

		return issueId;
	}

	public MantisCache getCache(IProgressMonitor progressMonitor) throws MantisException {

		refreshIfNeeded(Policy.monitorFor(progressMonitor), location.getUrl());

		return cache;
	}

	private final List<RunnableWithProgress> globalRefreshRunnables = new ArrayList<RunnableWithProgress>();
	{
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheRepositoryVersion(restClient.getVersion(monitor));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				List<TagData> tags = cache.getRepositoryVersion().isHasTagSupport() ? restClient.getAllTags(50, monitor)
						: Collections.<TagData>emptyList();
				cache.cacheTags(MantisConverter.convert(tags));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheReporterThreshold(
						safeGetInt(restClient.getStringConfiguration(monitor, REPORTER_THRESHOLD.getValue()),
								DefaultConstantValues.Threshold.REPORT_BUG_THRESHOLD.getValue()));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDeveloperThreshold(
						safeGetInt(restClient.getStringConfiguration(monitor, DEVELOPER_THRESHOLD.getValue()),
								DefaultConstantValues.Threshold.UPDATE_BUG_ASSIGN_THRESHOLD.getValue()));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheAssignedStatus(
						safeGetInt(restClient.getStringConfiguration(monitor, BUG_ASSIGNED_STATUS.getValue()),
								DefaultConstantValues.Status.ASSIGNED.getValue()));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheSubmitStatus(
						safeGetInt(restClient.getStringConfiguration(monitor, BUG_SUBMIT_STATUS.getValue()),
								DefaultConstantValues.Status.NEW.getValue()));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheEnableProfiles(safeGetBoolean(monitor, ENABLE_PROFILES.getValue(),
						DefaultConstantValues.Attribute.PROFILES_ENABLED));
			}
		});

		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				try {
					cache.cacheDueDateViewThreshold(
							safeGetInt(restClient.getStringConfiguration(monitor, DUE_DATE_VIEW_THRESOLD.getValue()),
									DefaultConstantValues.Role.NOBODY.getValue()));
				} catch (MantisException e) {
					MantisCorePlugin.warn(
							"Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
					cache.cacheDueDateViewThreshold(DefaultConstantValues.Role.NOBODY.getValue());
				}
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				try {
					String mantisValue = restClient.getStringConfiguration(monitor,
							DUE_DATE_UPDATE_THRESOLD.getValue());
					cache.cacheDueDateUpdateThreshold(
							safeGetInt(mantisValue, DefaultConstantValues.Role.NOBODY.getValue()));
				} catch (MantisException e) {
					MantisCorePlugin.warn(
							"Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
					cache.cacheDueDateUpdateThreshold(DefaultConstantValues.Role.NOBODY.getValue());
				}
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				try {
					cache.cacheTimeTrackingEnabled(
							restClient.getStringConfiguration(monitor, TIME_TRACKING_ENABLED.getValue()));
				} catch (MantisException e) {
					MantisCorePlugin.warn(
							"Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
					cache.cacheTimeTrackingEnabled(Boolean.FALSE.toString());
				}
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheResolvedStatus(
						restClient.getStringConfiguration(monitor, RESOLVED_STATUS_THRESHOLD.getValue()));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cachePriorities(MantisConverter.convert(restClient.getPriorities(monitor), MantisPriority.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheStatuses(MantisConverter.convert(restClient.getStatuses(monitor), MantisTicketStatus.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheSeverities(MantisConverter.convert(restClient.getSeverities(monitor), MantisSeverity.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheResolutions(
						MantisConverter.convert(restClient.getResolutions(monitor), MantisResolution.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheReproducibilites(
						MantisConverter.convert(restClient.getReproducibilities(monitor), MantisReproducibility.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheProjections(
						MantisConverter.convert(restClient.getProjections(monitor), MantisProjection.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheEtas(MantisConverter.convert(restClient.getEtas(monitor), MantisETA.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheViewStates(
						MantisConverter.convert(restClient.getViewStates(monitor), MantisViewState.class));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.SEVERITY, safeGetThreshold(monitor, "default_bug_severity",
						DefaultConstantValues.Attribute.BUG_SEVERITY));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.PRIORITY, safeGetThreshold(monitor, "default_bug_priority",
						DefaultConstantValues.Attribute.BUG_PRIORITY));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.ETA,
						safeGetThreshold(monitor, "default_bug_eta", DefaultConstantValues.Attribute.BUG_ETA));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.REPRODUCIBILITY, safeGetThreshold(monitor,
						"default_bug_reproducibility", DefaultConstantValues.Attribute.BUG_REPRODUCIBILITY));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.RESOLUTION, safeGetThreshold(monitor, "default_bug_resolution",
						DefaultConstantValues.Attribute.BUG_RESOLUTION));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.PROJECTION, safeGetThreshold(monitor, "default_bug_projection",
						DefaultConstantValues.Attribute.BUG_PROJECTION));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheDefaultAttributeValue(Key.VIEW_STATE, safeGetThreshold(monitor, "default_bug_view_status",
						DefaultConstantValues.Attribute.BUG_VIEW_STATUS));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.getCacheData().putDefaultValueForStringAttribute(Key.STEPS_TO_REPRODUCE,
						restClient.getStringConfiguration(monitor, "default_bug_steps_to_reproduce"));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.getCacheData().putDefaultValueForStringAttribute(Key.ADDITIONAL_INFO,
						restClient.getStringConfiguration(monitor, "default_bug_additional_info"));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.getCacheData()
						.setBugResolutionFixedThreshold(safeGetThreshold(monitor, "bug_resolution_fixed_threshold",
								DefaultConstantValues.Attribute.BUG_RESOLUTION_FIXED_THRESHOLD));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.getCacheData().setEtaEnabled(safeGetBoolean(monitor, "enable_eta", ETA_ENABLED));
			}
		});
		globalRefreshRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.getCacheData()
						.setProjectionEnabled(safeGetBoolean(monitor, "enable_projection", PROJECTION_ENABLED));
			}
		});
	}

	private final List<RunnableWithProgress> projectSpecificRunnables = new ArrayList<RunnableWithProgress>();
	{
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheFilters(project.getValue(),
						MantisConverter.convert(restClient.getProjectFilters(project.getValue(), monitor)));
			}
		});
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheProjectCustomFields(project.getValue(),
						MantisConverter.convert(restClient.getProjectCustomFields(project.getValue(), monitor)));
			}
		});
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheProjectCategories(project.getValue(),
						restClient.getProjectCategories(project.getValue(), monitor));
			}
		});
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheProjectDevelopers(project.getValue(), MantisConverter.convert(restClient
						.getProjectUsers(project.getValue(), cache.getCacheData().getDeveloperThreshold(), monitor)));
			}
		});
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				try {
					cache.cacheProjectReporters(project.getValue(),
							MantisConverter.convert(restClient.getProjectUsers(project.getValue(),
									cache.getCacheData().getReporterThreshold(), monitor)));
				} catch (MantisException e) {
					if (cache.getCacheData().getReportersByProjectId().containsKey(project.getValue())) {
						MantisCorePlugin.warn("Failed retrieving reporter information, using previously loaded values.",
								e);
					} else {
						cache.copyReportersFromDevelopers(project.getValue());
						MantisCorePlugin.warn(
								"Failed retrieving reporter information, using developers list for reporters.", e);
					}
				}
			}
		});
		projectSpecificRunnables.add(new RunnableWithProgress() {
			public void run(IProgressMonitor monitor, MantisProject project) throws MantisException {
				cache.cacheProjectVersions(project.getValue(),
						MantisConverter.convert(restClient.getProjectVersions(project.getValue(), monitor)));
			}
		});
	}

	public void refreshForProject(IProgressMonitor monitor, String url, int projectId) throws MantisException {

		refresh0(monitor, url, projectId);
	}

	private void refreshIfNeeded(IProgressMonitor progressMonitor, String repositoryUrl) throws MantisException {
		synchronized (sync) {
			if (!cache.getCacheData().hasBeenRefreshed())
				refresh(progressMonitor, repositoryUrl);
		}
	}

	public void refresh(IProgressMonitor monitor, String repositoryUrl) throws MantisException {

		refresh0(monitor, repositoryUrl, MantisProject.ALL_PROJECTS.getValue());
	}

	private void refresh0(IProgressMonitor monitor, String repositoryUrl, int projectId) throws MantisException {

		synchronized (sync) {

			long start = System.currentTimeMillis();

			// set up an initial estimate of needed work
			SubMonitor subMonitor = SubMonitor.convert(monitor, "Refreshing repository configuration", 100);

			try {
				cache.cacheProjects(MantisConverter.convert(restClient.getProjectData(subMonitor.newChild(10))));

				int projectsToRefresh = projectId == MantisProject.ALL_PROJECTS.getValue()
						? cache.getCacheData().getProjects().size()
						: 1;
				int progressTicks = projectsToRefresh * projectSpecificRunnables.size() + globalRefreshRunnables.size();

				tracer.trace(TraceLocation.SYNC,
						"Refreshing {0} projects, {1} progress ticks, passed in monitor is {2}", projectsToRefresh,
						progressTicks, monitor);

				// set up the real estimate for needed work
				subMonitor.setWorkRemaining(progressTicks);

				for (RunnableWithProgress runnable : globalRefreshRunnables)
					runnable.run(subMonitor.newChild(1), null);

				for (MantisProject project : cache.getProjects()) {

					if (projectId != MantisProject.ALL_PROJECTS.getValue() && projectId != project.getValue())
						continue;

					subMonitor.setTaskName("Refreshing configuration for project " + project.getName());
					tracer.trace(TraceLocation.SYNC, "Refreshing configuration for project {0}", project.getName());

					for (RunnableWithProgress runnable : projectSpecificRunnables)
						runnable.run(subMonitor.newChild(1), project);
				}

				cache.getCacheData().setLastUpdate(System.currentTimeMillis());
			} finally {
				tracer.trace(TraceLocation.CONFIG, "Repository sync for {0} complete in {1} seconds.", repositoryUrl,
						format(start));
			}
		}
	}

	public MantisCacheData getCacheData() {

		return cache.getCacheData();
	}

	public void setCacheData(MantisCacheData cacheData) {

		cache.setCacheData(cacheData);
	}

	public RepositoryValidationResult validate(IProgressMonitor monitor) throws MantisException {

		monitor.beginTask("Validating", 2);

		try {

			// get and validate remote version
			String remoteVersion = restClient.getVersion(monitor);
			Policy.advance(monitor, 1);

			// test to see if the current user has proper access privileges,
			// since getVersion() does not require a valid user
			restClient.getProjectData(monitor);
			Policy.advance(monitor, 1);

			return new RepositoryValidationResult(remoteVersion);

		} finally {

			monitor.done();
		}

	}

	public boolean isDueDateEnabled(IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		return cache.getRepositoryVersion().isHasDueDateSupport() && cache.dueDateIsEnabled();
	}

	public boolean isTimeTrackingEnabled(IProgressMonitor monitor) throws MantisException {

		refreshIfNeeded(monitor, location.getUrl());

		return cache.getCacheData().timeTrackingEnabled && cache.getRepositoryVersion().isHasTimeTrackingSupport();
	}

	public void deleteTicket(int ticketId, IProgressMonitor monitor) throws MantisException {
		// TODO Auto-generated method stub

	}

	public MantisIssueHistory getHistory(int issueId, IProgressMonitor monitor) throws MantisException {
		// TODO Auto-generated method stub
		return null;
	}

	private int safeGetThreshold(IProgressMonitor monitor, String configName,
			DefaultConstantValues.Attribute attribute) {

		try {
			return safeGetInt(restClient.getStringConfiguration(monitor, configName), attribute.getValue());
		} catch (MantisException e) {
			MantisCorePlugin.warn("Unable to retrieve configuration value '" + configName + "' . Using default value '"
					+ attribute.getValue() + "'");
			return attribute.getValue();
		}
	}

	private int safeGetInt(String stringConfiguration, int defaultValue) {

		try {
			return Integer.parseInt(stringConfiguration);
		} catch (NumberFormatException e) {
			MantisCorePlugin
					.warn("Failed parsing config option value " + stringConfiguration + ". Using default value.", e);
			return defaultValue;
		}
	}

	private boolean safeGetBoolean(IProgressMonitor monitor, String configName,
			DefaultConstantValues.Attribute attribute) {

		try {
			return safeGetInt(restClient.getStringConfiguration(monitor, configName), attribute.getValue()) == 1;
		} catch (MantisException e) {
			MantisCorePlugin.warn("Unable to retrieve configuration value '" + configName + "' . Using default value '"
					+ attribute.getValue() + "'");
			return attribute.getValue() == 1;
		}
	}

	private static interface RunnableWithProgress {
		void run(IProgressMonitor monitor, MantisProject project) throws MantisException;
	}

	private String format(long start) {

		double millis = (System.currentTimeMillis() - start) / (double) 1000;
		return formatter.format(millis);

	}

}