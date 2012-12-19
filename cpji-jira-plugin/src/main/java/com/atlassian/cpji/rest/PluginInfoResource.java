package com.atlassian.cpji.rest;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @since v1.1
 */
@Path (PluginInfoResource.RESOURCE_PATH)
@Consumes ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Produces ( { MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public class PluginInfoResource
{
    public static final String RESOURCE_PATH = "plugininfo";
    public static final String RESOURCE_VERSION_PATH = "version";
    public static final String PLUGIN_INSTALLED = "installed";
    public static final String PLUGIN_VERSION = "3.0";

    @GET
    @AnonymousAllowed
    public Response pluginInfo()
    {
        return Response.ok(PLUGIN_INSTALLED).build();
    }

    @GET
    @Path(RESOURCE_VERSION_PATH)
    @AnonymousAllowed
    public Response pluginVersion()
    {
        return Response.ok(PLUGIN_VERSION).build();
    }
}
