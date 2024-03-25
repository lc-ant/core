package net.lecousin.ant.core.springboot.serviceregistry;

import java.util.Random;

import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;

import lombok.Getter;
import net.lecousin.ant.core.reflection.ReflectionUtils;

public class LocalInstanceInfo implements ApplicationListener<InstanceRegisteredEvent<?>> {

	@Getter
	private String instanceId;
	
	@Override
	public void onApplicationEvent(InstanceRegisteredEvent<?> event) {
		Object config = event.getConfig();
		if (config != null) {
			var propOpt = ReflectionUtils.getClassProperty(config.getClass(), "instanceId");
			if (propOpt.isPresent()) {
				var prop = propOpt.get();
				if (prop.canGet()) {
					try {
						Object id = prop.getValue(config);
						if (id != null) {
							instanceId = id.toString();
							return;
						}
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}
		instanceId = System.currentTimeMillis() + "-" + new Random().nextLong();
	}
	
}
