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

public class FileStateFactory implements StateFactory {
	
	private String				path	= null;
	private Boolean				json	= false;
	private Logger				logger	= Logger.getLogger(this.getClass()
												.getSimpleName());
	private Map<String, State>	states	= new HashMap<String, State>();
	
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
		if (!file.exists() && !file.mkdir()) {
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
	public State get(String agentId, boolean json) {
		State state = null;
		if (exists(agentId)) {
			if (states.containsKey(agentId)) {
				state = states.get(agentId);
			} else {
				if (json) {
					state = new ConcurrentJsonFileState(agentId,
							getFilename(agentId));
				} else {
					state = new ConcurrentSerializableFileState(agentId,
							getFilename(agentId));
				}
				states.put(agentId, state);
			}
		}
		return state;
	}
	
	@Override
	public State get(String agentId) {
		return get(agentId, this.json);
	}
	
	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * 
	 * @param agentId
	 * @return state
	 */
	public synchronized State create(String agentId, boolean json)
			throws IOException {
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
		
		State state = null;
		// instantiate the state
		if (json) {
			state = new ConcurrentJsonFileState(agentId, filename);
		} else {
			state = new ConcurrentSerializableFileState(agentId, filename);
		}
		states.put(agentId, state);
		return state;
	}
	
	@Override
	public synchronized State create(String agentId) throws IOException {
		return create(agentId, this.json);
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

		String apath = path!=null ? path : "./";

		//try 1 level of subdirs. I need this badly, tymon
		File folder = new File(apath);
		File[] files = folder.listFiles();
		List<File> totalList = Arrays.asList(files);
		for (File file : totalList)
		{
			if( !file.isDirectory() )continue;
			String ret = apath + file.getName()+"/"+ agentId;
			if( new File( ret ).exists() )return ret;
		}

		return apath + agentId;
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
		if (files == null) {
			files = new File[0];
		}
		final List<File> list = new ArrayList<File>(files.length);
		
		if (files.length > 0) {
			List<File> totalList = Arrays.asList(files);
			for (File file : totalList) {
				if (file.isFile() && file.canRead() && !file.isHidden()
						&& file.length() > 2) {
					list.add(file);
				}
		
				//try 1 level of subdirs. i need this badly, tymon
				if( file.isDirectory() && file.canRead() )
				{
					File folder2 = new File(path+ file.getName() );
					File[] files2 = folder2.listFiles();
					List<File> totalList2 = Arrays.asList(files2);
					for (File file2 : totalList2) {
						if( file2.isFile() && file2.canRead() )
							list.add( file2 );
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
