package net.lecousin.ant.core.springboot.http;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService.TraceStart;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@RequiredArgsConstructor
public class HttpFilter implements WebFilter {
	
	private static final String EXCHANGE_ATTRIBUTE_START_CONTEXT = "lc-ant-start-context";
	private static final String EXCHANGE_ATTRIBUTE_TRACE_START = "lc-ant-trace-start";

	private final TraceabilityService traceabilityService;
	
	@Value("${spring.application.name}")
	private String appName;
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (exchange.getRequest().getPath().pathWithinApplication().value().startsWith("/actuator")) return chain.filter(exchange);
		exchange.getResponse().beforeCommit(() -> Mono.deferContextual(ctx -> {
			var traceOpt = Traceability.fromContext(ctx)
				.or(() -> Optional.ofNullable((ContextView) exchange.getAttributes().get(EXCHANGE_ATTRIBUTE_START_CONTEXT)).flatMap(Traceability::fromContext));
			if (traceOpt.isEmpty()) return Mono.<Void>empty();
			return Mono.<Void>fromRunnable(() -> {
				TraceStart traceStart = exchange.getAttribute(EXCHANGE_ATTRIBUTE_TRACE_START);
				if (traceStart != null)
					traceabilityService.end(traceStart, exchange.getResponse().getStatusCode().value(), null);
			}).contextWrite(traceOpt.get().toContext());
		}));
		return Mono.deferContextual(ctx -> {
			var trace = Traceability.fromContext(ctx)
					.or(() -> Traceability.fromHeaders(exchange.getRequest().getHeaders()))
					.orElseGet(Traceability::create);
			return Mono.fromRunnable(() -> {
				var traceStart = traceabilityService.start(trace, null, TraceType.REST_SERVER, exchange.getRequest().getMethod().name() + ' ' + exchange.getRequest().getPath());
				exchange.getAttributes().put(EXCHANGE_ATTRIBUTE_TRACE_START, traceStart);
			}).then(chain.filter(exchange))
			.contextWrite(context -> {
				context = trace.toContext(context);
				context = context.put(Locale.class, exchange.getLocaleContext().getLocale());
				exchange.getAttributes().put(EXCHANGE_ATTRIBUTE_START_CONTEXT, context);
				return context;
			});
		});
	}
	
}
