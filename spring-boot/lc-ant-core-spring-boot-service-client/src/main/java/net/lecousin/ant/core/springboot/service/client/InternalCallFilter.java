package net.lecousin.ant.core.springboot.service.client;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.api.ApiError;
import net.lecousin.ant.core.api.exceptions.ApiException;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService.TraceStart;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class InternalCallFilter implements ExchangeFilterFunction {

	private final TraceabilityService traceabilityService;
	
	@Value("${spring.application.name}")
	private String appName;
	
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (request.url().getPath().startsWith("/actuator")) return next.exchange(request);
		return Mono.deferContextual(ctx -> {
			var trace = Traceability.fromContext(ctx).orElseGet(Traceability::create);
			var traceStart = traceabilityService.start(trace, null,  TraceType.REST_CLIENT, request.url().toString());
			Locale locale = ctx.getOrDefault(Locale.class, Locale.ENGLISH);
			ClientRequest.Builder newRequest = ClientRequest.from(request)
				.headers(trace::toHeaders)
				.header(HttpHeaders.ACCEPT_LANGUAGE, locale.toString());
			return ctx.<Mono<SecurityContext>>getOrEmpty(SecurityContext.class).orElse(Mono.<SecurityContext>empty())
				.map(sec -> Optional.ofNullable(sec.getAuthentication()))
				.switchIfEmpty(Mono.just(Optional.empty()))
				.flatMap(authOpt -> {
					if (authOpt.isPresent()) {
						var auth = authOpt.get();
						Object cred = auth.getCredentials();
						if (cred instanceof String token)
							newRequest.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
					}
					return next.exchange(newRequest.build())
						.doOnError(error -> responseReceived(traceStart, request, null, error))
						.flatMap(response -> {
							responseReceived(traceStart, request, response, null);
							if (response.statusCode().isError())
								return response.createException().flatMap(error -> Mono.error(error));
							return Mono.just(response);
						})
						.onErrorMap(error -> {
							if (error instanceof ApiException) return error;
							if (error instanceof WebClientResponseException wce) {
								try {
									ApiError apiError = wce.getResponseBodyAs(ApiError.class);
									if (apiError.getHttpCode() != 0)
										return ApiException.create(apiError);
								} catch (Exception e) {
									// ignore
								}
								return ApiException.create(wce.getStatusCode().value(), wce.getStatusText(), trace.getCorrelationId());
							}
							return ApiException.create(error, trace.getCorrelationId());
						});
				})
				.contextWrite(trace.toContext());
		});
	}
	
	private void responseReceived(TraceStart trace, ClientRequest request, ClientResponse response, Throwable error) {
		traceabilityService.end(trace, response != null ? response.statusCode().value() : 0, error != null ? error.toString() : null);
	}
	
}
