/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Class ConcurrentSerializableFileState.
 * 
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
 * @author jos
 * @author ludo
 */
public class ConcurrentSerializableFileState extends
		AbstractState<Serializable> {
	private static final Logger				LOG			= Logger.getLogger("ConcurrentFileState");
	private String							filename	= null;
	private FileChannel						channel		= null;
	private FileLock						lock		= null;
	private InputStream						fis			= null;
	private OutputStream					fos			= null;
	private static Map<String, Boolean>		locked		= new ConcurrentHashMap<String, Boolean>();
	private final Map<String, Serializable>	properties	= Collections
																.synchronizedMap(new HashMap<String, Serializable>());
	
	/**
	 * Instantiates a new concurrent serializable file state.
	 * 
	 * @param agentId
	 *            the agent id
	 * @param filename
	 *            the filename
	 */
	public ConcurrentSerializableFileState(final String agentId,
			final String filename) {
		super(agentId);
		this.filename = filename;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#finalize()
	 */
	@Override
	public void finalize() throws Throwable {
		closeFile();
		super.finalize();
	}
	
	/**
	 * Open file.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("resource")
	protected void openFile() throws IOException {
		synchronized (locked) {
			while (locked.containsKey(filename) && locked.get(filename)) {
				try {
					locked.wait();
				} catch (final InterruptedException e) {
				}
			}
			locked.put(filename, true);
			
			final File file = new File(filename);
			if (!file.exists()) {
				locked.put(filename, false);
				locked.notifyAll();
				throw new IllegalStateException(
						"Warning: File doesn't exist (anymore):'" + filename
								+ "'");
			}
			channel = new RandomAccessFile(file, "rw").getChannel();
			
			try {
				// TODO: add support for shared locks, allowing parallel reading
				// operations.
				lock = channel.lock();
			} catch (final Exception e) {
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
		}
	}
	
	/**
	 * Close file.
	 */
	protected void closeFile() {
		
		synchronized (locked) {
			
			if (lock != null && lock.isValid()) {
				try {
					
					lock.release();
				} catch (final IOException e) {
					LOG.log(Level.WARNING, "", e);
				}
			}
			
			try {
				if (fos != null) {
					fos.close();
				}
				if (fis != null) {
					fis.close();
				}
				if (channel != null) {
					channel.close();
				}
			} catch (final IOException e) {
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
	 * write properties to disk.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void write() throws IOException {
		if (channel != null) {
			channel.position(0);
		}
		final ObjectOutput out = new ObjectOutputStream(fos);
		out.writeObject(properties);
		out.flush();
		
		if (channel != null) {
			channel.truncate(channel.position());
		}
	}
	
	/**
	 * read properties from disk.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	@SuppressWarnings("unchecked")
	private void read() throws IOException, ClassNotFoundException {
		try {
			if (channel != null) {
				channel.position(0);
			}
			
			properties.clear();
			final ObjectInput in = new ObjectInputStream(fis);
			
			properties.putAll((Map<String, Serializable>) in.readObject());
			
		} catch (final EOFException eof) {
			// empty file, new agent?
		}
	}
	
	/**
	 * init is executed once before the agent method is invoked.
	 */
	@Override
	public void init() {
	}
	
	/**
	 * destroy is executed once after the agent method is invoked if the
	 * properties are changed, they will be saved.
	 */
	@Override
	public void destroy() {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.State#clear()
	 */
	@Override
	public synchronized void clear() {
		try {
			openFile();
			final String agentType = (String) properties.get(KEY_AGENT_TYPE);
			properties.clear();
			properties.put(KEY_AGENT_TYPE, agentType);
			write();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.State#keySet()
	 */
	@Override
	public synchronized Set<String> keySet() {
		Set<String> result = null;
		try {
			openFile();
			read();
			result = new HashSet<String>(properties.keySet());
			
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.State#containsKey(java.lang.String)
	 */
	@Override
	public synchronized boolean containsKey(final String key) {
		boolean result = false;
		try {
			openFile();
			read();
			result = properties.containsKey(key);
			
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.AbstractState#get(java.lang.String)
	 */
	@Override
	public synchronized Serializable get(final String key) {
		Serializable result = null;
		try {
			openFile();
			read();
			result = properties.get(key);
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.AbstractState#locPut(java.lang.String,
	 * java.io.Serializable)
	 */
	@Override
	public synchronized Serializable locPut(final String key,
			final Serializable value) {
		Serializable result = null;
		try {
			openFile();
			read();
			result = properties.put(key, value);
			write();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.almende.eve.state.AbstractState#locPutIfUnchanged(java.lang.String,
	 * java.io.Serializable, java.io.Serializable)
	 */
	@Override
	public synchronized boolean locPutIfUnchanged(final String key,
			final Serializable newVal, final Serializable oldVal) {
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
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
			// Don't let users loop if exception is thrown. They
			// would get into a deadlock....
			result = true;
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.State#remove(java.lang.String)
	 */
	@Override
	public synchronized Object remove(final String key) {
		Object result = null;
		try {
			openFile();
			read();
			result = properties.remove(key);
			write();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.almende.eve.state.State#size()
	 */
	@Override
	public synchronized int size() {
		int result = -1;
		try {
			openFile();
			read();
			result = properties.size();
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
		closeFile();
		return result;
	}
	
}
