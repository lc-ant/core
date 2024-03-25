package net.lecousin.ant.core.springboot.service.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import net.lecousin.ant.core.springboot.LcAntCoreConfiguration;
import net.lecousin.ant.core.springboot.LcAntCoreHttpConfiguration;
import net.lecousin.ant.core.springboot.traceability.TraceabilityService;

@Configuration
@Import({LcAntCoreConfiguration.class, LcAntCoreHttpConfiguration.class, LcAntServiceClientFactory.class})
@ComponentScan(basePackages = "net.lecousin.ant.service.client")
public class LcAntServiceClientConfiguration {
	
	@Bean
	InternalCallFilter internalCallFilter(TraceabilityService traceabilityService) {
		return new InternalCallFilter(traceabilityService);
	}

	@Bean
	LcAntServiceClientBuilder clientBuilder() {
		return new LcAntServiceClientBuilder();
	}
	
}
