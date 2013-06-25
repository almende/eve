package com.almende.eve.state;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @class FileState
 * 
 *        A persistent state for an Eve Agent, which stores the data on disk.
 *        Data is stored in the path provided by the configuration file.
 * 
 *        The state provides general information for the agent (about itself,
 *        the environment, and the system configuration), and the agent can
 *        store its state in the state. The state extends a standard Java
 *        Map.
 * 
 *        All operations on this FileState are thread-safe. It also provides two
 *        aditional methods: PutIfNotChanged() and PutAllIfNotChanged().
 * 
 *        Usage:<br>
 *        AgentHost factory = AgentHost.getInstance(config);<br>
 *        ConcurrentFileState state = new
 *        ConcurrentFileState("agentId",".eveagents");<br>
 *        state.put("key", "value");<br>
 *        System.out.println(state.get("key")); // "value"<br>
 * 
 * @author jos
 * @author ludo
 */
public class ConcurrentFileState extends FileState {
	private static final Logger			LOG			= Logger.getLogger("ConcurrentFileState");
	
	private boolean						json		= false;
	private String						filename	= null;
	private FileChannel					channel		= null;
	private FileLock					lock		= null;
	private InputStream					fis			= null;
	private OutputStream				fos			= null;
	private ObjectMapper				om			= null;
	private static Map<String, Boolean>	locked		= new ConcurrentHashMap<String, Boolean>();
	
	private Map<String, Serializable>	properties	= Collections
															.synchronizedMap(new HashMap<String, Serializable>());
	
	public ConcurrentFileState(String agentId, String filename) {
		super(agentId);
		this.filename = filename;
	}
	
	public ConcurrentFileState(String agentId, String filename, boolean json) {
		super(agentId);
		this.filename = filename;
		this.json = json;
		om = JOM.getInstance();
		om.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		om.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
	}
	
	@Override
	public void finalize() throws Throwable {
		closeFile();
		super.finalize();
	}
	
	@SuppressWarnings("resource")
	protected void openFile() throws IOException {
		synchronized (locked) {
			while (locked.containsKey(filename) && locked.get(filename)) {
				// logger.warning("Starting to wait for locked! "+filename);
				try {
					locked.wait();
				} catch (InterruptedException e) {
				}
			}
			locked.put(filename, true);
			File file = new File(this.filename);
			if (!file.exists()) {
				locked.put(filename, false);
				locked.notifyAll();
				throw new IllegalStateException(
						"Warning: File doesn't exist (anymore):'"
								+ this.filename + "'");
			}
			channel = new RandomAccessFile(file, "rw").getChannel();
			try {
				// TODO: add support for shared locks, allowing parallel reading
				// operations.
				lock = channel.lock();
			} catch (Exception e) {
				channel.close();
				channel = null;
				lock = null;
				locked.put(filename, false);
				locked.notifyAll();
				throw new IllegalStateException(
						"error, couldn't obtain file lock on:" + filename, e);
			}
			fis = Channels.newInputStream(channel);
			fos = Channels.newOutputStream(channel);
		}
	}
	
	protected void closeFile() {
		synchronized (locked) {
			if (lock.isValid()) {
				try {
					
					lock.release();
				} catch (IOException e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
			try {
				fos.close();
				fis.close();
				channel.close();
			} catch (IOException e) {
				LOG.log(Level.WARNING, "", e);
			}
			channel = null;
			fis = null;
			fos = null;
			lock = null;
			locked.put(filename, false);
			locked.notifyAll();
		}
	}
	
	/**
	 * write properties to disk
	 * 
	 * @return success True if successfully written
	 * @throws IOException
	 */
	private void write() throws IOException {
		if (channel != null) {
			channel.position(0);
		}
		if (json) {
			om.writeValue(fos, properties);
			fos.flush();
		} else {
			ObjectOutput out = new ObjectOutputStream(fos);
			out.writeObject(properties);
			out.flush();
		}
	}
	
	/**
	 * read properties from disk
	 * 
	 * @return success True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void read() throws IOException, ClassNotFoundException {
		_read(false);
	}
	
	@SuppressWarnings("unchecked")
	private void _read(boolean retry) throws IOException,
			ClassNotFoundException {
		try {
			if (channel != null) {
				channel.position(0);
			}
			properties.clear();
			if (json ^ retry) {
				properties.putAll(om.readValue(fis, HashMap.class));
			} else {
				ObjectInput in = new ObjectInputStream(fis);
				properties.putAll((Map<String, Serializable>) in.readObject());
			}
		} catch (EOFException eof) {
			// empty file, new agent?
		} catch (StreamCorruptedException sce) {
			if (channel != null && channel.position() != 0) {
				if (!retry) {
					_read(true);
				} else {
					throw sce;
				}
			}
			// empty file, new agent?
		} catch (JsonMappingException map) {
			if (channel != null && channel.position() != 0) {
				if (!retry) {
					_read(true);
				} else {
					throw map;
				}
			}
			// empty file, new agent?
		}
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
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
	}
	
	@Override
	public synchronized Set<String> keySet() {
		Set<String> result = null;
		try {
			openFile();
			read();
			result = properties.keySet();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized boolean containsKey(Object key) {
		boolean result = false;
		try {
			openFile();
			read();
			result = properties.containsKey(key);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized boolean containsValue(Object value) {
		boolean result = false;
		try {
			openFile();
			read();
			result = properties.containsValue(value);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized Set<java.util.Map.Entry<String, Serializable>> entrySet() {
		Set<java.util.Map.Entry<String, Serializable>> result = null;
		try {
			openFile();
			read();
			result = properties.entrySet();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized Serializable get(Object key) {
		Serializable result = null;
		try {
			openFile();
			read();
			result = properties.get(key);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized boolean isEmpty() {
		boolean result = false;
		try {
			openFile();
			read();
			result = properties.isEmpty();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized Serializable put(String key, Serializable value) {
		Serializable result = null;
		try {
			openFile();
			read();
			result = properties.put(key, value);
			write();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized void putAll(
			Map<? extends String, ? extends Serializable> map) {
		try {
			openFile();
			read();
			properties.putAll(map);
			write();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
	}
	
	@Override
	public synchronized boolean putIfUnchanged(String key, Serializable newVal,
			Serializable oldVal) {
		boolean result = false;
		try {
			openFile();
			read();
			if (!(oldVal == null && properties.containsKey(key))
					|| properties.get(key).equals(oldVal)) {
				properties.put(key, newVal);
				write();
				result = true;
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
			// Don't let users loop if exception is thrown. They
			// would get into a deadlock....
			result = true;
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized Serializable remove(Object key) {
		Serializable result = null;
		try {
			openFile();
			read();
			result = properties.remove(key);
			write();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized int size() {
		int result = -1;
		try {
			openFile();
			read();
			result = properties.size();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	@Override
	public synchronized Collection<Serializable> values() {
		Collection<Serializable> result = null;
		try {
			openFile();
			read();
			result = properties.values();
		} catch (Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
}