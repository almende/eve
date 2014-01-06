package com.almende.eve.agent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;

import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.log.EventLogger;
import com.almende.eve.config.Config;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.scheduler.SchedulerFactory;
import com.almende.eve.state.StateFactory;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;

/**
 * The AgentHost is a factory to instantiate and invoke Eve Agents within the
 * configured state. The AgentHost can invoke local as well as remote agents.
 * 
 * An AgentHost must be instantiated with a valid Eve configuration file.
 * This configuration is needed to load the configured agent classes and
 * instantiate a state for each agent.
 * 
 * Example usage: // generic constructor Config config = new Config("eve.yaml");
 * AgentHost factory = new AgentHost(config);
 * 
 * // construct in servlet InputStream is =
 * getServletContext().getResourceAsStream("/WEB-INF/eve.yaml"); Config config =
 * new Config(is); AgentHost factory = new AgentHost(config);
 * 
 * // create or get a shared instance of the AgentHost AgentHost factory =
 * AgentHost.createInstance(namespace, config); AgentHost factory =
 * AgentHost.getInstance(namespace);
 * 
 * // invoke a local agent by its id response = factory.invoke(agentId,
 * request);
 * 
 * // invoke a local or remote agent by its url response =
 * factory.send(senderId, receiverUrl, request);
 * 
 * // create a new agent Agent agent = factory.createAgent(agentType, agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * // instantiate an existing agent Agent agent = factory.getAgent(agentId);
 * String desc = agent.getDescription(); // use the agent agent.destroy(); //
 * neatly shutdown the agents state
 * 
 * @author jos
 */
public interface AgentHost {
	
	/**
	 * Instantiate the services from the given config.
	 * 
	 * @param config
	 */
	void loadConfig(Config config);
	
	/**
	 * Signal all agents about AgentHost event.
	 * 
	 * @param event
	 */
	void signalAgents(AgentSignal<?> event);
	
	/**
	 * Get an agent by its id. Returns null if the agent does not exist
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentId
	 * @return agent
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 */
	Agent getAgent(String agentId) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException;
	
