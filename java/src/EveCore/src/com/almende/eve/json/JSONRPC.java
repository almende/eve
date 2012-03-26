package com.almende.eve.json;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import com.almende.eve.json.annotation.ParameterName;
import com.almende.eve.json.annotation.ParameterRequired;
import com.almende.eve.json.util.HttpUtil;


import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

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
			jsonResponse = new JSONResponse((JSONObject) JSONSerializer.toJSON(resp));
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
	 */
	static public String invoke (Object object, String request) {
		JSONResponse jsonResponse = null;
		try {
			JSONRequest jsonRequest = new JSONRequest(
					(JSONObject) JSONSerializer.toJSON(request));

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
					result = JSONNull.getInstance();
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
				String name = m.getName();
				
				if (name.equals(method)) {
					return m;
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
		Class<?>[] paramTypes = method.getParameterTypes();
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		
		if (paramTypes.length == 0) {
			return new Object[0];
		}
		
		if (params instanceof JSONObject) {
			// JSON-RPC 2.0 with named parameters in a JSONObject

			// find named parameter annotations
			boolean hasParamNames = false;
			String[] paramNames = new String[paramTypes.length];
			Boolean[] paramRequired = new Boolean[paramTypes.length];
			for(int i = 0; i < paramTypes.length; i++){
				paramRequired[i] = true;
				Annotation[] annotations = paramAnnotations[i];
				for(Annotation annotation : annotations){
					if(annotation instanceof ParameterName){
						ParameterName name = (ParameterName) annotation;
						paramNames[i] = name.value();
						hasParamNames = true;
					}
					if(annotation instanceof ParameterRequired){
						ParameterRequired required = (ParameterRequired) annotation;
						paramRequired[i] = required.value();
					}
				}
			}
			
			if (hasParamNames) {
				JSONObject paramsObject = (JSONObject)params;
				
				Object[] objects = new Object[paramTypes.length];
				for (int i = 0; i < paramTypes.length; i++) {
					Class<?> paramType = paramTypes[i];
					String paramName = paramNames[i];
					if (paramName == null) {
						throw new Exception("Name of parameter " + i + " not defined");
					}
					if (paramsObject.containsKey(paramName)) {
						objects[i] = castJSONObject(paramsObject, paramName, paramType);
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
					paramTypes[0].equals(JSONObject.class)) {
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
	 * Cast an element from a JSONArray to the given type 
	 * @param <T>
	 * @param array
	 * @param index
	 * @param type
	 * @return
	 * @throws JSONException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("unchecked")
	static private <T> T castJSONObject (JSONObject object, String key, 
			Class<T> type) {
		T param = null;

		// TODO: can I leave all this stuff to the JSON library?
		
		if (type.equals(Boolean.class) || type.equals(boolean.class) ) {
			param = (T) (Boolean)object.getBoolean(key);
		}
		else if (type.equals(Double.class) || type.equals(double.class) ) {
			param = (T) (Double)object.getDouble(key);
		}
		else if (type.equals(Integer.class) || type.equals(int.class) ) {
			param = (T) (Integer)object.getInt(key);
		}
		else if (type.equals(Long.class) || type.equals(long.class) ) {
			param = (T) (Long)object.getLong(key);
		}
		else if (type.equals(String.class)) {
			param = (T) (String)object.getString(key);
		}
		else if (type.equals(JSONArray.class)) {
			param = (T) (JSONArray)object.getJSONArray(key);
		}
		else if (type.equals(JSONObject.class)) {
			param = (T) (JSONObject)object.getJSONObject(key);
		}
		else if (type.isArray()) {
			// TODO: use beans
			if (type.equals(JSONArray.class)) {
				param = (T) (JSONArray)object.getJSONArray(key);
			}
			else {
				JSONArray json = object.getJSONArray(key);
				Class<?> componentType = type.getComponentType();
				param = (T) createArray(json, componentType);
			}
		}
		else if (object.getJSONObject(key) != null) {
			// TODO: use beans
			// http://json-lib.sourceforge.net/snippets.html#JSONObject to JavaBean
			JsonConfig jsonConfig = new JsonConfig();  
			jsonConfig.setRootClass(type);
			JSON value = object.getJSONObject(key);
			param = (T) JSONSerializer.toJava(value, jsonConfig);
		}
		else if (object.getJSONArray(key) != null) {
			// TODO: use beans
			// http://json-lib.sourceforge.net/snippets.html#JSONObject to JavaBean
			JsonConfig jsonConfig = new JsonConfig();  
			jsonConfig.setRootClass(type);
			JSON value = object.getJSONArray(key);
			param = (T) JSONSerializer.toJava(value, jsonConfig);
		}
		else {
			// TODO: throw error?
			param = null;
		}
		
		return param;
	}
	
	/**
	 * Cast an element from a JSONArray to the given type 
	 * @param <T>
	 * @param array
	 * @param index
	 * @param type
	 * @return
	 * @throws JSONException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("unchecked")
	final private static <T> T castJSONArray(JSONArray array, int index, 
			Class<T> type) {
		T param = null;

		// TODO: can I leave all this stuff to the JSON library?

		if (type.equals(Boolean.class) || type.equals(boolean.class) ) {
			param = (T) (Boolean)array.getBoolean(index);
		}
		else if (type.equals(Double.class) || type.equals(double.class) ) {
			param = (T) (Double)array.getDouble(index);
		}
		else if (type.equals(Integer.class) || type.equals(int.class) ) {
			param = (T) (Integer)array.getInt(index);
		}
		else if (type.equals(Long.class) || type.equals(long.class) ) {
			param = (T) (Long)array.getLong(index);
		}
		else if (type.equals(String.class)) {
			param = (T) (String)array.getString(index);
		}
		else if (type.isArray()) {
			if (type.equals(JSONArray.class)) {
				param = (T) (JSONArray)array.getJSONArray(index);
			}
			else {
				JSONArray json = array.getJSONArray(index);
				Class<?> componentType = type.getComponentType();
				param = (T) createArray(json, componentType);
			}
		}
		else if (type.equals(JSONArray.class)) {
			param = (T) (JSONArray)array.getJSONArray(index);
		}
		else if (type.equals(JSONObject.class)) {
			param = (T) (JSONObject)array.getJSONObject(index);
		}
		else if (array.getJSONObject(index) != null) {
			// TODO: use beans
			// http://json-lib.sourceforge.net/snippets.html#JSONObject to JavaBean
			JsonConfig jsonConfig = new JsonConfig();  
			jsonConfig.setRootClass(type);
			JSON value = array.getJSONObject(index);
			param = (T) JSONSerializer.toJava(value, jsonConfig);
		}
		else if (array.getJSONArray(index) != null) {
			// TODO: use beans
			// http://json-lib.sourceforge.net/snippets.html#JSONObject to JavaBean
			JsonConfig jsonConfig = new JsonConfig();  
			jsonConfig.setRootClass(type);
			JSON value = array.getJSONArray(index);
			param = (T) JSONSerializer.toJava(value, jsonConfig);
		}		
		else {
			param = null;
		}			

		return param;
	}

	// http://forums.techarena.in/software-development/1117589.htm	
	@SuppressWarnings("unchecked")
	final private static <T> T[] createArray(JSONArray jsonArray, Class<T> type) {
		T[] array = (T[]) Array.newInstance(type, jsonArray.size());
		for (int i = 0, len = jsonArray.size(); i < len; i++) {
			array[i] =  (T) castJSONArray(jsonArray, i, type);			
		}
		return array;
	}
	

	/* TODO
	final private boolean isInternalUrl(String url) 
			throws FileNotFoundException, IOException {
		return url.startsWith(AgentsContext.getServletUrl());		
	}
	*/
}
