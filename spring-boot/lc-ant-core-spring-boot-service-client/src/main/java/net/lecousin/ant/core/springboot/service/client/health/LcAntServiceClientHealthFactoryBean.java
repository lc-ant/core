package net.lecousin.ant.core.springboot.service.client.health;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import lombok.Setter;
import net.lecousin.ant.core.springboot.service.client.LcAntServiceClientBuilder;
import reactor.core.publisher.Mono;

public class LcAntServiceClientHealthFactoryBean implements SmartFactoryBean<Object>, ApplicationContextAware {

	@Setter
	private ApplicationContext applicationContext;
	
	@Setter
	private String serviceName;
	@Setter
	private String serviceUrl;
	
	@Override
	public boolean isEagerInit() {
		return false;
	}
	
	@Override
	public boolean isSingleton() {
		return true;
	}
	
	@Override
	public boolean isPrototype() {
		return false;
	}
	
	@Override
	public Object getObject() {
		try {
			Class<?> serviceProviderClass = Class.forName("net.lecousin.ant.core.springboot.service.provider.LcAntServiceProvider");
			for (var localService : applicationContext.getBeansOfType(serviceProviderClass).values()) {
				String localServiceName = (String) serviceProviderClass.getMethod("getServiceName").invoke(localService);
				if (serviceName.equals(localServiceName))
					return new LcAntServiceClientHealth() {
						@Override
						public Mono<Response> ping() {
							return Mono.just(new Response("UP"));
						}
					};
			}
		} catch (Exception e) {
			// ignore
		}
		String resolvedUrl = applicationContext.getEnvironment().resolvePlaceholders(serviceUrl);
		WebClient client = applicationContext.getBean(LcAntServiceClientBuilder.class).createBuilder()
			.baseUrl("http://" + resolvedUrl)
			.build();
		return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client))
			.build().createClient(LcAntServiceClientHealth.class);
	}
	
	@Override
	public Class<?> getObjectType() {
		return LcAntServiceClientHealth.class;
	}
	
}
