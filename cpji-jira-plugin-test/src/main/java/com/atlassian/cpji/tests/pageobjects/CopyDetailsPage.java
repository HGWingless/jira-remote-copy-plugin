package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

/**
 * Step 2 for doing a Remote Copy.
 *
 * @since v2.1
 */
public class CopyDetailsPage extends AbstractJiraPage
{
    private static final String URI_TEMPLATE = "/secure/CopyDetailsAction.jspa?id=%d&targetEntityLink=%s";

    private final Long issueId;
    private final String url;

    @ElementBy(className = "submit")
    private PageElement nextButton;

    public CopyDetailsPage(final Long issueId, final String targetEntityLink)
    {
        this.issueId = issueId;
        this.url = String.format(URI_TEMPLATE, issueId, targetEntityLink);
    }

    @Override
    public TimedCondition isAt()
    {
        return Conditions.forMatcher(elementFinder.find(By.className("current")).timed().getText(), Matchers.equalToIgnoringCase("Select Issue Type"));
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public PermissionChecksPage next()
    {
        nextButton.click();
        return pageBinder.bind(PermissionChecksPage.class);
    }

	public boolean isCreateIssueLinksGroupVisible() {
		return elementFinder.find(By.id("create-issue-links-group")).timed().isVisible().now();
	}

	public boolean isCopyCommentsGroupVisible() {
		return elementFinder.find(By.id("copy-comments-group")).timed().isVisible().now();
	}

	public boolean isCopyIssueLinksGroupVisible() {
		return elementFinder.find(By.id("copy-issue-links-group")).timed().isVisible().now();
	}

	public boolean isCopyAttachmentsGroupVisible() {
		return elementFinder.find(By.id("copy-attachments-group")).timed().isVisible().now();
	}

}
