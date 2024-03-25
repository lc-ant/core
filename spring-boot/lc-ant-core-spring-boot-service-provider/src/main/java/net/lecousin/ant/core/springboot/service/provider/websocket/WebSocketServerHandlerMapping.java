package net.lecousin.ant.core.springboot.service.provider.websocket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class WebSocketServerHandlerMapping implements HandlerMapping, Ordered {

	private Map<String, WebSocketServerHandler> mapping = new HashMap<>();
	
	void addHandler(String path, WebSocketServerHandler handler) {
		log.info("Web socket handler registered for path " + path + ": " + handler);
		mapping.put(path, handler);
	}
	
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
	@Override
	public Mono<Object> getHandler(ServerWebExchange exchange) {
		return Mono.fromSupplier(() -> {
			String path = exchange.getRequest().getPath().value();
			return mapping.get(path);
		});
	}
	
	public Map<String, WebSocketServerHandler> getMapping() {
		return mapping;
	}
	
}
