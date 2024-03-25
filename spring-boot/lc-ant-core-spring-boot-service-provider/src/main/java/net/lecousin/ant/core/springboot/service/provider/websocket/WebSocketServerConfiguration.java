package net.lecousin.ant.core.springboot.service.provider.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.lecousin.ant.core.springboot.traceability.TraceabilityService;

@Configuration
public class WebSocketServerConfiguration {

	@Bean
	WebSocketServerHandlerMapping webSocketServerHandlerMapping() {
		return new WebSocketServerHandlerMapping();
	}
	
	@Bean
	WebSocketControllerPostProcessor webSocketControllerPostProcessor(WebSocketServerHandlerMapping handlerMapping, TraceabilityService traceabilityService) {
		return new WebSocketControllerPostProcessor(handlerMapping, traceabilityService);
	}
	
}
