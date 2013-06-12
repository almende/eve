/**
 * @file EchoAgent.java
 * 
 * @brief 
 * TODO: brief
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2010-2011 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-03-05
 */
package com.almende.eve.agent.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;

@Access(AccessType.PUBLIC)
public class EchoAgent extends Agent {
	public Object ping(@Name("message") Object message) throws IOException {
		// trigger event
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("message", message);
		getEventsFactory().trigger("ping", params);
		
		// return the message itself
		return message;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	@Override
	public String getDescription() {
		return 
		"This agent can be used for test purposes. " +
		"It contains a simple ping method.";
	}	
}
