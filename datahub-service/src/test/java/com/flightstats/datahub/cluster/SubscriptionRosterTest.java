package com.flightstats.datahub.cluster;

import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.flightstats.datahub.service.eventing.Consumer;
import com.flightstats.datahub.service.eventing.SubscriptionRoster;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SubscriptionRosterTest {

	@Test
	public void testSubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";
        String key = "54321";

		Message message = mock(Message.class);
		Consumer<String> consumer = mock(Consumer.class);
		ChannelInsertionPublisher channelInsertionPublisher = mock(ChannelInsertionPublisher.class);
        when(channelInsertionPublisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn("54321");
		ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		SubscriptionRoster testClass = new SubscriptionRoster(channelInsertionPublisher);

		//WHEN
		when(message.getMessageObject()).thenReturn(key);

		testClass.subscribe(channelName, consumer);

		//THEN
		verify(channelInsertionPublisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		messageListenerCaptor.getValue().onMessage(message);
		verify(consumer).apply(key);
	}

	@Test
	public void testUnsubscribe() throws Exception {
		//GIVEN
		String channelName = "4chan";

		Consumer<String> consumer = mock(Consumer.class);
		ChannelInsertionPublisher channelInsertionPublisher = mock(ChannelInsertionPublisher.class);
        String id = "12345";
        when(channelInsertionPublisher.subscribe(any(String.class), any(MessageListener.class))).thenReturn(id);
        ArgumentCaptor<MessageListener> messageListenerCaptor = ArgumentCaptor.forClass(MessageListener.class);

		SubscriptionRoster testClass = new SubscriptionRoster(channelInsertionPublisher);

		//WHEN
		testClass.subscribe(channelName, consumer);        //Need to subscribe first because this class is stateful
		testClass.unsubscribe(channelName, consumer);

		//THEN
		verify(channelInsertionPublisher).subscribe(eq(channelName), messageListenerCaptor.capture());
		verify(channelInsertionPublisher).unsubscribe(channelName, id);
		assertEquals(0, testClass.getTotalSubscriberCount());
	}
}
