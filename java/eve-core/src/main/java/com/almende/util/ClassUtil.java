package com.almende.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassUtil {
	
	private ClassUtil(){};
	/**
	 * Check if checkClass has implemented interfaceClass
	 * 
	 * @param checkClass
	 * @param interfaceClass
	 */
	public static boolean hasInterface(Class<?> checkClass,
			Class<?> interfaceClass) {
		String name = interfaceClass.getName();
		Class<?> s = checkClass;
		while (s != null) {
			Class<?>[] interfaces = s.getInterfaces();
			for (Class<?> i : interfaces) {
				if (i.getName().equals(name)) {
					return true;
				}
				if (hasInterface(s, i)) {
					return true;
				}
			}
			
			s = s.getSuperclass();
		}
		
		return false;
	}
	
	/**
	 * Check if checkClass extends superClass
	 * 
	 * @param checkClass
	 * @param superClass
	 */
	public static boolean hasSuperClass(Class<?> checkClass, Class<?> superClass) {
		// TODO: replace with return (checkClass instanceof superClass); ?
		String name = superClass.getName();
		Class<?> s = (checkClass != null) ? checkClass.getSuperclass() : null;
		while (s != null) {
			if (s.getName().equals(name)) {
				return true;
			}
			s = s.getSuperclass();
		}
		
		return false;
	}
	
	/**
	 * Wraps any primitive type in it's boxed version
	 * returns other types unmodified
	 * 
	 * @param class type
	 * @return class type
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrap(Class<T> c) {
		return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
	}
	
	/**
	 * Unwraps any boxed type in it's primitive version
	 * returns other types unmodified
	 * 
	 * @param class type
	 * @return class type
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> unWrap(Class<T> c) {
		return WRAPPERS_TO_PRIMITIVES.containsKey(c) ? (Class<T>) WRAPPERS_TO_PRIMITIVES
				.get(c) : c;
	}
	
	private static final Map<Class<?>, Class<?>>	PRIMITIVES_TO_WRAPPERS	= new HashMap<Class<?>, Class<?>>();
	static {
		PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
		PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
		PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
		PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
		PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
		PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
		PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
		PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
		PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
	}
	
	private static final Map<Class<?>, Class<?>>	WRAPPERS_TO_PRIMITIVES	= new HashMap<Class<?>, Class<?>>();
	static {
		WRAPPERS_TO_PRIMITIVES.put(Boolean.class, boolean.class);
		WRAPPERS_TO_PRIMITIVES.put(Byte.class, byte.class);
		WRAPPERS_TO_PRIMITIVES.put(Character.class, char.class);
		WRAPPERS_TO_PRIMITIVES.put(Double.class, double.class);
		WRAPPERS_TO_PRIMITIVES.put(Float.class, float.class);
		WRAPPERS_TO_PRIMITIVES.put(Integer.class, int.class);
		WRAPPERS_TO_PRIMITIVES.put(Long.class, long.class);
		WRAPPERS_TO_PRIMITIVES.put(Short.class, short.class);
		WRAPPERS_TO_PRIMITIVES.put(Void.class, void.class);
	}
	
	/**
	 * Search for method (reflection) which fits the given argument types. Works
	 * for any combination of
	 * primitive types, boxed types and normal objects.
	 * 
	 * @author PSpeed
	 *         http://stackoverflow.com/questions/1894740/any-solution-for
	 *         -class-getmethod-reflection-and-autoboxing
	 * 
	 * @param type
	 *            Class in which the method is searched
	 * @param name
	 *            Method name to search for
	 * @param parms
	 *            Class types of the requested arguments
	 * @return Method
	 */
	public static Method searchForMethod(Class<?> type, String name,
			Class<?>[] parms) {
		Method[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			// Has to be named the same of course.
			if (!methods[i].getName().equals(name)) {
				continue;
			}
			
			Class<?>[] types = methods[i].getParameterTypes();
			
			// Does it have the same number of arguments that we're looking for.
			if (types.length != parms.length) {
				continue;
			}
			
			// Check for type compatibility
			if (areTypesCompatible(types, parms)) {
				return methods[i];
			}
		}
		return null;
	}
	
	public static boolean areTypesCompatible(Class<?>[] targets,
			Class<?>[] sources) {
		
		if (targets.length != sources.length) {
			return false;
		}
		
		for (int i = 0; i < targets.length; i++) {
			if (sources[i] == null) {
				continue;
			}
			
			if (!wrap(targets[i]).isAssignableFrom(sources[i])) {
				return false;
			}
		}
		return (true);
	}
	
	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type. See <a
	 * href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860"
	 * >description</a>
	 * 
	 * @param type
	 *            the type
	 * @return the underlying class
	 */
	public static Class<?> getClass(final Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		
		if (type instanceof ParameterizedType) {
			return getClass(((ParameterizedType) type).getRawType());
		}
		
		if (type instanceof GenericArrayType) {
			final Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			final Class<?> componentClass = getClass(componentType);
			if (componentClass != null) {
				return Array.newInstance(componentClass, 0).getClass();
			}
			
		}
		return null;
	}
	
	/**
	 * Get the actual type arguments a child class has used to extend a generic
	 * base class. See <a
	 * href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860"
	 * >description</a>
	 * 
	 * @param baseClass
	 *            the base class
	 * @param childClass
	 *            the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> getTypeArguments(final Class<T> baseClass,
			final Class<? extends T> childClass) {
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = childClass;
		// start walking up the inheritance hierarchy until we hit baseClass
		while (!getClass(type).equals(baseClass)) {
			if (type instanceof Class) {
				// there is no useful information for us in raw types, so just
				// keep going.
				type = ((Class<?>) type).getGenericSuperclass();
			} else {
				final ParameterizedType parameterizedType = (ParameterizedType) type;
				final Class<?> rawType = (Class<?>) parameterizedType
						.getRawType();
				
				final Type[] actualTypeArguments = parameterizedType
						.getActualTypeArguments();
				final TypeVariable<?>[] typeParameters = rawType
						.getTypeParameters();
				for (int i = 0; i < actualTypeArguments.length; i++) {
					resolvedTypes
							.put(typeParameters[i], actualTypeArguments[i]);
				}
				
				if (!rawType.equals(baseClass)) {
					type = rawType.getGenericSuperclass();
				}
			}
		}
		
		// finally, for each actual type argument provided to baseClass,
		// determine (if possible)
		// the raw class for that type argument.
		final Type[] actualTypeArguments;
		if (type instanceof Class) {
			actualTypeArguments = ((Class<?>) type).getTypeParameters();
		} else {
			actualTypeArguments = ((ParameterizedType) type)
					.getActualTypeArguments();
		}
		final List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		return typeArgumentsAsClasses;
	}
	
	
	//These methods were found at: http://www.javacodegeeks.com/2011/12/cloning-of-serializable-and-non.html
	//@Author Craig Flichel
	
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T cloneThroughSerialize(T t) throws Exception {
	   ByteArrayOutputStream bos = new ByteArrayOutputStream();
	   serializeToOutputStream(t, bos);
	   byte[] bytes = bos.toByteArray();
	   ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
	   return (T)ois.readObject();
	}

	private static void serializeToOutputStream(Serializable ser, OutputStream os)
	                                                          throws IOException {
	   ObjectOutputStream oos = null;
	   try {
	      oos = new ObjectOutputStream(os);
	      oos.writeObject(ser);
	      oos.flush();
	   } finally {
	      oos.close();
	   }
	}
}
