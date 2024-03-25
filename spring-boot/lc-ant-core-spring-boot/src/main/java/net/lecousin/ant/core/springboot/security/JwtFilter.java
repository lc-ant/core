package net.lecousin.ant.core.springboot.security;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.api.traceability.Traceability;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class JwtFilter implements WebFilter {

	private static final String BEARER = "Bearer ";

	private final ReactiveAuthenticationManager authManager;
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return Mono.deferContextual(ctx -> {
			var trace = Traceability.fromContext(ctx)
				.or(() -> Traceability.fromHeaders(exchange.getRequest().getHeaders()))
				.orElseGet(Traceability::create);
			return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
			.filter(authHeader -> authHeader.startsWith(BEARER))
			.map(authHeader -> authHeader.substring(BEARER.length()))
			.switchIfEmpty(Mono.defer(() -> {
				if ("websocket".equals(exchange.getRequest().getHeaders().getUpgrade())) {
					return Mono.justOrEmpty(exchange.getRequest().getQueryParams().getFirst("authorization"));
				}
				return Mono.empty();
			}))
			.flatMap(token -> authManager.authenticate(new UsernamePasswordAuthenticationToken(null, token)))
			.map(Optional::of)
			.switchIfEmpty(Mono.just(Optional.empty()))
			.flatMap(auth -> {
				if (auth.isEmpty()) return chain.filter(exchange);
				SecurityContextImpl securityContext = new SecurityContextImpl(auth.get());
				return chain.filter(exchange)
					.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
			})
			.contextWrite(trace::toContext);
		});
	}
	
}
