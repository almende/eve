/**
 * @file GoogleCalculatorAgent.java
 * 
 * @brief 
 * GoogleCalculatorAgent can evaluate mathematical expressions. 
 * It uses the Google calculator API.
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
 * Copyright © 2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-04-13
 */

package com.almende.eve.agent.google;

import java.net.URLEncoder;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GoogleCalculatorAgent extends Agent {
	static private String CALC_API_URL = "http://www.google.com/ig/calculator";

	/**
	 * Evaluate given expression
	 * For example expr="2.5 + 3 / sqrt(16)" will return "3.25"
	 * @param expr
	 * @return result
	 * @throws Exception
	 */
	public String eval(@Name("expr") String expr) throws Exception {
		String url = CALC_API_URL + "?q=" + URLEncoder.encode(expr, "UTF-8");
		String resp = HttpUtil.get(url);

		// the field names in resp are not enclosed by quotes :( 
		resp = resp.replaceAll("lhs:", "\"lhs\":");
		resp = resp.replaceAll("rhs:", "\"rhs\":");
		resp = resp.replaceAll("error:", "\"error\":");
		resp = resp.replaceAll("icc:", "\"icc\":");
		
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode json = mapper.readValue(resp, ObjectNode.class);
		
		String error = json.get("error").asText();
		if (error != null && !error.isEmpty()) {
			throw new Exception(error);
		}
		
		String rhs = json.get("rhs").asText();
		return rhs;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return 
			"GoogleCalculatorEvent can evaluate mathematical expressions. " + 
			"It uses the Google calculator API.";
	}
}
