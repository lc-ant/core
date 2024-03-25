package net.lecousin.ant.core.validation;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.ant.core.reflection.ObjectPropertiesCache;
import net.lecousin.ant.core.validation.exceptions.ValidationException;

class ValidatorChain {

	private List<ChainableValidator> chain = new LinkedList<>();
	
	void add(ChainableValidator validator) {
		chain.add(validator);
	}
	
	void validate(Object object, ObjectPropertiesCache cache) throws ValidationException {
		for (ChainableValidator validator : chain)
			if (!validator.validate(object, cache))
				return;
	}
	
	public interface ChainableValidator {
		
		boolean validate(Object object, ObjectPropertiesCache cache) throws ValidationException;
		
	}
}
