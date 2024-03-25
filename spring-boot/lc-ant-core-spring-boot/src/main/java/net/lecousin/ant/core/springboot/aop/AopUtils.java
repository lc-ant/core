package net.lecousin.ant.core.springboot.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class AopUtils {

	public static <A extends Annotation> List<Triple<Object, Parameter, A>> getAnnotatedMethodParameters(JoinPoint jp, Class<A> annotationClass) {
		List<Triple<Object, Parameter, A>> result = new LinkedList<>();
		MethodSignature signature = (MethodSignature) jp.getSignature();
		Object target = jp.getTarget();
		Method m;
		try {
			m = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Parameter[] params = m.getParameters();
		Object[] args = jp.getArgs();
		for (int i = 0; i < args.length; ++i) {
			A annotation = params[i].getAnnotation(annotationClass);
			if (annotation != null) {
				result.add(Triple.of(args[i], params[i], annotation));
			}
		}
		return result;
	}
	
	public static <A extends Annotation> A getMethodAnnotation(JoinPoint jp, Class<A> annotationClass) {
		MethodSignature signature = (MethodSignature) jp.getSignature();
	    Method method = signature.getMethod();
	    return method.getAnnotation(annotationClass);
	}
	
}
