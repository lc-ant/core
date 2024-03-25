package net.lecousin.ant.core.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public interface TypeVisitor {

	boolean enterClass(Class<?> clazz, Map<String, ResolvedType> parameters);
	
	boolean doVisitFields(Class<?> clazz, Map<String, ResolvedType> parameters);
	
	boolean visitField(Field field, ResolvedType type);
	
	boolean doVisitMethods(Class<?> clazz, Map<String, ResolvedType> parameters);
	
	boolean visitMethod(Method method, ResolvedType returnType, ResolvedType[] parameters);
	
	boolean leaveClass(Class<?> clazz, Map<String, ResolvedType> parameters);
	
}
