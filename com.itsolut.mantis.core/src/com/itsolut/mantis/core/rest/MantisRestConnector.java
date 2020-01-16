package com.itsolut.mantis.core.rest;

import java.io.IOException;
import java.math.BigInteger;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.http.CommonHttpClient3;

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
import biz.futureware.mantis.rpc.soap.client.TagDataSearchResult;

public class MantisRestConnector {

	private CommonHttpClient3 client;
	String authToken;

	public MantisRestConnector(AbstractWebLocation location) {
		client = new CommonHttpClient3(location);
		AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
		// HTTP-only authentication
		if (credentials == null)
			credentials = location.getCredentials(AuthenticationType.HTTP);
		authToken = credentials.getUserName();
	}

	public FilterData[] mc_filter_get(BigInteger valueOf) {
		try {
			client.execute(null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		return null;
	}

	public IssueData mc_issue_get(BigInteger valueOf) {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] mc_issue_attachment_get(BigInteger valueOf) {
		// TODO Auto-generated method stub
		return null;
	}

	public void mc_issue_attachment_add(BigInteger valueOf, String filename, String string,
			byte[] data) {
		// TODO Auto-generated method stub

	}

	public void mc_issue_attachment_delete(BigInteger valueOf) {
		// TODO Auto-generated method stub

	}

	public IssueHeaderData[] mc_filter_get_issue_headers(BigInteger valueOf, BigInteger valueOf2,
			BigInteger one, BigInteger valueOf3) {
		// TODO Auto-generated method stub
		return null;
	}

	public IssueHeaderData[] mc_project_get_issue_headers(BigInteger valueOf, BigInteger one,
			BigInteger valueOf2) {
		// TODO Auto-generated method stub
		return null;
	}

	public BigInteger mc_issue_add(IssueData issue) {
		// TODO Auto-generated method stub
		return null;
	}

	public void mc_issue_relationship_add(BigInteger valueOf, RelationshipData relationshipData) {
		// TODO Auto-generated method stub

	}

	public void mc_issue_relationship_delete(BigInteger valueOf, BigInteger valueOf2) {
		// TODO Auto-generated method stub

	}

	public void mc_issue_note_add(BigInteger valueOf, IssueNoteData ind) {
		// TODO Auto-generated method stub

	}

	public void mc_issue_update(BigInteger id, IssueData issue) {
		// TODO Auto-generated method stub

	}

	public void mc_issue_delete(BigInteger valueOf) {
		// TODO Auto-generated method stub

	}

	public ProjectData[] mc_projects_get_user_accessible() {
		// TODO Auto-generated method stub
		return null;
	}

	public CustomFieldDefinitionData[] mc_project_get_custom_fields(BigInteger valueOf) {
		// TODO Auto-generated method stub
		return null;
	}

	public String mc_version() {
		// TODO Auto-generated method stub
		return "2.4";
	}

    /**
     * Get the value for the specified configuration variable.
     */
	public String mc_config_get_string(String configurationParameter) {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Get the enumeration for priorities.
     */
	public ObjectRef[] mc_enum_priorities() {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Get the enumeration for statuses.
     */
	public ObjectRef[] mc_enum_status() {
		// TODO Auto-generated method stub
		return null;
	}

    /**
     * Get the enumeration for severities.
     */
	public ObjectRef[] mc_enum_severities() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectRef[] mc_enum_resolutions() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectRef[] mc_enum_reproducibilities() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectRef[] mc_enum_projections() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectRef[] mc_enum_etas() {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectRef[] mc_enum_view_states() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] mc_project_get_categories(BigInteger valueOf) {
		// TODO Auto-generated method stub
		return null;
	}

	public AccountData[] mc_project_get_users(BigInteger valueOf, BigInteger valueOf2) {
		// TODO Auto-generated method stub
		return null;
	}

	public ProjectVersionData[] mc_project_get_versions(BigInteger valueOf) {
		// TODO Auto-generated method stub
		return null;
	}

	public TagDataSearchResult mc_tag_get_all(BigInteger valueOf, BigInteger valueOf2) {
		// TODO Auto-generated method stub
		return null;
	}

}
