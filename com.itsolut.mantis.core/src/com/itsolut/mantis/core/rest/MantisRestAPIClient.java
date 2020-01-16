package com.itsolut.mantis.core.rest;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.commons.net.WebRequest;
import org.eclipse.mylyn.commons.net.WebUtil;

import com.google.common.collect.Lists;
import com.itsolut.mantis.core.exception.MantisException;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.FilterData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueHeaderData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;
import biz.futureware.mantis.rpc.soap.client.RelationshipData;
import biz.futureware.mantis.rpc.soap.client.TagData;
import biz.futureware.mantis.rpc.soap.client.TagDataSearchResult;

public class MantisRestAPIClient {
	private AbstractWebLocation location;
	private transient MantisRestConnector rest;

	public MantisRestAPIClient(AbstractWebLocation webLocation) {
		synchronized (this) {

			this.location = webLocation;
			rest = this.getSOAP();

			configureHttpAuthentication();
		}
	}

	private void configureHttpAuthentication() {

		AuthenticationCredentials httpCredentials = location.getCredentials(AuthenticationType.HTTP);
		if (httpCredentials == null)
			return;

		// Stub stub = (Stub) soap;
		// stub._setProperty(Call.USERNAME_PROPERTY, httpCredentials.getUserName());
		// stub._setProperty(Call.PASSWORD_PROPERTY, httpCredentials.getPassword());
	}

	// private boolean doesNotHaveHttpAuth() {
	//
	// return location.getCredentials(AuthenticationType.HTTP) == null;
	// }
	//
	// private MantisRemoteException wrap(RemoteException e) {
	//
	// boolean unexpected = false;
	//
	// StringBuilder message = new StringBuilder();
	//
	// if (isSourceforgeRepoWithoutHttpAuth())
	// message.append("For SF.net hosted apps, please make sure to use HTTP
	// authentication only.").append('\n');
	//
	// if (location.getUrl().startsWith(SourceForgeConstants.OLD_SF_NET_URL))
	// message.append("SF.net hosted apps have been moved to
	// https://sourceforge.net/apps/mantisbt/").append('\n');
	//
	// if (e instanceof AxisFault) {
	//
	// AxisFault axisFault = (AxisFault) e;
	//
	// if (axisFault.getCause() instanceof SAXException)
	// message.append("The repository has returned an invalid XML response : "
	// + axisFault.getCause().getMessage());
	// else if (e.getMessage() != null)
	// message.append(e.getMessage());
	//
	// unexpected = true;
	//
	// } else if (e.getMessage() != null)
	// message.append(e.getMessage());
	//
	// return new MantisRemoteException(message.toString(), e, unexpected);
	//
	// }

	// private boolean isSourceforgeRepoWithoutHttpAuth() {
	//
	// return location.getUrl().startsWith(SourceForgeConstants.NEW_SF_NET_URL) &&
	// doesNotHaveHttpAuth();
	// }

