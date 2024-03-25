package net.lecousin.ant.core.springboot.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class LcAntMessagingConfiguration {

	@Bean
	TopicExchange genericDataChangeExchange() {
		return new TopicExchange(ApiDataChangeEvent.TOPIC_EXCHANGE);
	}
	
}
