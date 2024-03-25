package net.lecousin.ant.core.springboot.service.provider.websocket;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.springboot.service.provider.websocket.WebSocketServerHandler.WebSocketServerSession;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@RequiredArgsConstructor
public class WebSocketControllerPostProcessor implements BeanPostProcessor {

	private final WebSocketServerHandlerMapping handlerMapping;
	private final TraceabilityService traceService;
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();
		WebSocketController annotation = AnnotationUtils.getAnnotation(clazz, WebSocketController.class);
		if (annotation != null) {
			WebSocketServerHandler server = new WebSocketServerHandler(traceService, annotation.serviceName());
			for (Method m : clazz.getMethods()) {
				if (m.getAnnotation(WebSocketController.OnSessionStarted.class) != null) {
					processMethodOnSessionStarted(server, bean, m);
				} else if (m.getAnnotation(WebSocketController.OnSessionClosed.class) != null) {
					processMethodOnSessionClosed(server, bean, m);
				} else {
					Optional<Tuple2<Class<?>, BiFunction<WebSocketServerSession, Object, Mono<Void>>>> opt = processMethod(bean, m);
					if (opt.isPresent()) {
						Tuple2<Class<?>, BiFunction<WebSocketServerSession, Object, Mono<Void>>> tuple = opt.get();
						server.on(tuple.getT1(), tuple.getT2());
					}
				}
			}
			log.info("Web socket controller found mapped on " + annotation.path() + ": " + clazz.getName());
			handlerMapping.addHandler(annotation.path(), server);
		}
		return bean;
	}
	
	private void processMethodOnSessionStarted(WebSocketServerHandler server, Object bean, Method m) {
		if (m.getParameterCount() != 1 || !WebSocketServerSession.class.equals(m.getParameterTypes()[0]))
			throw new FatalBeanException("Method @OnSessionStarted must have a single parameter of type WebSocketServerSession");
		server.onSessionStarted(createSessionListener(bean, m));
	}
	
	private void processMethodOnSessionClosed(WebSocketServerHandler server, Object bean, Method m) {
		if (m.getParameterCount() != 1 || !WebSocketServerSession.class.equals(m.getParameterTypes()[0]))
			throw new FatalBeanException("Method OnSessionClosed must have a single parameter of type WebSocketServerSession");
		server.onSessionClosed(createSessionListener(bean, m));
	}
	
	@SuppressWarnings({ "java:S112", "unchecked", "rawtypes" })
	private Function<WebSocketServerSession, Mono<Void>> createSessionListener(Object bean, Method m) {
		return session -> {
			try {
				if (Publisher.class.isAssignableFrom(m.getReturnType()))
					return Flux.from((Publisher) m.invoke(bean, session)).then();
				return Mono.fromCallable(() -> m.invoke(bean, session)).then();
			} catch (Exception e) {
				return Mono.error(e);
			}
		};
	}
	
	private Optional<Tuple2<Class<?>, BiFunction<WebSocketServerSession, Object, Mono<Void>>>> processMethod(Object bean, Method m) {
		Parameter[] params = m.getParameters();
		@SuppressWarnings("unchecked")
		BiFunction<WebSocketServerSession, Object, Object>[] paramsProviders = new BiFunction[params.length];
		Class<?> bodyFound = null;
		FatalBeanException error = null;
		for (int i = 0; i < params.length; ++i) {
			Parameter p = params[i];
			Class<?> type = p.getType();
			RequestBody body = p.getAnnotation(RequestBody.class);
			if (body != null) {
				if (bodyFound != null) {
					throw new FatalBeanException("Invalid web socket method " + m.getName() + ": only one @RequestBody allowed");
				}
				bodyFound = type;
				paramsProviders[i] = (session, msg) -> msg;
			} else if (type.equals(WebSocketServerSession.class)) {
				paramsProviders[i] = (session, msg) -> session;
			} else if (error == null) {
				error = new FatalBeanException("Unsupported web socket parameter type " + type.getName() + " on method " + m.getName());
			}
		}
		if (bodyFound == null)
			return Optional.empty();
		if (error != null)
			throw error;
		Class<?> resultType = m.getReturnType();
		BiFunction<WebSocketServerSession, Object, Mono<Void>> fct;
		if (Publisher.class.isAssignableFrom(resultType))
			fct = (session, msg) -> Mono.from((Publisher<?>) callMethod(bean, m, session, msg, paramsProviders)).then();
		else
			fct = (session, msg) -> Mono.fromRunnable(() -> callMethod(bean, m, session, msg, paramsProviders));
		return Optional.of(Tuples.of(bodyFound, fct));
	}
	
	@SuppressWarnings("java:S112")
	private Object callMethod(Object bean, Method m, WebSocketServerSession session, Object msg, BiFunction<WebSocketServerSession, Object, Object>[] paramsProviders) {
		Object[] parameters = new Object[paramsProviders.length];
		for (int i = 0; i < paramsProviders.length; ++i)
			parameters[i] = paramsProviders[i].apply(session, msg);
		try {
			return m.invoke(bean, parameters);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
