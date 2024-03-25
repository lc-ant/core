package net.lecousin.ant.core.springboot.aop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.lecousin.ant.core.validation.annotations.StringConstraint;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateMe {

	@StringConstraint(minLength = 10)
	private String str;
	
}
