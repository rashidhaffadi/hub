package com.flightstats.hub.util;

import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
public class HubUtils {

    public static final int NOT_FOUND = -1;
    public static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
    private final static Logger logger = LoggerFactory.getLogger(HubUtils.class);
    private final Client noRedirectsClient;
    private final Client followClient;

    @Inject
    public HubUtils(@Named("NoRedirects") Client noRedirectsClient, Client followClient) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
    }

    public Optional<String> getLatest(String channelUrl) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = noRedirectsClient.resource(channelUrl + "latest")
                .accept(MediaType.WILDCARD_TYPE)
                .head();
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return Optional.absent();
        }
        return Optional.of(response.getLocation().toString());
    }

    private String appendSlash(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        return channelUrl;
    }

    public ClientResponse startGroupCallback(Group group) {
        String groupUrl = getSourceUrl(group.getChannelUrl()) + "/group/" + group.getName();
        String json = group.toJson();
        logger.info("starting {} with {}", groupUrl, json);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, json);
        logger.info("start group response {}", response);
        return response;
    }

    public void stopGroupCallback(String groupName, String sourceChannel) {
        String groupUrl = getSourceUrl(sourceChannel) + "/group/" + groupName;
        logger.info("stopping {} ", groupUrl);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .delete(ClientResponse.class);
        logger.debug("stop group response {}", response);

    }

    private String getSourceUrl(String sourceChannel) {
        return StringUtils.substringBefore(sourceChannel, "/channel/");
    }

    public boolean putChannel(String url, ChannelConfig channelConfig) {
        ClientResponse response = followClient.resource(url)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, channelConfig.toJson());
        logger.info("put channel response {} {}", channelConfig, response);
        return response.getStatus() < 400;
    }

}
