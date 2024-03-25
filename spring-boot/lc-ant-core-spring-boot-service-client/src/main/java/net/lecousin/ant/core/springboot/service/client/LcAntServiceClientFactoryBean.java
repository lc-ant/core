package net.lecousin.ant.core.springboot.service.client;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.springboot.http.PageRequestHttpServiceArgumentResolver;

@Slf4j
public class LcAntServiceClientFactoryBean implements SmartFactoryBean<Object>, ApplicationContextAware {

	@Setter
	private ApplicationContext applicationContext;
	
	@Setter
	private Class<?> clazz;
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
	
	private Object singleton = null;
	
	@Override
	public Object getObject() {
		if (singleton != null) return singleton;
		String resolvedUrl = applicationContext.getEnvironment().resolvePlaceholders(serviceUrl);
		log.info("Service client URL {} resolved to {} from client {}", serviceUrl, resolvedUrl, clazz.getSimpleName());
		WebClient client = applicationContext.getBean(LcAntServiceClientBuilder.class).createBuilder()
			.baseUrl("http://" + resolvedUrl)
			.build();
		singleton = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(client))
			.customArgumentResolver(new PageRequestHttpServiceArgumentResolver())
			.build()
			.createClient(clazz);
		return singleton;
	}
	
	@Override
	public Class<?> getObjectType() {
		return clazz;
	}
	
}
