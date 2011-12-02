package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.TimeTrackingBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.TimeTrackingSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;

import java.util.Collections;

/**
 * @since v2.1
 */
public class TimeTrackingFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final TimeTrackingConfiguration timeTrackingConfiguration;

    public TimeTrackingFieldMapper(Field field, final TimeTrackingConfiguration timeTrackingConfiguration)
    {
        super(field);
        this.timeTrackingConfiguration = timeTrackingConfiguration;
    }

    @Override
    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    @Override
    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        TimeTrackingBean timeTracking = bean.getTimeTracking();
        if (!timeTrackingConfiguration.enabled())
        {
            return new MappingResult(Collections.<String>emptyList(), true, false);
        }
        return new MappingResult(Collections.<String>emptyList(), timeTracking != null, timeTracking == null);
    }

    @Override
    public Class<? extends OrderableField> getField()
    {
        return TimeTrackingSystemField.class;
    }

    @Override
    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        TimeTrackingBean timeTracking = bean.getTimeTracking();
        if (timeTrackingConfiguration.enabled() && timeTracking != null)
        {
            if (timeTrackingConfiguration.getMode().equals(TimeTrackingConfiguration.Mode.LEGACY))
            {
                Long remainingEstimate = timeTracking.getEstimate();
                if (remainingEstimate != null)
                {
                    inputParameters.getActionParameters().put(IssueFieldConstants.TIMETRACKING, toArr(String.valueOf(remainingEstimate / timeTrackingConfiguration.getDefaultUnit().getSeconds())));
                }
            }
            else
            {
                Long originalEstimate = timeTracking.getOriginalEstimate();
                if (originalEstimate != null)
                {
                    inputParameters.getActionParameters().put(TimeTrackingSystemField.TIMETRACKING_ORIGINALESTIMATE, toArr(String.valueOf(originalEstimate / timeTrackingConfiguration.getDefaultUnit().getSeconds())));
                }
                Long remainingEstimate = timeTracking.getEstimate();
                if (remainingEstimate != null)
                {
                    inputParameters.getActionParameters().put(TimeTrackingSystemField.TIMETRACKING_REMAININGESTIMATE, toArr(String.valueOf(remainingEstimate / timeTrackingConfiguration.getDefaultUnit().getSeconds())));
                }
            }
        }
    }

    private String[] toArr(final String fieldValue)
    {
        return new String[] { fieldValue };
    }
}