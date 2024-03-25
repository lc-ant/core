package net.lecousin.ant.core.springboot.service.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

public class LcAntServiceClientBuilder {

	@Autowired(required = false)
	private DeferringLoadBalancerExchangeFilterFunction<?> loadBalancerFilter;
	
	@Value("${spring.cloud.discovery.enabled:true}")
	private boolean discoveryEnabled;
	
	@Autowired
	private InternalCallFilter internalCallFilter;
	
	public WebClient.Builder createBuilder() {
		WebClient.Builder builder = WebClient.builder();
		if (discoveryEnabled && loadBalancerFilter != null)
			builder.filter(loadBalancerFilter);
		builder.filter(internalCallFilter);
		return builder;
	}
	
}
