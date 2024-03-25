package net.lecousin.ant.core.patch;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.lecousin.ant.core.expression.impl.CollectionFieldReference;
import net.lecousin.ant.core.expression.impl.EnumFieldReference;
import net.lecousin.ant.core.expression.impl.FieldReference;
import net.lecousin.ant.core.expression.impl.NumberFieldReference;
import net.lecousin.ant.core.expression.impl.StringFieldReference;

@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
	@JsonSubTypes.Type(PatchSetField.class),
	@JsonSubTypes.Type(PatchIntegerField.class),
	@JsonSubTypes.Type(PatchAppendElement.class),
	@JsonSubTypes.Type(PatchRemoveElement.class),
})
public abstract class Patch implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String fieldName;

	public static PatchBuilder field(FieldReference<?> field) {
		return new PatchBuilder(field.getField());
	}

	public static PatchBuilder field(FieldReference.Nullable<?> field) {
		return new PatchBuilder(field.getNullableField());
	}

	public static PatchBuilder field(EnumFieldReference<?> field) {
		return new PatchBuilder(field.getEnumField());
	}

	public static PatchBuilder field(EnumFieldReference.Nullable<?> field) {
		return new PatchBuilder(field.getNullableEnumField());
	}
	
	public static PatchNumberBuilder field(StringFieldReference stringField) {
		return new PatchNumberBuilder(stringField.getStringField());
	}

	public static PatchBuilder field(StringFieldReference.Nullable stringField) {
		return new PatchBuilder(stringField.getNullableStringField());
	}
	
	public static PatchNumberBuilder field(NumberFieldReference<?> numberField) {
		return new PatchNumberBuilder(numberField.getNumberField());
	}
	
	public static PatchNumberBuilder field(NumberFieldReference.Nullable<?> numberField) {
		return new PatchNumberBuilder(numberField.getNullableNumberField());
	}
	
	public static <T extends Serializable> PatchCollectionBuilder<T> field(CollectionFieldReference<T, ?> collectionField) {
		return new PatchCollectionBuilder<>(collectionField.getCollectionField());
	}
	
	@AllArgsConstructor
	public static class PatchBuilder {
		
		protected final String fieldName;
		
		public PatchSetField set(Serializable value) {
			return new PatchSetField(fieldName, value);
		}
		
	}
	
	public static class PatchNumberBuilder extends PatchBuilder {
		
		public PatchNumberBuilder(String fieldName) {
			super(fieldName);
		}

		public PatchIntegerField inc() {
			return add(1L);
		}
		
		public PatchIntegerField dec() {
			return add(-1L);
		}
		
		public PatchIntegerField add(long add) {
			return new PatchIntegerField(fieldName, add);
		}

		public PatchIntegerField substract(long minus) {
			return add(-minus);
		}
		
	}
	
	public static class PatchCollectionBuilder<T extends Serializable> extends PatchBuilder {

		public PatchCollectionBuilder(String fieldName) {
			super(fieldName);
		}

		public PatchAppendElement appendElement(T newElement) {
			return new PatchAppendElement(fieldName, newElement);
		}
		
		public PatchRemoveElement removeElement(T newElement) {
			return new PatchRemoveElement(fieldName, newElement);
		}
		
	}
	
}
