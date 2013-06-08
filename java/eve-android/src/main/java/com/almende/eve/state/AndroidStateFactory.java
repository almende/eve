package com.almende.eve.state;



import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;

public class AndroidStateFactory implements StateFactory {
		private Context appCtx;

		public AndroidStateFactory (Map<String, Object> params) throws Exception {
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
		 * @throws IOException, FileNotFoundException 
		 */
		@Override
		public synchronized AndroidState create(String agentId) throws IOException, FileNotFoundException{
			if (exists(agentId)) {
				throw new IllegalStateException("Cannot create state, " + 
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


		@Override
		public Iterator<String> getAllAgentIds() {
			List<String> files = Arrays.asList(appCtx.fileList());
			return files.iterator();
		}

	}
