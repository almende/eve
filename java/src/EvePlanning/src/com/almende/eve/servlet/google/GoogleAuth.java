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
import java.net.URLEncoder;
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
	String REDIRECT_URI = "https://eveagents.appspot.com/auth/google"; 
	//String REDIRECT_URI = "http://localhost:8888/auth/google"; // TODO: remove
	String AGENTS_URI   = "http://eveagents.appspot.com/agents";
	String OAUTH_URI    = "https://accounts.google.com/o/oauth2";
	
	@SuppressWarnings("unused")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// read parameters code and access_token from query parameters or cookies
		String code = req.getParameter("code");
		String state = req.getParameter("state");
		String error = req.getParameter("error");
		String agent = req.getParameter("agent");
		String callback = req.getParameter("callback");

		// directly redirect to google authorization page if an agents url is provided
		if (agent != null) {
			String url = createAuthorizationUrl();
			ObjectNode st = JOM.createObjectNode();
			st.put("agent", agent);
			if (callback != null) {
				st.put("callback", callback);
			}

			url += "&state=" + 
				URLEncoder.encode(JOM.getInstance().writeValueAsString(st), "UTF-8");
			resp.sendRedirect(url);
		}
		
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
		
		// First step: show a form to authenticate
		if (code == null && state == null && error == null) {
			String url = createAuthorizationUrl();
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

		// Second step: exchange code with authentication token
		if (code != null && state != null){
			Map<String, String> params = new HashMap<String, String>();
			params.put("code", code);
			params.put("client_id", CLIENT_ID);
			params.put("client_secret", CLIENT_SECRET);
			params.put("redirect_uri", REDIRECT_URI);
			params.put("grant_type", "authorization_code");
			String res = HttpUtil.postForm(OAUTH_URI + "/token", params);

			ObjectMapper mapper = new ObjectMapper();
			ObjectNode json = mapper.readValue(res, ObjectNode.class);

			if (!json.has("error")) {
				ObjectNode st = mapper.readValue(state, ObjectNode.class); 
				String id = st.has("id") ? st.get("id").asText() : null;
				String type = st.has("type") ? st.get("type").asText() : null;
				String agentUrl = st.has("agent") ? st.get("agent").asText() : null;
				String statusCallback = st.has("callback") ? st.get("callback").asText() : null;
				
				// TODO: actually store the retrieved token in the agent
				if (agentUrl == null) {
					agentUrl = AGENTS_URI + "/" + type + "/" + id;
				}
				JSONRequest rpcRequest = new JSONRequest("setAuthorization", json);
				JSONResponse rpcResp = JSONRPC.send(agentUrl, rpcRequest);
				
				if (statusCallback != null) {
					resp.sendRedirect(statusCallback);
				}
				
				out.print("<p>Agent is succesfully authorized.</p>");
				out.print("<p><a href=\"" + agentUrl + "\">" + agentUrl + "</a></p>");
			}
			else {
				out.print("<p>An error occurred</p>");				
				out.print("<pre class='error'>" + 
						mapper.writeValueAsString(json.get("error")) + 
						"</pre>");				
			}
			out.print("<button onclick='window.location.href=\"" + 
						REDIRECT_URI + "\";'>Ok</button>");

			// TODO: cleanup
			out.println("<script type='text/javascript'>");
			out.println("console.log('Authorization code exchanged for access token:', " + res + ")");
			out.println("</script>");
			
			// TODO: cleanup
			if (!json.has("error")) {
				String access_token = null;
				if (json.has("access_token")) {
					access_token = json.get("access_token").asText().toString();
				}
				String token_type = null;
				if (json.has("token_type")) {
					token_type = json.get("token_type").asText();
				}
								
				// Third step: use the access token to call a Google API
				String url2 = "https://www.googleapis.com/oauth2/v1/userinfo";
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Authorization", token_type + " " + access_token);
				String info = HttpUtil.get(url2, headers);

				out.println("<script type='text/javascript'>");
				out.println("console.log('Retrieved user information:', " + info + ")");
				out.println("</script>");
			}
		}
		
		if (error != null) {
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
	
	private String createAuthorizationUrl() throws IOException {
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
		
		return url;
	}
}
