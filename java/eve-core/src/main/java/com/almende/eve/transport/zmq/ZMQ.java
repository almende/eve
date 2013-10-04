package com.almende.eve.transport.zmq;

import org.zeromq.ZContext;

public class ZMQ extends org.zeromq.ZMQ {
	private static ZContext zcontext = null;
	public final static byte[] NORMAL = new byte[]{new Integer(0).byteValue()};
	public final static byte[] HANDSHAKE = new byte[]{new Integer(1).byteValue()};
	
	public static ZContext getInstance(){
		if (zcontext == null){
			zcontext = new ZContext();
		}
		return zcontext;
	}
}
