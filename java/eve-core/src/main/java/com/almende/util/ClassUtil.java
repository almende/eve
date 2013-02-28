package com.almende.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ClassUtil {
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

	// safe because both Long.class and long.class are of type Class<Long>
	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrap(Class<T> c) {
		return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
	}

	// safe because both Long.class and long.class are of type Class<Long>
	@SuppressWarnings("unchecked")
	public static <T> Class<T> unWrap(Class<T> c) {
		return CLASS_MAP.containsKey(c) ? (Class<T>) CLASS_MAP.get(c) : c;
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = generateMap();

	private static Map<Class<?>, Class<?>> generateMap() {
		Map<Class<?>, Class<?>> result = new HashMap<Class<?>, Class<?>>();
		result.put(boolean.class, Boolean.class);
		result.put(byte.class, Byte.class);
		result.put(char.class, Character.class);
		result.put(double.class, Double.class);
		result.put(float.class, Float.class);
		result.put(int.class, Integer.class);
		result.put(long.class, Long.class);
		result.put(short.class, Short.class);
		result.put(void.class, Void.class);
		return result;
	}

	private static final Map<Class<?>, Class<?>> CLASS_MAP = generatePrimitiveMap();

	private static Map<Class<?>, Class<?>> generatePrimitiveMap() {
		Map<Class<?>, Class<?>> result = new HashMap<Class<?>, Class<?>>();
		result.put(Boolean.class, boolean.class);
		result.put(Byte.class, byte.class);
		result.put(Character.class, char.class);
		result.put(Double.class, double.class);
		result.put(Float.class, float.class);
		result.put(Integer.class, int.class);
		result.put(Long.class, long.class);
		result.put(Short.class, short.class);
		result.put(Void.class, void.class);
		return result;
	}

	public static Method searchForMethod(Class<?> type, String name, Class<?>[] parms) {
		Method[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			// Has to be named the same of course.
			if (!methods[i].getName().equals(name))
				continue;

			Class<?>[] types = methods[i].getParameterTypes();

			// Does it have the same number of arguments that we're looking for.
			if (types.length != parms.length)
				continue;

			// Check for type compatibility
			if (areTypesCompatible(types, parms))
				return methods[i];
		}
		return null;
	}

	public static boolean areTypesCompatible(Class<?>[] targets,
			Class<?>[] sources) {

		if (targets.length != sources.length)
			return false;

		for (int i = 0; i < targets.length; i++) {
			if (sources[i] == null)
				continue;

			if (!wrap(targets[i]).isAssignableFrom(sources[i]))
				return false;
		}
		return (true);
	}
}
