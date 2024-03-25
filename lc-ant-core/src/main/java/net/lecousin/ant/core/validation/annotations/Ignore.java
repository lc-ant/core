package net.lecousin.ant.core.validation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.ant.core.validation.ValidationContext;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Repeatable(Ignore.List.class)
public @interface Ignore {

	boolean value() default true;
	
	ValidationContext[] context() default {};
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface List {
		Ignore[] value();
	}
	
}
