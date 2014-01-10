package com.almende.eve.transport.zmq;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ.Socket;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.ObjectCache;
import com.almende.util.tokens.TokenRet;
import com.almende.util.tokens.TokenStore;

public class ZmqConnection {
	private static final Logger	LOG		= Logger.getLogger(ZmqConnection.class
												.getCanonicalName());
	
	private final Socket		socket;
	private final ZmqService	service;
	private String				zmqUrl	= null;
	private AgentHost			host	= null;
	private String				agentId	= null;
	
	public ZmqConnection(final Socket socket, final ZmqService service) {
		this.socket = socket;
		this.service = service;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public String getZmqUrl() {
		return zmqUrl;
	}
	
	public void setZmqUrl(final String zmqUrl) {
		this.zmqUrl = zmqUrl;
	}
	
	public AgentHost getHost() {
		return host;
	}
	
	public void setHost(final AgentHost host) {
		this.host = host;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	public String getAgentUrl() {
		return "zmq:" + getZmqUrl();
	}
	
	public void setAgentUrl(final String agentUrl) {
		zmqUrl = agentUrl.replaceFirst("zmq:/?/?", "");
	}
	
	private ByteBuffer[] getRequest(final Socket socket) {
		final byte[] res = socket.recv();
		final ByteBuffer[] result = new ByteBuffer[4];
		if (res != null) {
			result[0] = ByteBuffer.wrap(res);
			result[1] = ByteBuffer.wrap(socket.recv());
			result[2] = ByteBuffer.wrap(socket.recv());
			result[3] = ByteBuffer.wrap(socket.recv());
		}
		return result;
		
	}
	
	/**
	 * process an incoming zmq message.
	 * If the message contains a valid JSON-RPC request or response,
	 * the message will be processed.
	 * 
	 * @param packet
	 */
	public void listen() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					// Receive
					// connID|emptyDelimiter|ZMQ.NORMAL|senderUrl|tokenJson|body
					// or
					// connID|emptyDelimiter|ZMQ.HANDSHAKE|senderUrl|tokenJson|timestamp
					
					try {
						final ByteBuffer[] msg = getRequest(socket);
						
						if (msg[0] != null) {
							handleMsg(msg);
							continue;
						}
					} catch (final Exception e) {
						LOG.log(Level.SEVERE, "Caught error:", e);
					}
				}
			}
		}).start();
	}
	
	private void handleMsg(final ByteBuffer[] msg)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException {
		
		// Receive
		// ZMQ.NORMAL|senderUrl|tokenJson|body
		// ZMQ.HANDSHAKE|senderUrl|tokenJson|timestamp
		// ZMQ.HANDSHAKE_RESPONSE|senderUrl|tokenJson|null
		
		final String senderUrl = new String(msg[1].array());
		final TokenRet token = JOM.getInstance().readValue(msg[2].array(),
				TokenRet.class);
		final String body = new String(msg[3].array());
		final String key = senderUrl + ":" + token.getToken();
		
		if (Arrays.equals(msg[0].array(), ZMQ.HANDSHAKE)) {
			// Reply token corresponding to timestamp.
			final String res = TokenStore.get(body);
			service.sendAsync(ZMQ.HANDSHAKE_RESPONSE, res, zmqUrl, senderUrl,
					res, null);
			return;
		} else if (Arrays.equals(msg[0].array(), ZMQ.HANDSHAKE_RESPONSE)) {
			// post response to callback for handling by other thread
			final CallbackInterface<String> callbacks = host
					.getCallbackService("zmqHandshakes", String.class);
			AsyncCallback<String> callback = callbacks.get(key);
			if (callback != null) {
				callback.onSuccess(body);
			} else {
				LOG.warning("Received ZMQ.HANDSHAKE_RESPONSE for unknown handshake..."
						+ senderUrl + " : " + token);
			}
			return;
		} else {
			final ObjectCache sessionCache = ObjectCache.get("ZMQSessions");
			if (!sessionCache.containsKey(key)
					&& host.getAgent(agentId).hasPrivate()) {
				final CallbackInterface<String> callbacks = host
						.getCallbackService("zmqHandshakes", String.class);
				
				SyncCallback<String> callback = new SyncCallback<String>();
				callbacks.store(key, callback);
				service.sendAsync(ZMQ.HANDSHAKE, token.toString(), zmqUrl,
						senderUrl, token.getTime(), null);
				
				String retToken = null;
				try {
					retToken = callback.get();
				} catch (Exception e) {
				}
				if (token.getToken().equals(retToken)) {
					sessionCache.put(key, true);
				} else {
					LOG.warning("Failed to complete handshake!");
					return;
				}
			}
		}
		
		if (body != null) {
			try {
				host.receive(agentId, body, senderUrl, null);
			} catch (final IOException e) {
				LOG.log(Level.WARNING,
						"Host threw an IOException, probably agent '" + agentId
								+ "' doesn't exist? ", e);
				return;
			}
		}
	}
	
}
