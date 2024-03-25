package net.lecousin.ant.core.springboot.messaging;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.ApiData;
import net.lecousin.ant.core.security.RequiredPermissions;
import net.lecousin.ant.core.springboot.messaging.ApiDataChangeEvent.EventType;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiDataChangeEventSenderService {

	private final AmqpTemplate amqp;
	
	public void sendChangeEvent(EventType eventType, String tenantId, ApiData dto, RequiredPermissions requiredPermissions) {
		ApiDataChangeEvent event = new ApiDataChangeEvent(
			dto.getClass().getName(),
			eventType,
			tenantId,
			dto.getId(),
			dto.getVersion(),
			dto,
			requiredPermissions
		);
		log.debug("Sending ApiData change event: {}", event);
		amqp.convertAndSend(
			ApiDataChangeEvent.TOPIC_EXCHANGE, 
			ApiDataChangeEvent.getRoutingKeyForDataType(dto.getClass().getName()),
			event
		);
	}
	
	public void sendNewData(String tenantId, ApiData dto, RequiredPermissions requiredPermissions) {
		this.sendChangeEvent(dto.getVersion() == 1 ? EventType.CREATED : EventType.UPDATED, tenantId, dto, requiredPermissions);
	}
	
	public void sendDeletedData(String tenantId, Class<? extends ApiData> type, String id, RequiredPermissions requiredPermissions) {
		ApiDataChangeEvent event = new ApiDataChangeEvent(
			type.getName(),
			EventType.DELETED,
			tenantId,
			id,
			-1,
			null,
			requiredPermissions
		);
		log.debug("Sending ApiData change event: {}", event);
		amqp.convertAndSend(
			ApiDataChangeEvent.TOPIC_EXCHANGE, 
			ApiDataChangeEvent.getRoutingKeyForDataType(type.getName()),
			event
		);
	}
	
}
