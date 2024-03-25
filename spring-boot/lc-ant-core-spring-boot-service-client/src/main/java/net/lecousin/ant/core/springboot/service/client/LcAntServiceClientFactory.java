package net.lecousin.ant.core.springboot.service.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.springboot.service.client.health.LcAntServiceClientHealthFactoryBean;

@Slf4j
public class LcAntServiceClientFactory implements ImportBeanDefinitionRegistrar, EnvironmentAware {

	@Setter
	private Environment environment;
	
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
		log.info("Looking for @LcAntServiceClient interfaces...");
		long startTime = System.currentTimeMillis();
		PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
		Resource[] classResources;
		try {
			classResources = resourceResolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/net/lecousin/ant/service/client/**/*.class");
		} catch (IOException e) {
			log.error("Cannot analyze classpath", e);
			return;
		}
		SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String annotationName = LcAntServiceClient.class.getName();
		int count = 0;
		for (Resource classResource : classResources) {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classResource);
				AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
				if (annotationMetadata.hasAnnotation(annotationName)) {
					Class<?> clazz = Class.forName(annotationMetadata.getClassName());
					try {
						LcAntServiceClient annotation = clazz.getAnnotation(LcAntServiceClient.class);
						registerClient(registry, clazz, annotation);
						count++;
					} catch (Exception e) {
						log.error("Error registering HttpExchange client {}", clazz.getName(), e);
					}
				}
			} catch (@SuppressWarnings("java:S1181") Throwable t) {
				// ignore
			}
		}
		log.info("{} @LcAntServiceClient registered in {} ms.", count, System.currentTimeMillis() - startTime);
	}
	
	private final Set<String> services = new HashSet<>();
	
	private void registerClient(BeanDefinitionRegistry registry, Class<?> clazz, LcAntServiceClient annotation) {
		String serviceUrl = annotation.serviceUrl();
		String serviceName = annotation.serviceName();
		var builder = BeanDefinitionBuilder.genericBeanDefinition(LcAntServiceClientFactoryBean.class)
		.addPropertyValue("clazz", clazz)
		.addPropertyValue("serviceUrl", serviceUrl)
		.setLazyInit(true);
		registry.registerBeanDefinition(annotation.qualifier(), builder.getBeanDefinition());
		if (!services.contains(serviceName)) {
			services.add(serviceName);
			builder = BeanDefinitionBuilder.genericBeanDefinition(LcAntServiceClientHealthFactoryBean.class)
			.addPropertyValue("serviceUrl", serviceUrl)
			.addPropertyValue("serviceName", serviceName)
			.setLazyInit(false);
			registry.registerBeanDefinition("lcAntServiceClientHealth_" + serviceName, builder.getBeanDefinition());
		}
	}
	
}
