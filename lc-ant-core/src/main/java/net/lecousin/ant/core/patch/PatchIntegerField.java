package net.lecousin.ant.core.patch;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatchIntegerField extends Patch {

	private static final long serialVersionUID = 1L;
	
	private long addInteger;
	
	public PatchIntegerField(String fieldName, long addInteger) {
		super(fieldName);
		this.addInteger = addInteger;
	}
	
}
