package net.lecousin.ant.core.springboot.connector;

import reactor.core.publisher.Mono;

public interface ConnectorFactory<C extends Connector, P> {

	String getType();
	
	String getImplementation();
	
	Class<C> getConnectorClass();
	
	Class<P> getPropertiesClass();
	
	Mono<C> create(P properties);
	
}
