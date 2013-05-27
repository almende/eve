package com.almende.eve.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.entity.Message;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TwigUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

/**
 * @class MessageAgent
 * The MessageAgent can send and receive messages from other MessageAgents.
 * It has an inbox and outbox for messages, which is stored in Google Datastore,
 * and can be queried.
 * 
 * A MessageAgent is stateless, and its messages are stored in the Google 
 * Datastore. Messages are stored in an inbox and an outbox, and can be 
 * retrieved and filtered.
 * Messages are sent in parallel, they are put in a task queue and are 
 * dispatched asynchronously.
 * 
 * Core methods:
 * - send      Send a message to one or multiple agents
 *             Messages will be retrieved by recipients via the method onMessage
 * - getInbox  Retrieve messages from the agents inbox
 * - getOutbox Retrieve messages from the agents outbox
 * - clear     Clear inbox and outbox, delete all data from this agent.
 * 
 * Example usage:
 * Open the web pages of the following agents in your browser:
 *     http://localhost:8080/agents/messageagent1/
 *     http://localhost:8080/agents/messageagent2/
 *     http://localhost:8080/agents/messageagent3/
 * 
 * Send the following JSON-RPC request using a REST Client or via the web page
 * of this agent
 *   url: 
 *       http://localhost:8080/agents/messageagent1/
 *   message: 
 *       {
 *         "id": 1,
 *         "method": "send",
 *         "params": {
 *           "message": {
 *             "description": "Test message",
 *             "to": [
 *               "http://localhost:8080/agents/messageagent2/",
 *               "http://localhost:8080/agents/messageagent3/"
 *             ]
 *           }
 *         }
 *       }
 * 
 * The message will be put in the outbox of agent 1, and will be sent to the
 * inbox of agent 2 and 3. To check if the message is in the inbox of agent 2,
 * Go the the webpage of agent 2 and execute the method getInbox.
 * 
 * Note that the used GAE TaskQueue processes maximum 5 tasks per second by 
 * default. This rate can be configured, see 
 *     https://developers.google.com/appengine/docs/java/config/queue
 * Example configuration:
 *     <queue-entries>
 *       <queue>
 *         <name>default</name>
 *         <rate>20/s</rate>
 *         <bucket-size>40</bucket-size>
 *         <max-concurrent-requests>10</max-concurrent-requests>
 *       </queue>
 *     </queue-entries>
 * 
 * @author jos
 * @date 2013-02-15
 */
public class MessageAgent extends Agent {
	@Override
	public void init() {
		TwigUtil.register(Message.class);
	}
	
	/**
	 * Receive a message. The message will be stored in the agents inbox.
	 * @param message
	 * @throws Exception 
	 */
	public void onMessage(@Name("message") Message message) throws Exception {
		// store the message in the inbox
		message.setAgent(getFirstUrl());
		message.setBox("inbox");
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.store(message);
		
		// trigger receive event (not necessary)
		String event = "receive";
		ObjectNode params = JOM.createObjectNode();
		params.put("message", JOM.getInstance().convertValue(message, ObjectNode.class));
		eventsFactory.trigger(event, params);
	}
	
	/**
	 * Send a message. Message will be send to all recipients, and will be
	 * stored in the agents outbox
	 * @param message
	 * @throws Exception 
	 */
	public void send (@Name("message") Message message) throws Exception {
		// set timestamp and from fields
		message.setTimestamp(DateTime.now().toString());
		message.setFrom(getFirstUrl());
		message.setStatus("unread");

		// store the message in the outbox
		message.setAgent(getFirstUrl());
		message.setBox("outbox");
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.store(message);
		
		// send the message to all recipients
		Set<String> to = message.getTo();
		if (to != null) {
			for (String url : to) {
				try {
					// create a task to send the message to this agent asynchronously
					// this is important, it parallelizes dispatching of the messages
					String method = "dispatch";
					ObjectNode params = JOM.createObjectNode();
					params.put("url", url);
					params.put("message", JOM.getInstance().convertValue(message, ObjectNode.class));
					JSONRequest request = new JSONRequest(method, params);
					
					long delay = 1; // milliseconds
					
					getScheduler().createTask(request, delay);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// trigger send event (not necessary)
		String event = "send";
		ObjectNode params = JOM.createObjectNode();
		params.put("message", JOM.getInstance().convertValue(message, ObjectNode.class));
		eventsFactory.trigger(event, params);
	}

	/**
	 * Send a message to one particular recipient
	 * This method is called asynchronously from the method send
	 * @param url
	 * @param message
	 */
	public void dispatch (@Name("url") String url, @Name("message") Message message) {
		try {
			String method = "onMessage";
			ObjectNode params = JOM.createObjectNode();
			params.put("message", JOM.getInstance().convertValue(message, ObjectNode.class));

			send(url, method, params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieve all messages in the agents inbox
	 * @param since   Optional. An ISO date. messages will be filtered on a 
	 *                 timestamp greater or equal than since
	 * @param status  Optional. Messages will be filtered on a status like
	 *                 "read", "unread", etc...
	 * @return messages
	 * @throws Exception 
	 */
	public List<Message> getInbox (
			@Name("since") @Required(false) String since,
			@Name("status") @Required(false) String status) throws Exception {
		return find("inbox", since, status);
	}

	/**
	 * Retrieve all messages in the agents outbox
	 * @param since   Optional. An ISO date. messages will be filtered on a 
	 *                 timestamp greater or equal than since
	 * @param status  Optional. Messages will be filtered on a status like
	 *                 "read", "unread", etc...
	 * @return messages
	 * @throws Exception 
	 */
	public List<Message> getOutbox (
			@Name("since") @Required(false) String since,
			@Name("status") @Required(false) String status) throws Exception {
		return find("outbox", since, status);
	}
	
	/**
	 * Find messages from this agent
	 * @param box    Optional. Messagebox like "inbox" or "outbox"
	 * @param since  Optional. ISO Date containing a timestamp
	 * @param status Optional. Status like "read", "unread"
	 * @return messages
	 * @throws Exception
	 */
	private List<Message> find (String box, String since, String status) 
			throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		
		RootFindCommand<Message> command = datastore.find()
			.type(Message.class)
			.addFilter("agent", FilterOperator.EQUAL, getFirstUrl());
		if (box != null) {
			command = command.addFilter("box", FilterOperator.EQUAL, box);
		}
		if (since != null) {
			command = command.addFilter("timestamp", 
					FilterOperator.GREATER_THAN_OR_EQUAL, since);
		}
		if (status != null) {
			command = command.addFilter("status", FilterOperator.EQUAL, status);
		}

		QueryResultIterator<Message> it = command.now();
		List<Message> messages = new ArrayList<Message>();
		while (it.hasNext()) {
			messages.add(it.next());
		}		
		
		return messages;
	}
	
	/**
	 * Clear inbox and outbox and everything the agent has stored.
	 */
	@Override
	public void delete() {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		QueryResultIterator<Message> it = datastore.find()
			.type(Message.class)
			.addFilter("agent", FilterOperator.EQUAL, getFirstUrl())
			.now();
		
		while (it.hasNext()) {
			Message message = it.next();
			datastore.delete(message);
			// TODO: bulk delete all messages instead of one by one
		}
		
		super.delete();
	}

	@Override
	public String getDescription() {
		return "The MessageAgent can send and receive messages from other " +
		 "MessageAgents. It has an persisted inbox and outbox for messages";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
