package com.almende.eve.transport.zmq;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ.Socket;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.transport.AsyncCallback;
import com.almende.eve.transport.TransportService;
import com.almende.util.tokens.TokenStore;

public class ZmqService implements TransportService {
	private static final Logger		LOG				= Logger.getLogger(ZmqService.class
															.getCanonicalName());
	
	private AgentHost				agentHost		= null;
	private String					baseUrl			= "";
	HashMap<String, ZmqConnection>	inboundSockets	= new HashMap<String, ZmqConnection>();
	
	protected ZmqService() {
	}
	
	/**
	 * Construct an ZmqService
	 * This constructor is called when the TransportService is constructed
	 * by the AgentHost
	 * 
	 * @param params
	 *            Available parameters:
	 *            {String} baseUrl
	 *            {Integer} basePort
	 */
	public ZmqService(AgentHost agentHost, Map<String, Object> params) {
		this.agentHost = agentHost;
		
		if (params != null) {
			baseUrl = (String) params.get("baseUrl");
		}
		
	}
	
	// Outbound Socket(s) per agent (1 socket per endaddress, so many sockets
	// per agent)
	// Inbound Socket per agent per ZmqService
	
	// Inbound Socket determines agentUrl
	// Socket is bound to address of form:
	// "tcp://<address>:<basePort+agentOffset>"
	// Decorate Socket class with agentId for inbound usage.
	
	// Within EVE zmq urls look like: "zmq:tcp://<address>:<inboundPort>" and
	// "zmq:ipc://<label>.ipc"
	
	@Override
	public String getAgentUrl(String agentId) {
		if (inboundSockets.containsKey(agentId)) {
			return inboundSockets.get(agentId).getAgentUrl();
		}
		return null;
	}
	
	@Override
	public String getAgentId(String agentUrl) {
		for (Entry<String, ZmqConnection> entry : inboundSockets.entrySet()) {
			if (entry.getValue().getAgentUrl().equals(agentUrl)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	@Override
	public JSONResponse send(String senderUrl, String receiverUrl,
			JSONRequest request) throws JSONRPCException {
		
		JSONResponse response = null;
		final String addr = receiverUrl.replaceFirst("zmq:/?/?", "");
		Socket socket = ZMQ.getSocket(ZMQ.REQ);
		synchronized (socket) {
			try {
				socket.connect(addr);
				socket.setIdentity(Long.valueOf(System.currentTimeMillis())
						.toString().getBytes());
				socket.send(ZMQ.NORMAL, ZMQ.SNDMORE);
				socket.send(senderUrl, ZMQ.SNDMORE);
				socket.send(TokenStore.create().toString(), ZMQ.SNDMORE);
				socket.send(request.toString());
				
				String result = socket.recvStr();
				response = new JSONResponse(result);
			} catch (Throwable e) {
				LOG.log(Level.WARNING, "Failed to send JSON through JMQ", e);
				response = new JSONResponse(e);
			}
			if (socket != null) {
				socket.setLinger(0);
				socket.setTCPKeepAlive(0);
				socket.disconnect(addr);
				socket.close();
				socket = null;
			}
		}
		return response;
		
	}
	
	@Override
	public void sendAsync(final String senderUrl, final String receiver,
			final JSONRequest request,
			final AsyncCallback<JSONResponse> callback) throws JSONRPCException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONResponse response;
				try {
					response = send(senderUrl, receiver, request);
					callback.onSuccess(response);
				} catch (Exception e) {
					callback.onFailure(e);
				}
			}
		}).start();
		
	}
	
	@Override
	public List<String> getProtocols() {
		return Arrays.asList("zmq");
	}
	
	private String genUrl(String agentId) {
		if (baseUrl.startsWith("tcp://")) {
			int basePort = Integer.parseInt(baseUrl.replaceAll(".*:", ""));
			// TODO: this is not nice. Agents might change address at server
			// restart.... How to handle this?
			return baseUrl.replaceFirst(":[0-9]*$", "") + ":"
					+ (basePort + inboundSockets.size());
		} else if (baseUrl.startsWith("inproc://")) {
			return baseUrl + agentId;
		} else if (baseUrl.startsWith("ipc://")) {
			return baseUrl + agentId;
		} else {
			throw new IllegalStateException("ZMQ baseUrl not valid! (baseUrl:'"
					+ baseUrl + "')");
		}
	}
	
	@Override
	public synchronized void reconnect(String agentId) throws JSONRPCException,
			IOException {
		try {
			if (inboundSockets.containsKey(agentId)) {
				ZmqConnection conn = inboundSockets.get(agentId);
				conn.getSocket().disconnect(conn.getZmqUrl());
				conn.getSocket().bind(conn.getZmqUrl());
				conn.listen();
			} else {
				ZmqConnection socket = new ZmqConnection(
						ZMQ.getSocket(ZMQ.ROUTER));
				
				String url = genUrl(agentId);
				socket.getSocket().setIdentity(
						Long.valueOf(System.currentTimeMillis()).toString()
								.getBytes());
				socket.getSocket().bind(url);
				socket.setAgentUrl(url);
				socket.setAgentId(agentId);
				socket.setHost(agentHost);
				socket.listen();
				inboundSockets.put(agentId, socket);
			}
		} catch (Throwable e) {
			LOG.severe("Caught error:" + e);
			e.printStackTrace();
		}
	}
	
	@Override
	public String getKey() {
		return "zmq:" + baseUrl;
	}
	
}
