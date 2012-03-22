package com.almende.eve.test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class Test {
	
	public static double add(double a, double b) {
		return a + b;
	}
	
    public static void main(String[] args) {
    	System.out.println("Test");

    	JSON j = new JSONObject();
    	
    	System.out.println("j=" + j);
    }
}
