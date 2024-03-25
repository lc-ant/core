package net.lecousin.ant.core.springboot.test;

import java.util.List;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

public class LcAntServiceTestContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
		LcAntServiceTest annotation = testClass.getAnnotation(LcAntServiceTest.class);
		if (annotation == null) return null;
		return new LcAntServiceTestContextCustomizer(annotation.service());
	}
	
}
