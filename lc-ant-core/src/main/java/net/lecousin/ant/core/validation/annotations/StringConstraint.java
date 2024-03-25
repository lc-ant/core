package net.lecousin.ant.core.validation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.ant.core.validation.ValidationContext;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Repeatable(StringConstraint.List.class)
public @interface StringConstraint {
	
	public static final String I18N_KEY_MIN_LENGTH = "must have at least {} characters";
	public static final String I18N_KEY_MAX_LENGTH = "must have maximum {} characters";
	
	int minLength() default 0;
	int maxLength() default -1;
	
	ValidationContext[] context() default {};
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface List {
		
		StringConstraint[] value();
		
	}
}
