package com.flightstats.hub.channel;

import datadog.trace.api.Trace;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@SuppressWarnings("WeakerAccess")
@Path("/tag/{tag}/time")
public class TagTimeResource {

    @Context
    private UriInfo uriInfo;

    @Trace
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefault() {
        return TimeLinkUtil.getDefault(uriInfo);
    }

    @Trace
    @Path("/second")
    @GET
    public Response getSecond(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getSecond(stable, uriInfo);
    }

    @Trace
    @Path("/minute")
    @GET
    public Response getMinute(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getMinute(stable, uriInfo);
    }

    @Trace
    @Path("/hour")
    @GET
    public Response getHour(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getHour(stable, uriInfo);
    }

    @Trace
    @Path("/day")
    @GET
    public Response getDay(@QueryParam("stable") @DefaultValue("true") boolean stable) {
        return TimeLinkUtil.getDay(stable, uriInfo);
    }
}
