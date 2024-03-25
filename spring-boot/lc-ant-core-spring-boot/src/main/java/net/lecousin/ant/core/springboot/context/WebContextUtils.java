package net.lecousin.ant.core.springboot.context;

import java.util.Locale;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WebContextUtils {

	public static <T> Mono<T> monoWithLocale(Function<Locale, Mono<T>> mono) {
		return Mono.deferContextual(ctx -> mono.apply(getLocale(ctx)));
	}
	
	public static Locale getLocale(ContextView ctx) {
		return ctx.<Locale>getOrEmpty(Locale.class).orElse(Locale.ENGLISH);
	}
	
}
