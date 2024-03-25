package net.lecousin.ant.core.springboot.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.lecousin.ant.core.api.ApiData;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ApiDataChangeEventMulticastListener {

	String service();
	
	Class<? extends ApiData> type();
	
}
