package net.lecousin.ant.core.springboot.connector;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.lecousin.ant.core.manifest.ConnectorManifest;
import net.lecousin.ant.core.springboot.cache.CacheExpirationService;
import net.lecousin.ant.core.springboot.utils.SpringContextUtils;
import reactor.core.publisher.Mono;

@Service
public class ConnectorService implements ApplicationContextAware {

	@Autowired
	@Lazy
	private List<ConnectorFactory<?, ?>> factories;
	@Autowired
	private CacheExpirationService cache;
	@Value("${lc-ant.connectors.cache-expiration:#{null}}")
	private Map<String, Duration> expirationByType;
	@Value("${lc-ant.connectors.cache-expiration.default:15m}")
	private Duration defaultExpiration;
	
	@Setter
	private ApplicationContext applicationContext;
	
	private static final String CACHE_KEY = "ConnectorService.connectors";
	
	public <C extends Connector> Mono<C> getConnector(Class<C> type) {
		return getConnector(type, null, null, null);
	}
	
	@SuppressWarnings("unchecked")
	public <C extends Connector> Mono<C> getConnector(Class<C> type, String implementation, String key, Map<String, Object> properties) {
		return Mono.defer(() -> {
			ConnectorFactory<C, ?> factory;
			String implName = Optional.ofNullable(implementation).orElseGet(() -> {
				for (var connector : ConnectorManifest.load()) {
					if (type.getName().equals(connector.getConnectorClass())) {
						String name = connector.getName();
						String defaultImpl = applicationContext.getEnvironment().getProperty("lc-ant.connector." + name + ".default");
						if (defaultImpl != null && defaultImpl.equals(connector.getImpl())) {
							return defaultImpl;
						}
					}
				}
				return null;
			});
			if (implName == null)
				return Mono.error(new IllegalStateException("No implementation given, and no property found for default connector: " + type.getSimpleName()));

			var optFactory = factories.stream().filter(f -> type.isAssignableFrom(f.getConnectorClass()) && f.getImplementation().equals(implName)).findAny();
			if (optFactory.isEmpty())
				return Mono.error(new IllegalArgumentException("No connector " + type.getName() + " found for implementation " + implName));
			factory = (ConnectorFactory<C, ?>) optFactory.get();

			CacheKey ck = new CacheKey(factory.getType(), factory.getImplementation(), key);
			Duration expiration = expirationByType != null ? expirationByType.get(factory.getType()) : null;
			if (expiration == null)
				expiration = defaultExpiration;
			return cache.get(CACHE_KEY, ck, () -> createConnector(factory, key, properties), expiration, this::destroyConnector);
		});
	}
	
	private <C extends Connector, P> Mono<C> createConnector(ConnectorFactory<C, P> factory, String key, Map<String, Object> properties) {
		P props = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(properties, factory.getPropertiesClass());
		return factory.create(props)
			.map(connector -> {
				SpringContextUtils.initBean(applicationContext, connector, "lc-ant-connector-" + factory.getType() + "-" + factory.getImplementation() + "-" + key);
				return connector;
			});
	}
	
	private <C extends Connector> Mono<Void> destroyConnector(C connector) {
		return Mono.defer(() -> {
			applicationContext.getAutowireCapableBeanFactory().destroyBean(connector);
			return connector.destroy();
		});
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static final class CacheKey {
		private String type;
		private String implementation;
		private String key;
	}
	
}
