package net.lecousin.ant.core.patch;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatchRemoveElement extends Patch {

	private static final long serialVersionUID = 1L;
	
	private Serializable elementToRemove;
	
	public PatchRemoveElement(String fieldName, Serializable elementToRemove) {
		super(fieldName);
		this.elementToRemove = elementToRemove;
	}
	
}
