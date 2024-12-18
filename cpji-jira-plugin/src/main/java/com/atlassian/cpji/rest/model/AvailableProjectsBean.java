package com.atlassian.cpji.rest.model;

import com.atlassian.cpji.components.model.NegativeResponseStatus;
import com.atlassian.cpji.components.model.Projects;
import com.atlassian.cpji.components.remote.JiraProxyFactory;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.atlassian.fugue.Either;
import io.atlassian.fugue.Eithers;

import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @since v2.1
 */
@XmlRootElement
public class AvailableProjectsBean {
	@XmlElement
	private List<ProjectGroupBean> projects;
	@XmlElement
	private RemoteFailuresBean failures;

	public AvailableProjectsBean(Iterable<ProjectGroupBean> projects, RemoteFailuresBean failures) {
		this.projects = ImmutableList.copyOf(Preconditions.checkNotNull(projects));
		this.failures = Preconditions.checkNotNull(failures);
	}

	public static AvailableProjectsBean create(final JiraProxyFactory proxyFactory, final String issueId, Iterable<Either<NegativeResponseStatus, Projects>> projects) {
        Preconditions.checkNotNull(proxyFactory);
        Preconditions.checkNotNull(issueId);
        Preconditions.checkNotNull(projects);
		return new AvailableProjectsBean(Iterables.transform(Eithers.filterRight(projects),
				new ProjectsToProjectGroupBean()),
				RemoteFailuresBean.create(proxyFactory, issueId, Eithers.filterLeft(projects)));
	}

	private static class ProjectsToProjectGroupBean implements Function<Projects, ProjectGroupBean> {
		@Override
		public ProjectGroupBean apply(final Projects entry) {
			Iterable<BasicProject> basicProjectsIterable = entry.getResult();
			Iterable<ProjectBean> projectsInServer = Iterables
					.transform(basicProjectsIterable, new Function<Object, ProjectBean>() {
						@Override
						public ProjectBean apply(final Object o) {
							return new ProjectBean((BasicProject) o);
						}
					});
			return new ProjectGroupBean(entry.getJiraLocation().getName(), entry.getJiraLocation().getId(), Lists
					.newArrayList(projectsInServer));
		}
	}

	@Nonnull
	public Iterable<ProjectGroupBean> getProjects() {
		return projects;
	}

	@Nonnull
	public RemoteFailuresBean getFailures() {
		return failures;
	}
}
