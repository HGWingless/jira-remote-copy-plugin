package it.com.atlassian.cpji.pages;

import com.atlassian.cpji.tests.pageobjects.MultiSelectUtil;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;
import com.atlassian.jira.pageobjects.project.summary.SettingsPanel;
import com.atlassian.jira.pageobjects.util.TraceContext;
import com.atlassian.jira.pageobjects.util.Tracer;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.webdriver.WebDriverQueryFunctions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * @since v2.1
 */
public class ConfigureCopyIssuesAdminActionPage extends AbstractJiraPage {
	private static final String URI_TEMPLATE = "/secure/ConfigureCopyIssuesAdminAction!default.jspa?projectKey=%s";

    protected final String projectKey;

    @ElementBy(id = "cpji-update-button")
	protected PageElement updateButton;

    @Inject
    private TraceContext traceContext;


	public ConfigureCopyIssuesAdminActionPage(@Nonnull String projectKey) {
		this.projectKey = projectKey;
    }

	public ConfigureCopyIssuesAdminActionPage setAllowedGroups(@Nullable Iterable<String> groups) {
		MultiSelectUtil.setMultiSelect(this.pageBinder, "groups", groups);
		return this;
	}

	@Nonnull
	public Iterable<String> getAllowedGroups() {
		return MultiSelectUtil.getMultiSelect(this.pageBinder, "groups");
	}

	public ConfigureCopyIssuesAdminActionPage submit() {
        Tracer checkpoint = traceContext.checkpoint();
		updateButton.click();
        traceContext.waitFor(checkpoint, "cpji.load.completed");
		return pageBinder.bind(ConfigureCopyIssuesAdminActionPage.class, projectKey);
	}

    public DefaultValuesFragment getDefaultValues(){
        return pageBinder.bind(DefaultValuesFragment.class);
    }



    public boolean hasGeneralErrorsMessage(){
        return driver.findElement(By.id("cpji-general-errors")).isDisplayed();
    }

	@Override
	public TimedCondition isAt() {
        return updateButton.timed().isVisible();
	}

	@Override
	public String getUrl() {
        return String.format(URI_TEMPLATE, projectKey);
	}

	@Nonnull
	public List<String> getRequiredFields() {
		return ImmutableList.copyOf(
				Iterables.transform(driver.findElements(By.cssSelector("#cpji-required-fields div.field-group label")),
						WebDriverQueryFunctions.getText()));
	}


    public List<String> getErrors(){
        return ImmutableList.copyOf(Iterables.transform(driver.findElements(By.className("error")), WebDriverQueryFunctions.getText()));
    }

    public List<String> getSuccessMessages(){
        final List<WebElement> messages = driver.findElements(By.cssSelector("#cpji-general-success li"));
        return ImmutableList.copyOf(Iterables.transform(messages, WebDriverQueryFunctions.getText()));
    }

    public static class AsDialog extends ConfigureCopyIssuesAdminActionPage{

        public AsDialog(@Nonnull String projectKey){
            super(projectKey);
        }

        public static AsDialog open(JiraTestedProduct jira, String projectKey){
            ProjectSummaryPageTab summary = jira.visit(ProjectSummaryPageTab.class, projectKey);
            final List<PageElement> plugins = summary.openPanel(SettingsPanel.class).getPluginElements();
            PageElement configureLink = Iterables.find(plugins, new Predicate<PageElement>() {
                @Override
                public boolean apply(@Nullable final PageElement input) {
                    return input.getText().startsWith("Remote Issue Copy");
                }
            });
            configureLink.find(By.id("configure_cpji")).click();
            return jira.getPageBinder().bind(AsDialog.class, projectKey);
        }

    }


}
