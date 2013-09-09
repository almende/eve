/**
 * @file GoogleTranslateAgent.java
 * 
 * @brief 
 * GoogleTranslateAgent can translate text, using the Google Translator API. 
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
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.util.HttpUtil;

@Access(AccessType.PUBLIC)
public class GoogleTranslateAgent extends Agent {
	static private String TRANSLATE_API_URL = 
		"https://www.googleapis.com/language/translate/v2";

	/**
	 * Set the API Key for using the paid Google Translate service.
	 * @param key
	 */
	public void setKey(@Name("key") String key) {
		getState().put("key", key);
	}
	
	/**
	 * Translate text
	 * @param text   The text to be translated
	 * @param target The target language code, for example "en"
	 * @param source The source language code, for example "nl". Optional
	 * @return
	 * @throws Exception
	 */
	public String translate(@Name("text") String text,
			@Name("target") String target, 
			@Name("source") @Required(false) String source) throws Exception {
		String key = getState().get("key",String.class);
		if (key == null) {
			throw new Exception("No valid API Key set. " +
					"Google Translate API is a paid service " +
					"and requires an API Key for billing.");
		}
		
		String url = TRANSLATE_API_URL + 
			"?q=" + URLEncoder.encode(text, "UTF-8") +
			"&key=" + URLEncoder.encode(key, "UTF-8") +
			"&target=" + URLEncoder.encode(target, "UTF-8");
		if (source != null) {
			url += "&source=" + URLEncoder.encode(source, "UTF-8");
		}
		String resp = HttpUtil.get(url);

		return resp;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return 
			"GoogleTranslateAgent can translate text, " +
			"using the Google Translator API. " +
			"Before you can use a GoogleTranslateAgent, " +
			"a valid API Key must be set using the method setKey.";
	}
}
