package net.lecousin.ant.core.springboot.aop;

import static org.assertj.core.api.Assertions.assertThat;

import net.lecousin.ant.core.validation.ValidationContext;

public class ValidatedServiceImpl implements ValidatedService {

	@Override
	public void test(@Valid(ValidationContext.CREATION) ValidateMe input) {
		assertThat(input.getStr().length()).isGreaterThanOrEqualTo(10);
	}
	
}
