package com.almende.eve.state;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ConcurrentJsonFileState extends AbstractState<JsonNode> {
	private static final Logger			LOG			= Logger.getLogger("ConcurrentFileState");
	
	private String						filename	= null;
	private FileChannel					channel		= null;
	private FileLock					lock		= null;
	private InputStream					fis			= null;
	private OutputStream				fos			= null;
	private ObjectMapper				om			= null;
	private static Map<String, Boolean>	locked		= new ConcurrentHashMap<String, Boolean>();
	
	private Map<String, JsonNode>		properties	= Collections
															.synchronizedMap(new HashMap<String, JsonNode>());
	
	public ConcurrentJsonFileState(String agentId, String filename) {
		super(agentId);
		this.filename = filename;
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
		System.err.println("OpenFile:" + System.currentTimeMillis());
		synchronized (locked) {
			System.err.println("locked sync:" + System.currentTimeMillis());
			while (locked.containsKey(filename) && locked.get(filename)) {
				// logger.warning("Starting to wait for locked! "+filename);
				try {
					locked.wait();
				} catch (InterruptedException e) {
				}
			}
			locked.put(filename, true);
			System.err.println("locked obtained:" + System.currentTimeMillis());
			
			File file = new File(this.filename);
			if (!file.exists()) {
				locked.put(filename, false);
				locked.notifyAll();
				throw new IllegalStateException(
						"Warning: File doesn't exist (anymore):'"
								+ this.filename + "'");
			}
			System.err.println("File exists:" + System.currentTimeMillis());
			
			channel = new RandomAccessFile(file, "rw").getChannel();
			System.err
					.println("Channel obtained:" + System.currentTimeMillis());
			
			try {
				// TODO: add support for shared locks, allowing parallel reading
				// operations.
				lock = channel.lock();
				
				System.err
						.println("Channel lock:" + System.currentTimeMillis());
				
			} catch (Exception e) {
				channel.close();
				channel = null;
				lock = null;
				locked.put(filename, false);
				locked.notifyAll();
				throw new IllegalStateException(
						"error, couldn't obtain file lock on:" + filename, e);
			}
			fis = new BufferedInputStream(Channels.newInputStream(channel));
			fos = new BufferedOutputStream(Channels.newOutputStream(channel));
			System.err.println("Streams initted:" + System.currentTimeMillis());
			
		}
	}
	
	protected void closeFile() {
		System.err.println("Closing file:" + System.currentTimeMillis());
		
		synchronized (locked) {
			System.err.println("lock sync:" + System.currentTimeMillis());
			
			if (lock != null && lock.isValid()) {
				try {
					
					lock.release();
				} catch (IOException e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
			System.err.println("lock released:" + System.currentTimeMillis());
			
			try {
				if (fos != null){
					fos.close();
				}
				if (fis != null){
					fis.close();
				}
				System.err.println("Streams closed:"
						+ System.currentTimeMillis());
				
				if (channel != null){
					channel.close();
				}
				System.err.println("Channel closed:"
						+ System.currentTimeMillis());
				
			} catch (IOException e) {
				LOG.log(Level.WARNING, "", e);
			}
			channel = null;
			fis = null;
			fos = null;
			lock = null;
			locked.put(filename, false);
			locked.notifyAll();
			System.err.println("Done:" + System.currentTimeMillis());
			
		}
	}
	
	/**
	 * write properties to disk
	 * 
	 * @return success True if successfully written
	 * @throws IOException
	 */
	private void write() throws IOException {
		System.err.println("Write start:" + System.currentTimeMillis());
		
		if (channel != null) {
			channel.position(0);
		}
		
		System.err.println("pos0:" + System.currentTimeMillis());
		om.writeValue(fos, properties);
		
		System.err.println("written:" + System.currentTimeMillis());
		
		fos.flush();
		System.err.println("write flushed:" + System.currentTimeMillis());
		
	}
	
	/**
	 * read properties from disk
	 * 
	 * @return success True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void read() throws IOException, ClassNotFoundException {
		try {
			System.err.println("Read start:" + System.currentTimeMillis());
			
			if (channel != null) {
				channel.position(0);
			}
			System.err.println("pos0:" + System.currentTimeMillis());
			
			properties.clear();
			JsonNode data = om.readTree(fis);
			System.err.println("Read:" + System.currentTimeMillis());
			Iterator<Entry<String, JsonNode>> fieldIter = data.fields();
			
			while (fieldIter.hasNext()) {
				Entry<String, JsonNode> item = fieldIter.next();
				properties.put(item.getKey(), item.getValue());
			}
			System.err.println("Read:" + System.currentTimeMillis());
			
		} catch (EOFException eof) {
			// empty file, new agent?
		} catch (JsonMappingException jme){
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
	public synchronized boolean containsKey(String key) {
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
	public synchronized JsonNode get(String key) {
		JsonNode result = null;
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
	public synchronized JsonNode locPut(String key, JsonNode value) {
		JsonNode result = null;
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
	public synchronized boolean locPutIfUnchanged(String key, JsonNode newVal,
			JsonNode oldVal) {
		boolean result = false;
		try {
			openFile();
			read();
			if (!(oldVal == null && properties.containsKey(key) && properties
					.get(key) != null)
					|| (properties.get(key) != null && properties.get(key)
							.equals(oldVal))) {
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
	public synchronized Object remove(String key) {
		Object result = null;
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

}
