package com.almende.eve.state;

public interface ExtendedState extends State {
	
	
	<V> V get(String key);
	
}
