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
}
