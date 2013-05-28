package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
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

import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.almende.util.AnnotationUtil.AnnotatedParam;
import com.almende.util.NamespaceUtil.CallTuple;
import com.almende.util.NamespaceUtil;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONRPC {
	// static private Logger logger = Logger.getLogger(JSONRPC.class.getName());

	// TODO: the integration with requestParams is quite a mess.

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
	static public String invoke(Object destination, String request, JSONAuthorizor auth)
			throws JsonGenerationException, JsonMappingException, IOException {
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
	static public String invoke(Object destination, String request,
			RequestParams requestParams, JSONAuthorizor auth) throws JsonGenerationException,
			JsonMappingException, IOException {
		JSONRequest jsonRequest = null;
		JSONResponse jsonResponse = null;
		try {
			jsonRequest = new JSONRequest(request);
			jsonResponse = invoke(destination, jsonRequest, requestParams, auth);
		} catch (JSONRPCException err) {
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
	static public JSONResponse invoke(Object destination, JSONRequest request, JSONAuthorizor auth) {
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
	static public JSONResponse invoke(Object destination, JSONRequest request,
			RequestParams requestParams, JSONAuthorizor auth) {
		JSONResponse resp = new JSONResponse();
		resp.setId(request.getId());

		try {
			//TODO: Get real destination and method from NamespaceCache:
			CallTuple tuple = NamespaceUtil.get(destination,request.getMethod());
			Object realDest = tuple.destination;
			String realMethod = tuple.methodName;
			
			AnnotatedMethod annotatedMethod = getMethod(realDest,
					realMethod, requestParams,auth);
			if (annotatedMethod == null) {
				throw new JSONRPCException(
						JSONRPCException.CODE.METHOD_NOT_FOUND, "Method '"
								+ request.getMethod() + "' not found. The method does not exist or you are not authorized.");
			}

			Method method = annotatedMethod.getActualMethod();
			Object[] params = castParams(request.getParams(),
					annotatedMethod.getParams(), requestParams);
			Object result = method.invoke(realDest, params);
			if (result == null) {
				result = JOM.createNullNode();
			}
			resp.setResult(result);
		} catch (Exception err) {
			if (err instanceof JSONRPCException) {
				resp.setError((JSONRPCException) err);
			} else if (err.getCause() != null
					&& err.getCause() instanceof JSONRPCException) {
				resp.setError((JSONRPCException) err.getCause());
			} else {
				err.printStackTrace(); // TODO: cleanup printing stacktrace
				
				JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, getMessage(err));
				// TODO: return useful, readable stacktrace
				jsonError.setData(err);
				resp.setError(jsonError);
				
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
	static public List<String> validate(Class<?> c, RequestParams requestParams) {
		List<String> errors = new ArrayList<String>();
		Set<String> methodNames = new HashSet<String>();

		AnnotatedClass ac = null;
		try {
			ac = AnnotationUtil.get(c);
		} catch (Exception e) {
			e.printStackTrace();
			errors.add("Class as a whole can't be wrapped for annotation");
		}
		if (ac != null) {
			for (AnnotatedMethod method : ac.getMethods()) {
				boolean available = false;
				try {
					available = isAvailable(method, null, requestParams, null);
				} catch (Exception e) {
					e.printStackTrace();
					errors.add("Problems running isAvailable method on annotated class.");
				}
				if (available) {
					// The method name may only occur once
					String name = method.getName();
					if (methodNames.contains(name)) {
						errors.add("Public method '" + name
								+ "' is defined more than once, which is not"
								+ " allowed for JSON-RPC.");
					}
					methodNames.add(name);

					// each of the method parameters must have the @Name
					// annotation
					List<AnnotatedParam> params = method.getParams();
					for (int i = 0; i < params.size(); i++) {
						List<Annotation> matches = new ArrayList<Annotation>();
						for (Annotation a : params.get(i).getAnnotations()) {
							if (requestParams != null && requestParams.has(a)) {
								matches.add(a);
							} else if (a instanceof Name) {
								matches.add(a);
							}
						}

						if (matches.size() == 0) {
							errors.add("Parameter "
									+ i
									+ " in public method '"
									+ name
									+ "' is missing the @Name annotation, which is"
									+ " required for JSON-RPC.");
						} else if (matches.size() > 1) {
							String str = "";
							for (Annotation a : matches) {
								str += a + " ";
							}

							errors.add("Parameter " + i + " in public method '"
									+ name + "' contains " + matches.size()
									+ " annotations " + "(" + str
									+ "), but only one is allowed.");
						}
					}
				}
			}
		}
		return errors;
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
	public static List<Object> describe(Class<?> c,
			RequestParams requestParams, Boolean asString) {
		try {
			Map<String, Object> methods = new TreeMap<String, Object>();

			AnnotatedClass annotatedClass = AnnotationUtil.get(c);
			for (AnnotatedMethod method : annotatedClass.getMethods()) {
				if (isAvailable(method, null, requestParams, null)) {
					if (asString == null || asString != true) {
						// format as JSON
						List<Object> descParams = new ArrayList<Object>();
						for (AnnotatedParam param : method.getParams()) {
							if (getRequestAnnotation(param, requestParams) == null) {
								String name = getName(param);
								Map<String, Object> paramData = new HashMap<String, Object>();
								paramData.put("name", name);
								paramData.put("type",
										typeToString(param.getGenericType()));
								paramData.put("required", isRequired(param));
								descParams.add(paramData);
							}
						}

						Map<String, Object> result = new HashMap<String, Object>();
						result.put("type",
								typeToString(method.getGenericReturnType()));

						Map<String, Object> desc = new HashMap<String, Object>();
						desc.put("method", method.getName());
						desc.put("params", descParams);
						desc.put("result", result);
						methods.put(method.getName(), desc);
					} else {
						// format as string
						String p = "";
						for (AnnotatedParam param : method.getParams()) {
							if (getRequestAnnotation(param, requestParams) == null) {
								String name = getName(param);
								String type = typeToString(param
										.getGenericType());
								if (!p.isEmpty()) {
									p += ", ";
								}
								String ps = type + " " + name;
								p += isRequired(param) ? ps : ("[" + ps + "]");
							}
						}
						String desc = typeToString(method
								.getGenericReturnType())
								+ " "
								+ method.getName() + "(" + p + ")";
						methods.put(method.getName(), desc);
					}
				}
			}

			// create a sorted array
			List<Object> sortedMethods = new ArrayList<Object>();
			TreeSet<String> methodNames = new TreeSet<String>(methods.keySet());
			for (String methodName : methodNames) {
				sortedMethods.add(methods.get(methodName));
			}
			return sortedMethods;
		} catch (Exception e) {
			e.printStackTrace();
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
	private static String typeToString(Type c) {
		String s = c.toString();

		// replace full namespaces to short names
		int point = s.lastIndexOf(".");
		while (point >= 0) {
			int angle = s.lastIndexOf("<", point);
			int space = s.lastIndexOf(" ", point);
			int start = Math.max(angle, space);
			s = s.substring(0, start + 1) + s.substring(point + 1);
			point = s.lastIndexOf(".");
		}

		// remove modifiers like "class blabla" or "interface blabla"
		int space = s.indexOf(" ");
		int angle = s.indexOf("<", point);
		if (space >= 0 && (angle < 0 || angle > space)) {
			s = s.substring(space + 1);
		}

		return s;

		/*
		 * // TODO: do some more professional reflection... String s =
		 * c.getSimpleName();
		 * 
		 * // the following seems not to work TypeVariable<?>[] types =
		 * c.getTypeParameters(); if (types.length > 0) { s += "<"; for (int j =
		 * 0; j < types.length; j++) { TypeVariable<?> jj = types[j]; s +=
		 * jj.getName(); ... not working //s +=
		 * types[j].getClass().getSimpleName(); } s += ">"; }
		 */
	}

	/**
	 * Retrieve a description of an error
	 * 
	 * @param error
	 * @return message String with the error description of the cause
	 */
	static private String getMessage(Throwable error) {
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
	static private AnnotatedMethod getMethod(Object destination,
			String method, RequestParams requestParams, JSONAuthorizor auth) {
		AnnotatedClass annotatedClass;
		try {
			annotatedClass = AnnotationUtil.get(destination.getClass());

			List<AnnotatedMethod> methods = annotatedClass.getMethods(method);
			for (AnnotatedMethod m : methods) {
				if (isAvailable(m, destination, requestParams, auth)) {
					return m;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	static private Object[] castParams(Object params,
			List<AnnotatedParam> annotatedParams, RequestParams requestParams)
			throws Exception {
		ObjectMapper mapper = JOM.getInstance();

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
				Object[] objects = new Object[1];
				objects[0] = params;
				return objects;
			} else {
				ObjectNode paramsObject = (ObjectNode) params;

				Object[] objects = new Object[annotatedParams.size()];
				for (int i = 0; i < annotatedParams.size(); i++) {
					AnnotatedParam p = annotatedParams.get(i);

					Annotation a = getRequestAnnotation(p, requestParams);
					if (a != null) {
						// this is a systems parameter
						objects[i] = requestParams.get(a);
					} else {
						String name = getName(p);
						if (name != null) {
							// this is a named parameter
							if (paramsObject.has(name)) {
								objects[i] = mapper.convertValue(
										paramsObject.get(name), p.getType());
							} else {
								if (isRequired(p)) {
									throw new Exception("Required parameter '"
											+ name + "' missing");
								}
								// else if (paramType.getSuperclass() == null) {
								else if (p.getType().isPrimitive()) {
									throw new Exception("Parameter '" + name
											+ "' cannot be both optional and "
											+ "a primitive type ("
											+ p.getType().getSimpleName() + ")");
								} else {
									objects[i] = null;
								}
							}
						} else {
							// this is a problem
							throw new Exception("Name of parameter " + i
									+ " not defined");
						}
					}
				}
				return objects;
			}
		} else {
			throw new Exception("params must be a JSONObject");
		}
	}

	/**
	 * Create a JSONRequest from a java method and arguments
	 * 
	 * @param method
	 * @param args
	 * @return
	 */
	public static JSONRequest createRequest(Method method, Object[] args) {
		AnnotatedMethod annotatedMethod = null;
		try {
			annotatedMethod = new AnnotationUtil.AnnotatedMethod(method);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Method '" + method.getName()
					+ "' can't be used as annotated method.");
		}
		List<AnnotatedParam> annotatedParams = annotatedMethod.getParams();

		ObjectNode params = JOM.createObjectNode();

		for (int i = 0; i < annotatedParams.size(); i++) {
			AnnotatedParam annotatedParam = annotatedParams.get(i);
			if (i < args.length && args[i] != null) {
				String name = getName(annotatedParam);
				if (name != null) {
					JsonNode paramValue = JOM.getInstance().convertValue(
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

		return new JSONRequest(method.getName(), params);
	}

	public static boolean hasPrivate(Class<?> clazz) throws SecurityException, Exception{
		AnnotatedClass annotated = AnnotationUtil.get(clazz);
		for (Annotation anno: annotated.getAnnotations()){
			if (anno.annotationType().equals(Access.class) && ((Access)anno).value() == AccessType.PRIVATE) return true;
			if (anno.annotationType().equals(Sender.class)) return true;
		}
		return false;
	}
	/**
	 * Check whether a method is available for JSON-RPC calls. This is the case
	 * when it is public, has named parameters, and has no annotation
	 * 
	 * @Access(UNAVAILABLE)
	 * 
	 * @param annotatedMethod
	 * @param requestParams
	 * @return available
	 * @throws Exception 
	 * @throws SecurityException 
	 */
	private static boolean isAvailable(AnnotatedMethod method, Object destination,
			RequestParams requestParams, JSONAuthorizor auth) throws SecurityException, Exception {

		int mod = method.getActualMethod().getModifiers(); 
		
		Access MethodAccess = method.getAnnotation(Access.class);
		if (destination != null && !method.getActualMethod().getDeclaringClass().isAssignableFrom(destination.getClass()))
			return false;
		if (!(Modifier.isPublic(mod) && hasNamedParams(method, requestParams)))
			return false;

		Access ClassAccess = AnnotationUtil.get(destination != null?destination.getClass():method.getActualMethod().getDeclaringClass())
				.getAnnotation(Access.class);
		if (MethodAccess == null)
			MethodAccess = ClassAccess;
		if (MethodAccess == null)
			return false; //New default: UNAVAILABLE!
		
		if (MethodAccess.value() == AccessType.UNAVAILABLE)
			return false;
		if (MethodAccess.value() == AccessType.PRIVATE) {
			return auth!= null?auth.onAccess((String) requestParams.get(Sender.class),
							MethodAccess.tag()):false;
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
	private static boolean hasNamedParams(AnnotatedMethod method,
			RequestParams requestParams) {
		for (AnnotatedParam param : method.getParams()) {
			boolean found = false;
			for (Annotation a : param.getAnnotations()) {
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
	private static boolean isRequired(AnnotatedParam param) {
		boolean required = true;
		Required requiredAnnotation = param.getAnnotation(Required.class);
		if (requiredAnnotation != null) {
			required = requiredAnnotation.value();
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
	private static String getName(AnnotatedParam param) {
		String name = null;
		Name nameAnnotation = param.getAnnotation(Name.class);
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
	private static Annotation getRequestAnnotation(AnnotatedParam param,
			RequestParams requestParams) {
		for (Annotation annotation : param.getAnnotations()) {
			if (requestParams != null && requestParams.has(annotation)) {
				return annotation;
			}
		}
		return null;
	}
}