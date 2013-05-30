package com.almende.test.agents;

import java.util.Arrays;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;

@Access(AccessType.PRIVATE)  //defaults to UNAVAILABLE...
public class TestAccessAgent extends Agent {

	@Override
	public void create(){
		this.getState().put("senderLabel", "trusted");
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<Object> getMethods() {
		return super.getMethods();
	}

	@Override
	public boolean onAccess(String sender, String functionTag){
		System.err.println("Got sender:"+sender);
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
	
	public boolean[] run(@Name("url") String url){
		boolean[] result = new boolean[0];
		result = Arrays.copyOf(result, 7);
		try{ result[0] = (Boolean)send(url,"allowed",JOM.getSimpleType(Boolean.class)); } catch (Exception e){ e.printStackTrace(); };
		try{ result[1] = (Boolean)send(url,"forbidden",JOM.getSimpleType(Boolean.class)); } catch (Exception e){};
		try{ result[2] = (Boolean)send(url,"depends",JOM.getSimpleType(Boolean.class)); } catch (Exception e){e.printStackTrace();};
		try{ result[3] = (Boolean)send(url,"dependTag",JOM.getSimpleType(Boolean.class)); } catch (Exception e){e.printStackTrace();};
		try{ result[4] = (Boolean)send(url,"dependUnTag",JOM.getSimpleType(Boolean.class)); } catch (Exception e){};
		try{ result[5] = (Boolean)send(url,"unmodified",JOM.getSimpleType(Boolean.class)); } catch (Exception e){e.printStackTrace();};
		try{ result[6] = (Boolean)send(url,"param",JOM.getSimpleType(Boolean.class)); } catch (Exception e){e.printStackTrace();};
		return result;
	}

	@Override
	@Access(AccessType.PUBLIC)
	public List<String> getUrls(){
		return super.getUrls();
	}
	@Override
	@Access(AccessType.PUBLIC)
	public String getType(){
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
