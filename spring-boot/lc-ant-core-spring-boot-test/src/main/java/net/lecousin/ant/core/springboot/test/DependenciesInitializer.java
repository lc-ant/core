package net.lecousin.ant.core.springboot.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.PropertyPlaceholderHelper;
import org.testcontainers.containers.GenericContainer;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.manifest.ConnectorManifest;
import net.lecousin.ant.core.manifest.DockerImageDependency;

@Slf4j
public class DependenciesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	static class Container extends GenericContainer<Container> {
		Container(String imageName) {
			super(imageName);
		}
		
		@Override
		public void close() {
			log.info("Shutting down docker container with image " + getDockerImageName());
			super.stop();
		}
	}
	
	private static final List<Container> CONTAINERS = new LinkedList<>();
	
	static final List<ConnectorManifest> CONNECTORS;
	
	static {
		CONNECTORS = ConnectorManifest.load();
	}
	
	static class DockerImage implements LcTestCase {
		protected DockerImageDependency descriptor;
		protected String tag;
		
		@Override
		public String displayName() {
			return "with " + descriptor.getImage() + ":" + tag;
		}
	}
	
	static final LinkedList<List<DockerImage>> DOCKER_IMAGES_TO_LAUNCH = new LinkedList<>();
	
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
			@Override
			public void onApplicationEvent(ContextClosedEvent event) {
				closeContainers();
			}
		});
		MutableObject<TestPropertyValues> values = new MutableObject<>(TestPropertyValues.empty());
		startRabbitMQ(applicationContext, values);
		List<DockerImage> images = DOCKER_IMAGES_TO_LAUNCH.removeFirst();
		for (var image : images) {
			log.info("Starting docker container with image " + image.descriptor.getImage() + ":" + image.tag);
			Container container = new Container(image.descriptor.getImage() + ":" + image.tag);
			applicationContext.getBeanFactory().registerSingleton("testcontainer-" + image.descriptor.getImage() + "-" + image.tag, container);
			if (image.descriptor.getExposedPorts() != null)
				image.descriptor.getExposedPorts().forEach(port -> container.addExposedPort(port));
			var testDescriptor = image.descriptor.getTest();
			if (testDescriptor.getInput() != null) {
				// TODO properties ?
				var inputEnv = testDescriptor.getInput().getEnv();
				if (inputEnv != null)
					for (var env : inputEnv.entrySet())
						container.addEnv(env.getKey(), env.getValue());
			}
			container.start();
			CONTAINERS.add(container);
			if (testDescriptor.getOutput() != null) {
				// TODO env ?
				var outputProperties = testDescriptor.getOutput().getProperties();
				if (outputProperties != null) {
					PropertyPlaceholderHelper ph = new PropertyPlaceholderHelper("{{", "}}");
					Properties props = new Properties();
					props.setProperty("host", container.getHost());
					if (image.descriptor.getExposedPorts() != null)
						image.descriptor.getExposedPorts().forEach(port -> props.setProperty("port." + port, Integer.toString(container.getMappedPort(port))));
					outputProperties.forEach((propName, propValue) -> {
						String value = ph.replacePlaceholders(propValue, props);
						values.setValue(values.getValue().and(propName + "=" + value));
						log.info("Set property: " + propName + "=" + value);
					});
				}
			}
		}
		// TODO eureka ?
		values.setValue(values.getValue().and("eureka.client.register-with-eureka=false", "eureka.client.fetch-registry=false", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"));
		values.setValue(values.getValue().and("management.health.config.enabled=false"));
		values.getValue().applyTo(applicationContext);
	}
	
	private void startRabbitMQ(ConfigurableApplicationContext applicationContext, MutableObject<TestPropertyValues> values) {
		Container container = new Container("rabbitmq:3.11-alpine");
		applicationContext.getBeanFactory().registerSingleton("testcontainer-rabbitmq-3.11-alpine", container);
		container.addExposedPort(5672);
		container.start();
		CONTAINERS.add(container);
		values.setValue(values.getValue().and(
			"spring.rabbitmq.host=localhost",
			"spring.rabbitmq.port=" + container.getMappedPort(5672),
			"lc-ant.messaging.implementation=rabbitmq"
		));
	}
	
	private void closeContainers() {
		CONTAINERS.forEach(Container::close);
		CONTAINERS.clear();
	}
	
}
