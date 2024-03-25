package net.lecousin.ant.core.api;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.lecousin.ant.core.expression.impl.NumberFieldReference;
import net.lecousin.ant.core.expression.impl.StringFieldReference;
import net.lecousin.ant.core.validation.ValidationContext;
import net.lecousin.ant.core.validation.annotations.Ignore;
import net.lecousin.ant.core.validation.annotations.Mandatory;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiData implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final StringFieldReference FIELD_ID = new StringFieldReference("id");
	public static final NumberFieldReference<Long> FIELD_VERSION = new NumberFieldReference<>("version");

	@Mandatory(value = true, context = ValidationContext.UPDATE)
	@Ignore(value = true, context = ValidationContext.CREATION)
	private String id;
	
	@Mandatory(value = true, context = ValidationContext.UPDATE)
	@Ignore(value = true, context = ValidationContext.CREATION)
	private long version;
	
}
