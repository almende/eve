package com.almende.eve.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @class FileState
 * 
 * A context for an Eve Agent, which stores the data on disk.
 * Data is stored in the path provided by the configuration file.
 * 
 * The context provides general information for the agent (about itself,
 * the environment, and the system configuration), and the agent can store its 
 * state in the context. 
 * The context extends a standard Java Map.
 * 
 * Usage:<br>
 *     AgentFactory factory = new AgentFactory(config);<br>
 *     State state = new State("agentId");<br>
 *     context.put("key", "value");<br>
 *     System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 */
// TODO: create an in memory cache and reduce the number of reads/writes
public class FileState extends State {
	protected FileState() {}

	public FileState(String agentId, String filename) {
		super(agentId);
		this.filename = filename;
	}

	/**
	 * write properties to disk
	 * @return success   True if successfully written
	 * @throws IOException
	 */
	private boolean write() {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			FileLock fl = fos.getChannel().lock();//block until lock is acquired.
		    if(fl != null) {
		    	ObjectOutput out = new ObjectOutputStream(fos);
		    	out.writeObject(properties);
				fl.release();
		    	out.close();
		    } else {
		    	System.err.println("Warning, couldn't get file lock for writing!");
		    }
		    fos.close();
			return (fl != null);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * read properties from disk
	 * @return success   True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private boolean read() {
		try {
			File file = new File(filename);
			if (file.length() > 0) {
				FileInputStream fis = new FileInputStream(filename);
				ObjectInput in = new ObjectInputStream(fis);
				properties.clear();
				properties.putAll((Map<String, Object>) in.readObject());
				in.close();
				fis.close();
				return true;
			}
		} catch (FileNotFoundException e) {
			//FIXME! Comment can't be right! no need to give an error, we suppose this is a new agent
			e.printStackTrace();
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
	 * destroy is executed once after the agent method is invoked
	 * if the properties are changed, they will be saved
	 */
	@Override
	public void destroy() {
	}

	@Override
	public void clear() {
		synchronized(properties){
			read();
			properties.clear();
			write();
		}
	}

	@Override
	public Set<String> keySet() {
		synchronized(properties){
			read();
			return properties.keySet();
		}
	}

	@Override
	public boolean containsKey(Object key) {
		synchronized(properties){
			read();
			return properties.containsKey(key);
		}
	}

	@Override
	public boolean containsValue(Object value) {
		synchronized(properties){
			read();
			return properties.containsValue(value);
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		synchronized(properties){
			read();
			return properties.entrySet();
		}
	}

	@Override
	public Object get(Object key) {
		synchronized(properties){
			read();
			return properties.get(key);
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized(properties){
			read();
			return properties.isEmpty();
		}
	}

	@Override
	public Object put(String key, Object value) {
		synchronized(properties){
			read();
			Object ret = properties.put(key, value);
			write();
			return ret;
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		synchronized(properties){
			read();
			properties.putAll(map);
			write();
		}
	}

	@Override
	public Object remove(Object key) {
		synchronized(properties){
			read();
			Object value = properties.remove(key);
			write();
			return value; 
		}
	}

	@Override
	public int size() {
		synchronized(properties){
			read();
			return properties.size();
		}
	}

	@Override
	public Collection<Object> values() {
		synchronized(properties){
			read();
			return properties.values();
		}
	}
	
	private String filename = null;
	private static Map<String, Object> properties = Collections.synchronizedMap(new HashMap<String, Object>());
}