package net.lecousin.ant.core.springboot.discovery;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DiscoveryURIService {

	private final ReactiveDiscoveryClient discoveryClient;
	
	@Value("${lc-ant.discovery.cache.duration:2m}")
	private Duration maxKeepDiscoveryResponse;
	
	private final Map<String, Cache> cacheByService = new HashMap<>();
	
	public Mono<URI> get(String serviceName) {
		Cache cache;
		synchronized (cacheByService) {
			cache = cacheByService.computeIfAbsent(serviceName, sn -> new Cache(sn));
		}
		return cache.get();
	}
	
	@RequiredArgsConstructor
	private class Cache {
		
		private final String serviceName;
		
		private LinkedList<URI> last = null;
		private long lastTime = 0;
		
		public Mono<URI> get() {
			synchronized (this) {
				if (last != null && !last.isEmpty() && System.currentTimeMillis() - lastTime < maxKeepDiscoveryResponse.toMillis())
					return Mono.just(last.removeFirst());
				last = null;
			}
			if (discoveryClient == null)
				return Mono.error(new IllegalStateException("Missing discovery client"));
			return discoveryClient.getInstances(serviceName).collectList()
				.map(instances -> {
					if (instances.isEmpty())
						throw new IllegalStateException("Service cannot be found: " + serviceName);
					LinkedList<URI> uris = new LinkedList<>();
					for (ServiceInstance instance : instances)
						uris.add(instance.getUri());
					URI uri;
					synchronized (Cache.this) {
						last = uris;
						lastTime = System.currentTimeMillis();
						uri = last.removeFirst();
					}
					return uri;
				});
		}
		
	}
	
}