	/**
	 * Create an agent proxy from an java interface
	 * 
	 * @param sender
	 *            Sender Agent, used to authentication purposes.
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	<T extends AgentInterface> T createAgentProxy(final AgentInterface sender,
			final URI receiverUrl, Class<T> agentInterface);
	
	/**
	 * Create an asynchronous agent proxy from an java interface, each call will
	 * return a future for handling the results.
	 * 
	 * @param senderId
	 *            Internal id of the sender agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests)
	 * @param receiverUrl
	 *            Url of the receiving agent
	 * @param agentInterface
	 *            A java Interface, extending AgentInterface
	 * @return
	 */
	<T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final AgentInterface sender, final URI receiverUrl,
			Class<T> agentInterface);
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentType
	 *            full class path
	 * @param agentId
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 */
	<T extends Agent> T createAgent(String agentType, String agentId)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, IOException;
	
	/**
	 * Create an agent.
	 * 
	 * Before deleting the agent, the method agent.destroy() must be executed to
	 * neatly shutdown the instantiated state.
	 * 
	 * @param agentType
	 * @param agentId
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 */
	<T extends Agent> T createAgent(Class<T> agentType, String agentId)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException;
	
	/**
	 * Create a new agent, using the base AspectAgent class. This agent has a
	 * namespace "sub", to which the given class's methods are added.
	 * 
	 * @param aspect
	 * @param agentId
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 */
	<T> AspectAgent<T> createAspectAgent(Class<? extends T> aspect,
			String agentId) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, IOException;
	
	/**
	 * Delete an agent
	 * 
	 * @param agentId
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	void deleteAgent(String agentId) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException;
	
	/**
	 * Test if an agent exists
	 * 
	 * @param agentId
	 * @return true if the agent exists
	 */
	boolean hasAgent(String agentId);
	
	/**
	 * Get the event logger. The event logger is used to temporary log triggered
	 * events, and display them on the agents web interface.
	 * 
	 * @return eventLogger
	 */
	EventLogger getEventLogger();
	
	/**
	 * Receive a message for an agent.
	 * 
	 * @param receiverUrl
	 * @param message
	 * @param senderUri
	 * @throws IOException 
	 */
	void receive(String receiverId, Object message, URI senderUri, String tag) throws IOException;
	
	/**
	 * Receive a message for an agent.
	 * 
	 * @param receiverUrl
	 * @param message
	 * @param senderUri
	 * @throws IOException 
	 */
	void receive(String receiverId, Object message, String senderUrl, String tag) throws IOException;
	
	/**
	 * Asynchronously send a message to an agent.
	 * 
	 * @param sender
	 *            Internal id of the sender agent. Not required for all
	 *            transport services (for example not for outgoing HTTP
	 *            requests)
	 * @param receiverUrl
	 * @param message
	 * @param sender
	 * @throws IOException
	 */
	
	void sendAsync(URI receiverUrl, Object message, AgentInterface sender,
			String tag) throws IOException;
	
	/**
	 * Get the agentId from given agentUrl. The url can be any protocol. If the
	 * url matches any of the registered transport services, an agentId is
	 * returned. This means that the url represents a local agent. It is
	 * possible that no agent with this id exists.
	 * 
	 * @param agentUrl
	 * @return agentId
	 */
	String getAgentId(String agentUrl);
	
	/**
	 * Determines best senderUrl for this agent, match receiverUrl transport
	 * method if possible. (fallback from HTTPS to HTTP included)
	 * 
	 * @param agentId
	 *            , receiverUrl
	 * @return URI SenderUrl
	 */
	URI getSenderUrl(String agentId, URI receiverUrl);
	
	/**
	 * Get the loaded config file
	 * 
	 * @return config A configuration file
	 */
	void setConfig(Config config);
	
	/**
	 * Get the loaded config file
	 * 
	 * @return config A configuration file
	 */
	Config getConfig();
	
	/**
	 * Utility method to keep reference of in-memory objects from the agent. The
	 * references are stored as WeakReferences, so the life-cycle of the
	 * referenced objects will not be influenced by storing them.
	 * 
	 * @param agentId
	 * @param key
	 * @param value
	 */
	<T> void putRef(String agentId, TypedKey<T> key, T value);
	
	/**
	 * Utility method to get back references to in-memory objects from the
	 * agent. The references are stored as WeakReferences, so the life-cycle of
	 * the referenced objects will not be influenced by storing them. However,
	 * the return value of this method is the hard-reference again.
	 * If the object has been garbage collected, this method will return null.
	 * 
	 * @param agentId
	 * @param key
	 * @return
	 */
	<T> T getRef(String agentId, TypedKey<T> key);
	
	/**
	 * Can transport services do a local invokation if the URL designates a
	 * local agent?
	 * 
	 * @return
	 */
	boolean isDoesShortcut();
	
	/**
	 * If true, transport services can do a local invokation if the URL
	 * designates a local agent.
	 * 
	 * @default true
	 * @return
	 */
	void setDoesShortcut(boolean doesShortcut);
	
	/**
	 * Load a state factory from config
	 * 
	 * @param config
	 */
	void setStateFactory(Config config);
	
	/**
	 * Create agents from a config (only when they do not yet exist). Agents
	 * will be read from the configuration path bootstrap.agents, which must
	 * contain a map where the keys are agentId's and the values are the agent
	 * types (full java class path).
	 * 
	 * @param config
	 */
	// TODO: private?
	void addAgents(Config config);
	
	/**
	 * Set a state factory. The state factory is used to get/create/delete an
	 * agents state.
	 * 
	 * @param stateFactory
	 */
	void setStateFactory(StateFactory stateFactory);
	
	/**
	 * Get the configured state factory.
	 * 
	 * @return stateFactory
	 */
	StateFactory getStateFactory();
	
	/**
	 * Load a scheduler factory from a config file
	 * 
	 * @param config
	 */
	// TODO: private?
	void setSchedulerFactory(Config config);
	
	/**
	 * Load transport services for incoming and outgoing messages from a config
	 * (for example http and xmpp services).
	 * 
	 * @param config
	 */
	// TODO: Private?
	void addTransportServices(Config config);
	
	/**
	 * Add a new transport service
	 * 
	 * @param transportService
	 */
	void addTransportService(TransportService transportService);
	
	/**
	 * Remove a registered a transport service
	 * 
	 * @param transportService
	 */
	void removeTransportService(TransportService transportService);
	
	/**
	 * Get all registered transport services
	 * 
	 * @return transportService
	 */
	List<TransportService> getTransportServices();
	
	/**
	 * Get all registered transport services which can handle given protocol
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return transportService
	 */
	List<TransportService> getTransportServices(String protocol);
	
	/**
	 * Get the first registered transport service which supports given protocol.
	 * Returns null when none of the registered transport services can handle
	 * the protocol.
	 * 
	 * @param protocol
	 *            A protocol, for example "http" or "xmpp"
	 * @return service
	 */
	TransportService getTransportService(String protocol);
	
	/**
	 * Set a scheduler factory. The scheduler factory is used to
	 * get/create/delete an agents scheduler.
	 * 
	 * @param schedulerFactory
	 */
	void setSchedulerFactory(SchedulerFactory schedulerFactory);
	
	/**
	 * create a scheduler for an agent
	 * 
	 * @param agentId
	 * @return scheduler
	 */
	Scheduler getScheduler(Agent agent);
	
	/**
	 * Get a callback storage service. This service keeps AsyncCallbacks in a
	 * global accessible in-memory store.
	 * 
	 * @param id
	 * @param clazz
	 * @return
	 */
	<T> CallbackInterface<T> getCallbackService(String id, Class<T> clazz);
	
}
