package com.almende.eve.state;



import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.almende.eve.agent.AgentFactory;

public class AndroidStateFactory implements StateFactory {
		private Context appCtx;

		public AndroidStateFactory (AgentFactory agentFactory, Map<String, Object> params) throws Exception {
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

		@Override
		public String toString() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("class", this.getClass().getName());
			return data.toString();
		}

	}
