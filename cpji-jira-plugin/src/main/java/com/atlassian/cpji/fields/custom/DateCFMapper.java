package com.atlassian.cpji.fields.custom;

import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.util.DateUtil;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.converters.DatePickerConverter;
import com.atlassian.jira.issue.customfields.converters.DateTimeConverter;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

import java.util.Date;

/**
 * Maps the {@link DateCFType} custom field type.
 *
 * @since v2.1
 */
public class DateCFMapper extends AbstractSingleValueCFMapper<Date>
{
    private final DatePickerConverter datePickerConverter;
	private final DateTimeConverter dateTimePickerConverter;

	public DateCFMapper(final DatePickerConverter datePickerConverter, final DateTimeConverter dateTimeConverter,
			final DefaultFieldValuesManager defaultFieldValuesManager)
    {
		super(defaultFieldValuesManager);
		this.datePickerConverter = datePickerConverter;
		this.dateTimePickerConverter = dateTimeConverter;
	}

    @Override
    public boolean acceptsType(CustomFieldType<?, ?> type)
    {
        return type instanceof DateCFType || type instanceof DateTimeCFType;
    }

    @Override
    protected String convertToString(final Date value)
    {
        return DateUtil.toString(value);
    }

    @Override
    protected String formatString(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
		final Date date = DateUtil.parseString(value);
        return customField.getCustomFieldType() instanceof DateTimeCFType ? dateTimePickerConverter.getString(date) : datePickerConverter.getString(date);
    }

    @Override
    protected boolean isValidValue(final String value, final CustomField customField, final Project project, final IssueType issueType)
    {
        return DateUtil.isValidDate(value);
    }
}
