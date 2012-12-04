package com.atlassian.cpji.action;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cpji.action.admin.CopyIssuePermissionManager;
import com.atlassian.cpji.components.model.ResponseStatus;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.FieldMapperFactory;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyInformationBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.fugue.Either;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.util.I18nHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @since v1.4
 */
public class CopyDetailsAction extends AbstractCopyIssueAction
{
    private Boolean remoteAttachmentsEnabled;
    private Boolean hasCreateAttachmentsPermission;

    private String remoteUserName;
    private String remoteFullUserName;

    private Collection<Option> availableIssueTypes;
    private List<Option> issueLinkOptions;
    private final I18nHelper.BeanFactory beanFactory;

    public class Option
    {
        private final String value;
        private final boolean selected;
        private String label;

        Option(String value, boolean selected)
        {
            this.value = value;
            this.selected = selected;
        }

        Option(String value, boolean selected, final String label)
        {
            this.value = value;
            this.selected = selected;
            this.label = label;
        }

        public String getValue()
        {
            return value;
        }

        public boolean isSelected()
        {
            return selected;
        }

        public String getLabel()
        {
            return label;
        }
    }


    public CopyDetailsAction(
            final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager,
            final CommentManager commentManager,
            final FieldManager fieldManager,
            final FieldMapperFactory fieldMapperFactory,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final BeanFactory beanFactory,
            final UserMappingManager userMappingManager,
			final ApplicationLinkService applicationLinkService,
            final JiraProxyFactory jiraProxyFactory
    )
    {
        super(subTaskManager, fieldLayoutManager, commentManager, fieldManager, fieldMapperFactory, fieldLayoutItemsRetriever,
				copyIssuePermissionManager, userMappingManager, applicationLinkService, jiraProxyFactory);
        this.beanFactory = beanFactory;
    }

	public boolean isIssueWithComments() {
		final MutableIssue issue = getIssueObject();
		if (issue != null) {
			return !commentManager.getComments(issue).isEmpty();
		}
		return false;
	}

    @Override
    protected String doExecute() throws Exception
    {
        String permissionCheck = checkPermissions();
        if (!permissionCheck.equals(SUCCESS))
        {
            return permissionCheck;
        }
        SelectedProject entityLink = getSelectedDestinationProject();
        if (entityLink == null)
        {
            addErrorMessage("Failed to find the entity link.");
            return ERROR;
        }

        JiraProxy proxy = jiraProxyFactory.createJiraProxy(entityLink.getJiraLocation());
        Either<ResponseStatus, CopyInformationBean> result = proxy.getCopyInformation(entityLink.getProjectKey());
        if(result.isLeft()) {
            ResponseStatus status = (ResponseStatus) result.left().get();
            if(ResponseStatus.Status.AUTHENTICATION_FAILED.equals(status.getResult())){
                log.error("Authentication failed.");
                addErrorMessage("Authentication failed. If using Trusted Apps, do you have a user with the same user name in the remote JIRA instance?");
            } else if(ResponseStatus.Status.AUTHORIZATION_REQUIRED.equals(status.getResult())){
                log.error("OAuth token invalid.");
            } else if(ResponseStatus.Status.COMMUNICATION_FAILED.equals(status.getResult())){
                log.error("Failed to retrieve the list of issue fields from the remote JIRA instance.");
                addErrorMessage("Failed to retrieve the list of issue fields from the remote JIRA instance.");
            }

            return ERROR;
        }

        CopyInformationBean copyInformationBean = (CopyInformationBean) result.right().get();
        if (!copyInformationBean.getHasCreateIssuePermission())
        {
            addErrorMessage("You don't have the create issue permission for this JIRA project!");
            return ERROR;
        }
        issueLinkOptions = new ArrayList<Option>();

		final String remoteJiraVersion = copyInformationBean.getVersion();
        if (remoteJiraVersion != null && remoteJiraVersion.startsWith("5"))
        {
            issueLinkOptions.add(new Option(RemoteIssueLinkType.RECIPROCAL.name(), false, getText(RemoteIssueLinkType.RECIPROCAL.getI18nKey())));
            issueLinkOptions.add(new Option(RemoteIssueLinkType.INCOMING.name(), false, getText(RemoteIssueLinkType.INCOMING.getI18nKey())));
        }
        issueLinkOptions.add(new Option(RemoteIssueLinkType.OUTGOING.name(), false, getText(RemoteIssueLinkType.OUTGOING.getI18nKey())));
        issueLinkOptions.add(new Option(RemoteIssueLinkType.NONE.name(), false, getText(RemoteIssueLinkType.NONE.getI18nKey())));

        checkIssueTypes(copyInformationBean.getIssueTypes().getGetTypes());
        remoteAttachmentsEnabled = copyInformationBean.getAttachmentsEnabled();
        hasCreateAttachmentsPermission = copyInformationBean.getHasCreateAttachmentPermission();

		UserBean user = copyInformationBean.getRemoteUser();
        remoteUserName = user.getUserName();
        remoteFullUserName = copyInformationBean.getRemoteUser().getFullName();

        return SUCCESS;
    }

    public String getRemoteUserName()
    {
        return remoteUserName;
    }

    public String getRemoteFullUserName()
    {
        return remoteFullUserName;
    }

    public boolean attachmentsEnabled()
    {
        return getApplicationProperties().getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
    }

    public boolean remoteAttachmentsEnabled()
    {
        return remoteAttachmentsEnabled;
    }

    public Boolean hasCreateAttachmentsPermission()
    {
        return hasCreateAttachmentsPermission;
    }

    public boolean isSALUpgradeRequired()
    {
        return false;
    }

    private void checkIssueTypes(final Collection<String> values)
    {
        MutableIssue issue = getIssueObject();
        availableIssueTypes = new ArrayList<Option>();
        for (String value : values)
        {
            if (value.equals(issue.getIssueTypeObject().getName()))
            {
                availableIssueTypes.add(new Option(value, true));
            }
            else
            {
                availableIssueTypes.add(new Option(value, false));
            }
        }
    }

    public List<Option> getIssueLinkOptions()
    {
        return issueLinkOptions;
    }

    public Collection<Option> getAvailableIssueTypes()
    {
        return availableIssueTypes;
    }
}
