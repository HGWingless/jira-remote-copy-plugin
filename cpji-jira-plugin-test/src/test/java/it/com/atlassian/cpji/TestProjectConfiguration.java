package it.com.atlassian.cpji;

import com.atlassian.jira.tests.annotations.JiraBuildNumberDependent;
import com.atlassian.jira.tests.rules.JiraBuildNumberRule;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.collect.Iterables;
import it.com.atlassian.cpji.pages.ConfigureCopyIssuesAdminActionPage;
import it.com.atlassian.cpji.pages.JiraLoginPageWithWarnings;
import it.com.atlassian.cpji.pages.PermissionViolationPage;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @since v2.1
 */
public class TestProjectConfiguration extends AbstractCopyIssueTest
{
	@Rule
	public JiraBuildNumberRule jiraBuildNumberRule = new JiraBuildNumberRule(jira1);

    @Before
    public void setUp()
    {
        login(jira1);
    }

	@Test
	public void testProjectConfigurationDoesntIncludeSummary() {
		ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "NEL");
		assertThat(adminPage.getRequiredFields(), expectedRequiredFields());
	}

	@JiraBuildNumberDependent(value = 788)
    @Test
    public void defaultReporterShouldReturnErrorOnInvalidUserAndSaveValidOne() {
        ConfigureCopyIssuesAdminActionPage.AsDialog adminPage = ConfigureCopyIssuesAdminActionPage.AsDialog.open(jira1,
				"NEL");

        adminPage.setReporter("FAKE USER").submit();

        assertTrue(adminPage.hasGeneralErrorsMessage());
        assertThat(adminPage.getErrors(), IsIterableContainingInAnyOrder.containsInAnyOrder("The reporter specified is not a user."));

        adminPage.setReporter("fred").submit();

        assertThat(adminPage.getSuccessMessages(), IsIterableContainingInAnyOrder.containsInAnyOrder("Saved default value for field 'Reporter'"));
        assertEquals("Fred Flinstone", adminPage.getReporterText());
    }

    private Matcher<Iterable<String>> expectedRequiredFields() {
        return IsIterableContainingInAnyOrder.<String>containsInAnyOrder(startsWith("Issue Type"), startsWith("Reporter"));
    }

	@Test
	public void permissionDeniedShouldBePresentedWhenAnonymous() {
		jira1.logout();
		jira1.getTester().getDriver().navigate().to(jira1.getProductInstance().getBaseUrl() + "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=TST");
		final JiraLoginPageWithWarnings jiraPage = jira1.getPageBinder().bind(JiraLoginPageWithWarnings.class);
		assertTrue(jiraPage.hasWarnings());
		final PageElement warning = Iterables.getFirst(jiraPage.getWarnings(), null);
		assertNotNull(warning);
		assertEquals("You must log in to access this page.", warning.find(By.tagName("p")).getText());
	}

	@Test
	public void permissionDeniedWhenRegularUser() {
		jira1.logout();
		jira1.gotoLoginPage().login("fred", "fred", PermissionViolationPage.class, "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=TST");
	}
}
