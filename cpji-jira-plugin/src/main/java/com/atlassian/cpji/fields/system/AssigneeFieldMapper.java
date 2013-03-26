package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.fields.value.UserMappingManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class AssigneeFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
	private final PermissionManager permissionManager;
	private final ApplicationProperties applicationProperties;
	private final UserMappingManager userMappingManager;

	private static class InternalMappingResult {
		private final User mappedUser;
		private final MappingResultDecision decision;

		public enum MappingResultDecision {
			FOUND, NOT_FOUND
		}

		private InternalMappingResult(User mappedUser, MappingResultDecision decision) {
			this.mappedUser = mappedUser;
			this.decision = decision;
		}
	}

	public AssigneeFieldMapper(PermissionManager permissionManager, final ApplicationProperties applicationProperties,
			FieldManager fieldManager, final UserMappingManager userMappingManager, final DefaultFieldValuesManager defaultFieldValuesManager) {
		super(getOrderableField(fieldManager, IssueFieldConstants.ASSIGNEE), defaultFieldValuesManager);
		this.permissionManager = permissionManager;
		this.applicationProperties = applicationProperties;
		this.userMappingManager = userMappingManager;
	}

	public Class<? extends OrderableField> getField() {
		return AssigneeSystemField.class;
	}

	public boolean userHasRequiredPermission(final Project project, final User user) {
		return permissionManager.hasPermission(Permissions.ASSIGN_ISSUE, project, user);
	}


	public MappingResult getMappingResult(final CopyIssueBean bean, final Project project) {
		boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
		InternalMappingResult mapResult = mapUser(bean.getAssignee(), project);
		switch (mapResult.decision) {
			case FOUND:
				return new MappingResult(ImmutableList.<String>of(), true, false, true);
			case NOT_FOUND:
				List<String> unmapped = bean.getAssignee() != null ?
						ImmutableList.of(bean.getAssignee().getUserName()) :
						ImmutableList.<String>of();
				if (unassignedAllowed) {
					return new MappingResult(unmapped, true, unmapped.isEmpty(), true);
				} else {
					return new MappingResult(unmapped, false, unmapped.isEmpty(), true);
				}
			default:
				return null;
		}
	}

	private InternalMappingResult mapUser(UserBean user, final Project project) {
		if (user == null) {
			return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
		}
		User assignee = findUser(user, project);
		if (assignee == null) {
			return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
		} else {
			if (permissionManager.hasPermission(Permissions.ASSIGNABLE_USER, project, assignee)) {
				return new InternalMappingResult(assignee, InternalMappingResult.MappingResultDecision.FOUND);
			} else {
				return new InternalMappingResult(null, InternalMappingResult.MappingResultDecision.NOT_FOUND);
			}
		}
	}

	public void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project) {
		InternalMappingResult assignee = mapUser(bean.getAssignee(), project);
		boolean unassignedAllowed = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED);
		switch (assignee.decision) {
			case FOUND:
				inputParameters.setAssigneeId(assignee.mappedUser.getName());
				break;
			case NOT_FOUND:
				if (!unassignedAllowed && hasDefaultValue(project, bean)) {
					String[] defaults = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(),
							fieldLayoutItem.getOrderableField().getId(), bean.getTargetIssueType());
					if (StringUtils.isNotBlank(defaults[0])) {
						inputParameters.setAssigneeId(defaults[0]);
					}
				}
				break;
		}
	}

	private User findUser(final UserBean user, final Project project) {
		return userMappingManager.mapUser(user, project);
	}
}
