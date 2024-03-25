package net.lecousin.ant.core.springboot.messaging;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;

public interface LcAntAmpqListenerContainerFactory {

	LcAntAmpqListenerContainer createDirectMessageListenerContainer(Queue queue, MessageListener messageListener);
	
}
