package com.almende.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.StateEntry;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public abstract class TypeUtil<T> {
	static final Logger		LOG	= Logger.getLogger(TypeUtil.class.getName());
	
	private final JavaType	valueType;
	
	/**
	 * Usage example: <br>
	 * 
	 * TypeUtil&lt;TreeSet&lt;TaskEntry>> injector = new
	 * TypeUtil&lt;TreeSet&lt;TaskEntry>>(){};<br>
	 * TreeSet&lt;TaskEntry> value = injector.inject(Treeset_with_tasks);<br>
	 * <br>
	 * Note the trivial anonymous class declaration, extending abstract class
	 * TypeUtil...
	 */
	public TypeUtil() {
		this.valueType = JOM.getTypeFactory().constructType(
				ClassUtil.getTypeArguments(TypeUtil.class, getClass()).get(0)
						.getGenericSuperclass());
	}
	
	/** @return the {@link StateEntry} value's type */
	public Type getType() {
		return this.valueType;
	}
	
	public T inject(Object value) {
		return inject(valueType, value);
	}
	
	public static <T> T inject(Class<T> type, Object value) {
		return inject(JOM.getTypeFactory().constructType(type), value);
	}
	
	public static <T> T inject(Type type, Object value) {
		return inject(JOM.getTypeFactory().constructType(type), value);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T inject(JavaType fullType, Object value) {
		if (value == null) {
			return null;
		}
		if (fullType.hasRawClass(Void.class)) {
			return null;
		}
		ObjectMapper mapper = JOM.getInstance();
		if (value instanceof JsonNode) {
			return mapper.convertValue(value, fullType);
		}
		return (T) fullType.getRawClass().cast(value);
	}
	
	public static <T> T inject(T ret, Object value) throws IOException {
		ObjectMapper mapper = JOM.getInstance();
		if (ret != null) {
			if (value instanceof JsonNode) {
				ObjectReader reader = mapper.readerForUpdating(ret);
				try {
					return reader.readValue((JsonNode) value);
				} catch (UnsupportedOperationException e1) {
					LOG.log(Level.WARNING,
							"Trying to update unmodifiable object", e1);
					return inject(ret.getClass().getGenericSuperclass(), value);
				}
			} else {
				LOG.log(Level.WARNING,
						"Can't update object with non-JSON value, returning cast value.");
				return inject(ret.getClass().getGenericSuperclass(), value);
			}
		}
		return ret;
	}
}
