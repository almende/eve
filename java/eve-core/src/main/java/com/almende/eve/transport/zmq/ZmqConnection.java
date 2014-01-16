/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.zmq;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ.Socket;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.AsyncCallbackQueue;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.ObjectCache;
import com.almende.util.tokens.TokenRet;
import com.almende.util.tokens.TokenStore;

/**
 * The Class ZmqConnection.
 */
public class ZmqConnection {
	private static final Logger	LOG		= Logger.getLogger(ZmqConnection.class
												.getCanonicalName());
	private final Socket		socket;
	private final ZmqService	service;
	private URI					zmqUrl	= null;
	private AgentHost			host	= null;
	private String				agentId	= null;
	
	/**
	 * Instantiates a new zmq connection.
	 * 
	 * @param socket
	 *            the socket
	 * @param service
	 *            the service
	 */
	public ZmqConnection(final Socket socket, final ZmqService service) {
		this.socket = socket;
		this.service = service;
	}
	
	/**
	 * Gets the socket.
	 * 
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * Gets the zmq url.
	 * 
	 * @return the zmq url
	 */
	public URI getZmqUrl() {
		return zmqUrl;
	}
	
	/**
	 * Sets the zmq url.
	 * 
	 * @param zmqUrl
	 *            the new zmq url
	 */
	public void setZmqUrl(final URI zmqUrl) {
		this.zmqUrl = zmqUrl;
	}
	
	/**
	 * Gets the host.
	 * 
	 * @return the host
	 */
	public AgentHost getHost() {
		return host;
	}
	
	/**
	 * Sets the host.
	 * 
	 * @param host
	 *            the new host
	 */
	public void setHost(final AgentHost host) {
		this.host = host;
	}
	
	/**
	 * Gets the agent id.
	 * 
	 * @return the agent id
	 */
	public String getAgentId() {
		return agentId;
	}
	
	/**
	 * Sets the agent id.
	 * 
	 * @param agentId
	 *            the new agent id
	 */
	public void setAgentId(final String agentId) {
		this.agentId = agentId;
	}
	
	/**
	 * Gets the agent url.
	 * 
	 * @return the agent url
	 */
	public URI getAgentUrl() {
		try {
			return new URI("zmq:" + getZmqUrl());
		} catch (URISyntaxException e) {
			LOG.warning("Strange, couldn't form agentUrl:" + "zmq:"
					+ getZmqUrl());
			return null;
		}
	}
	
	/**
	 * Sets the agent url.
	 * 
	 * @param agentUrl
	 *            the new agent url
	 * @throws URISyntaxException
	 */
	public void setAgentUrl(final URI agentUrl) throws URISyntaxException {
		zmqUrl = new URI(agentUrl.toString().replaceFirst("zmq:/?/?", ""));
	}
	
	/**
	 * Gets the request.
	 * 
	 * @param socket
	 *            the socket
	 * @return the request
	 */
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
	 */
	public void listen() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
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
	
	/**
	 * Handle msg.
	 * 
	 * @param msg
	 *            the msg
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws URISyntaxException
	 */
	private void handleMsg(final ByteBuffer[] msg)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException, URISyntaxException {
		
		// Receive
		// ZMQ.NORMAL|senderUrl|tokenJson|body
		// ZMQ.HANDSHAKE|senderUrl|tokenJson|timestamp
		// ZMQ.HANDSHAKE_RESPONSE|senderUrl|tokenJson|null
		
		final URI senderUrl = new URI(new String(msg[1].array()));
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
			final AsyncCallbackQueue<String> callbacks = host.getCallbackQueue(
					"zmqHandshakes", String.class);
			AsyncCallback<String> callback = callbacks.pull(key);
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
				final AsyncCallbackQueue<String> callbacks = host
						.getCallbackQueue("zmqHandshakes", String.class);
				
				SyncCallback<String> callback = new SyncCallback<String>();
				callbacks.push(key, "", callback);
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
