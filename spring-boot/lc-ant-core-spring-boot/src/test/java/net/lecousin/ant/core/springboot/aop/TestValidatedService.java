package net.lecousin.ant.core.springboot.aop;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import net.lecousin.ant.core.springboot.LcAntCoreConfiguration;
import net.lecousin.ant.core.validation.exceptions.ValidationException;

@SpringBootTest(classes = TestValidatedService.Config.class)
class TestValidatedService {

	@Configuration
	@Import(LcAntCoreConfiguration.class)
	public static class Config {
		@Bean
		ValidatedService validatedService() {
			return new ValidatedServiceImpl();
		}
	}
	
	@Autowired ValidatedService service;
	
	@Test
	void test() {
		service.test(new ValidateMe("abcdefghijkl"));
		assertThrows(ValidationException.class, () -> service.test(new ValidateMe("abc")));
	}
	
}
