package net.lecousin.ant.core.springboot.service.provider.info;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.security.Grant;
import net.lecousin.ant.core.security.GrantedPermission;
import net.lecousin.ant.core.security.LcAntSecurity;
import net.lecousin.ant.core.security.NodePermission;
import net.lecousin.ant.core.security.NodePermissionDeclaration;
import net.lecousin.ant.core.security.PermissionDeclaration;
import net.lecousin.ant.core.security.Root;
import net.lecousin.ant.core.springboot.service.client.InternalCallFilter;
import net.lecousin.ant.core.springboot.service.provider.LcAntServiceProvider;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ServiceInfo {

	@Autowired(required = false)
	@Lazy
	private List<LcAntServiceProvider> localServices;
	
	@Autowired
	@Lazy
	private ReactiveDiscoveryClient discovery;
	
	@Autowired
	@Lazy
	private InternalCallFilter internalFilter;
	
	@Value("${spring.application.name}")
	private String localAppName;
	
	public List<String> getLocalServiceNames() {
		if (localServices == null) return List.of();
		return localServices.stream().map(LcAntServiceProvider::getServiceName).toList();
	}
	
	public List<PermissionDeclaration> getLocalPermissions() {
		if (localServices == null) return List.of();
		List<PermissionDeclaration> permissions = new LinkedList<>();
		localServices.forEach(service -> permissions.addAll(service.getServicePermissions()));
		return permissions;
	}

	public List<NodePermissionDeclaration> getLocalNodePermissions() {
		if (localServices == null) return List.of();
		List<NodePermissionDeclaration> permissions = new LinkedList<>();
		localServices.forEach(service -> permissions.addAll(service.getServiceNodePermissions()));
		return permissions;
	}
	
	public Stream<Grant> resolveLocalGrants(List<String> authorities) {
		List<PermissionDeclaration> localPermissions = getLocalPermissions();
		List<NodePermissionDeclaration> localNodePermissions = getLocalNodePermissions();
		if (authorities.contains(Root.AUTHORITY)) return Stream.concat(
			localPermissions.stream().map(PermissionDeclaration::toGrant),
			localNodePermissions.stream().map(n -> n.toGrant(NodePermission.TENANT_ID_ALL, NodePermission.NODE_ID_ROOT, true))
		);
		return LcAntSecurity.toGrants(authorities.stream())
			.flatMap(grant -> {
				if (grant instanceof GrantedPermission p)
					return localPermissions.stream().filter(pd -> pd.isDeclarationFor(p)).findAny()
						.map(pd -> pd.allIncluded().stream().map(PermissionDeclaration::toGrant))
						.orElseGet(Stream::of);
				if (grant instanceof NodePermission n)
					return localNodePermissions.stream().filter(d -> d.isDeclarationFor(n)).findAny()
						.map(d -> d.allIncluded().stream().map(de -> de.toGrant(n.getTenantId(), n.getNodeId(), n.isGranted())))
						.orElseGet(Stream::of);
				return Stream.of();
			});
	}
	
	public Mono<List<Grant>> resolveAllPermissions(List<String> authorities) {
		List<Grant> list = resolveLocalGrants(authorities).toList();
		return discovery.getServices()
		.flatMap(service -> {
			if (service.equals(localAppName)) return Mono.empty();
			log.debug("Retrieve service instances for {}", service);
			return discovery.getInstances(service)
			.collectList()
			.flatMap(instances -> resolveAllPermissionsOnService(authorities, list, instances, 0));
		})
		.then(Mono.just(list));
	}
	
	private Mono<Void> resolveAllPermissionsOnService(List<String> authorities, List<Grant> list, List<ServiceInstance> instances, int instanceIndex) {
		if (instanceIndex >= instances.size()) return Mono.empty();
		var uri = instances.get(instanceIndex).getUri().resolve("/public-api/service/v1/resolveGrants");
		log.debug("Resolve permissions with {}", uri);
		var client = WebClient.builder().filter(internalFilter).build();
		return client.post().uri(uri).bodyValue(authorities).exchangeToFlux(response -> response.bodyToFlux(String.class))
		.collectList()
		.flatMap(result -> {
			synchronized (list) {
				list.addAll(LcAntSecurity.toGrants(result.stream()).toList());
			}
			return Mono.<Void>empty();
		})
		.onErrorResume(error -> {
			log.warn("Cannot resolve permissions on {}", uri, error);
			return resolveAllPermissionsOnService(authorities, list, instances, instanceIndex + 1);
		});
	}
	
}
