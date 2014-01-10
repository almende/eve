package com.almende.test.agents;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;

@Access(AccessType.PRIVATE)
// defaults to UNAVAILABLE...
public class TestAccessAgent extends Agent {
	
	@Override
	public void sigCreate() {
		getState().put("senderLabel", "trusted");
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<Object> getMethods() {
		return super.getMethods();
	}
	
	@Override
	public boolean onAccess(final String sender, final String functionTag) {
		if (sender == null) {
			return false;
		}
		
		final String senderLabel = getState().get("senderLabel", String.class);
		if (functionTag != null && !functionTag.equals("")) {
			return "trust".equals(functionTag) && sender.contains(senderLabel);
		}
		
		return sender.contains(senderLabel);
	}
	
	@Access(AccessType.SELF)
	public boolean self() {
		return true;
	}
	
	@Access(AccessType.PUBLIC)
	public boolean allowed() {
		return true;
	}
	
	@Access(AccessType.UNAVAILABLE)
	public boolean forbidden() {
		return true;
	}
	
	@Access(AccessType.PRIVATE)
	// checks onAccess method before being called.
	public boolean depends() {
		return true;
	}
	
	@Access(value = AccessType.PRIVATE, tag = "trust")
	// checks onAccess method before being called.
	public boolean dependTag() {
		return true;
	}
	
	@Access(value = AccessType.PRIVATE, tag = "untrust")
	// checks onAccess method before being called.
	public boolean dependUnTag() {
		return true;
	}
	
	public boolean unmodified() { // Depends on default annotation of entire
									// agent, in this case through onAccess()
									// check
		return true;
	}
	
	public boolean param(@Sender final String sender) {
		final String senderLabel = getState().get("senderLabel", String.class);
		if (sender == null || !sender.contains(senderLabel)) { // will always
																// fail in this
																// case.
			return true;
		}
		return false;
	}
	
	public boolean[] run(@Name("url") final String urls) {
		final URI url = URI.create(urls);
		boolean[] result = new boolean[0];
		result = Arrays.copyOf(result, 8);
		try {
			result[0] = send(url, "allowed", Boolean.class);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		;
		try {
			result[1] = send(url, "forbidden", Boolean.class);
		} catch (final Exception e) {
		}
		;
		try {
			result[2] = send(url, "depends", Boolean.class);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		;
		try {
			result[3] = send(url, "dependTag", Boolean.class);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		;
		try {
			result[4] = send(url, "dependUnTag", Boolean.class);
		} catch (final Exception e) {
		}
		;
		try {
			result[5] = send(url, "unmodified", Boolean.class);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		;
		try {
			result[6] = send(url, "param", Boolean.class);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		;
		try {
			result[7] = send(url, "self", Boolean.class);
		} catch (final Exception e) {
		}
		;
		return result;
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<String> getUrls() {
		return super.getUrls();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getType() {
		return super.getType();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getDescription() {
		return "Agent to test the access control features of Eve";
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getVersion() {
		return "0.1";
	}
	
}
