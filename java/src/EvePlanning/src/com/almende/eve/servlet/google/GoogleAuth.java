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
	private String AGENTS_URI = "http://eveagents.appspot.com/agents"; // TODO: do not hardcode
	private String OAUTH_URI  = "https://accounts.google.com/o/oauth2";

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public void init() {
		try {
			// load configuration
			Config config = new Config(this);

			CLIENT_ID = config.get("google.client_id");
			if (CLIENT_ID == null) {
				throw new Exception("google.client_id missing in config");
			}
			
			CLIENT_SECRET = config.get("google.client_secret");
			if (CLIENT_SECRET == null) {
				throw new Exception("google.client_secret missing in config");
			}

			REDIRECT_URI = config.get("auth.google.servlet_url");
			if (REDIRECT_URI == null) {
				throw new Exception("auth.google.servlet_url missing in config");
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
		String agent = req.getParameter("agent");
		String callback = req.getParameter("callback");

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
		if (agent != null) {
			redirectToGoogleAuthorization(resp, agent, callback);
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
 
			ObjectNode authJson = mapper.readValue(state, ObjectNode.class); 
			String agentUrl = authJson.has("agent") ? authJson.get("agent").asText() : null;
			String statusCallback = authJson.has("callback") ? authJson.get("callback").asText() : null;

			// send the retrieved authorization to the agent
			sendAuthorizationToAgent(agentUrl, auth);

			if (statusCallback != null) {
				resp.sendRedirect(statusCallback);
				return;
			}
			else {
				printPageStart(out);
				printSuccess(out, agentUrl);
				printPageEnd(out);
				return;
			}
		}		
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
	
	private void redirectToGoogleAuthorization(HttpServletResponse resp,
			String agentUrl, String callbackUrl) throws IOException {
		String url = createAuthorizationUrl();
		ObjectNode st = JOM.createObjectNode();
		st.put("agent", agentUrl);
		if (callbackUrl != null) {
			st.put("callback", callbackUrl);
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
	
	private void sendAuthorizationToAgent(String agentUrl, ObjectNode auth) 
			throws IOException {
		JSONRequest rpcRequest = new JSONRequest("setAuthorization", auth);
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
			"    \"agent\": document.getElementById('agent').value" +
			"  };" +
			"  var url='" + url + "'+ '&state=' + encodeURI(JSON.stringify(state));" +
			"  window.location.href=url;" + 
			"}" +
			"</script>" +
			"Agent url: <input type='text' id='agent' value='" + 
				AGENTS_URI + "/googlecalendaragent/id' style='width: 400px;'/>" + 
			"<button onclick='auth();'>Authorize</button>");		
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
