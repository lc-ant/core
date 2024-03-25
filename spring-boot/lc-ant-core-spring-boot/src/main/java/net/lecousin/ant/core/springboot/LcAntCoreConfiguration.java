package net.lecousin.ant.core.springboot;

import java.security.SecureRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import net.lecousin.ant.core.springboot.cache.CacheExpirationService;
import net.lecousin.ant.core.springboot.serviceregistry.LocalInstanceInfo;
import net.lecousin.ant.core.springboot.traceability.TraceabilityLog;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;
import net.lecousin.ant.core.springboot.utils.ThreadCountConverter;

@Configuration
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@ComponentScan(basePackages = { "net.lecousin.ant.core.springboot.aop" })
public class LcAntCoreConfiguration {

	@Bean
	CacheExpirationService cacheExpirationService() {
		return new CacheExpirationService();
	}
	
	@Bean
	@ConditionalOnMissingBean(SecureRandom.class)
	SecureRandom secureRandom() {
		return new SecureRandom();
	}
	
	@Bean
	LocalInstanceInfo localInstanceInfo() {
		return new LocalInstanceInfo();
	}
	
	@Bean
	ThreadCountConverter threadCountConverter() {
		return new ThreadCountConverter();
	}
	
	@Bean
	TraceabilityService traceabilityService() {
		return new TraceabilityService();
	}
	
	@Bean
	@ConditionalOnProperty(name = "lc-ant.traceability.log", havingValue = "true", matchIfMissing = true)
	TraceabilityLog traceabilityLog() {
		return new TraceabilityLog();
	}

	
}
