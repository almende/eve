package com.almende.eve.transport.zmq;

import org.zeromq.ZContext;

public class ZMQ extends org.zeromq.ZMQ {
	private static ZContext zcontext = null;
	
	public static ZContext getInstance(){
		if (zcontext == null){
			zcontext = new ZContext();
		}
		return zcontext;
	}
}
