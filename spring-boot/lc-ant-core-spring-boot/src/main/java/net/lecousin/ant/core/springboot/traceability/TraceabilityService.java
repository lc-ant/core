package net.lecousin.ant.core.springboot.traceability;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TraceabilityService {
	
	@Lazy
	@Autowired
	private List<TraceabilityHandler> handlers;
	
	@Value("${spring.application.name")
	private String appName;

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class TraceStart {
		private Traceability trace;
		private String service;
		private TraceType type;
		private String detail;
		private int sequence;
	}
	
	public TraceStart start(Traceability trace, String service, TraceType type, String detail) {
		int sequence = trace.newSequence();
		handlers.forEach(handler -> handler.start(trace, appName, sequence, service, type, detail));
		return new TraceStart(trace, service, type, detail, sequence);
	}
	
	public void end(TraceStart start, int resultCode, String error) {
		handlers.forEach(handler -> handler.end(start.trace, appName, start.sequence, start.service, start.type, start.detail, resultCode, error));
	}
	
	public <T> Mono<T> start(String service, TraceType type, String detail, Mono<T> mono) {
		return Mono.deferContextual(ctx -> {
			var traceOpt = Traceability.fromContext(ctx);
			if (traceOpt.isEmpty()) return mono;
			Traceability trace = traceOpt.get();
			TraceStart traceStart = start(trace, service, type, detail);
			return mono
			.doOnError(error -> end(traceStart, -1, ((Throwable) error).getMessage()))
			.doOnSuccess(r -> end(traceStart, 0, null));
		});
	}
	
	public <T> Flux<T> start(String service, TraceType type, String detail, Flux<T> flux) {
		return Flux.deferContextual(ctx -> {
			var traceOpt = Traceability.fromContext(ctx);
			if (traceOpt.isEmpty()) return flux;
			Traceability trace = traceOpt.get();
			TraceStart traceStart = start(trace, service, type, detail);
			return flux
			.doOnError(error -> end(traceStart, -1, ((Throwable) error).getMessage()))
			.doOnComplete(() -> end(traceStart, 0, null));
		});
	}
	
}
