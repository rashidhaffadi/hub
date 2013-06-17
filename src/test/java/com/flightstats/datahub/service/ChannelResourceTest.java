package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationRequest;
import com.flightstats.datahub.model.exception.AlreadyExistsException;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.HalLink;
import com.flightstats.rest.HalLinks;
import com.flightstats.rest.Linked;
import com.google.common.collect.Multimap;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ChannelResourceTest {

	@Test
	public void testChannelCreation() throws Exception {
		String channelName = "UHF";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		Date date = new Date();
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelName, date);
		String channelUri = "http://path/to/UHF";
		String latestUri = "http://path/to/UHF/latest";
		String wsUri = "ws://path/to/UHF/ws";
		Linked<ChannelConfiguration> expected = Linked.linked(channelConfiguration)
													  .withLink("self", channelUri)
													  .withLink("latest", latestUri)
													  .withLink("ws", wsUri)
													  .build();
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = mock(CreateChannelValidator.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		when(uriInfo.getRequestUri()).thenReturn(URI.create("http://path/to"));
		when(dao.channelExists(channelName)).thenReturn(false);
		when(dao.createChannel(channelName)).thenReturn(channelConfiguration);
		when(linkBuilder.buildChannelUri(channelConfiguration)).thenReturn(URI.create(channelUri));
		when(linkBuilder.buildLatestUri(channelName)).thenReturn(URI.create(latestUri));
		when(linkBuilder.buildWsLinkFor(channelName)).thenReturn(URI.create(wsUri));

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator, null);

		Response response = testClass.createChannel(channelCreationRequest);

		verify(dao).createChannel(channelName);

		assertEquals(201, response.getStatus());
		assertEquals(new URI(channelUri), response.getMetadata().getFirst("location"));
		assertEquals(expected, response.getEntity());
	}

	@Test(expected = InvalidRequestException.class)
	public void testChannelCreation_emptyChannelName() throws Exception {
		String channelName = "  ";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = new CreateChannelValidator(dao);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator, null);

		testClass.createChannel(channelCreationRequest);
	}

	@Test(expected = AlreadyExistsException.class)
	public void testChannelCreation_channelAlreadyExists() throws Exception {
		String channelName = "zippy";

		ChannelCreationRequest channelCreationRequest = new ChannelCreationRequest(channelName);
		ChannelDao dao = mock(ChannelDao.class);
		CreateChannelValidator createChannelValidator = new CreateChannelValidator(dao);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, createChannelValidator, null);
		when(dao.channelExists(any(String.class))).thenReturn(true);
		testClass.createChannel(channelCreationRequest);
	}

	@Test
	public void testGetChannels() throws Exception {
		//GIVEN
		ChannelConfiguration channel1 = new ChannelConfiguration("foo", null);
		ChannelConfiguration channel2 = new ChannelConfiguration("bar", null);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel1, channel2);
		String channel1Uri = "http://superfoo";
		String channel2Uri = "http://superbar";
		String requestUri = "http://datahüb/channel";

		ChannelDao dao = mock(ChannelDao.class);
		UriInfo uriInfo = mock(UriInfo.class);
		ChannelHypermediaLinkBuilder linkBuilder = mock(ChannelHypermediaLinkBuilder.class);

		ChannelResource testClass = new ChannelResource(dao, linkBuilder, null, uriInfo);

		//WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(linkBuilder.buildChannelUri(channel1.getName())).thenReturn(URI.create(channel1Uri));
		when(linkBuilder.buildChannelUri(channel2.getName())).thenReturn(URI.create(channel2Uri));
		when(uriInfo.getRequestUri()).thenReturn(URI.create(requestUri));

		Response result = testClass.getChannels();

		//THEN

		Linked<Map> resultEntity = (Linked<Map>) result.getEntity();
		HalLinks resultHalLinks = resultEntity.getHalLinks();
		List<HalLink> links = resultHalLinks.getLinks();
		assertNull(resultEntity.getObject());
		assertEquals(1, links.size());
		assertEquals(new HalLink("self", URI.create(requestUri)), links.get(0));

		Multimap<String, HalLink> resultMultiLinks = resultHalLinks.getMultiLinks();
		assertEquals(1, resultMultiLinks.keySet().size());
		assertEquals(2, resultMultiLinks.size());
		Collection<HalLink> resultChannelLinks = resultMultiLinks.asMap().get("channels");
		assertThat(resultChannelLinks, CoreMatchers.hasItems(new HalLink(channel1.getName(), URI.create(channel1Uri)),
                new HalLink(channel2.getName(), URI.create(channel2Uri))));
	}
}
