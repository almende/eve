package com.almende.eve.json;

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
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
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
		if (request.hasCallback()) {
			System.out.println("WARNING: JSON-RPC async is not yet implemented");
			
			/* TODO: implement async json-rpc, as soon as a schedular is available
			if (isJSON_RPC_2) {
				// return callback in JSON-RPC 2.0 format
				send(callbackUrl, callbackMethod, resp);
			}
			else {
				// return callback in JSON-RPC 1.0 format
				JSONArray params = createJSONArray(result);
				send(callbackUrl, callbackMethod, params);
			}
			*/
		}
		
		JSONResponse resp = new JSONResponse(); 
		resp.setId(request.getId());

		try {
			Method method = getMethod(object, request.getMethod());
			Object[] params = castParams(request.getParams(), method);
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
				err.printStackTrace(); // TODO: remove printing stacktrace?
				
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
				int mod = method.getModifiers();
				Access access = method.getAnnotation(Access.class);
				boolean available = 
					Modifier.isPublic(mod) &&
					(access == null || 
							(access.value() != AccessType.UNAVAILABLE && 
							access.visible()));
				
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
		Map<String, Object> methods = new TreeMap<String, Object>();
		if (asJSON == null) {
			asJSON = false;
		}

		while (c != null && c != Object.class) {
			for (Method method : c.getDeclaredMethods()) {
				String methodName = method.getName();
				int mod = method.getModifiers();
				Access access = method.getAnnotation(Access.class); 
				// TODO: apply access when invoking a method of the agent

				boolean available = 
					Modifier.isPublic(mod) &&
					(access == null || 
							(access.value() != AccessType.UNAVAILABLE &&
							 access.visible()));

				if (available) {
					//Class<?>[] types = method.getParameterTypes();
					Type[] types = method.getGenericParameterTypes();
					int paramNum = types.length;
					Annotation[][] paramAnnotations = method.getParameterAnnotations();
					String[] paramTypes = new String[paramNum];
					for(int i = 0; i < paramNum; i++){
						paramTypes[i] = typeToString(types[i]);	
					}
					
					// get parameters
					boolean validParamNames = true;
					String[] paramNames = new String[paramNum];
					boolean[] paramRequired = new boolean[paramNum];
					for(int i = 0; i < paramNum; i++){
						paramTypes[i] = typeToString(types[i]);	
						paramRequired[i] = true;
						
						Annotation[] annotations = paramAnnotations[i];
						for(Annotation annotation : annotations){
							if(annotation instanceof Name){
								Name pn = (Name) annotation;
								paramNames[i] = pn.value();
							}
							if(annotation instanceof Required){
								Required pr = (Required) annotation;
								paramRequired[i] = pr.value();
							}
						}
						
						if (paramNames[i] == null) {
							validParamNames = false;
						}
					}

					// TODO: not so nice 
					if (!validParamNames) {
						Class<?>[] pt = method.getParameterTypes();
						if (pt.length == 1 && pt[0].equals(ObjectNode.class)) {
							paramNames[0] = "params";
							validParamNames = true;
						}
					}
					
					// get return type
					String returnType = typeToString(method.getGenericReturnType());
					
					if (validParamNames) {
						if (asJSON) {
							// format as JSON
							List<Object> descParams = new ArrayList<Object>();
							for(int i = 0; i < paramNum; i++){
								Map<String, Object> paramData = new HashMap<String, Object>();
								paramData.put("name", paramNames[i]);
								paramData.put("type", paramTypes[i]);
								paramData.put("required", paramRequired[i]);
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
							for(int i = 0; i < paramNum; i++){
								if (!p.isEmpty()) {
									p += ", ";
								}
								if (paramRequired[i]) {
									p += paramTypes[i] + " " + paramNames[i];
								}
								else {
									p += "[" + paramTypes[i] + " " + paramNames[i] + "]";
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
	 * Find a method by name
	 * @param object
	 * @param method
	 * @return
	 * @throws JSONRPCException 
	 */
	static private Method getMethod(Object object, String method) 
			throws JSONRPCException {
		Class<?> c = object.getClass();
		while (c != null && c != Object.class) {
			for (Method m : c.getDeclaredMethods()) {

				int mod = m.getModifiers();
				Access access = m.getAnnotation(Access.class);
				boolean available = 
					Modifier.isPublic(mod) &&
					(access == null || access.value() != AccessType.UNAVAILABLE);
				
				if (available) {
					String name = m.getName();
					
					if (name.equals(method)) {
						return m;
					}
				}
			}
			
			// continue search in the super class 
			c = c.getSuperclass();
		}
		
		JSONRPCException err = new JSONRPCException(JSONRPCException.CODE.METHOD_NOT_FOUND, 
				"Method '" + method + "' not found");
		throw err;
	}

	/**
	 * Cast a JSONArray or JSONObject params to the desired paramTypes 
	 * @param params
	 * @param paramTypes
	 * @return 
	 * @throws Exception 
	 */
	static private Object[] castParams(Object params, Method method) 
			throws Exception {
		ObjectMapper mapper = JOM.getInstance();
		Class<?>[] paramTypes = method.getParameterTypes();
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		
		if (paramTypes.length == 0) {
			return new Object[0];
		}
		
		if (params instanceof ObjectNode) {
			// JSON-RPC 2.0 with named parameters in a JSONObject

			// find named parameter annotations
			boolean hasParamNames = false;
			String[] paramNames = new String[paramTypes.length];
			Boolean[] paramRequired = new Boolean[paramTypes.length];
			for(int i = 0; i < paramTypes.length; i++){
				paramRequired[i] = true;
				Annotation[] annotations = paramAnnotations[i];
				for(Annotation annotation : annotations){
					if(annotation instanceof Name){
						Name name = (Name) annotation;
						paramNames[i] = name.value();
						hasParamNames = true;
					}
					if(annotation instanceof Required){
						Required required = (Required) annotation;
						paramRequired[i] = required.value();
					}
				}
			}
			
			if (hasParamNames) {
				ObjectNode paramsObject = (ObjectNode)params;
				
				Object[] objects = new Object[paramTypes.length];
				for (int i = 0; i < paramTypes.length; i++) {
					Class<?> paramType = paramTypes[i];
					String paramName = paramNames[i];
					if (paramName == null) {
						throw new Exception("Name of parameter " + i + " not defined");
					}
					if (paramsObject.has(paramName)) {
						objects[i] = mapper.convertValue(
								paramsObject.get(paramName), paramType);

						/* TODO
						objects[i] = convertParam(paramsObject.get(paramName), paramType);
						//*/
					}
					else {
						if (paramRequired[i]) {
							throw new Exception("Required parameter '" + 
									paramName + "' missing");
						}
						//else if (paramType.getSuperclass() == null) {
						else if (paramType.isPrimitive()) {
							throw new Exception(
									"Parameter '" + paramName +
									"' cannot be both optional and " +
									"a primitive type (" +
									paramType.getSimpleName() + ")");
						}
						else {
							objects[i] = null;
						}
					}
				}
				return objects;
			}
			else if (paramTypes.length == 1 && 
					paramTypes[0].equals(ObjectNode.class)) {
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
		ObjectNode params = JOM.createObjectNode();
		
		Type[] types = method.getGenericParameterTypes();
		int paramNum = types.length;
		
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		for(int i = 0; i < paramNum; i++){
			String paramName = null;
			Boolean required = true;
			
			Annotation[] annotations = paramAnnotations[i];
			for(Annotation annotation : annotations) {
				if(annotation instanceof Name){
					paramName =  ((Name) annotation).value();
				}
				if(annotation instanceof Required){
					required = ((Required) annotation).value();
				}
			}
			
			if (i < args.length && args[i] != null) {
				if (paramName != null) {
					JsonNode paramValue = JOM.getInstance().convertValue(args[i], 
							JsonNode.class);
					params.put(paramName, paramValue);
				}
				else {
					throw new IllegalArgumentException(
							"Parameter " + i + " in method '" + method.getName() + 
							"' is missing the @Name annotation.");
				}
			}
			else if (required) {
				throw new IllegalArgumentException(
						"Required parameter " + i + " in method '" + method.getName() + 
						"' is null.");
			}
		}
		
		return new JSONRequest(method.getName(), params);		
	}
}