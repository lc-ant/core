package net.lecousin.ant.core.springboot.service.provider.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.socket.WebSocketSession;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.exceptions.ApiException;
import net.lecousin.ant.core.api.exceptions.InternalServerException;
import net.lecousin.ant.core.api.exceptions.UnauthorizedException;
import net.lecousin.ant.core.api.traceability.TraceType;
import net.lecousin.ant.core.api.traceability.Traceability;
import net.lecousin.ant.core.security.Grant;
import net.lecousin.ant.core.security.LcAntSecurity;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService.TraceStart;
import net.lecousin.ant.core.springboot.websocket.AbstractWebSocketHandler;
import net.lecousin.ant.core.springboot.websocket.InvalidPayloadException;
import net.lecousin.commons.reactive.MonoUtils;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class WebSocketServerHandler extends AbstractWebSocketHandler {
	
	private static final String KEY_SESSION = "lc-ant-web-socket-server-session";
	
	private final TraceabilityService traceService;
	private final String serviceName;
	
	private List<WebSocketServerSession> sessions = new LinkedList<>();
	private Map<Class<?>, BiFunction<WebSocketServerSession, Object, Mono<Void>>> messageHandlers = new HashMap<>();
	private List<Function<WebSocketServerSession, Mono<Void>>> sessionStartListeners = new LinkedList<>();
	private List<Function<WebSocketServerSession, Mono<Void>>> sessionCloseListeners = new LinkedList<>();

	@Override
	protected Mono<Void> acceptSession(WebSocketSession session) {
		log.info("New web socket session id {}", session.getId());
		return Mono.deferContextual(context -> {
			var traceability = Traceability.fromContext(context);
			if (traceability.isEmpty())
				return Mono.error(new InternalServerException("Missing Traceability"));
			Optional<Mono<SecurityContext>> securityContext = context.getOrEmpty(SecurityContext.class);
			if (securityContext.isEmpty())
				return Mono.error(new UnauthorizedException());
			return securityContext.get().flatMap(sec -> {
				WebSocketServerSession internalSession = new WebSocketServerSession(session, traceability.get(), sec.getAuthentication());
				session.getAttributes().put(KEY_SESSION, internalSession);
				synchronized (sessions) {
					sessions.add(internalSession);
				}
				return Mono.empty();
			});
		});
	}
	
	@Override
	protected Mono<Void> unregisterSession(WebSocketSession session) {
		log.info("Web socket session closing: id {}", session.getId());
		WebSocketServerSession internalSession = getSession(session);
		synchronized (sessions) {
			sessions.remove(internalSession);
		}
		traceService.end(internalSession.startSessionTrace, 0, null);
		List<Function<WebSocketServerSession, Mono<Void>>> listeners;
		synchronized (sessionCloseListeners) {
			listeners = new ArrayList<>(sessionCloseListeners);
		}
		return callListeners(internalSession, listeners);
	}
	
	@Override
	protected Mono<Void> sessionStarted(WebSocketSession session) {
		WebSocketServerSession internalSession = getSession(session);
		internalSession.startSessionTrace = traceService.start(internalSession.traceability, serviceName, TraceType.WEBSOCKET_SERVER_SESSION, session.getId());
		List<Function<WebSocketServerSession, Mono<Void>>> listeners;
		synchronized (sessionStartListeners) {
			listeners = new ArrayList<>(sessionStartListeners);
		}
		return callListeners(internalSession, listeners);
	}
	
	private WebSocketServerSession getSession(WebSocketSession session) {
		return (WebSocketServerSession) session.getAttributes().get(KEY_SESSION);
	}
	
	private Mono<Void> callListeners(WebSocketServerSession internalSession, List<Function<WebSocketServerSession, Mono<Void>>> listeners) {
		List<Mono<Void>> monos = new ArrayList<>(listeners.size());
		listeners.forEach(listener -> monos.add(listener.apply(internalSession)));
		return MonoUtils.zipVoidParallel(monos);
	}
	
	public void on(Class<?> messageClass, BiFunction<WebSocketServerSession, Object, Mono<Void>> handler) {
		messageHandlers.put(messageClass, handler);
	}
	
	public void onSessionStarted(Function<WebSocketServerSession, Mono<Void>> listener) {
		sessionStartListeners.add(listener);
	}
	
	public void onSessionClosed(Function<WebSocketServerSession, Mono<Void>> listener) {
		sessionCloseListeners.add(listener);
	}
	
	@Override
	protected Mono<Void> consumeMessage(WebSocketSession session, Class<?> messageClass, Object messageObject) {
		WebSocketServerSession internalSession = getSession(session);
		BiFunction<WebSocketServerSession, Object, Mono<Void>> handler = messageHandlers.get(messageClass);
		if (handler == null)
			return error(session, new InvalidPayloadException("Unknown message: " + messageClass.getName()));
		return traceService.start(serviceName, TraceType.WEBSOCKET_SERVER_MESSAGE, messageClass.getName(),
			handler.apply(internalSession, messageObject)
			.onErrorResume(error -> {
				ApiException e;
				if (error instanceof ApiException ye) {
					e = ye;
				} else {
					e = new InternalServerException(error.getMessage());
				}
				return error(session, e);
			}));
	}
	
	public class WebSocketServerSession {
		
		private final WebSocketSession session;
		@Getter
		private final Traceability traceability;
		@Getter
		private final Authentication authentication;
		@Getter
		private final List<Grant> grants;	
		private TraceStart startSessionTrace;
		
		private WebSocketServerSession(WebSocketSession session, Traceability traceability, Authentication authentication) {
			this.session = session;
			this.traceability = traceability;
			this.authentication = authentication;
			grants = LcAntSecurity.toGrants(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)).toList();
		}
		
		public Mono<Void> send(Object message) {
			return WebSocketServerHandler.this.send(session, message);
		}
		
		public Map<String, Object> getAttributes() {
			return session.getAttributes();
		}
		
		public String getId() {
			return session.getId();
		}
		
	}
}
