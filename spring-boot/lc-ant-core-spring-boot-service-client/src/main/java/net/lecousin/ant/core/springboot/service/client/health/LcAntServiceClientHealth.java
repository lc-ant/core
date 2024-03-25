package net.lecousin.ant.core.springboot.service.client.health;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@HttpExchange("/actuator/health")
public interface LcAntServiceClientHealth {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	class Response {
		
		private String status;
		
	}
	
	@GetExchange
	Mono<Response> ping();
	
}
