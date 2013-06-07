/**
 * @file AnnotationUtil.java
 * 
 * AnnotationUtil is a utility to get all annotations of a class, its methods, 
 * and the method parameters. Returned annotations include all annotations of 
 * the classes interfaces and super classes.
 * Requested classes are cached, so requesting a classes annotations repeatedly
 * is fast.
 * 
 * Example usage:
 * 
 *     AnnotatedClass annotatedClass = AnnotationUtil.get(MyClass.class);
 *     List<AnnotatedMethod> methods = annotatedClass.getMethods();
 *     for (AnnotatedMethod method : methods) {
 *         System.out.println("Method: " + method.getName());
 *         List<Annotation> annotations = method.getAnnotations();
 *         for (Annotation annotation : annotations) {
 *             System.out.println("    Annotation: " + annotation.toString());
 *         }
 *     }
 * 
 * @brief 
 * AnnotationUtil is a utility to retrieve merged annotations from a class
 * including all its superclasses and interfaces.
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright (c) 2013 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2013-01-21
 */
package com.almende.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationUtil {
	private static Map<String, AnnotatedClass> cache = 
			new ConcurrentHashMap<String, AnnotatedClass>();
	private static Map<String, AnnotatedClass> cacheIncludingObject = 
			new ConcurrentHashMap<String, AnnotatedClass>();
	
	/**
	 * Get all annotations of a class, methods, and parameters.
	 * Returned annotations include all annotations of the classes interfaces
	 * and super classes (excluding java.lang.Object).
	 * @param clazz
	 * @return annotatedClazz
	 * @throws Exception 
	 * @throws SecurityException 
	 */
	public static AnnotatedClass get(Class<?> clazz) {
		final boolean includeObject = false;
		return get(clazz, includeObject);
	}
	
	/**
	 * Get all annotations of a class, methods, and parameters.
	 * Returned annotations include all annotations of the classes interfaces
	 * and super classes.
	 * @param clazz
	 * @param includeObject    If true, methods of java.lang.Object will be 
	 *                         included in the superclasses too.
	 * @return annotatedClazz
	 * @throws Exception 
	 * @throws SecurityException 
	 */
	public static AnnotatedClass get(Class<?> clazz, boolean includeObject) {
		Map<String, AnnotatedClass> _cache = includeObject ? cacheIncludingObject : cache;
		AnnotatedClass annotatedClazz = _cache.get(clazz.getName());
		if (annotatedClazz == null) {
			annotatedClazz = new AnnotatedClass(clazz, includeObject);
			_cache.put(clazz.getName(), annotatedClazz);
		}		
		return annotatedClazz;
	}
	
	/**
	 * AnnotatedClass describes a class, its annotations, and its methods.
	 */
	public static class AnnotatedClass {
		private Class<?> clazz = null;
		private List<Annotation> annotations = new ArrayList<Annotation>();
		private List<AnnotatedMethod> methods = new ArrayList<AnnotatedMethod>();

		/**
		 * Create a new AnnotatedClass
		 * @param clazz
		 * @param includeObject  If true, the methods of super class 
		 *                       java.lang.Object will be included too.
		 * @throws Exception 
		 * @throws SecurityException 
		 */
		public AnnotatedClass(Class<?> clazz, boolean includeObject) {
			this.clazz = clazz;
			merge(clazz, includeObject);
		}

		/**
		 * Recursively merge a class into this AnnotatedClass.
		 * The method loops over all the classess interfaces and superclasses
		 * Methods with will be merged.
		 * @param clazz
		 * @param includeObject     if true, superclass java.lang.Object will
		 *                          be included too.
		 * @throws Exception 
		 * @throws SecurityException 
		 */
		private void merge(Class<?> clazz, boolean includeObject) {
			Class<?> c = clazz;
			while (c != null && (includeObject || c != Object.class)) {
				// merge the annotations
				AnnotationUtil.merge(annotations, c.getDeclaredAnnotations());
				
				// merge the methods
				AnnotationUtil.merge(methods, c.getDeclaredMethods());

				// merge all interfaces and the superclasses of the interfaces
				for (Class<?> i : c.getInterfaces()) {
					merge(i, includeObject);
				}
				
				// ok now again for the superclass
				c = c.getSuperclass();
			}
		}
		
		/**
		 * Get the actual Java class described by this AnnotatedClass.
		 * @return clazz
		 */
		public Class<?> getActualClass () {
			return this.clazz;
		}
		
		/**
		 * Get all methods including methods declared in superclasses.
		 * @return methods
		 */
		public List<AnnotatedMethod> getMethods () {
			return methods;
		}
		
		/**
		 * Get all methods including methods declared in superclasses, filtered
		 * by name
		 * @param name
		 * @return filteredMethods
		 */
		public List<AnnotatedMethod> getMethods (String name) {
			List<AnnotatedMethod> filteredMethods = new ArrayList<AnnotatedMethod>();
			for (AnnotatedMethod method : methods) {
				if (method.getName().equals(name)) {
					filteredMethods.add(method);
				}
			}
			return filteredMethods;
		}

		/**
		 * Get all methods including methods declared in superclasses, filtered
		 * by annotation
		 * @param annotation
		 * @return filteredMethods
		 */
		public <T> List<AnnotatedMethod> getAnnotatedMethods (Class<T> annotation) {
			List<AnnotatedMethod> filteredMethods = new ArrayList<AnnotatedMethod>();
			for (AnnotatedMethod method : methods) {
				if (method.getAnnotation(annotation) != null) {
					filteredMethods.add(method);
				}
			}
			return filteredMethods;
		}
		/**
		 * Get all annotations defined on this class, its superclasses, and its
		 * interfaces
		 * @return annotations
		 */
		public List<Annotation> getAnnotations () {
			return annotations;
		}

		/**
		 * Get an annotation of this class by type. 
		 * Returns null if not available.
		 * @param annotationClass
		 * @return annotation
		 */
		@SuppressWarnings("unchecked")
		public <T> T getAnnotation (Class<T> type) {
			for (Annotation annotation : annotations) {
				if (annotation.annotationType() == type) {
					return (T) annotation;
				}
			}
			return null;
		}
	}
	
	/**
	 * AnnotatedMethod describes a method and its parameters.
	 */
	public static class AnnotatedMethod {
		private Method method = null;
		private String name = null;
		private Class<?> returnType = null;
		private Type genericReturnType = null;
		private List<Annotation> annotations = new ArrayList<Annotation>();
		private List<AnnotatedParam> params = new ArrayList<AnnotatedParam>();
		
		public AnnotatedMethod(Method method) {
			this.method = method;
			this.name = method.getName();
			this.returnType = method.getReturnType();
			this.genericReturnType = method.getGenericReturnType();

			merge(method);		
		}
		
		/**
		 * Merge a java method into this Annotated method.
		 * Annotations and parameter annotations will be merged.
		 * @param method
		 */
		private void merge(Method method) {
			// merge the annotations
			AnnotationUtil.merge(annotations, method.getDeclaredAnnotations());
			
			// merge the params
			Annotation[][] params = method.getParameterAnnotations();
			Class<?>[] types = method.getParameterTypes();
			Type[] genericTypes = method.getGenericParameterTypes();
			for (int i = 0; i < params.length; i++) {
				if (i > this.params.size() - 1) {
					this.params.add(new AnnotatedParam(params[i], types[i], 
							genericTypes[i]));
				}
				else {
					this.params.get(i).merge(params[i]);
				}
			}
		}

		/**
		 * Get the actual Java method described by this AnnotatedMethod.
		 * @return method
		 */
		public Method getActualMethod () {
			return method;
		}

		/**
		 * Get the method name
		 * @return name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Get the return type of the method
		 * @return returnType
		 */
		public Class<?> getReturnType() {
			return returnType;
		}
		
		/**
		 * Get the generic return type of the method
		 * @return genericType
		 */
		public Type getGenericReturnType() {
			return genericReturnType;
		}
		
		/**
		 * Get all annotations of this method, defined in all implementations
		 * and interfaces of the class.
		 * @return annotations
		 */
		public List<Annotation> getAnnotations () {
			return annotations;
		}

		/**
		 * Get an annotation of this method by type. 
		 * Returns null if not available.
		 * @param annotationClass
		 * @return annotation
		 */
		@SuppressWarnings("unchecked")
		public <T> T getAnnotation (Class<T> type) {
			for (Annotation annotation : annotations) {
				if (annotation.annotationType() == type) {
					return (T) annotation;
				}
			}
			return null;
		}
		
		/**
		 * Get all parameter annotations of this method, defined in all 
		 * implementations and interfaces of the methods declaring class.
		 * @return params
		 */
		public List<AnnotatedParam> getParams() {
			return params;
		}
	}
	
	/**
	 * AnnotatedParam describes all annotations of a parameter.
	 */
	public static class AnnotatedParam {
		private List<Annotation> annotations = new ArrayList<Annotation>();
		private Class<?> type = null;
		private Type genericType = null;
		
		private AnnotatedParam() {}
		
		private AnnotatedParam(Annotation[] annotations, Class<?> type, Type genericType) {
			this.type = type;
			this.genericType = genericType;
			
			merge(annotations);
		}
		
		private void merge(Annotation[] annotations) {
			// merge the annotations
			AnnotationUtil.merge(this.annotations, annotations);
		}

		/**
		 * Get all annotations of this parameter, defined in all implementations
		 * and interfaces of the class.
		 * @return annotations
		 */
		public List<Annotation> getAnnotations () {
			return annotations;
		}

		/**
		 * Get an annotation of this parameter by type. 
		 * Returns null if not available.
		 * @param annotationClass
		 * @return annotation
		 */
		@SuppressWarnings("unchecked")
		public <T> T getAnnotation (Class<T> type) {
			for (Annotation annotation : annotations) {
				if (annotation.annotationType() == type) {
					return (T) annotation;
				}
			}
			return null;
		}
		
		/**
		 * Get the type of the parameter
		 * @return type
		 */
		public Class<?> getType() {
			return type;
		}
		
		/**
		 * Get the generic type of the parameter
		 * @return genericType
		 */
		public Type getGenericType() {
			return genericType;
		}
	}

	/**
	 * Merge an array with annotations (listB) into a list with 
	 * annotations (listA)
	 * @param listA
	 * @param listB
	 */
	private static void merge(List<Annotation> listA, Annotation[] listB) {
		for (Annotation b : listB) {
			boolean found = false;
			for (Annotation a : listA) {
				if (a.getClass() == b.getClass()) {
					found = true;
					break;
				}
			}
			if (!found) {
				listA.add(b);
			}
		}
	}

	/**
	 * Merge an array of methods (listB) into a list with method 
	 * annotations (listA)
	 * @param listA
	 * @param listB
	 * @throws Exception 
	 */
	private static void merge(List<AnnotatedMethod> listA, Method[] listB) {
		for (Method b : listB) {
			AnnotatedMethod methodAnnotations = null;
			for (AnnotatedMethod a : listA) {
				if (equals(a.method, b)) {
					methodAnnotations = a;
					break;
				}
			}
			
			if (methodAnnotations != null) {
				methodAnnotations.merge(b);
			}
			else {
				listA.add(new AnnotatedMethod(b));
			}
		}
	}

	/**
	 * Test if two methods have equal names, return type, param count, 
	 * and param types
	 * @param a
	 * @param b
	 * @return
	 */
	private static boolean equals(Method a, Method b) {
		// http://stackoverflow.com/q/10062957/1262753
		if (!a.getName().equals(b.getName())) {
			return false;
		}
		if (a.getReturnType() != b.getReturnType()) {
			return false;
		}
		
		Class<?>[] paramsa = a.getParameterTypes();
        Class<?>[] paramsb = b.getParameterTypes();
        if (paramsa.length != paramsb.length) {
        	return false;
        }
        for (int i = 0; i < paramsa.length; i++) {
            if (paramsa[i] != paramsb[i]) {
                return false;
            }
        }
		
		return true;
	}
}
