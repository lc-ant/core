package net.lecousin.ant.core.springboot.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import net.lecousin.ant.core.validation.Validator;
import net.lecousin.ant.core.validation.exceptions.MissingRequiredPropertyException;

@Aspect
@Component
@Order(0)
public class ValidAspect {
	
	@Pointcut("execution(* *(.., @Valid (*), ..))")
	public void methodsWithValidParameter() {
	}
	
	@Before(value = "methodsWithValidParameter()")
	public void beforeMethodsWithValidParameter(JoinPoint jp) throws Throwable {
		AopUtils.getAnnotatedMethodParameters(jp, Valid.class).forEach(triple -> {
			if (triple.getLeft() == null) throw new MissingRequiredPropertyException(triple.getMiddle().getName());
			// TODO use cache
			new Validator(triple.getLeft().getClass()).validate(triple.getLeft(), triple.getRight().value());
		});
	}
	
}
