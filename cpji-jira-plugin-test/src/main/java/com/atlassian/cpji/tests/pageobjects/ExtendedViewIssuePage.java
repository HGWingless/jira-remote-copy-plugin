package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.PageElementFinder;

import javax.inject.Inject;

/**
 * @since v3.0
 */
public class ExtendedViewIssuePage extends ViewIssuePage {

    @Inject
    protected PageBinder pageBinder;

    @Inject
    protected PageElementFinder pageElementFinder;


    protected IssueActionsFragment issueActionsFragment;



    public ExtendedViewIssuePage(String issueKey) {
        super(issueKey);
    }

    @Init
    public void init()
    {
        issueActionsFragment = pageBinder.bind(IssueActionsFragment.class);
    }

    public IssueActionsFragment getIssueActionsFragment() {
        return issueActionsFragment;
    }

    public void invokeRIC(){
        getIssueMenu().invoke(issueActionsFragment.getRICOperation());
    }
}
