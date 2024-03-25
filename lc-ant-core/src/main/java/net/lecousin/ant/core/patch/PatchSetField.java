package net.lecousin.ant.core.patch;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatchSetField extends Patch {

	private static final long serialVersionUID = 1L;
	
	private Serializable value;
	
	public PatchSetField(String fieldName, Serializable value) {
		super(fieldName);
		this.value = value;
	}
	
}
