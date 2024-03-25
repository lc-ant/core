package net.lecousin.ant.core.springboot.messaging;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lecousin.ant.core.api.ApiData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiDataChangeEventConsumerService implements DisposableBean {
	
	private final AmqpAdmin admin;
	private final LcAntAmpqListenerContainerFactory containerFactory;
	
	private String uid = Long.toHexString(System.currentTimeMillis()) + '-' + Long.toHexString(new SecureRandom().nextLong());
	
	private Map<String, Queue> multicastQueues = new HashMap<>();
	private Map<String, LcAntAmpqListenerContainer> multicastContainers = new HashMap<>();
	private Map<String, Map<String, List<Function<ApiDataChangeEvent, Mono<Void>>>>> multicastListeners = new HashMap<>();
	private Map<String, Map<String, Binding>> multicastBindings = new HashMap<>();
	
	private Map<String, Map<String, Map<String, List<Function<ApiDataChangeEvent, Mono<Void>>>>>> unicastListeners = new HashMap<>();
	private Map<String, Map<String, Map<String, Binding>>> unicastBindings = new HashMap<>();
	private Map<String, Map<String, Map<String, Queue>>> unicastQueues = new HashMap<>();
	private Map<String, Map<String, Map<String, LcAntAmpqListenerContainer>>> unicastContainers = new HashMap<>();

	@Override
	public void destroy() throws Exception {
		log.info("Closing ApiDataChangeEvent consumers: stop multicast containers");
		synchronized (multicastQueues) {
			multicastContainers.values().forEach(LcAntAmpqListenerContainer::stop);
			multicastContainers.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: stop unicast containers");
		synchronized (unicastQueues) {
			unicastContainers.values().forEach(byService -> byService.values().forEach(byName -> byName.values().forEach(LcAntAmpqListenerContainer::stop)));
			unicastContainers.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: remove multicast bindings");
		synchronized (multicastListeners) {
			multicastBindings.values().forEach(byService -> byService.values().forEach(admin::removeBinding));
			multicastBindings.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: remove unicast bindings");
		synchronized (unicastListeners) {
			unicastBindings.values().forEach(byService -> byService.values().forEach(byName -> byName.values().forEach(admin::removeBinding)));
			unicastBindings.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: delete multicast queues");
		synchronized (multicastQueues) {
			multicastQueues.values().forEach(queue -> admin.deleteQueue(queue.getName()));
			multicastQueues.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: delete unicast queues");
		synchronized (unicastQueues) {
			unicastQueues.values().forEach(byService -> byService.values().forEach(byName -> byName.values().forEach(queue -> admin.deleteQueue(queue.getName()))));
			unicastQueues.clear();
		}
		log.info("Closing ApiDataChangeEvent consumers: done.");
	}

	public void listenMulticast(String serviceName, Class<? extends ApiData> dataType, Function<ApiDataChangeEvent, Mono<Void>> consumer) {
		boolean needBinding = false;
		String type = dataType.getName();
		synchronized (multicastListeners) {
			var listenersForService = multicastListeners.computeIfAbsent(serviceName, k -> new HashMap<>());
			var listeners = listenersForService.get(type);
			if (listeners == null) {
				needBinding = true;
				listeners = new LinkedList<>();
				listenersForService.put(type, listeners);
			}
			listeners.add(consumer);
		}
		if (needBinding) {
			Queue q = getMulticastQueue(serviceName);
			String routingKey = ApiDataChangeEvent.getRoutingKeyForDataType(type);
			Binding binding = new Binding(q.getName(), Binding.DestinationType.QUEUE, ApiDataChangeEvent.TOPIC_EXCHANGE, routingKey, null);
			admin.declareBinding(binding);
			synchronized (multicastBindings) {
				multicastBindings.computeIfAbsent(serviceName, k -> new HashMap<>()).put(type, binding);
			}
		}
	}
	
	public void unlistenMulticast(String serviceName, Class<? extends ApiData> dataType, Function<ApiDataChangeEvent, Mono<Void>> consumer) {
		String type = dataType.getName();
		boolean deleteBinding = false;
		synchronized (multicastListeners) {
			var listenersForService = multicastListeners.computeIfAbsent(serviceName, k -> new HashMap<>());
			var listeners = listenersForService.get(type);
			if (listeners == null)
				return;
			listeners.remove(consumer);
			if (listeners.isEmpty()) {
				deleteBinding = true;
				listenersForService.remove(type);
			}
		}
		if (deleteBinding) {
			Binding binding;
			synchronized (multicastBindings) {
				var bindingsForService = multicastBindings.get(serviceName);
				binding = bindingsForService.get(type);
				bindingsForService.remove(type);
			}
			if (binding != null)
				admin.removeBinding(binding);
		}
	}
	
	public Mono<Void> processMulticastMessage(String serviceName, ApiDataChangeEvent event) {
		log.debug("ApiDataChangeEvent received on listener {}.{}: {}", serviceName, uid, event);
		List<Function<ApiDataChangeEvent, Mono<Void>>> listeners;
		synchronized (multicastListeners) {
			var listenersForService = multicastListeners.get(serviceName);
			if (listenersForService == null)
				return Mono.empty();
			listeners = listenersForService.get(event.getDataType());
			if (listeners == null)
				return Mono.empty();
			listeners = new ArrayList<>(listeners);
		}
		return processMessage(event, listeners);
	}
	
	private Mono<Void> processMessage(ApiDataChangeEvent event, List<Function<ApiDataChangeEvent, Mono<Void>>> listeners) {
		List<Mono<Void>> list = new ArrayList<>(listeners.size());
		listeners.forEach(listener -> 
			list.add(listener.apply(event)
			.checkpoint("ApiDataChangeEvent " + event.getEventType() + " " + event.getDataType() + " id " + event.getDataId() + " version " + event.getDataVersion() + " tenant " + event.getTenantId())));
		return Flux.merge(list).then();
	}
	
	private Queue getMulticastQueue(String serviceName) {
		synchronized (multicastQueues) {
			var multicastQueue = multicastQueues.get(serviceName);
			if (multicastQueue == null) {
				multicastQueue = new Queue("lc-ant.api-data-events." + serviceName + ".listener-" + uid, false, true, true);
				admin.declareQueue(multicastQueue);
				var container = containerFactory.createDirectMessageListenerContainer(multicastQueue, msg -> {
					ApiDataChangeEvent event;
					try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(msg.getBody()))) {
						event = (ApiDataChangeEvent) input.readObject();
					} catch (Exception e) {
						log.error("Unable to decode ApiDataChangeEvent", e);
						return;
					}
					processMulticastMessage(serviceName, event).subscribe();
				});
				multicastQueues.put(serviceName, multicastQueue);
				synchronized (multicastContainers) {
					multicastContainers.put(serviceName, container);
				}
			}
			return multicastQueue;
		}
	}
	
	public void listenUnicast(String serviceName, String listenerName, Class<? extends ApiData> dataType, Function<ApiDataChangeEvent, Mono<Void>> consumer) {
		Queue q = new Queue("lc-ant.api-data-events." + serviceName + ".listener." + listenerName, true, false, false);
		admin.declareQueue(q);
		String routingKey = ApiDataChangeEvent.getRoutingKeyForDataType(dataType.getName());
		Binding binding = new Binding(q.getName(), Binding.DestinationType.QUEUE, ApiDataChangeEvent.TOPIC_EXCHANGE, routingKey, null);
		synchronized (unicastListeners) {
			unicastListeners.computeIfAbsent(serviceName, k -> new HashMap<>())
			.computeIfAbsent(listenerName, k -> new HashMap<>())
			.computeIfAbsent(routingKey, k -> new LinkedList<>())
			.add(consumer);
			unicastBindings.computeIfAbsent(serviceName, k -> new HashMap<>())
			.computeIfAbsent(listenerName, k -> new HashMap<>())
			.put(routingKey, binding);
		}
		admin.declareBinding(binding);
		var container = containerFactory.createDirectMessageListenerContainer(q, msg -> {
			ApiDataChangeEvent event;
			try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(msg.getBody()))) {
				event = (ApiDataChangeEvent) input.readObject();
			} catch (Exception e) {
				log.error("Unable to decode ApiDataChangeEvent", e);
				return;
			}
			log.debug("ApiDataChangeEvent received on listener {}.{}: {}", serviceName, listenerName, event);
			List<Function<ApiDataChangeEvent, Mono<Void>>> listeners;
			synchronized (unicastListeners) {
				var serviceListeners = unicastListeners.get(serviceName);
				if (serviceListeners == null) return;
				var listenerNameListeners = serviceListeners.get(listenerName);
				if (unicastListeners == null) return;
				listeners = new ArrayList<>(listenerNameListeners.get(routingKey));
			}
			
			processMessage(event, listeners);
		});
		synchronized (unicastQueues) {
			unicastQueues.computeIfAbsent(serviceName, k -> new HashMap<>())
			.computeIfAbsent(listenerName, k -> new HashMap<>())
			.put(routingKey, q);
			unicastContainers.computeIfAbsent(serviceName, k -> new HashMap<>())
			.computeIfAbsent(listenerName, k -> new HashMap<>())
			.put(routingKey, container);
		}
		log.info("Binding on ApiDataChangeEvent listener {}.{} created", serviceName, listenerName);
	}

}
