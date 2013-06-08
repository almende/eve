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

public class TypeUtil<T> {
	static final Logger		LOG	= Logger.getLogger(TypeUtil.class.getName());
	
	private final JavaType	valueType;
	
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
	public static <T> T inject(JavaType full_type, Object value) {
		if (value == null) {
			return null;
		}
		if (full_type.hasRawClass(Void.class)) {
			return null;
		}
		ObjectMapper mapper = JOM.getInstance();
		if (value instanceof JsonNode) {
			return mapper.convertValue(value, full_type);
		}
		return (T) full_type.getRawClass().cast(value);
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
						"Can't update object with non-JSON value.");
				return inject(ret.getClass().getGenericSuperclass(), value);
			}
		}
		return ret;
	}
}
