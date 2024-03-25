package net.lecousin.ant.core.springboot.service.provider.health;

import java.util.Arrays;
import java.util.Iterator;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import lombok.Setter;
import net.lecousin.ant.core.springboot.service.client.health.LcAntServiceClientHealth;

@Component
public class LcAntServiceClientsHealthIndicator implements CompositeHealthContributor, ApplicationContextAware {

	@Setter
	private ApplicationContext applicationContext;

	@Override
	public Iterator<NamedContributor<HealthContributor>> iterator() {
		return Arrays.stream(applicationContext.getBeanNamesForType(LcAntServiceClientHealth.class))
			.map(beanName -> (NamedContributor<HealthContributor>) new NamedContributor<HealthContributor>() {
				@Override
				public String getName() {
					return beanName;
				}
				
				@Override
				public HealthContributor getContributor() {
					return LcAntServiceClientsHealthIndicator.this.getContributor(beanName);
				}
			})
			.toList().iterator();
	}
	
	@Override
	public HealthIndicator getContributor(String name) {
		return () -> {
			LcAntServiceClientHealth client = applicationContext.getBean(name, LcAntServiceClientHealth.class);
			try {
				return Health.status(client.ping().block().getStatus()).build();
			} catch (Exception e) {
				return Health.down(e).build();
			}
		};
	}
	
}
