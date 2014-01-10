/**
 * Singleton Jackson ObjectMapper
 */
package com.almende.eve.rpc.jsonrpc.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public final class JOM {
	private static final ObjectMapper	MAPPER;
	static {
		MAPPER = createInstance();
	}
	
	protected JOM() {
	}
	
	public static ObjectMapper getInstance() {
		return MAPPER;
	}
	
	public static ObjectNode createObjectNode() {
		return MAPPER.createObjectNode();
	}
	
	public static ArrayNode createArrayNode() {
		return MAPPER.createArrayNode();
	}
	
	public static NullNode createNullNode() {
		return NullNode.getInstance();
	}
	
	private static synchronized ObjectMapper createInstance() {
		final ObjectMapper mapper = new ObjectMapper();
		
		// set configuration
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);
		mapper.configure(
				DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, false);
		
		// Needed for o.a. JsonFileState
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
		
		mapper.registerModule(new JodaModule());
		
		return mapper;
	}
	
	public static TypeFactory getTypeFactory() {
		return MAPPER.getTypeFactory();
	}
	
	/**
	 * @deprecated This method is no longer needed, you can directly use
	 *             Void.class
	 * @return
	 */
	@Deprecated
	public static JavaType getVoid() {
		return MAPPER.getTypeFactory().uncheckedSimpleType(Void.class);
	}
	
	/**
	 * @deprecated This method is no longer needed, you can directly use
	 *             <Class>.class, e.g. String.class
	 * @return
	 */
	@Deprecated
	public static JavaType getSimpleType(final Class<?> c) {
		return MAPPER.getTypeFactory().uncheckedSimpleType(c);
	}
}
