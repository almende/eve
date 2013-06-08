package com.almende.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;

public class NamespaceUtil {
	private static Map<String, String[]>	cache		= new HashMap<String, String[]>();
	private static NamespaceUtil			instance	= new NamespaceUtil();
	
	public static CallTuple get(Object destination, String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		return instance._get(destination, path);
	}
	
	private void populateCache(Object destination, String path, String methods)
			throws IllegalAccessException, InvocationTargetException {
		AnnotatedClass clazz = AnnotationUtil.get(destination.getClass());
		for (AnnotatedMethod method : clazz
				.getAnnotatedMethods(Namespace.class)) {
			Object newDest = method.getActualMethod().invoke(destination,
					(Object[]) null);
			// recurse:
			if (newDest != null) {
				populateCache(
						newDest,
						path + "."
								+ method.getAnnotation(Namespace.class).value(),
						methods + "." + method.getName());
			}
		}
		cache.put(path, methods.split("\\."));
	}
	
	private CallTuple _get(Object destination, String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		CallTuple result = new CallTuple();
		
		String reducedPath = path.replaceFirst("\\.?[^.]+$", "");
		String reducedMethod = path.replaceAll(".*\\.", "");
		
		String fullPath = destination.getClass().getName();
		if (!reducedPath.isEmpty()) {
			fullPath += "." + reducedPath;
		}
		
		if (!cache.containsKey(fullPath)) {
			populateCache(destination, destination.getClass().getName(), "");
		}
		if (!cache.containsKey(fullPath)) {
			throw new IllegalStateException("Non resolveable path given:'"
					+ fullPath + "' \n checked:" + cache);
		}
		String[] methods = cache.get(fullPath);
		for (String methodName : methods) {
			if (!methodName.equals("")) {
				Method method = destination.getClass().getMethod(methodName,
						(Class<?>[]) null);
				destination = method.invoke(destination, (Object[]) null);
			}
		}
		result.destination = destination;
		result.methodName = reducedMethod;
		return result;
	}
	
	public class CallTuple {
		public Object	destination;
		public String	methodName;
	}
}
