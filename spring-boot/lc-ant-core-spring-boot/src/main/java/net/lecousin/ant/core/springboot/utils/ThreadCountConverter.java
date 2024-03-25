package net.lecousin.ant.core.springboot.utils;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class ThreadCountConverter implements Converter<String, ThreadCount> {

	@Override
	public ThreadCount convert(String source) {
		return ThreadCount.parse(source);
	}
	
}
