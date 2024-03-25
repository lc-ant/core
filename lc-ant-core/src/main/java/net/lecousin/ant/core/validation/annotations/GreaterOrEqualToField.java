package net.lecousin.ant.core.validation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.ant.core.validation.ValidationContext;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Repeatable(GreaterOrEqualToField.List.class)
public @interface GreaterOrEqualToField {

	String value();
	
	ValidationContext[] context() default {};
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface List {
		GreaterOrEqualToField[] value();
	}
	
}
