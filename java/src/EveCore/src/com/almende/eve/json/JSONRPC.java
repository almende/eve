package com.almende.eve.json;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.eve.agent.annotation.Access;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSONRPC {
	//static private Logger logger = Logger.getLogger(JSONRPC.class.getName());

	// TODO: implement JSONRPC 2.0 Batch
	
	/* TODO: implement again
	static public Object send (String url, String method, JSONObject params) 
			throws Exception {

		final int id = 1;
		resp = send(url, new JSONRequest(id, method, params));
		
				// check if an error occurred
		if (jsonResponse.has("error")) {
			if (jsonResponse.get("error") != null) {
				JSONObject jsonError = jsonResponse.getJSONObject("error");
				
				if (jsonError.has("message")) {
					Object message = jsonError.get("message");
					throw new Exception(message.toString());
				}
				else {
					throw new Exception("Unknown error");
				}
			}
		}
	}
	*/
	
	/**
	 * Send a request to an agent in JSON-RPC format
	 * @param url         The url of the agent
	 * @param jsonRequest The request, containing id, method and parameters
	 * @return jsonResponse       
	 * @throws IOException 
	 */
	static public JSONResponse send (String url, JSONRequest jsonRequest) 
			throws IOException {
		String req = jsonRequest.toString();
		String resp = HttpUtil.post(url, req);

		JSONResponse jsonResponse;
		try {
			jsonResponse = new JSONResponse(resp);
		} catch (JSONRPCException err) {
			jsonResponse = new JSONResponse(err);
		}
		
		return jsonResponse;
	}

	/**
	 * Send a request to an agent in JSON-RPC 1.0 format (array with parameters)
	 * @param callbackMethod  The method to be executed on callback
	 * @param url             The url of the agent to be called
	 * @param method          The name of the method
	 * @param params          A JSONObject or JSONArray containing the parameter 
	 *                        values of the method
	 * @return response       A Confirmation message or error message in JSON 
	 *                        format
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	/* TODO: implement async json-rpc again
	static public void sendAsync (String url, String method, Object params,
			String callbackMethod) throws IOException, URISyntaxException {
		final String id = context.getId();
		final String id = "1";

		// built up the JSON request
		JSONObject jsonRequest = new JSONObject();
		jsonRequest.put("id", id);
		jsonRequest.put("method", method);
		jsonRequest.put("params", params);

		JSONObject callback = new JSONObject();
		callback.put("url", url);
		callback.put("method", callbackMethod);
		jsonRequest.put("callback", callback);

		// send the request to the agent. 
		// there will be no response, as we send an async message
		if (isInternalUrl(url)) {
			fetchInternalAsync(url, jsonRequest.toString());
		}
		else {
			fetch(url, jsonRequest.toString());
		}
	}
	*/
	
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
		JSONResponse jsonResponse = null;
		try {
			JSONRequest jsonRequest = new JSONRequest(request);

			try {
				jsonResponse = invoke(object, jsonRequest);
			}
			catch (Throwable err) {
				// TODO: make printing the stack trace optional
				err.printStackTrace(); 
				throw err;
			}
		}
		catch (Throwable err) {
			if (err instanceof JSONRPCException) {
				jsonResponse = new JSONResponse((JSONRPCException) err);
			}
			else {
				JSONRPCException jsonError = new JSONRPCException(
						JSONRPCException.CODE.PARSE_ERROR, getMessage(err));
				jsonResponse = new JSONResponse(jsonError);
			}
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
			try {
				Object result = method.invoke(object, params);
				if (result == null) {
					result = JOM.createNullNode();
				}
				resp.setResult(result);
			}
			catch (Throwable err) {
				// TODO: make printing the stack trace optional
				err.printStackTrace(); 
				throw err;
			}
		}
		catch (Throwable err) {
			if (err instanceof JSONRPCException) {
				resp.setError((JSONRPCException) err);
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
				int modifiers = method.getModifiers();
				Access access = method.getAnnotation(Access.class);
				boolean isPublic = Modifier.isPublic(modifiers) && 
					(access == null || access.value() == AccessType.PUBLIC);
				if (isPublic) {
					// The method name may only occur once
					String name = method.getName();
					if (methodNames.contains(name)) {
						errors.add("Public method \"" + name + 
							"\" is defined more than once, which is not" + 
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
							errors.add("Parameter " + i + " in public method \"" + name + 
								"\" is missing the @Name annotation, which is" + 
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
				int modifiers = m.getModifiers();
				if (Modifier.isPublic(modifiers)) {
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
						else if (paramType.getSuperclass() == null) {
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

	/* TODO: use or cleanup
	static private <T> T convertParam (JsonNode param, Class<T> type) {
		ObjectMapper mapper = JOM.getInstance();
		return mapper.convertValue(param, new TypeReference<T>() {});
	}
	//*/
	
	/* TODO
	final private boolean isInternalUrl(String url) 
			throws FileNotFoundException, IOException {
		return url.startsWith(AgentsContext.getServletUrl());		
	}
	*/
}