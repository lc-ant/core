package net.lecousin.ant.core.springboot.service.client.websocket;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.springboot.context.WebContextUtils;
import net.lecousin.ant.core.springboot.discovery.DiscoveryURIService;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@RequiredArgsConstructor
public class WebSocketClientService {
	
	@Getter
	private final TraceabilityService traceabilityService;
	private final DiscoveryURIService discoveryService;
	
	@Value("${spring.application.name}")
	private String appName;
	
	public Mono<WebSocketClientConnection> connect(String fromServiceName, URI uri, Duration pingInterval) {
		return Mono.create(sink ->
			createHeaders(fromServiceName, uri)
			.flatMap(tuple -> {
				WebSocketClientConnection c = new WebSocketClientConnection(uri, tuple.getT1(), Tuples.of(
					sink::success,
					sink::error
				), pingInterval);
				return c.start(tuple.getT2(), traceabilityService, fromServiceName);
			})
			.subscribe()
		);
	}

	public Mono<WebSocketClientConnection> connect(String fromServiceName, String toServiceName, String path, Duration pingInterval) {
		return discoveryService.get(toServiceName)
		.flatMap(serviceUri -> connect(fromServiceName, serviceUri.resolve(path), pingInterval));
	}
	
	public Mono<WebSocketClientAutoReconnection> autoReconnect(String fromServiceName, String toServiceName, String path, Duration reconnectEvery, Duration pingInterval, UnaryOperator<Mono<Void>> authenticationWrapper) {
		Supplier<Mono<URI>> uriSupplier = () -> discoveryService.get(toServiceName).map(uri -> uri.resolve(path));
		return autoReconnect(fromServiceName, uriSupplier, reconnectEvery, pingInterval, authenticationWrapper);
	}
	
	public Mono<WebSocketClientAutoReconnection> autoReconnect(String fromServiceName, URI uri, Duration reconnectEvery, Duration pingInterval, UnaryOperator<Mono<Void>> authenticationWrapper) {
		return autoReconnect(fromServiceName, () -> Mono.just(uri), reconnectEvery, pingInterval, authenticationWrapper);
	}

	public Mono<WebSocketClientAutoReconnection> autoReconnect(String fromServiceName, Supplier<Mono<URI>> uriSupplier, Duration reconnectEvery, Duration pingInterval, UnaryOperator<Mono<Void>> authenticationWrapper) {
		return Mono.deferContextual(ctx -> {
			var traceability = Traceability.fromContext(ctx).orElseGet(Traceability::create);
			return Mono.just(new WebSocketClientAutoReconnection(this, fromServiceName, uriSupplier, reconnectEvery, pingInterval, authenticationWrapper, traceability));
		});
	}
	
	Mono<Tuple2<HttpHeaders, Traceability>> createHeaders(String serviceName, URI uri) {
		return Mono.deferContextual(ctx -> {
			var traceability = Traceability.fromContext(ctx).orElseGet(Traceability::create);
			HttpHeaders headers = new HttpHeaders();
			traceability.toHeaders(headers);
			headers.put(HttpHeaders.ACCEPT_LANGUAGE, List.of(WebContextUtils.getLocale(ctx).toString()));
			return ReactiveSecurityContextHolder.getContext()
			.map(security -> {
				Authentication auth = security.getAuthentication();
				if (auth != null)
					headers.put(HttpHeaders.AUTHORIZATION, List.of("Bearer " + auth.getCredentials()));
				return Tuples.of(headers, traceability);
			});
		});
	}
	
}
