/**
 * @file GoogleDirectionsAgent.java
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
 * @date	  2011-03-25
 */

package com.almende.eve.agent.google;

import java.io.IOException;
import java.net.URISyntaxException;
//import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.ParameterName;
import com.almende.eve.json.util.HttpUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

@SuppressWarnings("serial")
public class GoogleDirectionsAgent extends Agent {
	static final String DIRECTIONS_SERVICE_URL = 
		"http://maps.googleapis.com/maps/api/directions/json";
	// TODO: get https working - requires SSL certificate

	// Documentation: 
	// http://code.google.com/apis/maps/documentation/directions/
	// http://code.google.com/apis/maps/documentation/webservices/index.html
    // http://code.google.com/apis/loader/signup.html
	// http://code.google.com/p/gwt-google-apis/source/browse/trunk/maps/samples/hellomaps/src/com/google/gwt/maps/sample/hellomaps/client/SimpleDirectionsDemo.java?r=1875
	// 
	// key for 
	//    http://agentplatform.appspot.com/ 
	// on account 
	//    wjosdejong@gmail.com
	// is: 
	//    ABQIAAAAQOJzPEiBDTDlB2oHxRVmTxRSrjmNg-hdT5E1_a3uQ7J2AKkR7hTFenoJvK-F_h8dho7B4VXJZx1pdg
	//
	// TODO: if signing url is not needed, remove classes Base64 and UrlSigner again
	
	//private static String keyString = "ABQIAAAAQOJzPEiBDTDlB2oHxRVmTxRSrjmNg-hdT5E1_a3uQ7J2AKkR7hTFenoJvK-F_h8dho7B4VXJZx1pdg";
	
	public JSONObject getDirections(@ParameterName("origin") String origin, 
			@ParameterName("destination") String destination) 
			throws IOException, JSONException, InvalidKeyException, 
			NoSuchAlgorithmException, URISyntaxException {
		
		// TODO: use java API instead of URL fetch? -> I get OVER_QUERY_LIMIT issues
		// when deployed.
		String url = DIRECTIONS_SERVICE_URL 
			+ "?origin="      + URLEncoder.encode(origin, "UTF-8")
			+ "&destination=" + URLEncoder.encode(destination, "UTF-8")
			// + "&mode=driving"    // driving, walking, or bicycling 
			// + "&language=nl"     // nl, en, ...
			+ "&sensor=false"
			//+ "&key=" + keyString // TODO: check if adding this key solves the issue...
			;
		
		//* Does not work when deployed on google app engine, we need to sign with key
		String response = HttpUtil.get(url);
		//*/
		
		/* TODO: use url signing
	    // Convert the string to a URL so we can parse it
	    System.out.println("key: " + keyString);
	    
	    URL u = new URL(url);
	    String clientID = ...
	    UrlSigner signer = new UrlSigner(cientId);
	    String request = u.getProtocol() + "://" + u.getHost() + 
	    	signer.signRequest(u.getPath(), u.getQuery());
	    
		System.out.println("url: " + url);
		System.out.println("request: " + request);

		String response = fetch(request);
		
		System.out.println("response: " + response);
		//*/

		JSONObject directions = (JSONObject) JSONSerializer.toJSON(response);
		
		// Check if status is "OK". Error status can for example be "NOT_FOUND"
		String status = directions.getString("status");
		if (!status.equals("OK")) {
			throw new RuntimeException(status);
		}
		
		return directions;
	}
	
	/**
	 * Retrieve the duration of the directions from origin to destination
	 * in seconds.
	 * @param origin
	 * @param destination
	 * @return duration      Duration in seconds
	 * @throws IOException
	 * @throws JSONException
	 * @throws URISyntaxException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public int getDuration(@ParameterName("origin") String origin, 
			@ParameterName("destination") String destination) 
			throws IOException, JSONException, InvalidKeyException, 
			NoSuchAlgorithmException, URISyntaxException {
		JSONObject directions = getDirections(origin, destination);
		
		// sum the durations of all steps
		JSONArray routes = directions.getJSONArray("routes");
		JSONObject route = routes.getJSONObject(0);
		JSONArray legs = route.getJSONArray("legs");
		JSONObject leg = legs.getJSONObject(0);
		JSONObject jsonDuration = leg.getJSONObject("duration");
	
		int duration = jsonDuration.getInt("value");		
		return duration;
	}
	
	/**
	 * Retrieve the duration of the directions from origin to destination
	 * in readable text, for example "59 mins"
	 * @param origin
	 * @param destination
	 * @return duration      Duration in text
	 * @throws IOException
	 * @throws JSONException
	 * @throws URISyntaxException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public String getDurationHuman(@ParameterName("origin") String origin, 
			@ParameterName("destination") String destination) 
			throws IOException, JSONException, InvalidKeyException, 
			NoSuchAlgorithmException, URISyntaxException {
		JSONObject directions = getDirections(origin, destination);
		
		// sum the durations of all steps
		JSONArray routes = directions.getJSONArray("routes");
		JSONObject route = routes.getJSONObject(0);
		JSONArray legs = route.getJSONArray("legs");
		JSONObject leg = legs.getJSONObject(0);
		JSONObject jsonDuration = leg.getJSONObject("duration");
	
		String duration = jsonDuration.getString("text");		
		return duration;
	}
	
	
	/**
	 * Retrieve the distance between origin to destination in meters
	 * @param origin
	 * @param destination
	 * @return duration      Distance in meters
	 * @throws IOException
	 * @throws JSONException
	 * @throws URISyntaxException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public int getDistance(@ParameterName("origin") String origin, 
			@ParameterName("destination") String destination) 
			throws IOException, JSONException, InvalidKeyException, 
			NoSuchAlgorithmException, URISyntaxException {
		JSONObject directions = getDirections(origin, destination);
		
		// sum the durations of all steps
		JSONArray routes = directions.getJSONArray("routes");
		JSONObject route = routes.getJSONObject(0);
		JSONArray legs = route.getJSONArray("legs");
		JSONObject leg = legs.getJSONObject(0);
		JSONObject jsonDistance = leg.getJSONObject("distance");
	
		int distance = jsonDistance.getInt("value");		
		return distance;
	}
		
	/**
	 * Retrieve the distance between origin to destination in readable text,
	 * for example "74.2 km"
	 * @param origin
	 * @param destination
	 * @return duration      Distance in meters
	 * @throws IOException
	 * @throws JSONException
	 * @throws URISyntaxException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public String getDistanceHuman(@ParameterName("origin") String origin, 
			@ParameterName("destination") String destination) 
			throws IOException, JSONException, InvalidKeyException, 
			NoSuchAlgorithmException, URISyntaxException {
		JSONObject directions = getDirections(origin, destination);
		
		// sum the durations of all steps
		JSONArray routes = directions.getJSONArray("routes");
		JSONObject route = routes.getJSONObject(0);
		JSONArray legs = route.getJSONArray("legs");
		JSONObject leg = legs.getJSONObject(0);
		JSONObject jsonDistance = leg.getJSONObject("distance");
	
		String distance = jsonDistance.getString("text");		
		return distance;
	}
	
	@Override
	public String getVersion() {
		return "0.1";
	}
	
	@Override
	public String getDescription() {
		return 
		"This agent is capable of providing directions information " +
		"using the Google Maps Web Services.";
	}	
}
