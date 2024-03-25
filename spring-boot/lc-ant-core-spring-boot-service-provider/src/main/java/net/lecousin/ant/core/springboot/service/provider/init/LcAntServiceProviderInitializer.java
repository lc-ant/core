package net.lecousin.ant.core.springboot.service.provider.init;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.springboot.events.LcAntApplicationReadyEvent;
import net.lecousin.ant.core.springboot.service.provider.LcAntServiceProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class LcAntServiceProviderInitializer implements SmartLifecycle, HealthIndicator, ApplicationContextAware, ApplicationListener<ApplicationReadyEvent> {

	private final List<LcAntServiceProvider> serviceProviders;
	
	@Autowired
	@Lazy
	private HealthEndpoint healthEndpoint;
	
	@Setter
	private ApplicationContext applicationContext;
	@Getter
	private boolean running = false;
	
	@Override
	public Health health() {
		return (running ? Health.up() : Health.down()).build();
	}
	
	private Map<String, String> getComponentsDown() {
		try {
			HealthComponent health = healthEndpoint.health();
			Map<String, String> down = new HashMap<>();
			getDown("", health, down);
			down.remove("lcAntServiceProviderInitializer");
			return down;
		} catch (Exception e) {
			log.error("Error retrieving health", e);
			return Map.of("healthEndpoint", e.getMessage());
		}
	}
	
	private static final List<String> HEALTH_COMPONENTS_TO_EXCLUDE = List.of(
		"lcAntServiceProviderInitializer"
	);
	
	private void getDown(String path, HealthComponent health, Map<String, String> down) {
		if (HEALTH_COMPONENTS_TO_EXCLUDE.contains(path)) return;
		if (health instanceof Health h) {
			Status status = h.getStatus();
			if (!Status.UP.getCode().equals(status.getCode())) {
				down.put(path, Objects.toString(h.getDetails()));
			}
		} else if (health instanceof CompositeHealth composite) {
			if (path.length() > 0)
				path = path + '.';
			for (Map.Entry<String, HealthComponent> entry : composite.getComponents().entrySet()) {
				getDown(path + entry.getKey(), entry.getValue(), down);
			}
		}
	}
	
	@Override
	public void start() {
		running = true;
	}
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.info("Application ready, waiting for all health components to be UP");
		Map<String, String> down = getComponentsDown();
		boolean change = true;
		long startTime = System.currentTimeMillis();
		while (!down.isEmpty()) {
			if (System.currentTimeMillis() - startTime > 60000) throw new RuntimeException("Timeout waiting for health indicators to be up: " + down.toString());
			if (change)
				log.info("Waiting for health UP: " + down.toString());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			Map<String, String> newDown = getComponentsDown();
			change = !newDown.equals(down);
			down = newDown;
		}
		log.info("All components UP, initialize service providers");
		List<LcAntServiceProvider> providers = new LinkedList<>(serviceProviders);
		Set<Object> ready = new HashSet<>();
		startTime = System.currentTimeMillis();
		while (!providers.isEmpty()) {
			if (System.currentTimeMillis() - startTime > 60000) throw new RuntimeException("Timeout waiting for service providers to be initialized: " + providers.stream().map(LcAntServiceProvider::getServiceName).toList().toString());
			change = false;
			for (var it = providers.iterator(); it.hasNext(); ) {
				var provider = it.next();
				boolean canInit = provider.getDependencies().stream().allMatch(dependency -> isReady(dependency, ready));
				if (canInit) {
					log.info("Initializing service " + provider.getServiceName());
					provider.init((ConfigurableApplicationContext) applicationContext)
					.checkpoint("Initialize service " + provider.getServiceName())
					.block();
					ready.add(provider);
					it.remove();
					change = true;
					log.info("Service ready: " + provider.getServiceName());
				}
			}
			if (!change) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		log.info("All service providers initialized.");
		applicationContext.publishEvent(new LcAntApplicationReadyEvent(applicationContext));
	}
	
	private boolean isReady(Object dependency, Set<Object> ready) {
		if (ready.contains(dependency)) return true;
		if (dependency instanceof LcAntServiceProvider) return false;
		return true;
	}
	
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		running = false;
	}
	
}
