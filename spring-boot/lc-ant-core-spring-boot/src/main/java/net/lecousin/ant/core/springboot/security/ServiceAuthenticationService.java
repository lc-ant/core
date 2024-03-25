package net.lecousin.ant.core.springboot.security;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ServiceAuthenticationService {

	@Autowired(required = false)
	private List<ServiceAuthenticationProvider> providers;
	
	public <T> Mono<T> executeMonoAs(String serviceName, Supplier<Mono<T>> supplier) {
		var opt = providers != null ? providers.stream().filter(p -> p.canAuthenticate(serviceName)).findAny() : Optional.<ServiceAuthenticationProvider>empty();
		if (opt.isEmpty()) return Mono.error(new RuntimeException("No ServiceAuthenticationProvider for service " + serviceName));
		return opt.get().executeMonoAs(serviceName, supplier);
	}
	
	public <T> Flux<T> executeFluxAs(String serviceName, Supplier<Flux<T>> supplier) {
		var opt = providers != null ? providers.stream().filter(p -> p.canAuthenticate(serviceName)).findAny() : Optional.<ServiceAuthenticationProvider>empty();
		if (opt.isEmpty()) return Flux.error(new RuntimeException("No ServiceAuthenticationProvider for service " + serviceName));
		return opt.get().executeFluxAs(serviceName, supplier);
	}
	
}
