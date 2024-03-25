package net.lecousin.ant.core.mapping.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriConsumer;

import net.lecousin.ant.core.mapping.GenericMapper;
import net.lecousin.ant.core.mapping.Mappers;
import net.lecousin.ant.core.reflection.ReflectionException;
import net.lecousin.ant.core.reflection.ResolvedType;
import net.lecousin.ant.core.utils.OptionalNullable;

public class CollectionHandler implements GenericMapper {
	
	private static final class SourceProvider {
		private Supplier<Iterator> iteratorSupplier;
		private Supplier<Integer> sizeSupplier;
	}
	
	private static final class TargetProvider {
		private Function<Integer, Object> instantiator;
		private BiConsumer<Object, Object> adder;
		private ResolvedType elementType;
	}

	@Override
	public boolean canMap(ResolvedType from, ResolvedType to) {
		if (!canMapFrom(from)) return false;
		return canMapTo(to);
	}
	
	@Override
	public OptionalNullable<Object> map(ResolvedType sourceType, Object sourceValue, ResolvedType targetType) {
		SourceProvider sourceProvider = getSourceProvider(sourceType, sourceValue);
		if (sourceProvider == null)
			return OptionalNullable.empty();
		
		if (sourceValue == null) {
			sourceProvider.iteratorSupplier = () -> Collections.emptyIterator();
			sourceProvider.sizeSupplier = () -> 0;
		}
		
		TargetProvider targetProvider = getTargetProvider(targetType);
		if (targetProvider == null)
			return OptionalNullable.empty();
		
		return map(sourceProvider, targetProvider);
	}
	
	private static SourceProvider getSourceProvider(ResolvedType type, Object value) {
		
		if (type instanceof ResolvedType.SingleClass singleClass) {
			return getSourceProviderFromClass(singleClass.getSingleClass(), value);
		}
		if (type instanceof ResolvedType.Parameterized p) {
			return getSourceProviderFromClass(p.getBase(), value);
		}
		if (type instanceof ResolvedType.Bounded b) {
			SourceProvider sourceProvider = null;
			for (int i = 0; i < b.getUpperBounds().length && sourceProvider == null; ++i) {
				sourceProvider = getSourceProvider(b.getUpperBounds()[i], value);
			}
			return sourceProvider;
		}
		if (type instanceof ResolvedType.Array a) {
			return getSourceProviderFromArray(value, a.getComponentType());
		}
		throw new ReflectionException("Unexpected resolved type: " + type);
	}
	
	private static boolean canMapFrom(ResolvedType type) {
		if (type instanceof ResolvedType.SingleClass singleClass) {
			return canMapFrom(singleClass.getSingleClass());
		}
		if (type instanceof ResolvedType.Parameterized p) {
			return canMapFrom(p.getBase());
		}
		if (type instanceof ResolvedType.Bounded b) {
			for (int i = 0; i < b.getUpperBounds().length; ++i) {
				if (canMapFrom(b.getUpperBounds()[i])) return true;
			}
			return false;
		}
		if (type instanceof ResolvedType.Array a) {
			return true;
		}
		return false;
	}
	
	private static SourceProvider getSourceProviderFromClass(Class<?> clazz, Object value) {
		SourceProvider provider = new SourceProvider();
		if (Iterable.class.isAssignableFrom(clazz)) {
			provider.iteratorSupplier = () -> ((Iterable) value).iterator();
			if (Collection.class.isAssignableFrom(clazz))
				provider.sizeSupplier = () -> ((Collection) value).size();
		} else if (Iterator.class.isAssignableFrom(clazz))
			provider.iteratorSupplier = () -> (Iterator) value;
		else if (Enumeration.class.isAssignableFrom(clazz))
			provider.iteratorSupplier = () -> ((Enumeration) value).asIterator();
		if (provider.iteratorSupplier != null)
			return provider;
		return null;
	}
	
	private static boolean canMapFrom(Class<?> clazz) {
		if (Iterable.class.isAssignableFrom(clazz)) return true;
		if (Iterator.class.isAssignableFrom(clazz)) return true;
		if (Enumeration.class.isAssignableFrom(clazz)) return true;
		return false;
	}
	
	private static SourceProvider getSourceProviderFromArray(Object value, ResolvedType componentType) {
		SourceProvider provider = new SourceProvider();
		provider.sizeSupplier = () -> Array.getLength(value);
		if (componentType instanceof ResolvedType.SingleClass sc && sc.getSingleClass().isPrimitive())
			provider.iteratorSupplier = () -> getPrimitiveIterator(sc.getSingleClass(), value);
		else
			provider.iteratorSupplier = () -> getObjectIterator(value);
		return provider;
	}
	
