package com.atlassian.cpji.components;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.fugue.Either;
import com.atlassian.jira.rest.client.domain.ServerInfo;
import com.atlassian.jira.rest.client.internal.json.BasicProjectsJsonParser;
import com.atlassian.jira.rest.client.internal.json.ServerInfoJsonParser;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RemoteJiraService {

	private static final Logger log = Logger.getLogger(RemoteJiraService.class);
	private static final int THREADS = 5;

	private final ApplicationLinkService applicationLinkService;

	public RemoteJiraService(final ApplicationLinkService applicationLinkService) {
		this.applicationLinkService = applicationLinkService;
	}

	@Nonnull
	public Map<ApplicationLink, Either<ResponseStatus, ServerInfo>> getServersInfo() {
		final Map<ApplicationLink, Either<ResponseStatus, ServerInfo>> result = Maps.newHashMap();
		for (ApplicationLink jira : applicationLinkService.getApplicationLinks(JiraApplicationType.class)) {
			result.put(jira, getServerInfo(jira));
		}
		return result;
	}

	private Either<ResponseStatus, ServerInfo> getServerInfo(ApplicationLink jiraServer) {
		return callRestService(jiraServer, jiraServer.createAuthenticatedRequestFactory(), "/rest/api/latest/serverInfo", new AbstractJsonResponseHandler<ServerInfo>(
				jiraServer) {
			@Override
			protected ServerInfo parseResponse(Response response) throws ResponseException, JSONException {
				return new ServerInfoJsonParser().parse(
						new JSONObject(new JSONTokener(response.getResponseBodyAsString())));
			}
		});
	}

	@Nonnull
	public Iterable<Either<ResponseStatus, Projects>> getProjects() {
		final ExecutorService es = Executors.newFixedThreadPool(THREADS);
		final Iterable<ApplicationLink> applicationLinks = applicationLinkService.getApplicationLinks(
				JiraApplicationType.class);

		final List<Callable<Either<ResponseStatus, Projects>>> queries = Lists.newArrayList(
				Iterables.transform(applicationLinks,
						new Function<ApplicationLink, Callable<Either<ResponseStatus, Projects>>>() {
							@Override
							public Callable<Either<ResponseStatus,Projects>> apply(final ApplicationLink applicationLink) {
								final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();

								return new Callable<Either<ResponseStatus, Projects>>() {
									@Override
									public Either<ResponseStatus, Projects> call() {
										return getProjects(applicationLink, requestFactory);
									}
								};
							}
						})
		);

		try {
			return ImmutableList.copyOf(Iterables.transform(es.invokeAll(queries),
					new Function<Future<Either<ResponseStatus, Projects>>, Either<ResponseStatus, Projects>>() {
						@Override
						public Either<ResponseStatus, Projects> apply(Future<Either<ResponseStatus, Projects>> eitherFuture) {
							try {
								return eitherFuture.get();
							} catch (Exception e) {
								log.warn("Failed to execute Application Links request", e);
								return Either.left(ResponseStatus.communicationFailed(null));
							}
						}
					}));
		} catch (InterruptedException e) {
			log.warn("Threads were interrupted during Application Links request", e);
			return Collections.emptyList();
		}
	}

	@Nonnull
	public Either<ResponseStatus, Projects> getProjects(ApplicationLink jiraServer) {
		return getProjects(jiraServer, jiraServer.createAuthenticatedRequestFactory());
	}

	@Nonnull
	protected Either<ResponseStatus, Projects> getProjects(final ApplicationLink applicationLink, ApplicationLinkRequestFactory requestFactory) {
		return callRestService(applicationLink, requestFactory, "/rest/copyissue/1.0/project", new AbstractJsonResponseHandler<Projects>(
				applicationLink) {
			@Override
			protected Projects parseResponse(Response response) throws ResponseException, JSONException {
				return new Projects(applicationLink, new BasicProjectsJsonParser().parse(
						new JSONArray(new JSONTokener(response.getResponseBodyAsString()))));
			}
		});
	}

	private <T> Either<ResponseStatus, T> callRestService(final ApplicationLink applicationLink,
			final ApplicationLinkRequestFactory requestFactor, final String path, final AbstractJsonResponseHandler handler) {
		try {
			ApplicationLinkRequest request = requestFactor.createRequest(Request.MethodType.GET, path);
			return (Either<ResponseStatus, T>) request.execute(handler);
		}
		catch (CredentialsRequiredException ex)
		{
			return Either.left(ResponseStatus.authorizationRequired(applicationLink));
		} catch (ResponseException e) {
			log.error(String.format("Failed to transform response from Application Link: %s (%s)", toString(applicationLink), e.getMessage()));
			return Either.left(ResponseStatus.communicationFailed(applicationLink));
		}
	}

	protected static abstract class AbstractJsonResponseHandler<T> implements ApplicationLinkResponseHandler<Either<ResponseStatus, T>>
	{
		private final ApplicationLink applicationLink;

		protected AbstractJsonResponseHandler(ApplicationLink applicationLink) {
			this.applicationLink = applicationLink;
		}

		public Either<ResponseStatus, T> credentialsRequired(final Response response) throws ResponseException
		{
			return Either.left(ResponseStatus.authorizationRequired(applicationLink));
		}

		public Either<ResponseStatus, T> handle(final Response response) throws ResponseException
		{
			if (log.isDebugEnabled())
			{
				log.debug("Response is: " + response.getResponseBodyAsString());
			}
			if (response.getStatusCode() == 401)
			{
				return Either.left(ResponseStatus.authenticationFailed(applicationLink));
			}
			try {
				return Either.right(parseResponse(response));
			} catch (JSONException e) {
				log.error(String.format("Failed to parse JSON from Application Link: %s (%s)", RemoteJiraService.toString(applicationLink), e.getMessage()));
				return Either.left(ResponseStatus.communicationFailed(applicationLink));
			}
		}

		protected abstract T parseResponse(Response response) throws ResponseException, JSONException;
	}

	protected static String toString(ApplicationLink applicationLink) {
		return applicationLink.getName() + " " + applicationLink.getId() + " " + applicationLink.getDisplayUrl();
	}
}