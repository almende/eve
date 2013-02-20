package com.almende.eve.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @class FileContext
 * 
 *        A context for an Eve Agent, which stores the data on disk. Data is
 *        stored in the path provided by the configuration file.
 * 
 *        The context provides general information for the agent (about itself,
 *        the environment, and the system configuration), and the agent can
 *        store its state in the context. The context extends a standard Java
 *        Map.
 * 
 *        All operations on this FileContext are thread-safe. It also provides
 *        two aditional methods: PutIfNotChanged() and PutAllIfNotChanged().
 * 
 *        Usage:<br>
 *        AgentFactory factory = new AgentFactory(config);<br>
 *        ConcurrentFileContext context = new
 *        ConcurrentFileContext("agentId",".eveagents");<br>
 *        context.put("key", "value");<br>
 *        System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 * @author ludo
 */
public class ConcurrentFileContext extends Context {
	protected ConcurrentFileContext() {
	}

	private String filename = null;
	private FileChannel channel = null;
	private FileLock lock = null;
	private InputStream fis = null;
	private OutputStream fos = null;
	
	private static Map<String, Object> properties = Collections
			.synchronizedMap(new HashMap<String, Object>());

	public ConcurrentFileContext(String agentId, String filename) {
		super(agentId);
		this.filename = filename;
	}

	@SuppressWarnings("resource")
	private void openFile() throws Exception {
		File file = new File(this.filename);
		channel = new RandomAccessFile(file, "rw").getChannel();
		lock = channel.lock();
		fis = Channels.newInputStream(channel);
		fos = Channels.newOutputStream(channel);
	}

	private void closeFile() throws Exception {
		if (channel.isOpen()){
			lock.release();
			fos.close();
			fis.close();
			channel.close();
			fis=null;
			fos=null;
			lock=null;
			channel=null;
		}
	}

	/**
	 * write properties to disk
	 * 
	 * @return success True if successfully written
	 * @throws IOException
	 */
	private void write() throws Exception {
		channel.position(0);
		ObjectOutput out = new ObjectOutputStream(fos);
		out.writeObject(properties);
		out.flush();
	}

	/**
	 * read properties from disk
	 * 
	 * @return success True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void read() throws Exception {
		channel.position(0);
		ObjectInput in = new ObjectInputStream(fis);
		properties.clear();
		properties.putAll((Map<String, Object>) in.readObject());
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
	public synchronized void clear() {
		try {
			openFile();
			properties.clear();
			write();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public synchronized Set<String> keySet() {
		Set<String> result = null;
		try {
			openFile();
			read();
			result = properties.keySet();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		boolean result=false;
		try {
			openFile();
			read();
			result = properties.containsKey(key);
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		boolean result=false;
		try {
			openFile();
			read();
			result = properties.containsValue(value);
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized Set<java.util.Map.Entry<String, Object>> entrySet() {
		Set<java.util.Map.Entry<String, Object>> result = null;
		try {
			openFile();
			read();
			result = properties.entrySet();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized Object get(Object key) {
		Object result=null;
		try {
			openFile();
			read();
			result = properties.get(key);
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized boolean isEmpty() {
		boolean result=false;
		try {
			openFile();
			read();
			result = properties.isEmpty();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized Object put(String key, Object value) {
		Object result=null;
		try {
			openFile();
			read();
			result = properties.put(key,value);
			write();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized void putAll(Map<? extends String, ? extends Object> map) {
		try {
			openFile();
			read();
			properties.putAll(map);
			write();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public synchronized Object remove(Object key) {
		Object result=null;
		try {
			openFile();
			read();
			result = properties.remove(key);
			write();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized int size() {
		int result=-1;
		try {
			openFile();
			read();
			result = properties.size();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public synchronized Collection<Object> values() {
		Collection<Object> result=null;
		try {
			openFile();
			read();
			result = properties.values();
			closeFile();
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

}