	private static TargetProvider getTargetProvider(ResolvedType targetType) {
		if (targetType instanceof ResolvedType.Array a) {
			TargetProvider provider = new TargetProvider();
			Class<?> clazz = null;
			ResolvedType type = a.getComponentType();
			if (type instanceof ResolvedType.SingleClass c)
				clazz = c.getSingleClass();
			else if (type instanceof ResolvedType.Parameterized p)
				clazz = p.getBase();
			else if (type instanceof ResolvedType.Array aa)
				throw new ReflectionException("TODO: array of array"); // TODO
			Class<?> componentType = clazz;
			provider.instantiator = size -> Array.newInstance(componentType, size);
			provider.elementType = type;
			provider.adder = new BiConsumer<>() {
				private int i = 0;
				
				@Override
				public void accept(Object array, Object value) {
					Array.set(array, i++, value);
				}
			};
			return provider;
		}

		if (targetType instanceof ResolvedType.Bounded b) {
			TargetProvider provider = null;
			for (var bound : b.getUpperBounds()) {
				provider = getTargetProvider(bound);
				if (provider != null) break;
			}
			if (provider != null) return provider;
		}
		
		Class<?> targetClass = null;
		ResolvedType elementType = null;
		if (targetType instanceof ResolvedType.Parameterized p) {
			targetClass = p.getBase();
			if (Collection.class.isAssignableFrom(targetClass)) {
				if (p.getParameters().length == 1)
					elementType = p.getParameters()[0];
			}
		}
		if (targetType instanceof ResolvedType.SingleClass c) {
			targetClass = c.getSingleClass();
		}

		if (targetClass == null) return null;

		Function<Integer, Object> instantiator = null;
		BiConsumer<Object, Object> adder = null;
		
		if (Collection.class.isAssignableFrom(targetClass)) {
			if (targetClass.isAssignableFrom(ArrayList.class))
				instantiator = size -> new ArrayList(size);
			else if (targetClass.isAssignableFrom(LinkedList.class))
				instantiator = size -> new LinkedList();
			else if (targetClass.isAssignableFrom(HashSet.class))
				instantiator = size -> new HashSet();
			else {
				try {
					Constructor ctor = targetClass.getConstructor();
					instantiator = size -> {
						try { 
							return ctor.newInstance();
						} catch (Exception e) {
							return null;
						}
					};
				} catch (Exception e) {
					// ignore
				}
			}
			adder = (col, element) -> ((Collection) col).add(element);
		}
		
		if (instantiator != null && adder != null) {
			if (elementType == null) elementType = new ResolvedType.SingleClass(Object.class);
			TargetProvider provider = new TargetProvider();
			provider.instantiator = instantiator;
			provider.adder = adder;
			provider.elementType = elementType;
			return provider;
		}
		
		return null;
	}
	
	private static boolean canMapTo(ResolvedType targetType) {
		if (targetType instanceof ResolvedType.Array a) return true;
		if (targetType instanceof ResolvedType.Bounded b) {
			for (var bound : b.getUpperBounds())
				if (canMapTo(bound)) return true;
			return false;
		}
		
		Class<?> targetClass = null;
		if (targetType instanceof ResolvedType.Parameterized p) {
			targetClass = p.getBase();
		}
		if (targetType instanceof ResolvedType.SingleClass c) {
			targetClass = c.getSingleClass();
		}

		if (targetClass == null) return false;

		if (Collection.class.isAssignableFrom(targetClass)) return true;

		return false;
	}

	private static Iterator getPrimitiveIterator(Class primitiveType, Object sourceValue) {
		if (boolean.class.equals(primitiveType)) return createIterator(sourceValue, Array::getBoolean);
		if (byte.class.equals(primitiveType)) return createIterator(sourceValue, Array::getByte);
		if (short.class.equals(primitiveType)) return createIterator(sourceValue, Array::getShort);
		if (int.class.equals(primitiveType)) return createIterator(sourceValue, Array::getInt);
		if (long.class.equals(primitiveType)) return createIterator(sourceValue, Array::getLong);
		if (char.class.equals(primitiveType)) return createIterator(sourceValue, Array::getChar);
		if (float.class.equals(primitiveType)) return createIterator(sourceValue, Array::getFloat);
		return createIterator(sourceValue, Array::getDouble);
	}
	
	private static TriConsumer<Object, Integer, Object> getPrimitiveArraySetter(Class primitiveType) {
		if (boolean.class.equals(primitiveType)) return (array, index, value) -> Array.setBoolean(array, index, (Boolean) value);
		if (byte.class.equals(primitiveType)) return (array, index, value) -> Array.setByte(array, index, (Byte) value);
		if (short.class.equals(primitiveType)) return (array, index, value) -> Array.setShort(array, index, (Short) value);
		if (int.class.equals(primitiveType)) return (array, index, value) -> Array.setInt(array, index, (Integer) value);
		if (long.class.equals(primitiveType)) return (array, index, value) -> Array.setLong(array, index, (Long) value);
		if (char.class.equals(primitiveType)) return (array, index, value) -> Array.setChar(array, index, (Character) value);
		if (float.class.equals(primitiveType)) return (array, index, value) -> Array.setFloat(array, index, (Float) value);
		return (array, index, value) -> Array.setDouble(array, index, (Double) value);
	}
	
	private static Iterator getObjectIterator(Object sourceValue) {
		return createIterator(sourceValue, Array::get);
	}
	
	private static Iterator createIterator(Object sourceValue, BiFunction<Object, Integer, Object> arrayGetter) {
		return new Iterator() {
			private final int l = Array.getLength(sourceValue);
			private int i = 0;
			
			@Override
			public boolean hasNext() {
				return i < l;
			}
			
			@Override
			public Object next() {
				return arrayGetter.apply(sourceValue, i++);
			}
		};
	}
	
	private OptionalNullable<Object> map(SourceProvider source, TargetProvider target) {
		Iterator it = source.iteratorSupplier.get();
		int size;
		if (source.sizeSupplier == null) {
			List list = new LinkedList();
			while (it.hasNext()) list.add(it.next());
			size = list.size();
			it = list.iterator();
		} else {
			size = source.sizeSupplier.get();
		}
		Object result = target.instantiator.apply(size);
		if (result == null)
			return OptionalNullable.empty();
		while (it.hasNext()) {
			Object element = it.next();
			target.adder.accept(result, Mappers.map(element, target.elementType));
		}
		return OptionalNullable.of(result);
	}

}
