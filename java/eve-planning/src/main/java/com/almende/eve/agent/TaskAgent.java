package com.almende.eve.agent;

import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.PUBLIC)
public interface TaskAgent {
	abstract public String getUsername();
	abstract public String getEmail();
	
	public ArrayNode getTaskList() throws Exception;
	
	public ArrayNode getTasks(
			@Required(false) @Name("start") String start, 
			@Required(false) @Name("end") String end, 
			@Required(false) @Name("listId") String taskListId) 
			throws Exception;
	
	public ObjectNode getTask (
			@Name("taskId") String taskId,
			@Required(false) @Name("taskListId") String taskListId) 
			throws Exception;
			
	public ObjectNode createTask (
			@Name("taskId") ObjectNode task,
			@Required(false) @Name("taskListId") String taskListId) 
			throws Exception;
	
	public ObjectNode updateTask (@Name("taskId") ObjectNode event,
			@Required(false) @Name("taskListId") String taskListId) 
			throws Exception;
	
	public void deleteTask (
			@Name("taskId") String eventId,
			@Required(false) @Name("taskListId") String taskListId) 
			throws Exception;
}
