package com.almende.eve.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.scheduler.RunnableScheduler;
import com.almende.eve.scheduler.Scheduler;

/**
 * @class FileContext
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
 *     AgentFactory factory = AgentFactory(config);<br>
 *     Context context = new Context(factory, "agentClass", "agentId");<br>
 *     context.put("key", "value");<br>
 *     System.out.println(context.get("key")); // "value"<br>
 * 
 * @author jos
 */
// TODO: create an in memory cache and reduce the number of reads/writes
public class FileContext extends Context {
	public FileContext() {}

	public FileContext(AgentFactory agentFactory, String agentClass, 
			String agentId) {
		super(agentFactory, agentClass, agentId);
	}
	
	@Override
	public String getEnvironment() {
		return "Production";
	}

	@Override
	public Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = new RunnableScheduler();
			scheduler.setContext(this);
		}
		return scheduler;
	}

	private String getFilename() {
		if (filename == null) {
			String path = getPath();
			filename = (path != null ? path : "") + agentClass + "." + agentId;
		}
		return filename;		
	}
	
	/**
	 * write properties to disk
	 * @return success   True if successfully written
	 * @throws IOException
	 */
	private boolean write() {
		try {
			FileOutputStream fos = new FileOutputStream(getFilename());
			ObjectOutput out = new ObjectOutputStream(fos);   
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
	 * @return success   True if successfully read
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private boolean read() {
		try {
			FileInputStream fis = new FileInputStream(getFilename());
			ObjectInput in = new ObjectInputStream(fis);
			properties = (Map<String, Object>) in.readObject();
			fis.close();
			in.close();
			return true;
		} catch (FileNotFoundException e) {
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
	 * destroy is executed once after the agent method is invoked
	 * if the properties are changed, they will be saved
	 */
	@Override
	public void destroy() {
	}
	
	/**
	 * Get the path where the data of the agents will be stored
	 * @return
	 */
	private String getPath() {
		if (path == null) {
			// retrieve the path from config file
			path = getConfig().get("context", "path");
			if (path == null) {
				path = ".eveagents";
				logger.warning(
					"Config parameter 'context.path' missing in Eve " +
					"configuration. Using the default path '" + path + "'");
			}
			if (!path.endsWith("/")) path += "/";
			
			// make the directory
			File f = new File(path);
            f.mkdir();
            
            // log info
            String info = "Agents data will be stored in ";
            try {
				info += f.getCanonicalPath();
			} catch (IOException e) {
				info += path;
			}
            logger.info(info);
		}
		return path;
	}
	
	/**
	 * Delete the file from disk
	 * @return
	 */
	private boolean delete() {
		File file = new File(getFilename());
		return file.delete();
	}
	
	@Override
	public synchronized void clear() {
		read();
		properties.clear();
		delete();
	}

	@Override
	public synchronized Set<String> keySet() {
		read();
		return properties.keySet();
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		read();
		return properties.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		read();
		return properties.containsValue(value);
	}

	@Override
	public synchronized Set<java.util.Map.Entry<String, Object>> entrySet() {
		read();
		return properties.entrySet();
	}

	@Override
	public synchronized Object get(Object key) {
		read();
		return properties.get(key);
	}

	@Override
	public synchronized boolean isEmpty() {
		read();
		return properties.isEmpty();
	}

	@Override
	public synchronized Object put(String key, Object value) {
		read();
		Object ret = properties.put(key, value);
		write();
		return ret;
	}

	@Override
	public synchronized void putAll(Map<? extends String, ? extends Object> map) {
		read();
		properties.putAll(map);
		write();
	}

	@Override
	public synchronized Object remove(Object key) {
		read();
		Object value = properties.remove(key);
		write();
		return value; 
	}

	@Override
	public synchronized int size() {
		read();
		return properties.size();
	}

	@Override
	public synchronized Collection<Object> values() {
		read();
		return properties.values();
	}
	
	private String filename = null;
	private Map<String, Object> properties = new HashMap<String, Object>();
	private Scheduler scheduler = null;
	
	private String path = null;
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
