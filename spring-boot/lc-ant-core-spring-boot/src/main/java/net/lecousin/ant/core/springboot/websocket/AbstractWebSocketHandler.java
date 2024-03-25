package net.lecousin.ant.core.springboot.websocket;

import java.nio.channels.ClosedChannelException;

import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.exceptions.ApiException;
import net.lecousin.ant.core.mapping.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Slf4j
public abstract class AbstractWebSocketHandler implements WebSocketHandler {

	protected static final String KEY_SESSION_SINK = "session-mono-sink";
	protected static final String KEY_SENDER_SINK = "sender-sink";
	
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		log.info("Handling WebSocketSession id {}", session.getId());
		return Mono.create(sink -> {
			session.getAttributes().put(KEY_SESSION_SINK, sink);
			acceptSession(session)
				.then(
					session.send(Flux.create(senderSink -> {
						session.getAttributes().put(KEY_SENDER_SINK, senderSink);
						sessionStarted(session)
						.doOnSuccess(v -> listenIncomingMessages(session))
						.doOnError(sink::error)
						.contextWrite(senderSink.contextView())
						.subscribe();
					}))
				)
				.doOnError(sink::error)
				.contextWrite(sink.contextView())
				.subscribe();
		});
	}
	
	@SuppressWarnings("java:S1172")
	protected abstract Mono<Void> acceptSession(WebSocketSession session);
	
	protected abstract Mono<Void> unregisterSession(WebSocketSession session);
	
	protected abstract Mono<Void> sessionStarted(WebSocketSession session);
	
	protected void listenIncomingMessages(WebSocketSession session) {
		@SuppressWarnings("unchecked")
		MonoSink<Void> sink = (MonoSink<Void>) session.getAttributes().get(KEY_SESSION_SINK);
		session.receive().subscribe(
			message -> consumeMessage(session, message).contextWrite(sink.contextView()).subscribe(),
			error -> close(session).contextWrite(sink.contextView()).subscribe(),
			() -> close(session).contextWrite(sink.contextView()).subscribe()
		);
	}
	
	protected Mono<Void> consumeMessage(WebSocketSession session, WebSocketMessage message) {
		if (WebSocketMessage.Type.TEXT.equals(message.getType()))
			return consumeTextMessage(session, message);
		return Mono.empty();
	}
	
	protected void sendPing(WebSocketSession session) {
		@SuppressWarnings("unchecked")
		FluxSink<WebSocketMessage> sender = (FluxSink<WebSocketMessage>) session.getAttributes().get(KEY_SENDER_SINK);
		sender.next(session.pingMessage(f -> f.wrap(new byte[0])));
	}
	
	protected Mono<Void> consumeTextMessage(WebSocketSession session, WebSocketMessage message) {
		return Mono.defer(() -> {
			String payload = message.getPayloadAsText();
			int i = payload.indexOf(':');
			if (i <= 0)
				return error(session, new InvalidPayloadException("invalid class name"));
			String messageClassName = payload.substring(0, i);
			Class<?> messageClass;
			try {
				messageClass = Class.forName(messageClassName);
			} catch (ClassNotFoundException e) {
				return error(session, new InvalidPayloadException("unknown message type: " + messageClassName));
			}
			String messageBody = payload.substring(i + 1);
			Object messageObject;
			try {
				messageObject = Mappers.OBJECT_MAPPER.readValue(messageBody, messageClass);
			} catch (JacksonException e) {
				return error(session, new InvalidPayloadException(e.getMessage()));
			}
			log.info("Web socket message received on session id {}: {} = {}", session.getId(), messageClass.getName(), messageBody);
			return consumeMessage(session, messageClass, messageObject);
		});
	}
	
	protected Mono<Void> error(WebSocketSession session, ApiException error) {
		return error.toApiError()
		.flatMap(msg -> {
			log.info("Sending error: {}", msg);
			return send(session, msg);
		});
	}
	
	protected abstract Mono<Void> consumeMessage(WebSocketSession session, Class<?> messageClass, Object messageObject);
	
	protected Mono<Void> send(WebSocketSession session, Object message) {
		@SuppressWarnings("unchecked")
		FluxSink<WebSocketMessage> sender = (FluxSink<WebSocketMessage>) session.getAttributes().get(KEY_SENDER_SINK);
		return Mono.<Void>defer(() -> {
			String body;
			try {
				body = Mappers.OBJECT_MAPPER.writeValueAsString(message);
			} catch (JsonProcessingException e) {
				log.error("Error sending message: " + message, e);
				return close(session).onErrorComplete().then(Mono.error(e));
			}
			if (!session.isOpen())
				return Mono.error(new ClosedChannelException());
			log.info("Sending web socket message on session id {}: {}", session.getId(), message.getClass().getName());
			sender.next(session.textMessage(message.getClass().getName() + ':' + body));
			return Mono.empty();
		}).contextWrite(sender.contextView());
	}
	
	protected Mono<Void> close(WebSocketSession session) {
		@SuppressWarnings("unchecked")
		MonoSink<Void> sink = (MonoSink<Void>) session.getAttributes().remove(KEY_SESSION_SINK);
		Mono<Void> close;
		if (sink != null)
			close = unregisterSession(session).doOnTerminate(sink::success).contextWrite(sink.contextView());
		else
			close = Mono.empty();
		if (session.isOpen())
			close = close.then(session.close());
		return close;
	}

}
