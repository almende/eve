/**
 * Test OAuth 2.0 with Google API
 * 
 * Jos de Jong, 2012-05-11
 * 
 * Used libraries:
 *     jackson-annotations-2.0.0.jar
 *     jackson-core-2.0.0.jar
 *     jackson-databind-2.0.0.jar
 * 
 * Documentation:
 *     https://developers.google.com/accounts/docs/OAuth2WebServer
 */
package com.almende.eve.servlet.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.*;

import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.JSONResponse;
import com.almende.eve.json.jackson.JOM;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@SuppressWarnings("serial")
public class GoogleAuth extends HttpServlet {
	// Specify the correct client id and secret for web applications
	// Create them at the Google API console: https://code.google.com/apis/console/

	// TODO: put these constants in a configuration file instead of having them hard coded
	private String CLIENT_ID = "231599786845-p4r6ka1emoj8enivejds6vma41ni2s26.apps.googleusercontent.com";
	private String CLIENT_SECRET = "tUtHesFJEAfiyVjbJd4q0Hvq";
	String REDIRECT_URI = "https://eveagents.appspot.com/auth";
	String AGENTS_URI   = "http://eveagents.appspot.com/agents";
	String OAUTH_URI    = "https://accounts.google.com/o/oauth2";
	
	@SuppressWarnings("unused")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		PrintWriter out = resp.getWriter();
		resp.setContentType("text/html");
		out.print("<html>" +
			"<head>" +
			"<title>Authorize Eve agents</title>" +
			"<style>" +
			"body {width: 400px;}" +
			"body, th, td, input {font-family: arial; font-size: 10pt; color: #4d4d4d;}" +
			"th {text-align: left;}" + 
			"input[type=text] {border: 1px solid lightgray;}" +
			".error {color: red;}" + 
			"</style>" +
			"</head>" +
			"<body>" + 
			"<h1>Authorize Eve agents</h1>" +
			"<p>" +
			"On this page, you can grant an Eve agent access to your data, " +
			"for example access to your calendar." +
			"</p>");
		
		// read parameters code and access_token from query parameters or cookies
		String code = req.getParameter("code");
		String state = req.getParameter("state");
		String error = req.getParameter("error");
		
		if (code == null && state == null && error == null) {
			// show a form to authenticate
			String space = " ";
			String scope = 
				"https://www.googleapis.com/auth/userinfo.email" + space + 
				"https://www.googleapis.com/auth/userinfo.profile" + space +
				"https://www.googleapis.com/auth/calendar";
			Map<String, String> params = new HashMap<String, String>();
			params.put("scope", scope);
			params.put("redirect_uri", REDIRECT_URI);
			params.put("response_type", "code");
			params.put("access_type", "offline");
			params.put("client_id", CLIENT_ID);
			params.put("approval_prompt", "force");
			String url = HttpUtil.appendQueryParams(OAUTH_URI + "/auth", params);

			out.print("<script type='text/javascript'>" +
				"function auth() {" +
				"var state={" +
				"\"type\": document.getElementById('type').value," +
				"\"id\": document.getElementById('id').value" +
				"};" +
				"var url='" + url + "'+ '&state=' + encodeURI(JSON.stringify(state));" +
				"window.location.href=url;" + 
				"}" +
				"</script>" +
				"<table>" +
				"<tr>" +
				"<th>Agent type</th>" + 
				"<td><select id='type' />" +
				"<option value='GoogleCalendarAgent'>GoogleCalendarAgent</option" +
				"</select></td>" +
				"</tr>" + 
				"<tr>" + 
				"<th>Agent id</th>" + 
				"<td><input type='text' id='id' value='1' /></td>" + 
				"</tr>" + 
				"<tr>" + 
				"<th></th>" + 
				"<td><button onclick='auth();'>Authorize</button></td>" + 
				"</tr>" + 
				"</table>");
		}

		if (code != null && state != null){
			// Second step: exchange code with authentication token

			Map<String, String> params = new HashMap<String, String>();
			params.put("code", code);
			params.put("client_id", CLIENT_ID);
			params.put("client_secret", CLIENT_SECRET);
			params.put("redirect_uri", REDIRECT_URI);
			params.put("grant_type", "authorization_code");
			String res = HttpUtil.postForm(OAUTH_URI + "/token", params);

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.readValue(res, ObjectNode.class);
			String access_token = null;
			if (json.has("access_token")) {
				access_token = json.get("access_token").asText().toString();
			}
			String refresh_token = null;
			if (json.has("refresh_token")) {
				refresh_token = json.get("refresh_token").asText().toString();
			}
			String token_type = null;
			if (json.has("token_type")) {
				token_type = json.get("token_type").asText();
			}			
			Integer expires_in = null;
			if (json.has("expires_in")) {
				expires_in = json.get("expires_in").asInt();
			}			
			
			if (access_token != null && refresh_token != null) {
				ObjectNode st = mapper.readValue(state, ObjectNode.class); 
				String id = st.has("id") ? st.get("id").asText() : null;
				String type = st.has("type") ? st.get("type").asText() : null;
				
				// TODO: actually store the retrieved token in the agent
				
				String agentUrl = AGENTS_URI + "/" + type + "/" + id;
				ObjectNode rpcParams = JOM.createObjectNode();
				rpcParams.put("access_token", access_token);
				rpcParams.put("token_type", token_type);
				rpcParams.put("expires_in", expires_in);
				rpcParams.put("refresh_token", refresh_token);
				JSONRequest rpcRequest = 
					new JSONRequest("setAuthorization", rpcParams);
				JSONResponse rpcResp = JSONRPC.send(agentUrl, rpcRequest);
				
				out.print("<p>" +
					type + " with id " + id + " is authorized succesfully." +
					"</p>");
				out.print("<p>" +
						"<a href=\"" + agentUrl + "\">" + agentUrl + "</a>" +
						"</p>");
			}
			
			if (json.has("error")) {
				out.print("<p>An error occurred</p>");				
				out.print("<pre class='error'>" + 
						mapper.writeValueAsString(json.get("error")) + 
						"</pre>");				
			}
			out.print("<button onclick='window.location.href=\"" + 
						REDIRECT_URI + "\";'>Ok</button>");

			// TODO: cleanup
			out.println("<pre>");
			out.println("Authorization code exchanged for access token:");
			out.println(res);
			out.println("</pre>");
			
			// TODO: cleanup
			if (access_token != null) {
				// Third step: use the access token to call a Google API
				String url2 = "https://www.googleapis.com/oauth2/v1/userinfo";
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Authorization", token_type + " " + access_token);
				String info = HttpUtil.get(url2, headers);

				out.println("<pre>");
				out.println("Retrieved user information:");
				out.println(info);
				out.println("</pre>");
			}
		}
		
		if (error != null && state != null) {
			out.print("<p>An error occurred</p>");				
			out.print("<pre class='error'>" + error + "</pre>");				
			out.print("<button onclick='window.location.href=\"" + 
						REDIRECT_URI + "\";'>Ok</button>");			
		}
		
		out.print(
			"</body>" +
			"</html>"			
		);
	}
}
