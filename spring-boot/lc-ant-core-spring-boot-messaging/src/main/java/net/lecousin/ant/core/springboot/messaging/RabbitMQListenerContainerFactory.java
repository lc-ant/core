package net.lecousin.ant.core.springboot.messaging;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnProperty(name = "lc-ant.messaging.implementation", havingValue = "rabbitmq")
@RequiredArgsConstructor
public class RabbitMQListenerContainerFactory implements LcAntAmpqListenerContainerFactory {

	private final RabbitTemplate rabbit;
	
	@Override
	public LcAntAmpqListenerContainer createDirectMessageListenerContainer(Queue queue, MessageListener messageListener) {
		DirectMessageListenerContainer container = new DirectMessageListenerContainer(rabbit.getConnectionFactory());
		container.setConsumersPerQueue(1);
		container.setMessageListener(messageListener);
		container.addQueues(queue);
		container.setAutoStartup(false);
		container.afterPropertiesSet();
		container.start();
		return new LcAntAmpqListenerContainer() {
			@Override
			public void stop() {
				container.removeQueues(queue);
				container.destroy();
			}
		};
	}
	
}
