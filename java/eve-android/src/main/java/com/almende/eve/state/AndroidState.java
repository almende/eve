package com.almende.eve.state;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;

public class AndroidState extends AbstractState {

	public AndroidState() {
	}

	Context appCtx = null;
	private String filename = null;
	private Map<String, Serializable> properties = Collections
			.synchronizedMap(new HashMap<String, Serializable>());

	public AndroidState(String agentId, Context appContext) {
		super(agentId);
		this.appCtx = appContext;
		this.filename = agentId;
	}

	/**
	 * write properties to disk
	 * 
	 * @return success True if successfully written
	 * @throws IOException
	 */
	private boolean write() {
		try {
			FileOutputStream fos = appCtx.openFileOutput(filename,
					Context.MODE_PRIVATE);
			ObjectOutput out = new ObjectOutputStream(fos);
			try {
				properties.remove("AppContext");
			} catch (Exception e) {
			}
			;
			out.writeObject(properties);
			out.close();
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * read properties from disk
	 * 
	 * @return success True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private boolean read() {
		try {
			FileInputStream fis = appCtx.openFileInput(filename);
			ObjectInput in = new ObjectInputStream(fis);
			properties.clear();
			properties.putAll((Map<String, Serializable>) in.readObject());
			fis.close();
			in.close();
			return true;
		} catch (FileNotFoundException e) {
			// no need to give an error, we suppose this is a new agent
		} catch (EOFException e) {
			// no need to give an error, we suppose this is a new agent
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * init is executed once before the agent method is invoked
	 */
	@Override
	public void init() {
	}

	/**
	 * destroy is executed once after the agent method is invoked if the
	 * properties are changed, they will be saved
	 */
	@Override
	public void destroy() {
	}

	@Override
	public void clear() {
		synchronized (properties) {
			read();
			properties.clear();
		}
	}
	
	public Context getContext(){
		return appCtx;
	}

	@Override
	public Set<String> keySet() {
		synchronized (properties) {
			read();
			return properties.keySet();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		synchronized (properties) {
			read();
			return properties.containsKey(key);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		synchronized (properties) {
			read();
			return properties.containsValue(value);
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, Serializable>> entrySet() {
		synchronized (properties) {
			read();
			return properties.entrySet();
		}
	}

	@Override
	public Serializable get(Object key) {
		synchronized (properties) {
			read();
			return properties.get(key);
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (properties) {
			read();
			return properties.isEmpty();
		}
	}

	@Override
	public Serializable put(String key, Serializable value) {
		synchronized (properties) {
			read();
			Serializable ret = properties.put(key, value);
			write();
			return ret;
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends Serializable> map) {
		synchronized (properties) {
			read();
			properties.putAll(map);
			write();
		}
	}

	@Override
	public boolean putIfUnchanged(String key, Serializable newVal, Serializable oldVal) {
		synchronized (properties) {
			boolean result = false;
			read();
			if (!(oldVal == null && properties.containsKey(key)) || properties.get(key).equals(oldVal)) {
				properties.put(key, newVal);
				write();
				result = true;
			}
			return result;
		}
	}

	@Override
	public Serializable remove(Object key) {
		synchronized (properties) {
			read();
			Serializable value = properties.remove(key);
			write();
			return value;
		}
	}

	@Override
	public int size() {
		synchronized (properties) {
			read();
			return properties.size();
		}
	}

	@Override
	public Collection<Serializable> values() {
		synchronized (properties) {
			read();
			return properties.values();
		}
	}

}