package net.lecousin.ant.core.springboot.security;

import java.util.function.Supplier;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceAuthenticationProvider {

	boolean canAuthenticate(String serviceName);
	
	<T> Mono<T> executeMonoAs(String serviceName, Supplier<Mono<T>> supplier);
	
	<T> Flux<T> executeFluxAs(String serviceName, Supplier<Flux<T>> supplier);
}
