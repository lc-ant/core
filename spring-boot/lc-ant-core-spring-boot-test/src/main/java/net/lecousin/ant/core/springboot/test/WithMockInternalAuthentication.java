package net.lecousin.ant.core.springboot.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockInternalAuthenticationSecurityContextFactory.class)
public @interface WithMockInternalAuthentication {

	String username() default "user";
	String[] authorities() default {};
	
}
