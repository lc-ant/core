package net.lecousin.ant.core.springboot.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.DisposableBean;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class CacheExpirationService implements DisposableBean {

	private Map<String, Map<Object, Item<?>>> map = new HashMap<>();
	private Disposable schedule;
	private boolean disposed = false;
	
	private static final class Item<T> {
		private Mono<T> value;
		private long expiresAt;
		private Function<T, Mono<Void>> destroy;
	}
	
	private static final int INITIAL_DELAY_MINUTES = 15;
	private static final int CHECK_INTERVAL_MINUTES = 5;
	
	public CacheExpirationService() {
		schedule = Schedulers.parallel().schedulePeriodically(this::checkExpired, INITIAL_DELAY_MINUTES, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
	}
	
	@SuppressWarnings("unchecked")
	public <K, V> Mono<V> get(String type, K key, Supplier<Mono<V>> itemSupplier, Duration expiration, Function<V, Mono<Void>> destroy) {
		return Mono.defer(() -> {
			if (disposed) return Mono.empty();
			Map<Object, Item<?>> typeMap;
			synchronized (map) {
				typeMap = map.computeIfAbsent(type, k -> new HashMap<>());
			}
			Item<V> item;
			if (disposed) return Mono.empty();
			MutableObject<CompletableFuture<Mono<V>>> creation = new MutableObject<>(null);
			synchronized (typeMap) {
				item = (Item<V>) typeMap.computeIfAbsent(key, k -> {
					Item<V> i = new Item<>();
					CompletableFuture<Mono<V>> future = new CompletableFuture<>();
					creation.setValue(future);
					i.value = Mono.fromFuture(future).flatMap(mono -> mono).cache();
					i.destroy = destroy;
					return i;
				});
				item.expiresAt = System.currentTimeMillis() + expiration.toMillis();
			}
			if (creation.getValue() != null) {
				creation.getValue().complete(itemSupplier.get());
			}
			if (disposed) return Mono.empty();
			return item.value;
		});
	}
	
	@Override
	public void destroy() throws Exception {
		disposed = true;
		schedule.dispose();
		Map<String, Map<Object, Item<?>>> mapCopy;
		synchronized (map) {
			mapCopy = new HashMap<>(map);
			map.clear();
		}
		for (var typeMap : mapCopy.values()) {
			Map<Object, Item<?>> typeMapCopy;
			synchronized (typeMap) {
				typeMapCopy = new HashMap<>(typeMap);
				typeMap.clear();
			}
			for (var item : typeMapCopy.values())
				destroyItem(item);
		}
	}
	
	private void checkExpired() {
		Collection<String> types;
		synchronized (map) {
			types = map.keySet();
		}
		for (String type : types) {
			Map<Object, Item<?>> typeMap;
			synchronized (map) {
				typeMap = map.get(type);
			}
			if (typeMap == null) continue;
			Collection<Object> keys;
			synchronized (typeMap) {
				keys = new LinkedList<>(typeMap.keySet());
			}
			for (Object key : keys) {
				Item<?> item;
				long now = System.currentTimeMillis();
				synchronized (typeMap) {
					item = typeMap.get(key);
					if (item == null) continue;
					if (now < item.expiresAt) continue;
					typeMap.remove(key);
				}
				destroyItem(item);
			}
		}
	}
	
	private <T> void destroyItem(Item<T> item) {
		item.value.flatMap(item.destroy::apply).subscribe();
	}
	
}
