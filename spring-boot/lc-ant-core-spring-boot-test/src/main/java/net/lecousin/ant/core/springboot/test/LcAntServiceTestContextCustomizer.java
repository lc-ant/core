package net.lecousin.ant.core.springboot.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
// CHECKSTYLE DISABLE: MagicNumber
public class LcAntServiceTestContextCustomizer implements ContextCustomizer {

	private final String serviceName;
	
	protected static RSAPrivateKey privateKey;
	
	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	    keyGen.initialize(2048);
	    KeyPair keys = keyGen.generateKeyPair();
	    privateKey = (RSAPrivateKey) keys.getPrivate();
	    
		TestPropertyValues properties = TestPropertyValues.of(
			"spring.application.name=" + serviceName,
			"lc-ant.services." + serviceName + "=localhost:#{'$'}{local.server.port}",
			"lc-ant.service.security.private-key=" + Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded()),
			"lc-ant.security.public-key=" + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded())
		);
		properties.applyTo(context);
	}
	
}
