package it.com.atlassian.cpji

import org.junit.{Before, Rule, Test}
import com.atlassian.cpji.tests.rules.CreateIssues
import com.atlassian.jira.rest.client.domain.{Comment, IssueFieldId, Issue}
import com.atlassian.jira.rest.client.domain.input.{ComplexIssueInputFieldValue, FieldInput}
import com.atlassian.jira.rest.client.domain.IssueFieldId._
import org.joda.time.DateTime
import com.atlassian.cpji.tests.pageobjects.{CopyDetailsPage, SelectTargetProjectPage}
import org.junit.Assert._
import java.io.ByteArrayInputStream
;

class TestCopyDetailsAction extends AbstractCopyIssueTest {
	var createIssues: CreateIssues = new CreateIssues(AbstractCopyIssueTest.restClient1)

	@Rule def createIssuesRule = createIssues

	@Before def setUp {
		login(AbstractCopyIssueTest.jira1)
	}

	@Test def testAdvancedSection() {
		val issue: Issue = createIssues.newIssue(new FieldInput(SUMMARY_FIELD, "Issue with comments"),
			new FieldInput(PROJECT_FIELD, ComplexIssueInputFieldValue.`with`("key", "TST")),
			new FieldInput(IssueFieldId.ISSUE_TYPE_FIELD, ComplexIssueInputFieldValue.`with`("id", "3")))

		AbstractCopyIssueTest.restClient1.getIssueClient.addComment(AbstractCopyIssueTest.NPM, issue.getCommentsUri,
			new Comment(null, "This is a comment", null, null, new DateTime, new DateTime, null, null))

		AbstractCopyIssueTest.restClient1.getIssueClient.addAttachment(AbstractCopyIssueTest.NPM,
			issue.getAttachmentsUri, new ByteArrayInputStream("this is a stream".getBytes("UTF-8")), this.getClass.getCanonicalName)

		val selectTargetProjectPage: SelectTargetProjectPage = AbstractCopyIssueTest.jira1.visit(classOf[SelectTargetProjectPage], issue.getId)
		val copyDetailsPage: CopyDetailsPage = selectTargetProjectPage.next()

		assertTrue(copyDetailsPage.isCopyCommentsGroupVisible)
		assertTrue(copyDetailsPage.isCopyAttachmentsGroupVisible)
		assertFalse(copyDetailsPage.isCopyIssueLinksGroupVisible)
		assertTrue(copyDetailsPage.isCreateIssueLinksGroupVisible)
	}

}
