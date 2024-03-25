package net.lecousin.ant.core.springboot.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Order(10000)
@RequiredArgsConstructor
@ConditionalOnBean(TraceabilityService.class)
public class TraceAspect {

	private final TraceabilityService service;
	
	@Pointcut("@annotation(net.lecousin.ant.core.springboot.aop.Trace)")
	public void methodsWithTraceAnnotation() {
	}
	
	@SuppressWarnings("unchecked")
	@Around(value = "methodsWithTraceAnnotation()")
	public Object handleMethodToTrace(ProceedingJoinPoint jp) throws Throwable {
		Trace annotation = AopUtils.getMethodAnnotation(jp, Trace.class);
		Object result = jp.proceed();
		if (annotation == null) return result;
		String serviceName = annotation.service();
		if (serviceName.length() == 0) {
			try {
				Method m = jp.getTarget().getClass().getMethod(annotation.serviceGetterMethod());
				serviceName = (String) m.invoke(jp.getTarget());
			} catch (Throwable t) {
				// ignore
			}
		}
		if (result instanceof Mono mono)
			return service.start(
				annotation.service(),
				TraceType.METHOD_CALL,
				jp.getSignature().getDeclaringTypeName() + '#' + jp.getSignature().getName(),
				mono);
		if (result instanceof Flux flux)
			return service.start(
				annotation.service(),
				TraceType.METHOD_CALL,
				jp.getSignature().getDeclaringTypeName() + '#' + jp.getSignature().getName(),
				flux);
		return result;
	}
	
}
