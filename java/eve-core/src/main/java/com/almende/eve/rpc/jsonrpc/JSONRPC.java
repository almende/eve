package com.almende.eve.rpc.jsonrpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.agent.annotation.Access;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONRPC {
	//static private Logger logger = Logger.getLogger(JSONRPC.class.getName());

	// TODO: implement JSONRPC 2.0 Batch
	
	/**
	 * Invoke a method on an object
	 * @param obj     Request will be invoked on the given object
	 * @param request A request in JSON-RPC format
	 * @return
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	static public String invoke (Object object, String request) 
			throws JsonGenerationException, JsonMappingException, IOException {
		JSONRequest jsonRequest = null;
		JSONResponse jsonResponse = null;
		try {
			jsonRequest = new JSONRequest(request);
			jsonResponse = invoke(object, jsonRequest);
		}
		catch (JSONRPCException err) {
			jsonResponse = new JSONResponse(err);
		}
		
		return jsonResponse.toString();
	}

	/**
	 * Invoke a method on an object
	 * @param obj     Request will be invoked on the given object
	 * @param request A request in JSON-RPC format
	 * @return
	 */
	static public JSONResponse invoke (Object object, JSONRequest request) {
		JSONResponse resp = new JSONResponse(); 
		resp.setId(request.getId());

		try {
			MethodType methodType = getMethod(object.getClass(), request.getMethod());
			if (methodType == null) {
				throw new JSONRPCException(
						JSONRPCException.CODE.METHOD_NOT_FOUND, 
						"Method '" + request.getMethod() + "' not found");
			}
			
			Method method = methodType.method;
			
			
			Object[] params = castParams(request.getParams(), methodType.params);
			Object result = method.invoke(object, params);
			if (result == null) {
				result = JOM.createNullNode();
			}
			resp.setResult(result);
		}
		catch (Exception err) {
			if (err instanceof JSONRPCException) {
				resp.setError((JSONRPCException) err);
			}
			else if (err.getCause() != null && 
					err.getCause() instanceof JSONRPCException) {
				resp.setError((JSONRPCException) err.getCause());
			}
			else {
				JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.INTERNAL_ERROR, getMessage(err));
				resp.setError(jsonError);
			}
		}
		
		return resp;
	}
	
	/**
	 * Validate whether the given class contains valid JSON-RPC methods.
	 * A class if valid when:<br>
	 * - There are no public methods with equal names<br>
	 * - The parameters of all public methods have the @Name annotation<br>
	 * If the class is not valid, an Exception is thrown
	 * @param c         The class to be verified
	 * @return errors   A list with validation errors. When no problems are 
	 *                   found, an empty list is returned 
	 */
	static public List<String> validate (Class<?> c) {
		List<String> errors = new ArrayList<String>();
		
		while (c != null && c != Object.class) {
			Set<String> methodNames = new HashSet<String>();
			for (Method method : c.getDeclaredMethods()) {
				boolean available = isAvailable(method);				
				if (available) {
					// The method name may only occur once
					String name = method.getName();
					if (methodNames.contains(name)) {
						errors.add("Public method '" + name + 
							"' is defined more than once, which is not" + 
							" allowed for JSON-RPC (Class " + c.getName() + ")");
					}
					methodNames.add(name);
					
					// each of the method parameters must have the @Name 
					// annotation
					Annotation[][] annotations = method.getParameterAnnotations();
					for(int i = 0; i < annotations.length; i++){
						boolean hasNameAnnotation = false;
						for(Annotation annotation : annotations[i]){
							if(annotation instanceof Name){
								hasNameAnnotation = true;
							}
						}
						if (!hasNameAnnotation) {
							errors.add("Parameter " + i + " in public method '" + name + 
								"' is missing the @Name annotation, which is" + 
								" required for JSON-RPC (Class " + c.getName() + ")");
						}
					}					
				}
			}
			
			// continue search in the super class 
			c = c.getSuperclass();
		}
		
		return errors;
	}
	
	/**
	 * Describe all JSON-RPC methods of given class
	 * @param c      The class to be described
	 * @param asJSON If true, the described methods will be in an easy to parse
	 *                JSON structure. If false, the returned description will
	 *                be in human readable format.
	 * @return
	 */
	public static List<Object> describe(Class<?> c, Boolean asJSON) {
		try {
			Map<String, Object> methods = new TreeMap<String, Object>();
			if (asJSON == null) {
				asJSON = false;
			}
	
			while (c != null && c != Object.class) {
				for (Method method : c.getDeclaredMethods()) {
					String methodName = method.getName();
					boolean available = isAvailable(method);
					if (available) {
						ParamType[] params = getParams(method);
						boolean validParamNames = true;
						for (ParamType param: params) {
							if (param.name == null) {
								validParamNames = false;
								break;
							}
						}
						
						// TODO: not so nice 
						if (!validParamNames) {
							if (params.length == 1 && params[0].type.equals(ObjectNode.class)) {
								params[0].name = "params";
								validParamNames = true;
							}
						}
						
						// get return type
						String returnType = typeToString(method.getGenericReturnType());
						
						if (validParamNames) {
							if (asJSON) {
								// format as JSON
								List<Object> descParams = new ArrayList<Object>();
								for(int i = 0; i < params.length; i++){
									Map<String, Object> paramData = new HashMap<String, Object>();
									paramData.put("name", params[i].name);
									paramData.put("type", typeToString(params[i].genericType));
									paramData.put("required", params[i].required);
									descParams.add(paramData);
								}
								
								Map<String, Object> result = new HashMap<String, Object>(); 
								result.put("type", returnType);
								
								Map<String, Object> desc = new HashMap<String, Object>();
								desc.put("method", methodName);
								desc.put("params", descParams);
								desc.put("result", result);
								methods.put(methodName, desc);
							}
							else {
								// format as string
								String p = "";
								for(int i = 0; i < params.length; i++){
									if (!p.isEmpty()) {
										p += ", ";
									}
									if (params[i].required) {
										p += typeToString(params[i].genericType) + " " + params[i].name;
									}
									else {
										p += "[" + typeToString(params[i].genericType) + " " + params[i].name + "]";
									}
								}
								String desc = returnType + " " + methodName + "(" + p + ")";
								methods.put(methodName, desc);							
							}
						}
					}
				}
	
				c = c.getSuperclass();
			}
	
			// create a sorted array
			List<Object> sortedMethods = new ArrayList<Object>();
			TreeSet<String> methodNames = new TreeSet<String>(methods.keySet());
			for (String methodName : methodNames) { 
			   sortedMethods.add(methods.get(methodName));
			   // do something
			}
			return sortedMethods;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get type description from a class. Returns for example "String" or 
	 * "List<String>".
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
		// TODO: do some more professional reflection...
		String s = c.getSimpleName();	

		// the following seems not to work
		TypeVariable<?>[] types = c.getTypeParameters();
		if (types.length > 0) {
			s += "<";
			for (int j = 0; j < types.length; j++) {
				TypeVariable<?> jj = types[j];
				s += jj.getName();
				 ... not working
				//s += types[j].getClass().getSimpleName();
			}
			s += ">";
		}
		*/
	}
	
	/**
	 * Try to retrieve the message description of an error
	 * @param error
	 * @return message  String with the error description, or null if not found.
	 */
	static private String getMessage(Throwable error) {
		String message = error.getMessage();
		if (message == null && error.getCause() != null) {
			message = error.getCause().getMessage();
		}
		return message;
	}
	
	/**
	 * Find a method by name, 
	 * which is available for JSON-RPC, and has named parameters
	 * @param objectClass
	 * @param method
	 * @return methodType   meta information on the method, or null if not found
	 */
	static private MethodType getMethod(Class<?> objectClass, String method) {
		Class<?> c = objectClass;
		while (c != null && c != Object.class) {
			for (Method m : c.getMethods()) {
				if (m.getName().equals(method) && isAvailable(m)) {
					MethodType meta = new MethodType();
					meta.method = m;
					meta.params = getParams(m);
					return meta;
				}
			}
			
			// continue search in the super class 
			c = c.getSuperclass();
		}
		
		return null;
	}

	/**
	 * Cast a JSONArray or JSONObject params to the desired paramTypes 
	 * @param params
	 * @param paramTypes
	 * @return 
	 * @throws Exception 
	 */
	static private Object[] castParams(Object params, ParamType[] paramTypes) 
			throws Exception {
		ObjectMapper mapper = JOM.getInstance();

		if (paramTypes.length == 0) {
			return new Object[0];
		}
		
		if (params instanceof ObjectNode) {
			// JSON-RPC 2.0 with named parameters in a JSONObject

			// check whether all method parameters are named
			boolean hasParamNames = false;
			for (ParamType param : paramTypes) {
				if (param.name != null) {
					hasParamNames = true;
					break;
				}
			}
			
			if (hasParamNames) {
				ObjectNode paramsObject = (ObjectNode)params;
				
				Object[] objects = new Object[paramTypes.length];
				for (int i = 0; i < paramTypes.length; i++) {
					ParamType p = paramTypes[i];
					if (p.name == null) {
						throw new Exception("Name of parameter " + i + " not defined");
					}
					if (paramsObject.has(p.name)) {
						objects[i] = mapper.convertValue(paramsObject.get(p.name), p.type);
					}
					else {
						if (p.required) {
							throw new Exception("Required parameter '" + 
									p.name + "' missing");
						}
						//else if (paramType.getSuperclass() == null) {
						else if (p.type.isPrimitive()) {
							throw new Exception(
									"Parameter '" + p.name +
									"' cannot be both optional and " +
									"a primitive type (" +
									p.type.getSimpleName() + ")");
						}
						else {
							objects[i] = null;
						}
					}
				}
				return objects;
			}
			else if (paramTypes.length == 1 && 
					paramTypes[0].type.equals(ObjectNode.class)) {
				// the method expects one parameter of type JSONObject
				// feed the params object itself to it.
				Object[] objects = new Object[1];
				objects[0] = params;
				return objects;
			}
			else {
				throw new Exception("Names of parameters are undefined");
			}
		}
		else {
			throw new Exception("params must be a JSONObject");
		}
	}

	/**
	 * Create a JSONRequest from a java method and arguments
	 * @param method
	 * @param args
	 * @return
	 */
	public static JSONRequest createRequest(Method method, Object[] args) {
		ParamType[] methodParams = getParams(method);
		
		ObjectNode params = JOM.createObjectNode();
		
		for(int i = 0; i < methodParams.length; i++) {
			if (i < args.length && args[i] != null) {
				if (methodParams[i].name != null) {
					JsonNode paramValue = JOM.getInstance().convertValue(args[i], 
							JsonNode.class);
					params.put(methodParams[i].name, paramValue);
				}
				else {
					throw new IllegalArgumentException(
							"Parameter " + i + " in method '" + method.getName() + 
							"' is missing the @Name annotation.");
				}
			}
			else if (methodParams[i].required) {
				throw new IllegalArgumentException(
						"Required parameter " + i + " in method '" + method.getName() + 
						"' is null.");
			}
		}
		
		return new JSONRequest(method.getName(), params);		
	}
	
	/**
	 * Get a description of a methods parameters
	 * @param method
	 * @return params
	 */
	private static ParamType[] getParams (Method method) {
		Class<?>[] types = method.getParameterTypes();
		Type[] genericTypes = method.getGenericParameterTypes();
		int paramNum = Math.min(types.length, genericTypes.length);

		ParamType[] params = new ParamType[paramNum];
		
		boolean annotationsComplete = true;
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		for(int i = 0; i < paramNum; i++){
			params[i] = new ParamType();
			params[i].type = types[i];
			params[i].genericType = genericTypes[i];
			
			Annotation[] annotations = paramAnnotations[i];
			for(Annotation annotation : annotations) {
				if(annotation instanceof Name){
					params[i].name = ((Name) annotation).value();
				}
				if(annotation instanceof Required){
					params[i].required = ((Required) annotation).value();
				}
			}
			
			if (params[i].name == null) {
				annotationsComplete = false;
			}
		}

		if (!annotationsComplete) {
			// when @Name annotations are missing, 
			// search for them in the interfaces of the method
			Class<?>[] interfaces = method.getDeclaringClass().getInterfaces();
			for (Class<?> i : interfaces) {
				MethodType m = getMethod(i, method.getName());
				if (m != null && m.hasNamedParameters()) {
					params = m.params;
					break;
				}
			}
		}
		
		return params;
	}
	
	/**
	 * Check whether a method is available for JSON-RPC calls. This is the
	 * case when it is public, and has no annotation @Access(UNAVAILABLE)
	 * @param method
	 * @return available
	 */
	private static boolean isAvailable(Method method) {
		int mod = method.getModifiers();
		Access access = method.getAnnotation(Access.class); 
		return Modifier.isPublic(mod) &&
				(access == null || 
				(access.value() != AccessType.UNAVAILABLE &&
				 access.visible()));
	}
	
	/**
	 * Helper class to describe a method parameter
	 */
	private static class ParamType {
		String name = null;
		boolean required = true;
		Class<?> type = null;
		Type genericType = null;
	}
	
	/**
	 * Helper class to describe a method
	 */
	private static class MethodType {
		Method method = null;
		ParamType[] params = null;
		
		boolean hasNamedParameters() {
			for (ParamType param : params) {
				if (param.name == null) {
					return false;
				}
			}
			return true;
		}
	}
}