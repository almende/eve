package com.almende.eve.state;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentFactory;

public class FileStateFactory implements StateFactory {

private String path = null;
private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
private Map<String,FileState> states = new HashMap<String,FileState>();
	
	/**
	 * This constructor is called when constructed by the AgentFactory
	 * @param agentFactory
	 * @param params
	 */
	public FileStateFactory (AgentFactory agentFactory, Map<String, Object> params) {
		// built the path where the agents will be stored
		this((params != null) ? (String) params.get("path") : null);
	}
	
	public FileStateFactory (String path) {
		setPath(path);
	}
	
	/**
	 * Set the path where the agents data will be stored
	 * @param path
	 */
	private void setPath(String path) {
		if (path == null) {
			path = ".eveagents";
			logger.warning(
				"Config parameter 'state.path' missing in Eve " +
				"configuration. Using the default path '" + path + "'");
		}
		if (!path.endsWith("/")) path += "/";
		this.path = path;
		
		// make the directory
		File file = new File(path);
		file.mkdir();
        
        // log info
        String info = "Agents will be stored in ";
        try {
			info += file.getCanonicalPath();
		} catch (IOException e) {
			info += path;
		}
        logger.info(info);
	}
	
	/**
	 * Get state with given id. Will return null if not found
	 * @param agentId
	 * @return state
	 */
	@Override
	public FileState get(String agentId) {
		FileState state = null;
		if (exists(agentId)) {
			if (states.containsKey(agentId)){
				state = states.get(agentId);
			} else {
				state = new ConcurrentFileState(agentId, getFilename(agentId));
				states.put(agentId, state);
			}
		}
		return state;
	}

	/**
	 * Create a state with given id. Will throw an exception when already.
	 * existing.
	 * @param agentId
	 * @return state
	 */
	@Override
	public synchronized FileState create(String agentId) throws Exception {
		if (exists(agentId)) {
			throw new Exception("Cannot create state, " + 
					"state with id '" + agentId + "' already exists.");
		}
		
		// store the new (empty) file
		// TODO: it is not so nice solution to create an empty file to mark the state as created.		
		String filename = getFilename(agentId);
		File file = new File(filename);
		file.createNewFile();
		
		// instantiate the state
		FileState state =  new ConcurrentFileState(agentId, filename);
		states.put(agentId, state);
		return state;
	}
	
	/**
	 * Delete a state. If the state does not exist, nothing will happen.
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
	 * @param agentId
	 */
	@Override
	public boolean exists(String agentId) {
		File file = new File(getFilename(agentId));
		return file.exists();
	}

	/**
	 * Get the current environment. 
	 * In case of a file state, it tries to read the environment name from a 
	 * file called "_environment", on error/non-existence this will return "Production".
	 * 
	 * @return environment
	 */
	/* TODO: cleanup getEnvironment
	@Override
	public String getEnvironment() {
		String environment = "Production";
		File file = new File((path != null ? path : "") + "_environment");
		if (file.exists()){
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				if (line != null && !"".equals(line)){
					environment = line;
				}
			} catch (Exception e){ 
				//TODO: How to handle this error? (File not readable, not containing text, etc.)			
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return environment;
	}
	*/

	/**
	 * Get the filename of the saved
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
}
