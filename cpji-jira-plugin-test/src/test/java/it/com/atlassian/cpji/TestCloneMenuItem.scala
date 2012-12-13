package it.com.atlassian.cpji

import org.junit.{Rule, Test, Before}
import org.junit.Assert._
import org.openqa.selenium.{WebElement, By}
import org.hamcrest.collection.IsIterableWithSize
import com.atlassian.cpji.tests.pageobjects.{ExtendedViewIssuePage, IssueActionsFragment}
import com.atlassian.pageobjects.elements.query.Poller
import com.atlassian.jira.security.Permissions
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._

/**
 * Check if Clone/Copy menu item is visible by conditions described at https://jdog.atlassian.net/browse/JRADEV-16762
 *
 * View issue page would display "Remote Copy" menu item if
 * user is logged in,
 * he is in the allowed groups (or the list of allowed groups is empty)
 * and (he has permission to create issues for at least one local project or at least one JIRA application link is defined)
 *
 * Check [[it.com.atlassian.cpji.TestAllowedGroups]] for a test for allowed groups.
 */
class TestCloneMenuItem extends AbstractCopyIssueTest {
	val createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient3)

	@Rule def createIssuesRule = createIssues

	val jira1 = AbstractCopyIssueTest.jira1
	val jira3 = AbstractCopyIssueTest.jira3
	val testkit1 = AbstractCopyIssueTest.testkit1
	val testkit3 = AbstractCopyIssueTest.testkit3

	@Test def shouldNotDisplayLinkIfUserIsNotLoggedIn() {
		val issuePage: ExtendedViewIssuePage = jira1.visit(classOf[ExtendedViewIssuePage], "AN-1")
		Poller.waitUntilFalse(issuePage.getIssueActionsFragment.hasRICCloneAction)
	}

	@Test def shouldNotDisplayIfUserHasNoPermissionToCreateIssuesAndThereAreNoApplicationLinks() {
		val issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "AFER")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		try {
			testkit3.permissionSchemes().removeProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
			login(jira3)
			val issuePage: ExtendedViewIssuePage = jira3.visit(classOf[ExtendedViewIssuePage], issue.getKey)
			Poller.waitUntilFalse(issuePage.getIssueActionsFragment.hasRICCloneAction)
		} finally {
			testkit3.permissionSchemes().addProjectRolePermission(0, Permissions.CREATE_ISSUE, 10000)
		}

	}

	@Test def shouldDisplayIfUserHasNoPermissionToCreateIssueButApplicationLinkExists() {

	}

	@Test def shouldShowAnErrorWhenUserHasNoPermissionToCreateIssuesInRemoteApplications() {

	}

}
