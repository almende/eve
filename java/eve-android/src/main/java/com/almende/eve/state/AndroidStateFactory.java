package com.almende.eve.state;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.almende.eve.agent.AgentFactory;
import com.almende.eve.state.StateFactory;

public class AndroidStateFactory extends StateFactory {
		private Context appCtx;

		public AndroidStateFactory (AgentFactory agentFactory, Map<String, Object> params) throws Exception {
			super(agentFactory, params);
			if (params == null || !params.containsKey("AppContext")) throw new Exception("AppContext parameter is required!");
			appCtx = (params != null) ? (Context) params.get("AppContext") : null;
		}
		
		
		/**
		 * Get state with given id. Will return null if not found
		 * @param agentId
		 * @return state
		 */
		@Override
		public AndroidState get(String agentId) {
			if (exists(agentId)) {
				return new AndroidState(agentId, appCtx);
			}
			return null;
		}

		/**
		 * Create a state with given id. Will throw an exception when already.
		 * existing.
		 * @param agentId
		 * @return state
		 */
		@Override
		public synchronized AndroidState create(String agentId) throws Exception {
			if (exists(agentId)) {
				throw new Exception("Cannot create state, " + 
						"state with id '" + agentId + "' already exists.");
			}
			
			FileOutputStream fos = appCtx.openFileOutput(agentId, Context.MODE_PRIVATE);
			fos.close();
			
			// instantiate the context
			return new AndroidState(agentId, appCtx);
		}
		
		/**
		 * Delete a state. If the context does not exist, nothing will happen.
		 * @param agentId
		 */
		@Override
		public void delete(String agentId) {
			if (exists(agentId)){
				appCtx.deleteFile(agentId);
			}
		}

		/**
		 * Test if a state with given agentId exists
		 * @param agentId
		 */
		@Override
		public boolean exists(String agentId) {
			String[] files = appCtx.fileList();
			return Arrays.asList(files).contains(agentId);
		}

		/**
		 * Get the current environment. 
		 * In case of a file context, it tries to read the environment name from a 
		 * file called "_environment", on error/non-existence this will return "Production".
		 * 
		 * @return environment
		 */
		@Override
		public String getEnvironment() {
			//TODO: How to handle this, all Android environments are Production?
			String environment = "Production";
			
			FileInputStream input = null;
			try {
				input = appCtx.openFileInput("_environment");
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new InputStreamReader(input));
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
			} catch (FileNotFoundException e1) {
			}
			return environment;
		}

		@Override
		public String toString() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("class", this.getClass().getName());
			return data.toString();
		}

	}
