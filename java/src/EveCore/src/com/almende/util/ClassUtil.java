package com.almende.util;

public class ClassUtil {
	/**
	 * Check if checkClass has implemented interfaceClass
	 * @param checkClass
	 * @param interfaceClass
	 */
	public static boolean hasInterface(Class<?> checkClass, Class<?> interfaceClass) {
		Class<?>[] interfaces = checkClass.getInterfaces();
		
		for (Class<?> i : interfaces) {
			if (i.equals(interfaceClass)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if checkClass extends superClass
	 * @param checkClass
	 * @param superClass
	 */
	public static boolean hasSuperClass(Class<?> checkClass, Class<?> superClass) {
		Class<?> s = null;
		while ((s = checkClass.getSuperclass()) != null) {
			if (s.equals(superClass)) {
				return true;
			}
		}
		
		return false;
	}
}
