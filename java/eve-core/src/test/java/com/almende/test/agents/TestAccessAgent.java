package com.almende.test.agents;

import java.util.Arrays;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.annotation.Access;
import com.almende.eve.agent.annotation.AccessType;
import com.almende.eve.agent.annotation.Sender;

@Access(AccessType.PRIVATE)  //defaults to PUBLIC...
public class TestAccessAgent extends Agent {

	@Override
	public void create(){
		this.getState().put("senderLabel", "trusted");
	}

	@Override
	public boolean onAccess(@Sender String sender, String functionTag){
		if (sender == null) return false;
		String senderLabel = (String) this.getState().get("senderLabel");
		if (functionTag != null && !functionTag.equals("")){
			return "trust".equals(functionTag) && sender.contains(senderLabel);
		}
		return sender.contains(senderLabel);
	}
	
	@Access(AccessType.PUBLIC)
	public boolean allowed(){
		return true;
	}
	@Access(AccessType.UNAVAILABLE)
	public boolean forbidden(){
		return true;
	}
	@Access(AccessType.PRIVATE)  //checks onAccess method before being called.
	public boolean depends(){
		return true;
	}
	@Access(value=AccessType.PRIVATE,tag="trust")  //checks onAccess method before being called.
	public boolean dependTag(){
		return true;
	}
	@Access(value=AccessType.PRIVATE,tag="untrust")  //checks onAccess method before being called.
	public boolean dependUnTag(){
		return true;
	}
	public boolean unmodified(){ //Depends on default annotation of entire agent, in this case through onAccess() check
		return true;
	}
	public boolean param(@Sender String sender){
		String senderLabel = (String) this.getState().get("senderLabel");
		if (sender == null || !sender.contains(senderLabel)){ //will always fail in this case.
			return true;
		}
		return false;
	}
	
	@Access(AccessType.UNAVAILABLE)
	public boolean[] run(String url){
		boolean[] result = new boolean[0];
		result = Arrays.copyOf(result, 7);
		try{ result[0] = send(url,"allowed",Boolean.class); } catch (Exception e){ e.printStackTrace(); };
		try{ result[1] = send(url,"forbidden",Boolean.class); } catch (Exception e){};
		try{ result[2] = send(url,"depends",Boolean.class); } catch (Exception e){e.printStackTrace();};
		try{ result[3] = send(url,"dependTag",Boolean.class); } catch (Exception e){e.printStackTrace();};
		try{ result[4] = send(url,"dependUnTag",Boolean.class); } catch (Exception e){};
		try{ result[5] = send(url,"unmodified",Boolean.class); } catch (Exception e){e.printStackTrace();};
		try{ result[6] = send(url,"param",Boolean.class); } catch (Exception e){e.printStackTrace();};
		return result;
	}
	
	@Override
	public String getDescription() {
		return "Agent to test the access control features of Eve";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

}
