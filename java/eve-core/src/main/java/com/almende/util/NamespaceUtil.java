package com.almende.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.fasterxml.jackson.core.JsonProcessingException;

public final class NamespaceUtil {
	
	// Default namespaces quick
	//
	private static Map<String, Method[]>	cache		= new HashMap<String, Method[]>();
	private static NamespaceUtil			instance	= new NamespaceUtil();
	private static final Pattern			PATTERN		= Pattern.compile("\\.[^.]+$");
	
	private NamespaceUtil() {
	};
	
	public static CallTuple get(Object destination, String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		return instance._get(destination, path);
	}
	
	private void populateCache(Object destination, String steps, Method[] methods)
			throws IllegalAccessException, InvocationTargetException {
		AnnotatedClass clazz = AnnotationUtil.get(destination.getClass());
		for (AnnotatedMethod method : clazz
				.getAnnotatedMethods(Namespace.class)) {
			String path = steps +"."+ method.getAnnotation(Namespace.class).value();
			methods[methods.length-1] = method.getActualMethod();
			cache.put(path, Arrays.copyOf(methods,methods.length));
			
			Object newDest = method.getActualMethod().invoke(destination,
					(Object[]) null);
			// recurse:
			if (newDest != null) {
				populateCache(
						newDest,
						path,
						Arrays.copyOf(methods, methods.length+1));
			}
		}
	}
	
	private CallTuple _get(Object destination, String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		CallTuple result = new CallTuple();

		if (!path.contains(".")) {
			// Quick shortcut back
			result.setDestination(destination);
			result.setMethodName(path);
			return result;
		}
		
		path = destination.getClass().getName()+"."+path;
		String[] steps = path.split("\\.");
		String reducedMethod = steps[steps.length - 1];
		steps = Arrays.copyOf(steps, steps.length-1);
		path = PATTERN.matcher(path).replaceFirst("");
		
		if (!cache.containsKey(path)) {
			Method[] methods = new Method[1];
			String newSteps = destination.getClass().getName();
			populateCache(destination, newSteps, methods);
		}
		if (!cache.containsKey(path)) {
			try {
				throw new IllegalStateException("Non resolveable path given:'"
						+ path
						+ "' \n checked:"
						+ JOM.getInstance().writeValueAsString(cache));
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("Non resolveable path given:'"
						+ path + "' \n checked:" + cache);
			}
		}
		Method[] methods = cache.get(path);
		for (Method method : methods) {
			if (method != null) {
				destination = method.invoke(destination, (Object[]) null);
			}
		}
		result.setDestination(destination);
		result.setMethodName(reducedMethod);
		return result;
	}
	
	public class CallTuple {
		private Object	destination;
		private String	methodName;
		
		public Object getDestination() {
			return destination;
		}
		
		public void setDestination(Object destination) {
			this.destination = destination;
		}
		
		public String getMethodName() {
			return methodName;
		}
		
		public void setMethodName(String methodName) {
			this.methodName = methodName;
		}
	}
}
