/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.rpc.jsonrpc.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * The Class JsonNullAwareDeserializer.
 */
public final class JsonNullAwareDeserializer extends JsonNodeDeserializer {
	private static final long	serialVersionUID	= 8700330491206317830L;
	
	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer#getNullValue()
	 */
	@Override
	public JsonNode getNullValue() {
		return NullNode.getInstance();
	}
}