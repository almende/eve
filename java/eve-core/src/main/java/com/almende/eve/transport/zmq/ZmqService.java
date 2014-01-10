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
import com.almende.eve.transport.TransportService;
import com.almende.util.tokens.TokenStore;

public class ZmqService implements TransportService {
	private static final Logger				LOG				= Logger.getLogger(ZmqService.class
																	.getCanonicalName());
	
	private AgentHost						host			= null;
	private String							baseUrl			= "";
	private final Map<String, ZmqConnection>	inboundSockets	= new HashMap<String, ZmqConnection>();
	
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
	public ZmqService(final AgentHost agentHost, final Map<String, Object> params) {
		host = agentHost;
		
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
	public String getAgentUrl(final String agentId) {
		if (inboundSockets.containsKey(agentId)) {
			return inboundSockets.get(agentId).getAgentUrl();
		}
		return null;
	}
	
	@Override
	public String getAgentId(final String agentUrl) {
		for (final Entry<String, ZmqConnection> entry : inboundSockets.entrySet()) {
			if (entry.getValue().getAgentUrl().equals(agentUrl)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	public void sendAsync(final byte[] zmqType, final String token,
			final String senderUrl, final String receiverUrl,
			final Object message, final String tag) {
		host.getPool().execute(new Runnable() {
			@Override
			public void run() {
				final String addr = receiverUrl.replaceFirst("zmq:/?/?", "");
				final Socket socket = ZMQ.getSocket(org.zeromq.ZMQ.PUSH);
				try {
					socket.connect(addr);
					socket.send(zmqType, org.zeromq.ZMQ.SNDMORE);
					socket.send(senderUrl, org.zeromq.ZMQ.SNDMORE);
					socket.send(token, org.zeromq.ZMQ.SNDMORE);
					socket.send(message.toString());
					
				} catch (final Exception e) {
					LOG.log(Level.WARNING, "Failed to send JSON through ZMQ", e);
				}
				socket.setLinger(-1);
				socket.close();
			}
		});
	}
	
	@Override
	public void sendAsync(final String senderUrl, final String receiverUrl,
			final Object message, final String tag) {
		sendAsync(ZMQ.NORMAL, TokenStore.create().toString(), senderUrl,
				receiverUrl, message, tag);
	}
	
	@Override
	public List<String> getProtocols() {
		return Arrays.asList("zmq");
	}
	
	private String genUrl(final String agentId) {
		if (baseUrl.startsWith("tcp://")) {
			final int basePort = Integer.parseInt(baseUrl.replaceAll(".*:", ""));
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
	public synchronized void reconnect(final String agentId) throws IOException {
		try {
			if (inboundSockets.containsKey(agentId)) {
				final ZmqConnection conn = inboundSockets.get(agentId);
				final Socket socket = conn.getSocket();
				socket.disconnect(conn.getZmqUrl());
				socket.bind(conn.getZmqUrl());
				conn.listen();
			} else {
				final ZmqConnection socket = new ZmqConnection(
						ZMQ.getSocket(org.zeromq.ZMQ.PULL), this);
				inboundSockets.put(agentId, socket);
				
				final String url = genUrl(agentId);
				socket.getSocket().bind(url);
				socket.setAgentUrl(url);
				socket.setAgentId(agentId);
				socket.setHost(host);
				socket.listen();
			}
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, "Caught error:", e);
		}
	}
	
	@Override
	public String getKey() {
		return "zmq:" + baseUrl;
	}
	
}
