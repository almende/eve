/**
 * Test OAuth 2.0 with Google API
 * 
 * Jos de Jong, 2012-05-31
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
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.*;

import com.almende.eve.config.Config;
import com.almende.eve.json.JSONRPC;
import com.almende.eve.json.JSONRequest;
import com.almende.eve.json.jackson.JOM;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@SuppressWarnings("serial")
public class GoogleAuth extends HttpServlet {
	//private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	// Specify the correct client id and secret for web applications
	// Create them at the Google API console: https://code.google.com/apis/console/
	private String CLIENT_ID = null;
	private String CLIENT_SECRET = null;
	String REDIRECT_URI = null; 
	
	// hard coded uri's
	private String AGENTS_URL = "http://eveagents.appspot.com/agents/googlecalendaragent/id"; // TODO: do not hardcode
	private String AGENTS_METHOD = "setAuthorization";
	private String OAUTH_URI  = "https://accounts.google.com/o/oauth2";
	private String CONFIG_FILENAME = "/WEB-INF/eve.yaml";
	private String SPACE = " ";
	private String SCOPE = 
		"https://www.googleapis.com/auth/userinfo.email" + SPACE + 
		"https://www.googleapis.com/auth/userinfo.profile" + SPACE +
		"https://www.googleapis.com/auth/calendar";

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public void init() {
		try {
			// load configuration
			InputStream is = getServletContext().getResourceAsStream(CONFIG_FILENAME);
			Config config = new Config(is);

			CLIENT_ID = config.get("google", "client_id");
			if (CLIENT_ID == null) {
				throw new Exception("Parameter 'google.client_id' missing in config");
			}
			
			CLIENT_SECRET = config.get("google", "client_secret");
			if (CLIENT_SECRET == null) {
				throw new Exception("Parameter 'google.client_secret' missing in config");
			}

			REDIRECT_URI = config.get("auth", "google", "servlet_url");
			if (REDIRECT_URI == null) {
				throw new Exception("Parameter 'auth.google.servlet_url' missing in config");
			}
			// TODO: redirect_uri should reckon with the current environment
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		// read parameters code and access_token from query parameters or cookies
		String code = req.getParameter("code");
		String state = req.getParameter("state");
		String error = req.getParameter("error");
		String agentUrl = req.getParameter("agentUrl");
		String agentMethod = req.getParameter("agentMethod");
		String applicationCallback = req.getParameter("applicationCallback");

		PrintWriter out = resp.getWriter();
		resp.setContentType("text/html");

		// print error if any
		if (error != null) {
			printPageStart(out);
			printError(out, error);		
			printPageEnd(out);
			return;
		}
		
		// directly redirect to Google authorization page if an agents URL is provided
		if (agentUrl != null && agentMethod != null) {
			redirectToGoogleAuthorization(resp, agentUrl, agentMethod, applicationCallback);
			return;
		}

		// First step: show a form to authenticate
		if (code == null && state == null) {
			printPageStart(out);
			printAuthorizeForm(out);
			printPageEnd(out);
			return;
		}

		// Second step: exchange code with authentication token
		// After having authorized at google, the user is send back to this
		// servlet, with the url containing a code and status
		if (code != null && state != null){
			ObjectNode auth = exchangeCodeForAuthorization(code);

			// TODO: remove logging
			//System.out.println("Authorization code exchanged for access token: " + mapper.writeValueAsString(auth));
			
			if (auth.has("error")) {
				printPageStart(out);
				printError(out, mapper.writeValueAsString(auth.get("error")));
				printPageEnd(out);							
				return;
			}
 
			ObjectNode stateJson = mapper.readValue(state, ObjectNode.class); 
			String statusAgentUrl = stateJson.has("agentUrl") ? stateJson.get("agentUrl").asText() : null;
			String statusAgentMethod = stateJson.has("agentMethod") ? stateJson.get("agentMethod").asText() : null;
			String statusApplicationCallback = stateJson.has("applicationCallback") ? stateJson.get("applicationCallback").asText() : null;

			// send the retrieved authorization to the agent
			sendAuthorizationToAgent(statusAgentUrl, statusAgentMethod, auth);

			if (statusApplicationCallback != null) {
				resp.sendRedirect(statusApplicationCallback);
				return;
			}
			else {
				printPageStart(out);
				printSuccess(out, statusAgentUrl);
				printPageEnd(out);
				return;
			}
		}		
	}
	
	private String createAuthorizationUrl() throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("scope", SCOPE);
		params.put("redirect_uri", REDIRECT_URI);
		params.put("response_type", "code");
		params.put("access_type", "offline");
		params.put("client_id", CLIENT_ID);
		params.put("approval_prompt", "force");
		String url = HttpUtil.appendQueryParams(OAUTH_URI + "/auth", params);
		
		return url;
	}
	
	private void redirectToGoogleAuthorization(HttpServletResponse resp,
			String agentUrl, String agentMethod, String applicationCallback) throws IOException {
		String url = createAuthorizationUrl();
		ObjectNode st = JOM.createObjectNode();
		st.put("agentUrl", agentUrl);
		st.put("agentMethod", agentMethod);
		if (applicationCallback != null) {
			st.put("applicationCallback", applicationCallback);
		}

		url += "&state=" + 
			URLEncoder.encode(JOM.getInstance().writeValueAsString(st), "UTF-8");
		resp.sendRedirect(url);		
	}

	private ObjectNode exchangeCodeForAuthorization(String code) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("code", code);
		params.put("client_id", CLIENT_ID);
		params.put("client_secret", CLIENT_SECRET);
		params.put("redirect_uri", REDIRECT_URI);
		params.put("grant_type", "authorization_code");
		String res = HttpUtil.postForm(OAUTH_URI + "/token", params);

		ObjectNode json = mapper.readValue(res, ObjectNode.class);
		return json;
	}
	
	private void sendAuthorizationToAgent(String agentUrl, String agentMethod, 
			ObjectNode auth) throws IOException {
		JSONRequest rpcRequest = new JSONRequest(agentMethod, auth);
		JSONRPC.send(agentUrl, rpcRequest);
	}
	
	private void printPageStart(PrintWriter out) {
		out.print("<html>" +
				"<head>" +
				"<title>Authorize Eve agents</title>" +
				"<style>" +
				"body {width: 700px;}" +
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
	}
	
	private void printPageEnd(PrintWriter out) {
		out.print(
			"</body>" +
			"</html>"			
		);
	}
	
	private void printAuthorizeForm(PrintWriter out) throws IOException {
		String url = createAuthorizationUrl();
		out.print("<script type='text/javascript'>" +
			"function auth() {" +
			"  var state={" +
			"    \"agentUrl\": document.getElementById('agentUrl').value," +
			"    \"agentMethod\": document.getElementById('agentMethod').value" +
			"  };" +
			"  var url='" + url + "'+ '&state=' + encodeURI(JSON.stringify(state));" +
			"  window.location.href=url;" + 
			"}" +
			"</script>" +
			"<table>" +
			"<tr><td>Agent url</td><td><input type='text' id='agentUrl' value='" + 
				AGENTS_URL + "' style='width: 400px;'/></td></tr>" + 
				"<tr><td>Agent method</td><td><input type='text' id='agentMethod' value='" + 
				AGENTS_METHOD + "' style='width: 400px;'/></td></tr>" +
			"<tr><td><button onclick='auth();'>Authorize</button></td></tr>" +
			"</table>"
		);		
	}
	
	private void printSuccess(PrintWriter out, String agentUrl) {
		out.print("<p>Agent is succesfully authorized.</p>");
		out.print("<p><a href=\"" + agentUrl + "\">" + agentUrl + "</a></p>");
		out.print("<button onclick='window.location.href=\"" + 
					REDIRECT_URI + "\";'>Ok</button>");
	}
	
	private void printError(PrintWriter out, String error) {
		out.print("<p>An error occurred</p>");				
		out.print("<pre class='error'>" + error + "</pre>");				
		out.print("<button onclick='window.location.href=\"" + 
					REDIRECT_URI + "\";'>Ok</button>");			
	}
}
