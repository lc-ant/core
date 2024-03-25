package net.lecousin.ant.core.reflection;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

public interface ResolvedType {
	
	Type toJavaType();
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	class SingleClass implements ResolvedType {
		private final Class<?> singleClass;
		
		@Override
		public String toString() {
			return singleClass.toString();
		}
		
		@Override
		public Type toJavaType() {
			return singleClass;
		}
	}
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	class Bounded implements ResolvedType {
		private final ResolvedType[] lowerBounds;
		private final ResolvedType[] upperBounds;
		
		@Override
		public String toString() {
			return "ResolvedType.Bounded[lower " + Arrays.toString(lowerBounds) + ", upper " + Arrays.toString(upperBounds) + "]";
		}
		
		@Override
		public Type toJavaType() {
			return new WildcardType() {
				@Override
				public Type[] getUpperBounds() {
					Type[] bounds = new Type[upperBounds.length];
					for (int i = 0; i < bounds.length; ++i) bounds[i] = upperBounds[i].toJavaType();
					return bounds;
				}
				
				@Override
				public Type[] getLowerBounds() {
					Type[] bounds = new Type[lowerBounds.length];
					for (int i = 0; i < bounds.length; ++i) bounds[i] = lowerBounds[i].toJavaType();
					return bounds;
				}
			};
		}
	}
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	class Parameterized implements ResolvedType {
		private final Class<?> base;
		private final ResolvedType[] parameters;
		
		@Override
		public String toString() {
			return base.toString() + "<" + Arrays.toString(parameters) + ">";
		}
		
		@Override
		public Type toJavaType() {
			return new ParameterizedType() {
				@Override
				public Type getRawType() {
					return base;
				}
				@Override
				public Type getOwnerType() {
					return null;
				}
				@Override
				public Type[] getActualTypeArguments() {
					Type[] types = new Type[parameters.length];
					for (int i = 0; i < types.length; ++i) types[i] = parameters[i].toJavaType();
					return types;
				}
			};
		}
	}
	
	@AllArgsConstructor
	@Getter
	@EqualsAndHashCode
	class Array implements ResolvedType {
		private final ResolvedType componentType;
		
		@Override
		public String toString() {
			return componentType.toString() + "[]";
		}
		
		@Override
		public Type toJavaType() {
			return new GenericArrayType() {
				@Override
				public Type getGenericComponentType() {
					return componentType.toJavaType();
				}
			};
		}
	}
	
	static Optional<Class<?>> getRawClass(ResolvedType type) {
		if (type instanceof SingleClass c) return Optional.of(c.getSingleClass());
		if (type instanceof Parameterized p) return Optional.of(p.getBase());
		return Optional.empty();
	}
	
}
