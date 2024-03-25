package net.lecousin.ant.core.springboot.service.provider;

import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;

import net.lecousin.ant.core.security.NodePermissionDeclaration;
import net.lecousin.ant.core.security.PermissionDeclaration;
import reactor.core.publisher.Mono;

public interface LcAntServiceProvider {

	String getServiceName();
	
	List<PermissionDeclaration> getServicePermissions();
	List<NodePermissionDeclaration> getServiceNodePermissions();
	
	List<Object> getDependencies();
	
	Mono<Void> init(ConfigurableApplicationContext applicationContext);
	
	Mono<Void> stop(ConfigurableApplicationContext applicationContext);
	
}
