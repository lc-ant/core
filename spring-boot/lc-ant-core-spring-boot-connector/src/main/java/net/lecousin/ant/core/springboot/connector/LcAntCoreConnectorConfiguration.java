package net.lecousin.ant.core.springboot.connector;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import net.lecousin.ant.core.springboot.LcAntCoreConfiguration;

@Configuration
@Import(LcAntCoreConfiguration.class)
public class LcAntCoreConnectorConfiguration {

	@Bean
	ConnectorService connectorService() {
		return new ConnectorService();
	}
	
}
