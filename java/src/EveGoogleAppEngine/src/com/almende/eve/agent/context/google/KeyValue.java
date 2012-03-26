package com.almende.eve.agent.context.google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.google.appengine.api.datastore.Blob;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Index;
import com.google.code.twig.annotation.Type;

@SuppressWarnings("serial")
public class KeyValue implements Serializable {
	// KeyValue stores a key and a serialized value
	@Id private String key = null;
	@Index(false) @Type(Blob.class) private byte[] value = null; 
	
	protected KeyValue () {}
	
	protected KeyValue (String key, Object value) throws IOException {
		setKey(key);
		setValue(value);
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setValue(Object value) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(value);
		this.value = bos.toByteArray();
		out.close();
		bos.close();
	}

	public Object getValue() throws ClassNotFoundException, IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(this.value);
		ObjectInput in = new ObjectInputStream(bis);
		Object value = in.readObject(); 
		bis.close();
		in.close();
		return value;
	}
}	