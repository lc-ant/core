package net.lecousin.ant.core.springboot.messaging;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiDataChangeEventListenerBeanPostProcessor implements BeanPostProcessor {
	
	private final ApiDataChangeEventConsumerService consumerService;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		ReflectionUtils.doWithMethods(targetClass, method -> {
			ApiDataChangeEventMulticastListener multicast = method.getAnnotation(ApiDataChangeEventMulticastListener.class);
			if (multicast != null) {
				if (method.getParameterCount() != 1 || !ApiDataChangeEvent.class.equals(method.getParameterTypes()[0]))
					throw new RuntimeException("Invalid method for @ApiDataChangeEventMulticastListener: must have a single parameter of type ApiDataChangeEvent");
				Function<ApiDataChangeEvent, Mono<Void>> consumer;
				if (Publisher.class.isAssignableFrom(method.getReturnType()))
					consumer = event -> {
						try {
							return Flux.from((Publisher<?>) method.invoke(bean, event))
								.checkpoint("ApiDataChangeEventMulticastListener: " + targetClass.getName() + '#' + method.getName())
								.then();
						} catch (Exception e) {
							return Mono.error(e);
						}
					};
				else
					consumer = event -> Mono.fromCallable(() -> method.invoke(bean, event))
						.checkpoint("ApiDataChangeEventMulticastListener: " + targetClass.getName() + '#' + method.getName())
						.then();
				consumerService.listenMulticast(multicast.service(), multicast.type(), consumer);
			}
			
			ApiDataChangeEventUnicastListener unicast = method.getAnnotation(ApiDataChangeEventUnicastListener.class);
			if (unicast != null) {
				if (method.getParameterCount() != 1 || !ApiDataChangeEvent.class.equals(method.getParameterTypes()[0]))
					throw new RuntimeException("Invalid method for @ApiDataChangeEventUnicastListener: must have a single parameter of type ApiDataChangeEvent");
				Function<ApiDataChangeEvent, Mono<Void>> consumer;
				if (Publisher.class.isAssignableFrom(method.getReturnType()))
					consumer = event -> {
						try {
							return Flux.from((Publisher<?>) method.invoke(bean, event))
								.checkpoint("ApiDataChangeEventUnicastListener: " + targetClass.getName() + '#' + method.getName())
								.then();
						} catch (Exception e) {
							return Mono.error(e);
						}
					};
				else
					consumer = event -> Mono.fromCallable(() -> method.invoke(bean, event))
						.checkpoint("ApiDataChangeEventUnicastListener: " + targetClass.getName() + '#' + method.getName())
						.then();
				consumerService.listenUnicast(unicast.service(), unicast.name(), unicast.type(), consumer);
			}
		}, ReflectionUtils.USER_DECLARED_METHODS);
		return bean;
	}
	
}
