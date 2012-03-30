/**
 * @file KeyValue.java
 * 
 * @brief 
 * The class KeyValue can store a single key-value pair in the Google DataStore.
 * The value can be any type of object, and is internally serialized to a byte 
 * array.
 * KeyValue uses TwigPersist annotations.
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2010-2011 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2012-03-26
 */
package com.almende.eve.context.google;

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