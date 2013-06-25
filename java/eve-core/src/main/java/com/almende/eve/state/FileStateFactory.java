package com.almende.eve.state;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sun.net.www.protocol.file.FileURLConnection;

@SuppressWarnings("restriction")
public class FileStateFactory implements StateFactory {
	
	private String					path	= null;
	private Boolean					json	= false;
	private Logger					logger	= Logger.getLogger(this.getClass()
													.getSimpleName());
	private Map<String, FileState>	states	= new HashMap<String, FileState>();
	
	/**
	 * This constructor is called when constructed by the AgentHost
	 * 
	 * @param params
	 */
	public FileStateFactory(Map<String, Object> params) {
		// built the path where the agents will be stored
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		if (params.containsKey("json")) {
			this.json = (Boolean) params.get("json");
		}
		
		if (params.containsKey("path")) {
			setPath((String) params.get("path"));
		}
	}
	
	public FileStateFactory(String path, Boolean json) {
		this.json = json;
		setPath(path);
	}
	
	public FileStateFactory(String path) {
		this(path, false);
	}
	
	/**
	 * Set the path where the agents data will be stored
	 * 
	 * @param path
	 */
	private synchronized void setPath(String path) {
		if (path == null) {
			path = ".eveagents";
			logger.warning("Config parameter 'state.path' missing in Eve "
					+ "configuration. Using the default path '" + path + "'");
		}
		if (!path.endsWith("/")) {
			path += "/";
		}
		this.path = path;
		
		// make the directory
		File file = new File(path);
		if (!file.exists() && !file.mkdir()){
			logger.severe("Could not create State folder!");
			throw new IllegalStateException();
		}
		
		// log info
		String info = "Agents will be stored in ";
		try {
			info += file.getCanonicalPath();
		} catch (IOException e) {
			info += path;
		}
		logger.info(info
				+ ". "
				+ (this.json ? "(stored in JSON format)"
						: "(stored in JavaObject format)"));
	}
	
	/**
	 * Get state with given id. Will return null if not found
	 * 
	 * @param agentId
	 * @return state
	 */
	@Override
	public FileState get(String agentId) {
		FileState state = null;
		if (exists(agentId)) {
			if (states.containsKey(agentId)) {
				state = states.get(agentId);
			} else {
				state = new ConcurrentFileState(agentId, getFilename(agentId),
						this.json);
				states.put(agentId, state);
			}
		}
		return state;
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * 
	 * @param agentId
	 * @return state
	 */
	@Override
	public synchronized FileState create(String agentId) throws IOException {
		if (exists(agentId)) {
			throw new IllegalStateException("Cannot create state, "
					+ "state with id '" + agentId + "' already exists.");
		}
		
		// store the new (empty) file
		// TODO: it is not so nice solution to create an empty file to mark the
		// state as created.
		String filename = getFilename(agentId);
		File file = new File(filename);
		file.createNewFile();
		
		// instantiate the state
		FileState state = new ConcurrentFileState(agentId, filename, this.json);
		states.put(agentId, state);
		return state;
	}
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
	 * 
	 * @param agentId
	 */
	@Override
	public void delete(String agentId) {
		File file = new File(getFilename(agentId));
		if (file.exists()) {
			file.delete();
		}
		states.remove(agentId);
	}
	
	/**
	 * Test if a state with given agentId exists
	 * 
	 * @param agentId
	 */
	@Override
	public boolean exists(String agentId) {
		File file = new File(getFilename(agentId));
		return file.exists();
	}
	
	/**
	 * Get the filename of the saved
	 * 
	 * @param agentId
	 * @return
	 */
	private String getFilename(String agentId) {
		return (path != null ? path : "") + agentId;
	}
	
	@Override
	public String toString() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("path", path);
		return data.toString();
	}
	
	@Override
	public Iterator<String> getAllAgentIds() {
		File folder = new File(path);
		File[] files = folder.listFiles();
		if (files == null){
			files=new File[0];
		}
		final List<File> list = new ArrayList<File>(files.length);
		
		if (files.length > 0) {
			List<File> totalList = Arrays.asList(files);
			for (File file : totalList) {
				if (file.isFile() && file.canRead() && !file.isHidden()
						&& file.length() > 2) {
					try {
						FileURLConnection conn = (FileURLConnection) file
								.toURI().toURL().openConnection();
						if (!json
								&& conn.getContentType().endsWith(
										"java-serialized-object")) {
							list.add(file);
						}
						if (json && conn.getContentType().contains("json")) {
							list.add(file);
						}
						conn.close();
					} catch (Exception e) {
						logger.warning("Couldn't check contentType of potential state file:"
								+ file.getName());
					} catch (java.lang.NoClassDefFoundError e){
						logger.warning("Couldn't check contentType of state file:"
								+ file.getName());
						list.add(file);
					}
				}
			}
		}
		return new Iterator<String>() {
			private int	pivot	= 0;
			
			@Override
			public boolean hasNext() {
				return pivot < list.size();
			}
			
			@Override
			public String next() {
				return (list.get(pivot++).getName());
			}
			
			@Override
			public void remove() {
				list.remove(pivot);
			}
		};
	}
}
