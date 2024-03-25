package net.lecousin.ant.core.patch;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatchAppendElement extends Patch {

	private static final long serialVersionUID = 1L;
	
	private Serializable elementToAppend;
	
	public PatchAppendElement(String fieldName, Serializable elementToAppend) {
		super(fieldName);
		this.elementToAppend = elementToAppend;
	}
	
}
