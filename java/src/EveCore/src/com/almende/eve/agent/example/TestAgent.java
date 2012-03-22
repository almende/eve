/**
 * @file TestAgent.java
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
 * Copyright © 2010-2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-03-05
 */
package com.almende.eve.agent.example;

import java.util.List;
import java.util.Map;

import com.almende.eve.agent.Agent;
import com.almende.eve.entity.Person;
import com.almende.eve.json.annotation.ParameterName;


// TODO: put TestAgent in a separate test project

@SuppressWarnings("serial")
public class TestAgent extends Agent {
	public String ping(@ParameterName("message") String message) {
		return message;
	}
	
	public String getName(@ParameterName("person") Person person) {
		return person.getName();
	}

	public Person getPerson(@ParameterName("name") String name) {
		Person person = new Person();
		person.setName(name);
		return person;
	}

	public Double add(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a + b;
	}

	public Double multiply(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a * b;
	}

	// TODO: get this working
	public Double sum(@ParameterName("values") List<Double> values) {
		Double sum = new Double(0);
		
		for (Double value : values) {
			sum += value;
		}
		
		return sum;
	}

	public void complexParameter(
			@ParameterName("values") Map<String, List<Double>> values) {
	}
	
	
	public Double increment() {
		Double value = new Double(0);
		if (context.has("count")) {
			value = (Double)context.get("count");
		}
		value++;
		context.put("count", value);

		return value;
	}
	
	public Object get(@ParameterName("key") String key) {
		return context.get(key);
	}

	public void put(@ParameterName("key") String key, 
			@ParameterName("value") Object value) {
		context.put(key, value);
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
