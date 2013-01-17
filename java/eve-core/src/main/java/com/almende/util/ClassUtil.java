package com.almende.util;

public class ClassUtil {
	/**
	 * Check if checkClass has implemented interfaceClass
	 * @param checkClass
	 * @param interfaceClass
	 */
	public static boolean hasInterface(Class<?> checkClass, Class<?> interfaceClass) {
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
	 * @param checkClass
	 * @param superClass
	 */
	public static boolean hasSuperClass(Class<?> checkClass, Class<?> superClass) {
		// TODO: replace with return (checkClass instanceof superClass);  ?
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
}
