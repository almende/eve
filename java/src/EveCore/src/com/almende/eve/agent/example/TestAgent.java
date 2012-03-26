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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
		System.out.println("marks:" + person.getMarks());
		Object mark = person.getMarks().get(3);
		System.out.println("mark[3]:" + mark.getClass().getName());
		return person.getName();
	}

	public Double getMarksAvg(@ParameterName("person") Person person) {
		List<Double> marks = person.getMarks();
		Double sum = new Double(0);
		if (marks != null) {
			for (Double mark : marks) {
				sum += mark;
			}
		}
		return sum;
	}

	public Person getPerson(@ParameterName("name") String name) {
		Person person = new Person();
		person.setName(name);
		List<Double> marks = new ArrayList<Double>();
		marks.add(6.8);
		marks.add(5.0);
		person.setMarks(marks);
		return person;
	}

	public Double add(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a + b;
	}

	public Double subtract(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a - b;
	}

	public Double multiply(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a * b;
	}

	public Double divide(@ParameterName("a") Double a, 
			@ParameterName("b") Double b) {
		return a / b;
	}

	public String printParams(JSONObject params) {
		return "fields: " + params.size();
	}

	public void throwInternalError() throws Exception {
		throw new Exception("Something went wrong...");
	}
	
	public Double sum(@ParameterName("values") List<Double> values) {
		Double sum = new Double(0);
		for (Double value : values) {
			sum += value;
		}
		return sum;
	}
	
	public Double sumArray(@ParameterName("values") Double[] values) {
		Double sum = new Double(0);
		for (Double value : values) {
			sum += value;
		}
		return sum;
	}

	public Double sumJSONArray(@ParameterName("values") JSONArray values) {
		Double sum = new Double(0);
		for (int i = 0; i < values.size(); i++) {
			sum += values.getDouble(i);
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
	
	public String get(@ParameterName("key") String key) {
		return (String)context.get(key);
	}

	public void put(@ParameterName("key") String key, 
			@ParameterName("value") String value) {
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
