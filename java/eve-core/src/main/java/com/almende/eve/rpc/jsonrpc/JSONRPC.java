package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.almende.util.AnnotationUtil.AnnotatedParam;
import com.almende.util.NamespaceUtil;
import com.almende.util.NamespaceUtil.CallTuple;
import com.almende.util.TypeUtil;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JSONRPC {
	private JSONRPC() {
	}
	
	private static Logger	logger	= Logger.getLogger(JSONRPC.class.getName());
	
	// TODO: implement JSONRPC 2.0 Batch
	/**
	 * Invoke a method on an object
	 * 
	 * @param obj
	 *            Request will be invoked on the given object
	 * @param request
	 *            A request in JSON-RPC format
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	public static String invoke(final Object destination, final String request,
			final JSONAuthorizor auth) throws IOException {
		return invoke(destination, request, null, auth);
	}
	
	/**
	 * Invoke a method on an object
	 * 
	 * @param obj
	 *            Request will be invoked on the given object
	 * @param request
	 *            A request in JSON-RPC format
	 * @param requestParams
	 *            Optional request parameters
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonGenerationException
	 */
	public static String invoke(final Object destination, final String request,
			final RequestParams requestParams, final JSONAuthorizor auth)
			throws IOException {
		JSONRequest jsonRequest = null;
		JSONResponse jsonResponse = null;
		try {
			jsonRequest = new JSONRequest(request);
			jsonResponse = invoke(destination, jsonRequest, requestParams, auth);
		} catch (final JSONRPCException err) {
			jsonResponse = new JSONResponse(err);
		}
		
		return jsonResponse.toString();
	}
	
	/**
	 * Invoke a method on an object
	 * 
	 * @param destination
	 *            destination url
	 * @param obj
	 *            will be invoked on the given object
	 * @return
	 */
	public static JSONResponse invoke(final Object destination, final JSONRequest request,
			final JSONAuthorizor auth) {
		return invoke(destination, request, null, auth);
	}
	
	/**
	 * Invoke a method on an object
	 * 
	 * @param obj
	 *            Request will be invoked on the given object
	 * @param request
	 *            A request in JSON-RPC format
	 * @param requestParams
	 *            Optional request parameters
	 * @return
	 */
	public static JSONResponse invoke(final Object destination, final JSONRequest request,
			final RequestParams requestParams, final JSONAuthorizor auth) {
		final JSONResponse resp = new JSONResponse(request.getId(), null);
		try {
			final CallTuple tuple = NamespaceUtil.get(destination,
					request.getMethod());
			final Object realDest = tuple.getDestination();
			final String realMethod = tuple.getMethodName();
			
			final AnnotatedMethod annotatedMethod = getMethod(realDest,
					realMethod, requestParams, auth);
			if (annotatedMethod == null) {
				throw new JSONRPCException(
						JSONRPCException.CODE.METHOD_NOT_FOUND,
						"Method '"
								+ request.getMethod()
								+ "' not found. The method does not exist or you are not authorized.");
			}
			
			final Method method = annotatedMethod.getActualMethod();
			final Object[] params = castParams(request.getParams(),
					annotatedMethod.getParams(), requestParams);
			Object result = method.invoke(realDest, params);
			if (result == null) {
				result = JOM.createNullNode();
			}
			resp.setResult(result);
		} catch (final JSONRPCException err) {
			resp.setError(err);
		} catch (final Exception err) {
			final Throwable cause = err.getCause();
			if (cause instanceof JSONRPCException) {
				resp.setError((JSONRPCException) cause);
			} else {
				if (err instanceof InvocationTargetException && cause != null) {
					logger.log(Level.WARNING,
							"Exception raised, returning its cause as JSONRPCException. Request:"
									+ request, cause);
					
					final JSONRPCException jsonError = new JSONRPCException(
							JSONRPCException.CODE.INTERNAL_ERROR,
							getMessage(cause), cause);
					jsonError.setData(cause);
					resp.setError(jsonError);
				} else {
					logger.log(Level.WARNING,
							"Exception raised, returning it as JSONRPCException. Request:"
									+ request, err);
					
					final JSONRPCException jsonError = new JSONRPCException(
							JSONRPCException.CODE.INTERNAL_ERROR,
							getMessage(err), err);
					jsonError.setData(err);
					resp.setError(jsonError);
				}
			}
		}
		
		return resp;
	}
	
	/**
	 * Validate whether the given class contains valid JSON-RPC methods. A class
	 * if valid when:<br>
	 * - There are no public methods with equal names<br>
	 * - The parameters of all public methods have the @Name annotation<br>
	 * If the class is not valid, an Exception is thrown
	 * 
	 * @param c
	 *            The class to be verified
	 * @param requestParams
	 *            optional request parameters
	 * @return errors A list with validation errors. When no problems are found,
	 *         an empty list is returned
	 */
	public static List<String> validate(final Class<?> c, final RequestParams requestParams) {
		final List<String> errors = new ArrayList<String>();
		final Set<String> methodNames = new HashSet<String>();
		
		AnnotatedClass ac = null;
		try {
			ac = AnnotationUtil.get(c);
			if (ac != null) {
				for (final AnnotatedMethod method : ac.getMethods()) {
					final boolean available = isAvailable(method, null,
							requestParams, null);
					if (available) {
						// The method name may only occur once
						final String name = method.getName();
						if (methodNames.contains(name)) {
							errors.add("Public method '"
									+ name
									+ "' is defined more than once, which is not"
									+ " allowed for JSON-RPC.");
						}
						methodNames.add(name);
						
						// TODO: I removed duplicate @Name check. If you reach
						// this point the function at least has named
						// parameters, due to the isAvailable() call. Should we
						// add a duplicates check to isAvailable()?
					}
				}
			}
		} catch (final Exception e) {
			logger.log(Level.WARNING, "Problems wrapping class for annotation",
					e);
			errors.add("Class can't be wrapped for annotation, exception raised:"
					+ e.getLocalizedMessage());
		}
		return errors;
	}
	
	private static Map<String, Object> _describe(final Object c,
			final RequestParams requestParams, final String namespace) {
		final Map<String, Object> methods = new TreeMap<String, Object>();
		try {
			if (c == null) {
				return methods;
			}
			final AnnotatedClass annotatedClass = AnnotationUtil.get(c.getClass());
			for (final AnnotatedMethod method : annotatedClass.getMethods()) {
				if (isAvailable(method, null, requestParams, null)) {
					// format as JSON
					final List<Object> descParams = new ArrayList<Object>();
					for (final AnnotatedParam param : method.getParams()) {
						if (getRequestAnnotation(param, requestParams) == null) {
							final String name = getName(param);
							final Map<String, Object> paramData = new HashMap<String, Object>();
							paramData.put("name", name);
							paramData.put("type",
									typeToString(param.getGenericType()));
							paramData.put("required", isRequired(param));
							descParams.add(paramData);
						}
					}
					
					final Map<String, Object> result = new HashMap<String, Object>();
					result.put("type",
							typeToString(method.getGenericReturnType()));
					
					final Map<String, Object> desc = new HashMap<String, Object>();
					final String methodName = namespace.equals("") ? method.getName()
							: namespace + "." + method.getName();
					desc.put("method", methodName);
					desc.put("params", descParams);
					desc.put("result", result);
					methods.put(methodName, desc);
				}
			}
			for (final AnnotatedMethod method : annotatedClass
					.getAnnotatedMethods(Namespace.class)) {
				final String innerNamespace = method.getAnnotation(Namespace.class)
						.value();
				methods.putAll(_describe(
						method.getActualMethod().invoke(c, (Object[]) null),
						requestParams, innerNamespace));
			}
		} catch (final Exception e) {
			logger.log(Level.WARNING,
					"Failed to describe class:" + c.toString(), e);
			return null;
		}
		return methods;
	}
	
	/**
	 * Describe all JSON-RPC methods of given class
	 * 
	 * @param c
	 *            The class to be described
	 * @param requestParams
	 *            Optional request parameters.
	 * @param asString
	 *            If false (default), the returned description is a JSON
	 *            structure. If true, the described methods will be in an easy
	 *            to read string.
	 * @return
	 */
	public static List<Object> describe(final Object c, final RequestParams requestParams) {
		try {
			final Map<String, Object> methods = _describe(c, requestParams, "");
			
			// create a sorted array
			final List<Object> sortedMethods = new ArrayList<Object>();
			final TreeSet<String> methodNames = new TreeSet<String>(methods.keySet());
			for (final String methodName : methodNames) {
				sortedMethods.add(methods.get(methodName));
			}
			return sortedMethods;
		} catch (final Exception e) {
			logger.log(Level.WARNING,
					"Failed to describe class:" + c.toString(), e);
			return null;
		}
	}
	
	/**
	 * Get type description from a class. Returns for example "String" or
	 * "List<String>".
	 * 
	 * @param c
	 * @return
	 */
	private static String typeToString(final Type c) {
		String s = c.toString();
		
		// replace full namespaces to short names
		int point = s.lastIndexOf('.');
		while (point >= 0) {
			final int angle = s.lastIndexOf('<', point);
			final int space = s.lastIndexOf(' ', point);
			final int start = Math.max(angle, space);
			s = s.substring(0, start + 1) + s.substring(point + 1);
			point = s.lastIndexOf('.');
		}
		
		// remove modifiers like "class blabla" or "interface blabla"
		final int space = s.indexOf(' ');
		final int angle = s.indexOf('<', point);
		if (space >= 0 && (angle < 0 || angle > space)) {
			s = s.substring(space + 1);
		}
		
		return s;
	}
	
	/**
	 * Retrieve a description of an error
	 * 
	 * @param error
	 * @return message String with the error description of the cause
	 */
	private static String getMessage(final Throwable error) {
		Throwable cause = error;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause.toString();
	}
	
	/**
	 * Find a method by name, which is available for JSON-RPC, and has named
	 * parameters
	 * 
	 * @param destination
	 * @param method
	 * @param requestParams
	 * @return methodType meta information on the method, or null if not found
	 */
	private static AnnotatedMethod getMethod(final Object destination, final String method,
			final RequestParams requestParams, final JSONAuthorizor auth) {
		AnnotatedClass annotatedClass;
		try {
			annotatedClass = AnnotationUtil.get(destination.getClass());
			
			final List<AnnotatedMethod> methods = annotatedClass.getMethods(method);
			for (final AnnotatedMethod m : methods) {
				if (isAvailable(m, destination, requestParams, auth)) {
					return m;
				}
			}
		} catch (final Exception e) {
			logger.log(Level.WARNING, "GetMethod failed:", e);
		}
		return null;
	}
	
	/**
	 * Cast a JSONArray or JSONObject params to the desired paramTypes
	 * 
	 * @param params
	 * @param paramTypes
	 * @param requestParams
	 * @return
	 * @throws Exception
	 */
	private static Object[] castParams(final Object params,
			final List<AnnotatedParam> annotatedParams, final RequestParams requestParams) {
		
		if (annotatedParams.size() == 0) {
			return new Object[0];
		}
		
		if (params instanceof ObjectNode) {
			// JSON-RPC 2.0 with named parameters in a JSONObject
			
			if (annotatedParams.size() == 1
					&& annotatedParams.get(0).getType()
							.equals(ObjectNode.class)
					&& annotatedParams.get(0).getAnnotations().size() == 0) {
				
				// the method expects one parameter of type JSONObject
				// feed the params object itself to it.
				final Object[] objects = new Object[1];
				objects[0] = params;
				return objects;
			} else {
				final ObjectNode paramsObject = (ObjectNode) params;
				
				final Object[] objects = new Object[annotatedParams.size()];
				for (int i = 0; i < annotatedParams.size(); i++) {
					final AnnotatedParam p = annotatedParams.get(i);
					
					final Annotation a = getRequestAnnotation(p, requestParams);
					if (a != null) {
						// this is a systems parameter
						objects[i] = requestParams.get(a);
					} else {
						final String name = getName(p);
						if (name != null) {
							// this is a named parameter
							if (paramsObject.has(name)) {
								objects[i] = TypeUtil.inject(
										paramsObject.get(name),
										p.getGenericType());
							} else {
								if (isRequired(p)) {
									throw new ClassCastException(
											"Required parameter '" + name
													+ "' missing.");
								} else if (p.getType().isPrimitive()) {
									// TODO: should this test be moved to
									// isAvailable()?
									throw new ClassCastException("Parameter '"
											+ name
											+ "' cannot be both optional and "
											+ "a primitive type ("
											+ p.getType().getSimpleName() + ")");
								} else {
									objects[i] = null;
								}
							}
						} else {
							// this is a problem
							throw new ClassCastException("Name of parameter "
									+ i + " not defined");
						}
					}
				}
				return objects;
			}
		} else {
			throw new ClassCastException("params must be a JSONObject");
		}
	}
	
	/**
	 * Create a JSONRequest from a java method and arguments
	 * 
	 * @param method
	 * @param args
	 * @return
	 */
	public static JSONRequest createRequest(final Method method, final Object[] args) {
		AnnotatedMethod annotatedMethod = null;
		try {
			annotatedMethod = new AnnotationUtil.AnnotatedMethod(method);
		} catch (final Exception e) {
			logger.log(Level.WARNING,
					"Method can't be used as annotated method", e);
			throw new IllegalArgumentException("Method '" + method.getName()
					+ "' can't be used as annotated method.", e);
		}
		final List<AnnotatedParam> annotatedParams = annotatedMethod.getParams();
		
		final ObjectNode params = JOM.createObjectNode();
		
		for (int i = 0; i < annotatedParams.size(); i++) {
			final AnnotatedParam annotatedParam = annotatedParams.get(i);
			if (i < args.length && args[i] != null) {
				final String name = getName(annotatedParam);
				if (name != null) {
					final JsonNode paramValue = JOM.getInstance().convertValue(
							args[i], JsonNode.class);
					params.put(name, paramValue);
				} else {
					throw new IllegalArgumentException("Parameter " + i
							+ " in method '" + method.getName()
							+ "' is missing the @Name annotation.");
				}
			} else if (isRequired(annotatedParam)) {
				throw new IllegalArgumentException("Required parameter " + i
						+ " in method '" + method.getName() + "' is null.");
			}
		}
		JsonNode id = null;
		try {
			id = JOM.getInstance().readTree(new UUID().toString());
		} catch (final Exception e) {
		}
		return new JSONRequest(id, method.getName(), params);
	}
	
	/**
	 * Check whether a method is available for JSON-RPC calls. This is the case
	 * when it is public, has named parameters, and has a public or private @Access
	 * annotation
	 * 
	 * @param annotatedMethod
	 * @param requestParams
	 * @return available
	 */
	private static boolean isAvailable(final AnnotatedMethod method,
			final Object destination, final RequestParams requestParams, final JSONAuthorizor auth) {
		
		final int mod = method.getActualMethod().getModifiers();
		
		Access methodAccess = method.getAnnotation(Access.class);
		if (destination != null
				&& !method.getActualMethod().getDeclaringClass()
						.isAssignableFrom(destination.getClass())) {
			return false;
		}
		if (!(Modifier.isPublic(mod) && hasNamedParams(method, requestParams))) {
			return false;
		}
		
		final Access classAccess = AnnotationUtil.get(
				destination != null ? destination.getClass() : method
						.getActualMethod().getDeclaringClass()).getAnnotation(
				Access.class);
		if (methodAccess == null) {
			methodAccess = classAccess;
		}
		if (methodAccess == null) {
			// New default: UNAVAILABLE!
			return false;
		}
		if (methodAccess.value() == AccessType.UNAVAILABLE) {
			return false;
		}
		
		if (methodAccess.value() == AccessType.PRIVATE) {
			return auth != null ? auth.onAccess(requestParams.get(Sender.class)
					.toString(), methodAccess.tag()) : false;
		}
		if (methodAccess.value() == AccessType.SELF) {
			return auth != null ? auth.isSelf(requestParams.get(Sender.class)
					.toString()) : false;
		}
		return true;
	}
	
	/**
	 * Test whether a method has named parameters
	 * 
	 * @param annotatedMethod
	 * @param requestParams
	 * @return hasNamedParams
	 */
	private static boolean hasNamedParams(final AnnotatedMethod method,
			final RequestParams requestParams) {
		for (final AnnotatedParam param : method.getParams()) {
			boolean found = false;
			for (final Annotation a : param.getAnnotations()) {
				if (requestParams != null && requestParams.has(a)) {
					found = true;
					break;
				} else if (a instanceof Name) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Test if a parameter is required Reads the parameter annotation @Required.
	 * Returns True if the annotation is not provided.
	 * 
	 * @param param
	 * @return required
	 */
	@SuppressWarnings("deprecation")
	private static boolean isRequired(final AnnotatedParam param) {
		boolean required = true;
		final com.almende.eve.rpc.annotation.Required requiredAnnotation = param
				.getAnnotation(com.almende.eve.rpc.annotation.Required.class);
		if (requiredAnnotation != null) {
			required = requiredAnnotation.value();
		}
		if (param.getAnnotation(Optional.class) != null) {
			required = false;
		}
		return required;
	}
	
	/**
	 * Get the name of a parameter Reads the parameter annotation @Name. Returns
	 * null if the annotation is not provided.
	 * 
	 * @param param
	 * @return name
	 */
	private static String getName(final AnnotatedParam param) {
		String name = null;
		final Name nameAnnotation = param.getAnnotation(Name.class);
		if (nameAnnotation != null) {
			name = nameAnnotation.value();
		}
		return name;
	}
	
	/**
	 * Find a request annotation in the given parameters Returns null if no
	 * system annotation is not found
	 * 
	 * @param param
	 * @param requestParams
	 * @return annotation
	 */
	private static Annotation getRequestAnnotation(final AnnotatedParam param,
			final RequestParams requestParams) {
		for (final Annotation annotation : param.getAnnotations()) {
			if (requestParams != null && requestParams.has(annotation)) {
				return annotation;
			}
		}
		return null;
	}
	
	/**
	 * Check if given json object contains all fields required for a
	 * json-rpc request (id, method, params)
	 * 
	 * @param json
	 * @return
	 */
	public static boolean isRequest(final ObjectNode json) {
		return json.has("method");
	}
	
	/**
	 * Check if given json object contains all fields required for a
	 * json-rpc response (id, result or error)
	 * 
	 * @param json
	 * @return
	 */
	public static boolean isResponse(final ObjectNode json) {
		return (json.has("result") || json.has("error"));
	}
	
}