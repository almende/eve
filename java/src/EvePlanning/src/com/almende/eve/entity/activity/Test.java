package com.almende.eve.entity.activity;


import org.joda.time.DateTime;

import com.almende.eve.json.jackson.JOM;


public class Test {
	public static void main(String[] args) {
		  String json = "{\"summary\":\"Test 5\",\"constraints\":{\"attendees\":[{\"displayName\":null,\"email\":null,\"agent\":\"http://eveagents.appspot.com/agents/googlecalendaragent/jos\",\"mustAttend\":true}],\"locations\":[],\"time\":{\"duration\":60,\"durationMin\":null,\"durationMax\":null,\"periodStart\":null,\"periodEnd\":null}},\"status\":{\"description\":null,\"attendees\":[],\"location\":null,\"start\":\"2012-06-01T10:00:00.000Z\",\"end\":\"2012-06-01T12:00:00.000Z\",\"updated\":\"2012-06-01T09:17:13.421Z\"}}";
		  
		  try {
			Activity a = JOM.getInstance().readValue (json, Activity.class);
			Activity b = new Activity();
			b.setSummary("B");
			
			a.getStatus().setUpdated(null);
			b.getStatus().setUpdated(DateTime.now().toString());
			Activity c = Activity.sync(a, b);
			
			System.out.println("a=" + JOM.getInstance().writeValueAsString(a));
			System.out.println("b=" + JOM.getInstance().writeValueAsString(b));
			System.out.println("c=" + JOM.getInstance().writeValueAsString(c));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		  
	}
}
