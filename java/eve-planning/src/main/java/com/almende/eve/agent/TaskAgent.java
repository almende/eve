package com.almende.eve.agent;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public interface TaskAgent {
	abstract public String getUsername();
	abstract public String getEmail();
	
	public ArrayNode getTaskList() throws Exception;
	
	public ArrayNode getTasks(
			@Optional @Name("start") String start, 
			@Optional @Name("end") String end, 
			@Optional @Name("listId") String taskListId) 
			throws Exception;
	
	public ObjectNode getTask (
			@Name("taskId") String taskId,
			@Optional @Name("taskListId") String taskListId) 
			throws Exception;
			
	public ObjectNode createTask (
			@Name("taskId") ObjectNode task,
			@Optional @Name("taskListId") String taskListId) 
			throws Exception;
	
	public ObjectNode updateTask (@Name("taskId") ObjectNode event,
			@Optional @Name("taskListId") String taskListId) 
			throws Exception;
	
	public void deleteTask (
			@Name("taskId") String eventId,
			@Optional @Name("taskListId") String taskListId) 
			throws Exception;
}
