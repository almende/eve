package com.almende.eve.rpc.jsonrpc.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;

public final class JsonNullAwareDeserializer extends JsonNodeDeserializer {
	
	private static final long	serialVersionUID	= 8700330491206317830L;
	
	@Override
	public JsonNode getNullValue() {
		return NullNode.getInstance();
	}
}