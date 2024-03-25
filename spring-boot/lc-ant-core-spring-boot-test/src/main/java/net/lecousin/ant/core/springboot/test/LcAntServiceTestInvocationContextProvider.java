package net.lecousin.ant.core.springboot.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.lecousin.ant.core.springboot.test.DependenciesInitializer.DockerImage;
import net.lecousin.ant.core.utils.Combinations;

@SuppressWarnings("rawtypes")
public class LcAntServiceTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

	private static class BeanToProvide implements LcTestCase {
		private Class<?> clazz;
		private String name;
		
		@Override
		public String displayName() {
			return "with bean " + name;
		}
	}
	
	
	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}
	
	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		List<List<LcTestCase>> list = new LinkedList<>();
		TestWithBeans[] beansTypesAnnotations = context.getRequiredTestClass().getAnnotationsByType(TestWithBeans.class);
		for (int i = 0; i < beansTypesAnnotations.length; ++i) {
			List<LcTestCase> beanType = new LinkedList<>();
			for (String qualifier : beansTypesAnnotations[i].qualifiers()) {
				BeanToProvide bean = new BeanToProvide();
				bean.clazz = beansTypesAnnotations[i].value();
				bean.name = qualifier;
				beanType.add(bean);
			}
			list.add(beanType);
		}
		for (var connector : DependenciesInitializer.CONNECTORS) {
			for (var dependency : connector.getDependencies().values()) {
				List<LcTestCase> connectorDependency = new LinkedList<>();
				for (var tag : dependency.getTest().getTags()) {
					DockerImage docker = new DockerImage();
					docker.descriptor = dependency;
					docker.tag = tag;
					connectorDependency.add(docker);
				}
				list.add(connectorDependency);
			}
		}
		DependenciesInitializer.DOCKER_IMAGES_TO_LAUNCH.clear();
		return Combinations.combine(list).stream().map(this::invocationContext);
	}
	
	private TestTemplateInvocationContext invocationContext(List<LcTestCase> parameters) {
		List<DockerImage> images = new LinkedList<>();
		for (var param : parameters)
			if (param instanceof DockerImage di)
				images.add(di);
		DependenciesInitializer.DOCKER_IMAGES_TO_LAUNCH.add(images);
		return new TestTemplateInvocationContext() {
			@Override
			public String getDisplayName(int invocationIndex) {
				StringBuilder s = new StringBuilder("");
				for (var param : parameters) {
					if (s.length() > 0) s.append(", ");
					s.append(param.displayName());
				}
				return s.toString();
			}
			
			@Override
			public List<Extension> getAdditionalExtensions() {
				Map<Class, String> beansMap = new HashMap<>();
				for (var param : parameters)
					if (param instanceof BeanToProvide b)
						beansMap.put(b.clazz, b.name);
				return List.of(
					new ParameterResolver() {
						@Override
						public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
							return beansMap.containsKey(parameterContext.getParameter().getType());
						}
						
						@SuppressWarnings("unchecked")
						@Override
						public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
							Class beanType = parameterContext.getParameter().getType();
							String beanName = beansMap.get(beanType);
							return SpringExtension.getApplicationContext(extensionContext).getBean(beanName, beanType);
						}
					}
				);
			}
		};
	}
	
}
