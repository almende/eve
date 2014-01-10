/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.zmq;

/**
 * The Class ZMQ.
 */
public class ZMQ extends org.zeromq.ZMQ {
	private static Context		context				= ZMQ.context(10);
	
	/** The Constant NORMAL. */
	public static final byte[]	NORMAL				= new byte[] { Integer
															.valueOf(0)
															.byteValue() };
	
	/** The Constant HANDSHAKE. */
	public static final byte[]	HANDSHAKE			= new byte[] { Integer
															.valueOf(1)
															.byteValue() };
	
	/** The Constant HANDSHAKE_RESPONSE. */
	public static final byte[]	HANDSHAKE_RESPONSE	= new byte[] { Integer
															.valueOf(2)
															.byteValue() };
	
	/**
	 * Gets the single instance of ZMQ.
	 *
	 * @return single instance of ZMQ
	 */
	public static Context getInstance() {
		return context;
	}
	
	/**
	 * Gets the socket.
	 *
	 * @param type the type
	 * @return the socket
	 */
	public static Socket getSocket(final int type) {
		synchronized (context) {
			return context.socket(type);
		}
	}
}
