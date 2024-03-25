package net.lecousin.ant.core.springboot.service.provider.info;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.security.Grant;
import net.lecousin.ant.core.security.PermissionDeclaration;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/public-api/service/v1")
@RequiredArgsConstructor
public class ServiceInfoPublicControllerV1 {

	private final ServiceInfo service;
	
	@GetMapping("/services")
	public Mono<List<String>> getLocalServices() {
		return Mono.just(service.getLocalServiceNames());
	}
	
	@GetMapping("/permissions")
	public Mono<List<PermissionDeclaration>> getLocalPermissions() {
		return Mono.just(service.getLocalPermissions());
	}
	
	@PostMapping("/resolveGrants")
	public Mono<List<String>> resolveGrants(@RequestBody List<String> authorities) {
		return Mono.just(service.resolveLocalGrants(authorities).map(Grant::toAuthority).toList());
	}
	
}