	public IssueData getIssueData(final int issueId, IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<IssueData>() {

			public IssueData call() throws MantisException, RemoteException {

				// try {
				return getSOAP().mc_issue_get(BigInteger.valueOf(issueId));
				// } catch (MantisException e) {
				// if ( e.getMessage().startsWith("Issue does not exist"))
				// throw new TicketNotFoundException(issueId);
				//
				// throw e;
				//
				// }
			}

		});
	}

	public byte[] getIssueAttachment(final int attachmentID, final IProgressMonitor monitor) throws MantisException {

		byte[] attachment = call(monitor, new Callable<byte[]>() {

			public byte[] call() throws Exception {

				return getSOAP().mc_issue_attachment_get(BigInteger.valueOf(attachmentID));
			}

		});

		return attachment;

	}

	public void addIssueAttachment(final int ticketID, final String filename, final byte[] data,
			final IProgressMonitor monitor) throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_attachment_add(BigInteger.valueOf(ticketID), filename, "bug", data);

				return null;
			}
		});

	}

	public void deleteIssueAttachment(final int attachmentId, final IProgressMonitor monitor) throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_attachment_delete(BigInteger.valueOf(attachmentId));

				return null;
			}

		});
	}

	public IssueHeaderData[] getIssueHeaders(final int projectId, final int filterId, final int limit,
			IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<IssueHeaderData[]>() {

			public IssueHeaderData[] call() throws Exception {

				return getSOAP().mc_filter_get_issue_headers(BigInteger.valueOf(projectId), // project
						BigInteger.valueOf(filterId), // filter
						BigInteger.ONE, // start page
						BigInteger.valueOf(limit)); // # per page

			}

		});
	}

	public IssueHeaderData[] getIssueHeaders(final int projectId, final int limit, IProgressMonitor monitor)
			throws MantisException {

		return call(monitor, new Callable<IssueHeaderData[]>() {

			public IssueHeaderData[] call() throws Exception {

				return getSOAP().mc_project_get_issue_headers(BigInteger.valueOf(projectId), BigInteger.ONE,
						BigInteger.valueOf(limit));
			}

		});
	}

	public int addIssue(final IssueData issue, IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<BigInteger>() {

			public BigInteger call() throws Exception {

				BigInteger result = getSOAP().mc_issue_add(issue);

				return result;

			}

		}).intValue();

	}

	public void addRelationship(final int ticketId, final RelationshipData relationshipData, IProgressMonitor monitor)
			throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_relationship_add(BigInteger.valueOf(ticketId), relationshipData);
				return null;

			}
		});
	}

	public void deleteRelationship(final int ticketId, final int relationshipId, IProgressMonitor monitor)
			throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_relationship_delete(BigInteger.valueOf(ticketId),
						BigInteger.valueOf(relationshipId));
				return null;

			}
		});
	}

	public void addNote(final int issueId, final IssueNoteData ind, IProgressMonitor monitor) throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_note_add(BigInteger.valueOf(issueId), ind);
				return null;

			}
		});

	}

	public void updateIssue(final IssueData issue, IProgressMonitor monitor) throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_update(issue.getId(), issue);
				return null;

			}
		});

	}

	public void deleteIssue(final int issueId, IProgressMonitor monitor) throws MantisException {

		call(monitor, new Callable<Void>() {

			public Void call() throws Exception {

				getSOAP().mc_issue_delete(BigInteger.valueOf(issueId));

				return null;
			}
		});
	}

	public ProjectData[] getProjectData(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ProjectData[]>() {

			public ProjectData[] call() throws Exception {

				return getSOAP().mc_projects_get_user_accessible();
			}
		});
	}

	public FilterData[] getProjectFilters(final int projectId, IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<FilterData[]>() {

			public FilterData[] call() throws Exception {

				return getSOAP().mc_filter_get(BigInteger.valueOf(projectId));
			}
		});

	}

	public CustomFieldDefinitionData[] getProjectCustomFields(final int projectId, IProgressMonitor monitor)
			throws MantisException {

		return call(monitor, new Callable<CustomFieldDefinitionData[]>() {

			public CustomFieldDefinitionData[] call() throws Exception {

				return getSOAP().mc_project_get_custom_fields(BigInteger.valueOf(projectId));
			}

		});

	}

	public String getVersion(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<String>() {

			public String call() throws Exception {

				return getSOAP().mc_version();
			}
		});

	}

	public String getStringConfiguration(IProgressMonitor monitor, final String configurationParameter)
			throws MantisException {

		return call(monitor, new Callable<String>() {

			public String call() throws Exception {

				return getSOAP().mc_config_get_string(configurationParameter);
			}
		});
	}

	public ObjectRef[] getPriorities(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_priorities();

			}
		});

	}

	public ObjectRef[] getStatuses(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_status();

			}
		});

	}

	public ObjectRef[] getSeverities(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_severities();

			}
		});

	}

	public ObjectRef[] getResolutions(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_resolutions();

			}
		});

	}

	public ObjectRef[] getReproducibilities(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_reproducibilities();

			}
		});

	}

	public ObjectRef[] getProjections(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_projections();

			}
		});

	}

	public ObjectRef[] getEtas(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_etas();

			}
		});

	}

	public ObjectRef[] getViewStates(IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<ObjectRef[]>() {

			public ObjectRef[] call() throws Exception {

				return getSOAP().mc_enum_view_states();

			}
		});

	}

	public String[] getProjectCategories(final int value, IProgressMonitor monitor) throws MantisException {

		return call(monitor, new Callable<String[]>() {

			public String[] call() throws Exception {

				return getSOAP().mc_project_get_categories(BigInteger.valueOf(value));

			}
		});
	}

	public AccountData[] getProjectUsers(final int projectId, final int reporterThreshold, IProgressMonitor monitor)
			throws MantisException {

		return call(monitor, new Callable<AccountData[]>() {

			public AccountData[] call() throws Exception {

				return getSOAP().mc_project_get_users(BigInteger.valueOf(projectId),
						BigInteger.valueOf(reporterThreshold));

			}
		});
	}

	public ProjectVersionData[] getProjectVersions(final int projectId, IProgressMonitor monitor)
			throws MantisException {

		return call(monitor, new Callable<ProjectVersionData[]>() {

			public ProjectVersionData[] call() throws Exception {

				return getSOAP().mc_project_get_versions(BigInteger.valueOf(projectId));
			}
		});
	}

	public TagDataSearchResult getTags(final int pageNumber, final int perPage, IProgressMonitor monitor)
			throws MantisException {

		return call(monitor, new Callable<TagDataSearchResult>() {

			public TagDataSearchResult call() throws Exception {

				return getSOAP().mc_tag_get_all(BigInteger.valueOf(pageNumber), BigInteger.valueOf(perPage));
			}
		});

	}

	protected MantisRestConnector getSOAP() {

		synchronized (this) {

			if (rest != null) {
				return rest;
			}

			rest = new MantisRestConnector(location);

			return rest;

		}

	}

	protected AbstractWebLocation getLocation() {

		return location;
	}

	protected <T> T call(IProgressMonitor monitor, Callable<T> runnable) throws MantisException {

		try {
			return call1(monitor, runnable);
		} catch (MantisException e) {
			throw e;
			// } catch (RemoteException e) {
			// throw wrap(e);
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T call1(IProgressMonitor monitor, final Callable<T> runnable) throws Exception {
		try {
			monitor = Policy.monitorFor(monitor);
			return WebUtil.execute(monitor, new WebRequest<T>() {

				@Override
				public void abort() {
					// request.cancel();
				}

				public T call() throws Exception {
					try {
						// SoapRequest.setCurrentRequest(request);
						return runnable.call();
					} finally {
						// request.done();
					}
				}

			});
		} catch (RemoteException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public String getAuthToken() {
		// usual case
		AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);

		// HTTP-only authentication
		if (credentials == null)
			credentials = location.getCredentials(AuthenticationType.HTTP);

		// no login specified is not supported ATM by the SOAP API, but there's no harm
		// done either
		if (credentials == null)
			return null;

		return credentials.getUserName();
	}

	protected <T> T callOnce(IProgressMonitor monitor, Callable<T> runnable) throws MantisException {

		try {
			return null; // super.callOnce(monitor, runnable);
			// } catch (MantisException e) {
			// throw e;
			// } catch (RemoteException e) {
			// throw wrap(e);
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convenience method to retrieve all tags without needing to consider multiple
	 * pages
	 * 
	 * @param pageSize
	 *            the number of tags to retrieve per individual call
	 * @param monitor
	 *            the progress monitor, used only to check for cancellation
	 * @return all the tags
	 * @throws MantisException
	 */
	public List<TagData> getAllTags(final int pageSize, IProgressMonitor monitor) throws MantisException {

		int page = 1;

		List<TagData> allTags = Lists.newArrayList();

		while (true) {

			TagDataSearchResult tagResult = getTags(page, pageSize, monitor);

			Policy.checkCanceled(monitor);

			if (tagResult.getResults() == null || tagResult.getResults().length == 0)
				break;

			allTags.addAll(Arrays.asList(tagResult.getResults()));

			if (allTags.size() >= tagResult.getTotal_results().intValue())
				break;

			page++;
		}

		return allTags;
	}

}
