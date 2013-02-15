package com.almende.util;

import java.util.Map;
import java.util.HashMap;

import com.google.code.twig.ObjectDatastoreFactory;

public class TwigUtil {
	private static Map<String, Class<?>> registeredClasses = 
			new HashMap<String, Class<?>>(); // <classname, class>
	
	/**
	 * Register a class to the ObjectDatastoreFactory of Twig once.
	 * If the class is already registered, nothing will happen
	 * @param clazz
	 */
	public static void register(Class<?> clazz) {
		String name = clazz.getName();
		synchronized (registeredClasses) {
			if (!registeredClasses.containsKey(name)) {
				try {
					registeredClasses.put(name, clazz);
					ObjectDatastoreFactory.register(clazz);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
