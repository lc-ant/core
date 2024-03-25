package net.lecousin.ant.core.springboot.service.client.websocket;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService.TraceStart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

@Slf4j
public class WebSocketClientAutoReconnection {

	private final WebSocketClientService clientService;
	private final String fromServiceName;
	private final Supplier<Mono<URI>> uriSupplier;
	private final Duration reconnectEvery;
	private final Duration pingInterval;
	private final UnaryOperator<Mono<Void>> authenticationWrapper;
	private final TraceStart traceStart;
	
	private boolean stop = false;
	
	private WebSocketClientConnection current = null;
	private List<Consumer<WebSocketClientConnection>> connectionListeners = new LinkedList<>();
	
	WebSocketClientAutoReconnection(WebSocketClientService clientService, String fromServiceName, Supplier<Mono<URI>> uriSupplier, Duration reconnectEvery, Duration pingInterval, UnaryOperator<Mono<Void>> authenticationWrapper, Traceability trace) {
		this.clientService = clientService;
		this.fromServiceName = fromServiceName;
		this.uriSupplier = uriSupplier;
		this.reconnectEvery = reconnectEvery;
		this.pingInterval = pingInterval;
		this.authenticationWrapper = authenticationWrapper;
		this.traceStart = clientService.getTraceabilityService().start(trace, fromServiceName, TraceType.METHOD_CALL, WebSocketClientAutoReconnection.class.getName());
		reconnect();
	}
	
	private void reconnect() {
		uriSupplier.get()
		.flatMap(uri -> {
			log.info("Trying to connect to WebSocket server {}", uri);
			return authenticationWrapper.apply(
				clientService.createHeaders(fromServiceName, uri)
				.flatMap(tuple -> {
					WebSocketClientConnection c = new WebSocketClientConnection(uri, tuple.getT1(), Tuples.of(this::connected, err -> { }), pingInterval);
					return c.start(tuple.getT2(), clientService.getTraceabilityService(), fromServiceName)
						.doFinally(signal -> connectionEnd())
						.onErrorComplete(); // avoid to call connectionEnd several times
				}));
		})
		.doOnError(err -> this.connectionEnd())
		.subscribe();
	}
	
	private void connected(WebSocketClientConnection connection) {
		log.info("WebSocketClientAutoReconnection is connected with id {}", connection.getId());
		List<Consumer<WebSocketClientConnection>> listeners;
		synchronized (connectionListeners) {
			current = connection;
			listeners = new ArrayList<>(connectionListeners);
		}
		for (Consumer<WebSocketClientConnection> listener : listeners)
			listener.accept(connection);
	}
	
	private void connectionEnd() {
		log.info("WebSocketClientAutoReconnection is disconnected");
		synchronized (connectionListeners) {
			current = null;
		}
		if (stop) {
			clientService.getTraceabilityService().end(traceStart, 0, null);
			return;
		}
		log.info("Reconnect in {}", reconnectEvery);
		Schedulers.parallel().schedule(this::reconnect, reconnectEvery.toMillis(), TimeUnit.MILLISECONDS);
	}
	
	public void addConnectionListener(Consumer<WebSocketClientConnection> listener) {
		WebSocketClientConnection c;
		synchronized (connectionListeners) {
			connectionListeners.add(listener);
			c = current;
		}
		if (c != null)
			listener.accept(c);
		else if (stop)
			listener.accept(null);
	}
	
	public void removeConnectionListener(Consumer<WebSocketClientConnection> listener) {
		synchronized (connectionListeners) {
			connectionListeners.remove(listener);
		}
	}
	
	public Mono<WebSocketClientConnection> waitConnection() {
		return Mono.create(sink -> {
			Consumer<WebSocketClientConnection> listener = new Consumer<>() {
				@Override
				public void accept(WebSocketClientConnection connection) {
					removeConnectionListener(this);
					if (connection != null)
						sink.success(connection);
					else
						sink.success();
				}
			};
			addConnectionListener(listener);
		});
	}
	
	public Optional<WebSocketClientConnection> getConnection() {
		return Optional.ofNullable(current);
	}
	
	public Mono<Void> stop() {
		return Mono.defer(() -> {
			this.stop = true;
			return waitConnection()
				.flatMap(WebSocketClientConnection::close)
				.doFinally(s -> {
					List<Consumer<WebSocketClientConnection>> listeners;
					synchronized (connectionListeners) {
						listeners = new ArrayList<>(connectionListeners);
					}
					for (Consumer<WebSocketClientConnection> listener : listeners)
						listener.accept(null);
				});
		});
	}
}
