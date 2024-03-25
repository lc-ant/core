package net.lecousin.ant.core.springboot.service.client.websocket;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.exceptions.ApiException;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import net.lecousin.ant.core.springboot.websocket.AbstractWebSocketHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

@Slf4j
public class WebSocketClientConnection {

	private URI uri;
	private HttpHeaders headers;
	private Tuple2<Consumer<WebSocketClientConnection>, Consumer<Throwable>> onConnected;
	private Duration pingInterval;

	private Handler handler;
	private WebSocketSession session;
	private Map<Class<?>, BiFunction<WebSocketClientConnection, Object, Mono<Void>>> messagesHandlers = new HashMap<>();

	WebSocketClientConnection(
		URI uri,
		HttpHeaders headers,
		Tuple2<Consumer<WebSocketClientConnection>, Consumer<Throwable>> onConnected,
		Duration pingInterval
	) {
		this.uri = uri;
		this.headers = headers;
		this.onConnected = onConnected;
		this.pingInterval = pingInterval;
	}
	
	Mono<Void> start(Traceability trace, TraceabilityService traceService, String fromServiceName) {
		log.info("Connect to web socket server {} with headers {}", uri, headers);
		WebSocketClient wsClient = new ReactorNettyWebSocketClient();
		handler = new Handler(traceService, fromServiceName);
		return traceService.start(fromServiceName, TraceType.WEBSOCKET_CLIENT_SESSION, uri.toString(),
			wsClient.execute(uri, headers, handler)
			.doOnError(error -> {
				log.error("Error connecting to web socket server", error);
				Throwable e = error;
				if (e instanceof WebSocketClientHandshakeException ce) {
					int status = ce.response().status().code();
					e = ApiException.create(status, ce.response().status().reasonPhrase(), trace.getCorrelationId());
				}
				onConnected.getT2().accept(e);
			}))
			.contextWrite(trace.toContext());
	}
	
	public String getId() {
		return session.getId();
	}

	public Mono<Void> send(Object message) {
		return handler.send(message);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void on(Class<T> messageClass, BiFunction<WebSocketClientConnection, T, Mono<Void>> consumer) {
		messagesHandlers.put(messageClass, (BiFunction<WebSocketClientConnection, Object, Mono<Void>>) consumer);
	}
	
	public boolean isConnected() {
		return session.isOpen();
	}
	
	public Mono<Void> close() {
		return session.close();
	}

	public WebSocketSession getSession() {
		return session;
	}

	@RequiredArgsConstructor
	private final class Handler extends AbstractWebSocketHandler {
		
		private final TraceabilityService traceService;
		private final String serviceName;
		
		public Mono<Void> send(Object message) {
			return handler.send(session, message);
		}
		
		@Override
		protected Mono<Void> acceptSession(WebSocketSession s) {
			log.info("Web socket client connected with session id {}", s.getId());
			session = s;
			return Mono.empty();
		}
		
		@Override
		protected Mono<Void> sessionStarted(WebSocketSession s) {
			log.info("Web socket client successfully connected: {}", s);
			onConnected.getT1().accept(WebSocketClientConnection.this);
			schedulePing();
			return Mono.empty();
		}
		
		@Override
		protected Mono<Void> unregisterSession(WebSocketSession s) {
			log.info("Web socket client disconnected: {}", s);
			return Mono.empty();
		}
		
		@Override
		protected Mono<Void> consumeMessage(WebSocketSession s, Class<?> messageClass, Object messageObject) {
			log.info("Web socket client received message on session id {}: {}", s.getId(), messageClass.getName());
			BiFunction<WebSocketClientConnection, Object, Mono<Void>> consumer = messagesHandlers.get(messageClass);
			if (consumer == null) return Mono.empty();
			return traceService.start(serviceName, TraceType.WEBSOCKET_CLIENT_MESSAGE, messageClass.getName(),
				consumer.apply(WebSocketClientConnection.this, messageObject));
		}
		
		private void schedulePing() {
			Schedulers.parallel().schedule(() -> {
				if (session.isOpen()) {
					sendPing(session);
					schedulePing();
				}
			}, pingInterval.toMillis(), TimeUnit.MILLISECONDS);
		}
		
	}

}
