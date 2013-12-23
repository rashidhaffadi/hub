package com.flightstats.datahub.app.config;

import com.flightstats.datahub.dao.ChannelMetadataDao;
import com.flightstats.datahub.dao.ChannelMetadataInitialization;
import com.flightstats.datahub.dao.cassandra.CassandraChannelMetadataDao;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelMetadataInitializationTest {

	@Test
	public void testHear() throws Exception {
		//GIVEN
		ChannelMetadataInitialization testClass = new ChannelMetadataInitialization();
		//WHEN
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		testClass.hear(type, encounter);

		//THEN
		verify(encounter).register(isA(InjectionListener.class));
	}

	@Test
	public void testInjectionListenerLifecycle_initOnlyCalledOnce() throws Exception {
		//GIVEN
		ChannelMetadataInitialization testClass = new ChannelMetadataInitialization();
		//WHEN
		TypeLiteral<Object> type = mock(TypeLiteral.class);
		TypeEncounter<Object> encounter = mock(TypeEncounter.class);
		ChannelMetadataDao channelMetadataDao = mock(CassandraChannelMetadataDao.class);
		testClass.hear(type, encounter);

		//THEN
		ArgumentCaptor<InjectionListener> captor = ArgumentCaptor.forClass(InjectionListener.class);
		verify(encounter).register(captor.capture());
		InjectionListener listener = captor.getValue();
		listener.afterInjection(channelMetadataDao);
		listener.afterInjection(channelMetadataDao);
		listener.afterInjection(channelMetadataDao);
		verify(channelMetadataDao).initializeMetadata();
	}
}
