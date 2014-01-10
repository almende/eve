package com.almende.eve.transport.zmq;

public class ZMQ extends org.zeromq.ZMQ {
	private static Context		context				= ZMQ.context(10);
	public static final byte[]	NORMAL				= new byte[] { Integer
															.valueOf(0)
															.byteValue() };
	public static final byte[]	HANDSHAKE			= new byte[] { Integer
															.valueOf(1)
															.byteValue() };
	public static final byte[]	HANDSHAKE_RESPONSE	= new byte[] { Integer
															.valueOf(2)
															.byteValue() };
	
	public static Context getInstance() {
		return context;
	}
	
	public static Socket getSocket(final int type) {
		synchronized (context) {
			return context.socket(type);
		}
	}
}
