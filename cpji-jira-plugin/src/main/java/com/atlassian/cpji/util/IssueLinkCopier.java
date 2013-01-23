package com.atlassian.cpji.util;

import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.SuccessfulResponse;
import com.atlassian.cpji.components.remote.JiraProxy;
import com.atlassian.fugue.Either;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Orchestrates copying local and remote links
 * @since v3.0
 */
public class IssueLinkCopier {

    private final IssueLinkManager issueLinkManager;
    private final JiraProxy remoteJira;
    private final RemoteIssueLinkManager remoteIssueLinkManager;


    public static final Predicate<IssueLink> isNotSubtaskIssueLink = new Predicate<IssueLink>() {
        @Override
        public boolean apply(IssueLink input) {
			Preconditions.checkNotNull(input);
            return !input.getIssueLinkType().isSubTaskLinkType();
        }
    };

    public IssueLinkCopier(IssueLinkManager issueLinkManager, RemoteIssueLinkManager remoteIssueLinkManager, JiraProxy remoteJira) {
        this.issueLinkManager = issueLinkManager;
        this.remoteJira = remoteJira;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
    }

    public Either<NegativeResponseStatus, SuccessfulResponse> copyLocalAndRemoteLinks(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) {
        copyLocalIssueLinks(localIssue, copiedIssueKey, copiedIssueId);
        copyRemoteIssueLinks(localIssue, copiedIssueKey);
        return remoteJira.convertRemoteIssueLinksIntoLocal(copiedIssueKey);
    }


    public void copyRemoteIssueLinks(final Issue localIssue, final String copiedIssueKey) {
        for (final RemoteIssueLink remoteIssueLink : remoteIssueLinkManager.getRemoteIssueLinksForIssue(localIssue)) {
            remoteJira.copyRemoteIssueLink(remoteIssueLink, copiedIssueKey);
        }
    }


    public void copyLocalIssueLinks(final Issue localIssue, final String copiedIssueKey, final Long copiedIssueId) {

        //we throw out all subtask links and re-create them after move only when its needed
        final Iterable<IssueLink> inwardLinks = Iterables.filter(issueLinkManager.getInwardLinks(localIssue.getId()), isNotSubtaskIssueLink);
        for (final IssueLink inwardLink : inwardLinks) {
            final IssueLinkType type = inwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(inwardLink.getSourceObject(), copiedIssueKey, copiedIssueId, type,
                    JiraProxy.LinkCreationDirection.OUTWARD, JiraProxy.LinkCreationDirection.INWARD);
        }
        final Iterable<IssueLink> outwardLinks = Iterables.filter(issueLinkManager.getOutwardLinks(localIssue.getId()), isNotSubtaskIssueLink);
        for (final IssueLink outwardLink : outwardLinks) {

            final IssueLinkType type = outwardLink.getIssueLinkType();
            remoteJira.copyLocalIssueLink(outwardLink.getDestinationObject(), copiedIssueKey, copiedIssueId, type,
                    JiraProxy.LinkCreationDirection.INWARD, JiraProxy.LinkCreationDirection.OUTWARD);
        }
    }
}
