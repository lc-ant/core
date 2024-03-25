package net.lecousin.ant.core.springboot.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TestWithBeans.List.class)
@Inherited
public @interface TestWithBeans {

	Class<?> value();
	
	String[] qualifiers();
	
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface List {
		TestWithBeans[] value();
	}
	
}
