package com.almende.eve.transport.zmq;


public class ZMQ extends org.zeromq.ZMQ {
	private static Context context =  ZMQ.context(10);
	public final static byte[] NORMAL = new byte[]{new Integer(0).byteValue()};
	public final static byte[] HANDSHAKE = new byte[]{new Integer(1).byteValue()};
	
	public static Context getInstance(){
		return context;
	}
	public static Socket getSocket(int type){
		synchronized(context){
			return context.socket(type);
		}
	}
}
