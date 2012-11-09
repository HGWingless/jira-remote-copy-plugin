package it.com.atlassian.cpji;

import it.com.atlassian.cpji.pages.ConfigureCopyIssuesAdminActionPage;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * @since v2.1
 */
public class TestProjectConfiguration extends AbstractCopyIssueTest
{
    @Before
    public void setUp()
    {
        login(jira1);
    }

	@Test
	public void testProjectConfigurationDoesntIncludeSummary() {
		ConfigureCopyIssuesAdminActionPage adminPage = jira1.visit(ConfigureCopyIssuesAdminActionPage.class, "NEL");

		assertThat(adminPage.getRequiredFields(),
				IsIterableContainingInAnyOrder.<String>containsInAnyOrder(startsWith("Issue Type"), startsWith("Reporter")));
	}
}
