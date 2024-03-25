package net.lecousin.ant.core.springboot.connector;

import reactor.core.publisher.Mono;

public interface Connector {

	Mono<Void> destroy();
	
}
