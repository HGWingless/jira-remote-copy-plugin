package com.atlassian.cpji.fields.custom;

import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

public interface CustomFieldDefaultValueEvaluationStrategy {

    void populateWithDefaultValue(final IssueInputParameters inputParameters, final CustomField customField, final Project project, final IssueType issueType);
    boolean hasDefaultValueDefined(final CustomField customField, final Project project, final IssueType issueType);

}
